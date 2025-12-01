/* ==========================================================================
   PROJET BD VALAISON - SQUELETTE DES TRANSACTIONS & REQUÊTES CLÉS
   ==========================================================================
   Ce fichier documente la logique transactionnelle implantée dans le code Java.
   Il montre l'usage des verrous (FOR UPDATE), l'atomicité (COMMIT/ROLLBACK)
   et les requêtes complexes.
   ========================================================================== */


/* ==========================================================================
   1. FONCTIONNALITÉ : PASSAGE DE COMMANDE (CLIENT)
   SOURCE : ServiceCommande.java
   PRINCIPE : Insertion atomique de l'entête et des lignes. Le prix est figé.
   ISOLATION : READ COMMITTED (Ne bloque pas les autres lecteurs)
   ========================================================================== */

-- Début de la transaction
-- (En Java : conn.setAutoCommit(false))

    -- 1. Création de l'entête de la commande
    -- Le statut initial est toujours 'En préparation'.
    INSERT INTO Commande (idClient, dateCommande, statut, modeRecuperation, modePaiement, montantTotal, fraisLivraison) 
    VALUES (:idClient, SYSDATE, 'En préparation', :modeLivraison, :modePaiement, :totalCalcule, :fraisLivraison);

    -- (Récupération de l'ID généré : idCmd)

    -- 2. Insertion des lignes (Boucle sur le panier)
    -- IMPORTANT : On insère le prix tel qu'il a été calculé par l'application à l'instant T.
    -- Cela protège la commande contre une hausse de prix future.
    INSERT INTO LigneCommande (idCommande, idItem, quantite, prixUnitaireApplique) 
    VALUES (:idCmd, :idItem1, :qte1, :prixSnapshot1);

    INSERT INTO LigneCommande (idCommande, idItem, quantite, prixUnitaireApplique) 
    VALUES (:idCmd, :idItem2, :qte2, :prixSnapshot2);

-- Validation : La commande existe
COMMIT;


/* ==========================================================================
   2. FONCTIONNALITÉ : PRÉPARATION & SORTIE DE STOCK (STAFF)
   SOURCE : CloturerCommande.java
   PRINCIPE : Transaction critique. Utilise le verrouillage pessimiste pour
              éviter les "Race Conditions" (deux staff prenant le même produit).
   GESTION STOCK : FIFO (Premier entré, premier sorti) pour les lots périssables.
   ========================================================================== */

-- Début de la transaction

    -- 1. Verrouillage de la commande
    -- Empêche une annulation simultanée par le propriétaire ou un autre processus.
    SELECT statut FROM Commande WHERE idCommande = :idCmd FOR UPDATE;

    -- (Vérification Java : Si statut != 'En préparation' -> ROLLBACK)

    -- 2. Boucle sur les articles de la commande : Débit du stock
    
        -- CAS A : C'est un ARTICLE (Gestion par Lot / FIFO)
        -- On cherche et VERROUILLE les lots disponibles, du plus vieux au plus récent (FIFO).
        SELECT idLot, quantiteDisponible 
        FROM Lot 
        WHERE idArticle = :idArt 
          AND quantiteDisponible > 0 
        ORDER BY datePeremption ASC 
        FOR UPDATE; -- <--- VERROU CRITIQUE

        -- Pour chaque lot trouvé, on prélève la quantité nécessaire
        UPDATE Lot 
        SET quantiteDisponible = quantiteDisponible - :qtePrelevee 
        WHERE idLot = :idLot;

        -- CAS B : C'est un CONTENANT (Gestion simple)
        SELECT stock FROM Contenant WHERE idContenant = :idCont FOR UPDATE;

        UPDATE Contenant 
        SET stock = stock - :qte 
        WHERE idContenant = :idCont;

    -- (Vérification Java : Si stock total insuffisant -> ROLLBACK GLOBAL)

    -- 3. Mise à jour du statut final
    -- Les produits sont physiquement sortis, la commande est prête.
    UPDATE Commande SET statut = 'Prête' WHERE idCommande = :idCmd;

-- Validation finale
COMMIT;


/* ==========================================================================
   3. FONCTIONNALITÉ : ANNULATION PAR LE PROPRIÉTAIRE (STAFF)
   SOURCE : CloturerCommande.java
   PRINCIPE : Gère le cas particulier où une commande déjà "Prête" est annulée.
              Le stock doit être recrédité (Inverse de la transaction 2).
   ========================================================================== */

-- Début de la transaction

    -- 1. Vérification et Verrouillage
    SELECT statut FROM Commande WHERE idCommande = :idCmd FOR UPDATE;

    -- Condition Logique (Java) : Autorisé si (statut='Prête' ET User='Staff')
    
    -- 2. Remise en stock (Si statut était 'Prête')
    -- Exemple simplifié pour les contenants
    UPDATE Contenant 
    SET stock = stock + :qteCommande 
    WHERE idContenant = :idCont;

    -- (Note : Pour les articles, la réintégration exacte dans les lots d'origine 
    -- nécessiterait une table d'historique des mouvements "StockMovement").

    -- 3. Passage au statut 'Annulée'
    UPDATE Commande SET statut = 'Annulée' WHERE idCommande = :idCmd;

-- Validation
COMMIT;


/* ==========================================================================
   4. FONCTIONNALITÉ : GESTION DES ALERTES PÉREMPTION (SYSTÈME)
   SOURCE : ServiceAlerte.java
   PRINCIPE : Maintenance de la base. Identification des lots périmés ou à risque.
   ========================================================================== */

-- A. Identification (Lecture)
SELECT idLot, idArticle, quantiteDisponible, 
       TRUNC(datePeremption - SYSDATE) AS nbJourRestant 
FROM Lot 
WHERE quantiteDisponible <> 0 
  AND datePeremption < SYSDATE + 7
ORDER BY datePeremption ASC;


-- B. Traitement : Produit PÉRIMÉ (Date dépassée)
-- Début Transaction
    -- 1. Le stock est mis à zéro (Perte sèche)
    UPDATE Lot SET quantiteDisponible = 0 WHERE idLot = :idLot;
    
    -- 2. Archivage dans la table Perte pour comptabilité
    INSERT INTO Perte (idLot, idContenant, datePerte, naturePerte, quantitePerdue) 
    VALUES (:idLot, NULL, SYSDATE, 'Péremption', :qtePerdue);
COMMIT;


-- C. Traitement : Produit EN ALERTE (J-7 à J-0)
-- Début Transaction
    -- Application d'une réduction pour écouler le stock rapidement
    UPDATE Lot 
    SET pourcentageReduction = :nouvelleReduction 
    WHERE idLot = :idLot;
COMMIT;


/* ==========================================================================
   5. FONCTIONNALITÉ : CONSULTATION CATALOGUE
   PRINCIPE : Vue agrégée pour le client. Assemble Item, Article, Produit et Stock.
   ========================================================================== */

SELECT 
    i.idItem, 
    i.typeItem, 
    p.nom AS nomProduit, 
    a.modeConditionnement, 
    a.prixVenteClient, 
    c.typeContenant, 
    c.capacite, 
    c.prixVente AS prixContenant, 
    -- Sous-requête pour calculer le stock total d'un article (somme des lots)
    (SELECT SUM(quantiteDisponible) FROM Lot WHERE idArticle = a.idArticle) as stockArticle, 
    c.stock as stockContenant 
FROM Item i 
LEFT JOIN Article a ON i.idArticle = a.idArticle 
LEFT JOIN Produit p ON a.idProduit = p.idProduit 
LEFT JOIN Contenant c ON i.idContenant = c.idContenant 
ORDER BY i.idItem;


/* ==========================================================================
   6. FONCTIONNALITÉ : DROIT À L'OUBLI (CLIENT)
   PRINCIPE : Anonymisation des données personnelles sans supprimer l'historique.
   CONTRAINTE : On ne peut pas anonymiser un client qui a une commande en cours.
   ========================================================================== */

-- Début Transaction
    -- 1. Vérification de sécurité (Pas de commande active)
    SELECT count(*) FROM Commande 
    WHERE idClient = :idClient 
      AND statut IN ('En préparation', 'En livraison');
    
    -- (Si count > 0, on ROLLBACK et on refuse la demande)

    -- 2. Anonymisation du Client
    -- L'email est construit avec l'ID pour garantir l'unicité (contrainte UNIQUE)
    UPDATE Client 
    SET nom = 'ANONYME',
        prenom = 'Client',
        mail = 'anon_' || :idClient || '@deleted.valaison.fr',
        telephone = '0000000000'
    WHERE idClient = :idClient;

    -- 3. Suppression des données géographiques (Adresse)
    UPDATE Adresse
    SET adressePostale = 'Adresse supprimée',
        latitude = 0,
        longitude = 0
    WHERE idClient = :idClient;

-- Validation
COMMIT;