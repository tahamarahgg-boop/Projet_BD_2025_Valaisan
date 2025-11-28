/* ============================================================
   Fichier : donnees_test.sql
   Description : Jeu de données pour tester le Projet BD 2025
   Compatibilité : Oracle
   ============================================================ */

-- 1. PRODUCTEURS [cite: 15]
-- On crée deux producteurs avec des profils différents
INSERT INTO Producteur (idProducteur, nom, adresse, longitude, latitude, typeActivite) 
VALUES (1, 'Ferme des Alpes', '12 Route du Vert, 38000 Grenoble', 5.72, 45.18, 'Agriculteur');

INSERT INTO Producteur (idProducteur, nom, adresse, longitude, latitude, typeActivite) 
VALUES (2, 'BioVrac 38', '45 Avenue de la Gare, 38500 Voiron', 5.60, 45.36, 'Grossiste');

-- 2. PRODUITS (Les définitions génériques) [cite: 17]
-- Produit 1 : Pommes (Saisonnier)
INSERT INTO Produit (idProduit, idProducteur, nom, categorie, description) 
VALUES (10, 1, 'Pommes Golden', 'Fruits', 'Pommes sucrées et croquantes locales.');

-- Produit 2 : Lentilles (Disponible en Vrac et Sachet) [cite: 28]
INSERT INTO Produit (idProduit, idProducteur, nom, categorie, description) 
VALUES (20, 2, 'Lentilles Vertes', 'Légumineuses', 'Lentilles riches en fer.');

-- Produit 3 : Jus de Pomme (Produit transformé)
INSERT INTO Produit (idProduit, idProducteur, nom, categorie, description) 
VALUES (30, 1, 'Jus de Pomme Artisanal', 'Boisson', 'Pur jus sans conservateurs.');

-- 3. SAISONNALITÉ [cite: 20]
-- Les pommes sont de saison de Septembre à Décembre
INSERT INTO Saison (dateDebut, dateFin) VALUES (TO_DATE('01/09/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY'));
INSERT INTO est_de_saison (idProduit, dateDebut, dateFin) VALUES (10, TO_DATE('01/09/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY'));

-- 4. ARTICLES (Ce qu'on vend réellement) [cite: 24, 25, 27]
-- Article 100 : Pommes en Vrac
INSERT INTO Article (idArticle, idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (100, 10, 'Vrac', NULL, 1.50, 3.00); 

-- Article 200 : Lentilles en Vrac
INSERT INTO Article (idArticle, idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (200, 20, 'Vrac', NULL, 2.00, 5.00);

-- Article 201 : Lentilles en Sachet de 500g (Pré-conditionné)
INSERT INTO Article (idArticle, idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (201, 20, 'Sachet', 0.5, 1.80, 3.50);

-- Article 300 : Bouteille de Jus
INSERT INTO Article (idArticle, idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (300, 30, 'Bouteille', 1.0, 2.50, 4.50);

-- 5. LOTS (Stocks) [cite: 36, 41]
-- Note : On utilise SYSDATE pour que les tests marchent quelle que soit la date d'exécution.

-- Lot 1 : Pommes reçues il y a 2 jours, périment dans 20 jours (OK)
INSERT INTO Lot (idLot, idArticle, dateReception, quantiteDisponible, datePeremption, typePeremption) 
VALUES (1, 100, SYSDATE - 2, 50.0, SYSDATE + 20, 'DLC');

-- Lot 2 : Lentilles Vrac (DLUO lointaine)
INSERT INTO Lot (idLot, idArticle, dateReception, quantiteDisponible, datePeremption, typePeremption) 
VALUES (2, 200, SYSDATE - 10, 100.0, SYSDATE + 365, 'DLUO');

-- Lot 3 : Un lot de sachets qui va périmer BIENTÔT (Pour tester l'alerte )
-- Périme dans 4 jours (< 7 jours)
INSERT INTO Lot (idLot, idArticle, dateReception, quantiteDisponible, datePeremption, typePeremption) 
VALUES (3, 201, SYSDATE - 30, 10, SYSDATE + 4, 'DLUO');

-- 6. CLIENTS & ADRESSES [cite: 47, 48]
INSERT INTO Client (idClient, mail, nom, prenom, telephone) 
VALUES (1, 'jean.dupont@email.com', 'Dupont', 'Jean', '0601020304');

INSERT INTO Adresse (idAdresse, idClient, adressePostale, pays) 
VALUES (1, 1, '10 Rue des Lilas, 38000 Grenoble', 'France');

INSERT INTO Client (idClient, mail, nom, prenom, telephone) 
VALUES (2, 'marie.curie@science.fr', 'Curie', 'Marie', '0699887766');

INSERT INTO Adresse (idAdresse, idClient, adressePostale, pays) 
VALUES (2, 2, '25 Avenue Jean Jaures, 75000 Paris', 'France');

-- 7. COMMANDES (Historique) [cite: 54, 55]
-- Commande 1 : Terminée (Jean Dupont)
INSERT INTO Commande (idCommande, idClient, idAdresseLivraison, dateCommande, statut, modePaiement, modeRecuperation, fraisLivraison, montantTotal) 
VALUES (1001, 1, 1, SYSDATE - 5, 'Livrée', 'Carte', 'Livraison', 5.00, 35.00);

-- Lignes de la commande 1 [cite: 61]
INSERT INTO LigneCommande (idLigne, idCommande, idArticle, quantite, prixUnitaireApplique) 
VALUES (1, 1001, 201, 2, 3.50); -- 2 Sachets de lentilles

-- Commande 2 : En cours de préparation (Marie Curie)
INSERT INTO Commande (idCommande, idClient, idAdresseLivraison, dateCommande, statut, modePaiement, modeRecuperation, fraisLivraison, montantTotal) 
VALUES (1002, 2, 2, SYSDATE, 'En préparation', 'Carte', 'Livraison', 8.00, 4.50);

INSERT INTO LigneCommande (idLigne, idCommande, idArticle, quantite, prixUnitaireApplique) 
VALUES (2, 1002, 300, 1, 4.50); -- 1 Jus de pomme

-- 8. PERTES (Pour tester la comptabilité) [cite: 45]
-- On a cassé 1 sachet de lentilles du lot 3 hier
INSERT INTO Perte (idPerte, idLot, datePerte, naturePerte, quantitePerdue) 
VALUES (1, 3, SYSDATE - 1, 'Casse', 1);

COMMIT;