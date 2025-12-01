/* ==========================================================================
   PROJET BD VALAISON - SQUELETTE DES TRANSACTIONS & REQUÊTES CLÉS
   ========================================================================== */


/* ==========================================================================
   1. FONCTIONNALITÉ : PASSAGE DE COMMANDE (CLIENT)
   OBJECTIF : Enregistrer une volonté d'achat et figer le prix.
   ISOLATION : READ COMMITTED (Pour ne pas bloquer les lectures de catalogue).
   CONTRAINTE : Le panier ne doit pas être vide (Vérifié en amont)
   ========================================================================== */

-- 1. Début de Transaction
BEGIN;

    -- 2. Création de l'entête de commande
    -- Initialisation statut 'En préparation'. La date est celle du système (SYSDATE).
    INSERT INTO Commande (idClient, dateCommande, statut, modeRecuperation, modePaiement, montantTotal, fraisLivraison) 
    VALUES (:idClient, SYSDATE, 'En préparation', :modeLivraison, :modePaiement, :totalCalcule, :fraisLivraison);

    -- [JAVA] : Récupération de la clé générée (idCommande) pour l'étape suivante.

    -- 3. Insertion des lignes (Itération sur le panier)
    -- PRINCIPE : Snapshot du prix. On insère le prix calculé à l'instant T (:prixSnapshot)
    -- pour gérer le changement de prix après la commande .
    INSERT INTO LigneCommande (idCommande, idItem, quantite, prixUnitaireApplique) 
    VALUES (:idCmd, :idItem_1, :qte_1, :prixSnapshot_1);

    INSERT INTO LigneCommande (idCommande, idItem, quantite, prixUnitaireApplique) 
    VALUES (:idCmd, :idItem_2, :qte_2, :prixSnapshot_2);
    -- ... (Répété pour N articles)

    -- 4. Validation (Commit)
    -- Si une erreur technique survient ici, tout est annulé (ROLLBACK implicite).
COMMIT;

/* ==========================================================================
   2. FONCTIONNALITÉ : PRÉPARATION & SORTIE DE STOCK (STAFF)
   OBJECTIF : Valider  la commande et décrémenter le stock.
   ISOLATION : SERIALIZABLE (Simulé via verrous pessimistes FOR UPDATE).
   CONTRAINTE : Stock Disponible >= Quantité Demandée.
   ========================================================================== */

-- Début de la transaction

    -- 1. Verrouillage de la commande
    -- Empêche une annulation simultanée par le propriétaire ou un autre processus.
    SELECT statut FROM Commande WHERE idCommande = :idCmd FOR UPDATE;

    -- (Vérification Java : Si statut != 'En préparation' -> ROLLBACK)

    -- 2. Boucle sur les articles de la commande : Débit du stock
    
        -- CAS A : C'est un ARTICLE 
        -- 3a. Identification et Verrouillage des Lots
        -- On sélectionne les lots disponibles, triés par date de péremption.
        -- FOR UPDATE verrouille ces lignes pour empêcher la "survente" à un autre client.
        SELECT idLot, quantiteDisponible 
        FROM Lot 
        WHERE idArticle = :idArt 
          AND quantiteDisponible > 0 
        ORDER BY datePeremption ASC 
        FOR UPDATE;

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
   OBJECTIF : Annuler une commande 'Prête' et restituer le stock.
   CONTRAINTE : Ne peut pas annuler si 'En livraison' ou 'Livrée'.
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
   OBJECTIF : Sortir les produits périmés et solder les produits à date courte.
   ========================================================================== */

-- SCÉNARIO A : PRODUIT PÉRIMÉ (Date > Date Limite)
BEGIN;
    -- 1. Mise à zéro du stock physique (Ne peut plus être vendu)
    UPDATE Lot SET quantiteDisponible = 0 WHERE idLot = :idLot;
    
    -- 2. Enregistrement comptable de la perte (Traçabilité)
    INSERT INTO Perte (idLot, datePerte, naturePerte, quantitePerdue) 
    VALUES (:idLot, SYSDATE, 'Péremption DLC', :qteStockAvant);
COMMIT;

-- SCÉNARIO B : PRODUIT EN ALERTE (J-7)
BEGIN;
    -- Application d'une réduction automatique sur le lot spécifique
    UPDATE Lot 
    SET pourcentageReduction = :tauxReduction -- (ex: 30%)
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
