/* ============================================================
   SCRIPT DE PEUPLEMENT COMPLET - PROJET EPICERIE
   Version compatible avec la table ITEM et ACTIVITES
   ============================================================ */

-- 1. PRODUCTEURS
INSERT INTO Producteur (nom, adresse, longitude, latitude, mail, telephone) VALUES 
('Ferme des Alpes', '120 Chemin du Vercors, 38250 Villard-de-Lans', 5.5342, 45.0718, 'contact@fermealpes.fr', '0476000001');
INSERT INTO Producteur (nom, adresse, longitude, latitude, mail, telephone) VALUES 
('Laiterie du Mont', 'Route des Cimes, 74000 Annecy', 6.1292, 45.8992, 'info@laiterie.fr', '0450000002');
INSERT INTO Producteur (nom, adresse, longitude, latitude, mail, telephone) VALUES 
('Moulins Bio', 'Zone Artisanale, 26000 Valence', 4.8917, 44.9333, 'bio@moulins.fr', '0475000003');
INSERT INTO Producteur (nom, adresse, longitude, latitude, mail, telephone) VALUES 
('Import Exotique', 'Port de Marseille', 5.3698, 43.2965, 'contact@import.fr', '0491000004');

-- 2. ACTIVITÉS DES PRODUCTEURS
INSERT INTO ActiviteProducteur (idProducteur, nomActivite) VALUES (1, 'Maraicher');
INSERT INTO ActiviteProducteur (idProducteur, nomActivite) VALUES (1, 'Eleveur'); -- Double activité
INSERT INTO ActiviteProducteur (idProducteur, nomActivite) VALUES (2, 'Fromager');
INSERT INTO ActiviteProducteur (idProducteur, nomActivite) VALUES (3, 'Céréalier');
INSERT INTO ActiviteProducteur (idProducteur, nomActivite) VALUES (4, 'Importateur');

-- 3. SAISONS (Année 2025-2026)
INSERT INTO Saison (dateDebut, dateFin) VALUES (TO_DATE('01/01/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY')); -- Année
INSERT INTO Saison (dateDebut, dateFin) VALUES (TO_DATE('21/03/2025','DD/MM/YYYY'), TO_DATE('20/06/2025','DD/MM/YYYY')); -- Printemps
INSERT INTO Saison (dateDebut, dateFin) VALUES (TO_DATE('21/06/2025','DD/MM/YYYY'), TO_DATE('20/09/2025','DD/MM/YYYY')); -- Été
INSERT INTO Saison (dateDebut, dateFin) VALUES (TO_DATE('21/09/2025','DD/MM/YYYY'), TO_DATE('20/12/2025','DD/MM/YYYY')); -- Automne
INSERT INTO Saison (dateDebut, dateFin) VALUES (TO_DATE('21/12/2025','DD/MM/YYYY'), TO_DATE('20/03/2026','DD/MM/YYYY')); -- Hiver

-- 4. CLIENTS & ADRESSES
-- Client 1 : Local
INSERT INTO Client (mail, nom, prenom, telephone) VALUES ('jean.dupont@mail.com', 'Dupont', 'Jean', '0601010101');
INSERT INTO Adresse (idClient, adressePostale, pays, latitude, longitude) VALUES (1, '10 Cours Berriat, 38000 Grenoble', 'France', 45.1885, 5.7245);

-- Client 2 : Parisien
INSERT INTO Client (mail, nom, prenom, telephone) VALUES ('marie.curie@science.fr', 'Curie', 'Marie', '0602020202');
INSERT INTO Adresse (idClient, adressePostale, pays, latitude, longitude) VALUES (2, '5 Rue de Rivoli, 75001 Paris', 'France', 48.8566, 2.3522);

-- 5. CONTENANTS & ITEMS
-- Bocal (Contenant 1 -> Item 1)
INSERT INTO Contenant (typeContenant, capacite, reutilisable, stock, prixVente) VALUES ('Bocal Verre', 1.0, 1, 50, 2.50);
INSERT INTO Item (typeItem, idContenant) VALUES ('CONTENANT', 1);

-- Sac Tissu (Contenant 2 -> Item 2)
INSERT INTO Contenant (typeContenant, capacite, reutilisable, stock, prixVente) VALUES ('Sac Coton Bio', 0.0, 1, 100, 1.50);
INSERT INTO Item (typeItem, idContenant) VALUES ('CONTENANT', 2);

-- Sachet Papier (Contenant 3 -> Item 3)
INSERT INTO Contenant (typeContenant, capacite, reutilisable, stock, prixVente) VALUES ('Sachet Kraft', 0.0, 0, 500, 0.10);
INSERT INTO Item (typeItem, idContenant) VALUES ('CONTENANT', 3);


-- 6. PRODUITS (Abstraits)
INSERT INTO Produit (idProducteur, nom, categorie, description) VALUES (1, 'Pommes Golden', 'Fruits', 'Pommes locales sucrées.');
INSERT INTO Produit (idProducteur, nom, categorie, description) VALUES (2, 'Reblochon', 'Produits Laitiers', 'AOP au lait cru.');
INSERT INTO Produit (idProducteur, nom, categorie, description) VALUES (3, 'Lentilles Vertes', 'Légumineuses', 'Riches en fer.');
INSERT INTO Produit (idProducteur, nom, categorie, description) VALUES (4, 'Café Grains', 'Boissons', 'Arabica torréfié.');

-- 7. ARTICLES (Concrets) & ITEMS

-- Article 1 : Pommes en Vrac (Produit 1) -> Item 4
INSERT INTO Article (idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (1, 'Vrac', NULL, 1.00, 2.50);
INSERT INTO Item (typeItem, idArticle) VALUES ('ARTICLE', 1);

-- Article 2 : Reblochon Unité (Produit 2) -> Item 5
INSERT INTO Article (idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (2, 'Unité', 0.450, 3.50, 6.00);
INSERT INTO Item (typeItem, idArticle) VALUES ('ARTICLE', 2);

-- Article 3 : Lentilles Vrac (Produit 3) -> Item 6
INSERT INTO Article (idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (3, 'Vrac', NULL, 1.50, 3.00);
INSERT INTO Item (typeItem, idArticle) VALUES ('ARTICLE', 3);

-- Article 4 : Lentilles Sachet 500g (Produit 3) -> Item 7
INSERT INTO Article (idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (3, 'Sachet 500g', 0.500, 2.00, 4.50);
INSERT INTO Item (typeItem, idArticle) VALUES ('ARTICLE', 4);

-- Article 5 : Café (Sur commande) (Produit 4) -> Item 8
INSERT INTO Article (idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient, delaiDisponibilite) 
VALUES (4, 'Paquet 1kg', 1.000, 10.00, 18.00, 7);
INSERT INTO Item (typeItem, idArticle) VALUES ('ARTICLE', 5);

-- 8. SAISONNALITÉ
INSERT INTO est_de_saison (idProduit, dateDebut, dateFin) VALUES (1, TO_DATE('21/09/2025','DD/MM/YYYY'), TO_DATE('20/12/2025','DD/MM/YYYY'));
INSERT INTO est_de_saison (idProduit, dateDebut, dateFin) VALUES (1, TO_DATE('21/12/2025','DD/MM/YYYY'), TO_DATE('20/03/2026','DD/MM/YYYY'));
INSERT INTO est_de_saison (idProduit, dateDebut, dateFin) VALUES (2, TO_DATE('01/01/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY'));
INSERT INTO est_de_saison (idProduit, dateDebut, dateFin) VALUES (3, TO_DATE('01/01/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY'));
INSERT INTO est_de_saison (idProduit, dateDebut, dateFin) VALUES (4, TO_DATE('01/01/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY'));

-- 9. LOTS (Stocks)
-- Pommes Vrac (Article 1)
INSERT INTO Lot (idArticle, dateReception, quantiteInitiale, quantiteDisponible, datePeremption, typePeremption, prixLot) 
VALUES (1, SYSDATE-2, 100, 80, SYSDATE+10, 'DLUO', 2.50);

INSERT INTO Lot (idArticle, dateReception, quantiteInitiale, quantiteDisponible, datePeremption, typePeremption, prixLot, pourcentageReduction) 
VALUES (1, SYSDATE-10, 50, 20, SYSDATE+2, 'DLUO', 2.50, 30); -- Lot en promo

-- Reblochon (Article 2)
INSERT INTO Lot (idArticle, dateReception, quantiteInitiale, quantiteDisponible, datePeremption, typePeremption, prixLot) 
VALUES (2, SYSDATE-5, 30, 30, SYSDATE+20, 'DLC', 6.00);

-- Lentilles Vrac (Article 3)
INSERT INTO Lot (idArticle, dateReception, quantiteInitiale, quantiteDisponible, datePeremption, typePeremption, prixLot) 
VALUES (3, SYSDATE-30, 200, 150, SYSDATE+365, 'DLUO', 3.00);

-- Lentilles Sachet (Article 4) : Rupture de stock
INSERT INTO Lot (idArticle, dateReception, quantiteInitiale, quantiteDisponible, datePeremption, typePeremption, prixLot) 
VALUES (4, SYSDATE-60, 20, 0, SYSDATE+100, 'DLUO', 4.50);

-- 10. COMMANDES & LIGNES (Avec ID ITEM)
-- Commande 1 : Jean (Livrée)
INSERT INTO Commande (idClient, idAdresseLivraison, dateCommande, statut, modePaiement, modeRecuperation, fraisLivraison, montantTotal)
VALUES (1, 1, SYSDATE-5, 'Livrée', 'Carte', 'Livraison', 5.00, 15.00);

-- Lignes de la commande 1
-- Achat de Pommes (Article 1 -> Item 4)
INSERT INTO LigneCommande (idCommande, idItem, quantite, prixUnitaireApplique) VALUES (1, 4, 2.0, 2.50); 
-- Achat d'un Bocal (Contenant 1 -> Item 1)
INSERT INTO LigneCommande (idCommande, idItem, quantite, prixUnitaireApplique) VALUES (1, 1, 1.0, 2.50);


-- Commande 2 : Marie (En préparation)
INSERT INTO Commande (idClient, idAdresseLivraison, dateCommande, statut, modePaiement, modeRecuperation, fraisLivraison, montantTotal)
VALUES (2, 2, SYSDATE, 'En préparation', 'Carte', 'Livraison', 8.00, 12.00);

-- Lignes de la commande 2
-- Achat de Reblochon (Article 2 -> Item 5)
INSERT INTO LigneCommande (idCommande, idItem, quantite, prixUnitaireApplique) VALUES (2, 5, 2.0, 6.00);


-- 11. PERTES
INSERT INTO Perte (idContenant, naturePerte, quantitePerdue) VALUES (1, 'Casse Rayon', 1);
INSERT INTO Perte (idLot, naturePerte, quantitePerdue) VALUES (1, 'Pourri', 5.0);

COMMIT;