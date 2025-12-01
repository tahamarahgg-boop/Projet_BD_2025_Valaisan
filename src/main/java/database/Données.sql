/* ============================================================
   Fichier : data_final.sql
   Jeu de données complet pour démonstration
   ============================================================ */

-- 1. PRODUCTEURS (Données géographiques réelles pour test distance)
-- ID 1 : Grenoble (Local)
INSERT INTO Producteur (nom, adresse, longitude, latitude, mail, telephone) 
VALUES ('Ferme du Vercors', 'Villard-de-Lans', 5.5342, 45.0718, 'contact@vercors.fr', '0476000001');

-- ID 2 : Provence (Moyen)
INSERT INTO Producteur (nom, adresse, longitude, latitude, mail, telephone) 
VALUES ('Moulin de Provence', 'Gordes', 5.2000, 43.9116, 'huiles@provence.fr', '0490000002');

-- ID 3 : Bretagne (Loin)
INSERT INTO Producteur (nom, adresse, longitude, latitude, mail, telephone) 
VALUES ('Coopérative Breizh', 'Quimper', -4.1027, 47.9975, 'coop@bzh.fr', '0298000003');

-- ID 4 : Alsace (Loin)
INSERT INTO Producteur (nom, adresse, longitude, latitude, mail, telephone) 
VALUES ('Vignoble Alsacien', 'Colmar', 7.3585, 48.0794, 'vin@alsace.fr', '0389000004');

-- ID 5 : La Réunion (DOM-TOM - Test frais élevés)
INSERT INTO Producteur (nom, adresse, longitude, latitude, mail, telephone) 
VALUES ('Plantation Vanille', 'Saint-Denis', 55.4481, -20.8823, 'vanille@reunion.fr', '0262000005');


-- 2. ACTIVITÉS
INSERT INTO ActiviteProducteur (idProducteur, nomActivite) VALUES (1, 'Maraicher');
INSERT INTO ActiviteProducteur (idProducteur, nomActivite) VALUES (1, 'Eleveur');
INSERT INTO ActiviteProducteur (idProducteur, nomActivite) VALUES (2, 'Moulinier');
INSERT INTO ActiviteProducteur (idProducteur, nomActivite) VALUES (3, 'Céréalier');
INSERT INTO ActiviteProducteur (idProducteur, nomActivite) VALUES (4, 'Vigneron');
INSERT INTO ActiviteProducteur (idProducteur, nomActivite) VALUES (5, 'Agriculteur');


-- 3. SAISONS (Année 2025 complète)
INSERT INTO Saison (dateDebut, dateFin) VALUES (TO_DATE('01/01/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY'));
INSERT INTO Saison (dateDebut, dateFin) VALUES (TO_DATE('21/03/2025','DD/MM/YYYY'), TO_DATE('20/06/2025','DD/MM/YYYY')); -- Printemps
INSERT INTO Saison (dateDebut, dateFin) VALUES (TO_DATE('21/06/2025','DD/MM/YYYY'), TO_DATE('20/09/2025','DD/MM/YYYY')); -- Été
INSERT INTO Saison (dateDebut, dateFin) VALUES (TO_DATE('21/09/2025','DD/MM/YYYY'), TO_DATE('20/12/2025','DD/MM/YYYY')); -- Automne
INSERT INTO Saison (dateDebut, dateFin) VALUES (TO_DATE('21/12/2025','DD/MM/YYYY'), TO_DATE('20/03/2026','DD/MM/YYYY')); -- Hiver


-- 4. CLIENTS & ADRESSES (Pour tester les frais de port)
-- Client 1 : Local (Grenoble) -> Frais distance minimes
INSERT INTO Client (mail, nom, prenom, telephone) VALUES ('alice.grenoble@mail.com', 'Martin', 'Alice', '0601010101');
INSERT INTO Adresse (idClient, adressePostale, pays, latitude, longitude) 
VALUES (1, '10 Cours Berriat, 38000 Grenoble', 'France', 45.1885, 5.7245);

-- Client 2 : Paris (Distance moyenne)
INSERT INTO Client (mail, nom, prenom, telephone) VALUES ('bob.paris@mail.com', 'Dupont', 'Bob', '0602020202');
INSERT INTO Adresse (idClient, adressePostale, pays, latitude, longitude) 
VALUES (2, '5 Rue de Rivoli, 75001 Paris', 'France', 48.8566, 2.3522);

-- Client 3 : Martinique (DOM-TOM) -> Frais distance max
INSERT INTO Client (mail, nom, prenom, telephone) VALUES ('charlie.domtom@mail.com', 'Zola', 'Charlie', '0696000000');
INSERT INTO Adresse (idClient, adressePostale, pays, latitude, longitude) 
VALUES (3, 'Fort-de-France', 'Martinique', 14.6161, -61.0588);


-- 5. CONTENANTS & ITEMS (Gestion ID ITEM)
-- ID ITEM 1 : Bocal (Consigné)
INSERT INTO Contenant (typeContenant, capacite, unite, reutilisable, stock, prixVente) VALUES ('Bocal Verre', 1, 'L', 1, 50, 2.50);
INSERT INTO Item (typeItem, idContenant) VALUES ('CONTENANT', 1);

-- ID ITEM 2 : Sac Tissu
INSERT INTO Contenant (typeContenant, capacite, unite, reutilisable, stock, prixVente) VALUES ('Sac Coton Bio', 0, 'Kg', 1, 100, 1.50);
INSERT INTO Item (typeItem, idContenant) VALUES ('CONTENANT', 2);

-- ID ITEM 3 : Sachet Papier (Gratuit ou presque)
INSERT INTO Contenant (typeContenant, capacite, unite, reutilisable, stock, prixVente) VALUES ('Sachet Kraft', 500, 'g', 0, 500, 0.10);
INSERT INTO Item (typeItem, idContenant) VALUES ('CONTENANT', 3);


-- 6. PRODUITS (Le Catalogue Abstrait)
INSERT INTO Produit (idProducteur, nom, categorie, description) VALUES (1, 'Pommes Golden', 'Fruits', 'Pommes locales sucrées.'); -- ID 1
INSERT INTO Produit (idProducteur, nom, categorie, description) VALUES (1, 'Carottes Sables', 'Legumes', 'Carottes non lavées.'); -- ID 2
INSERT INTO Produit (idProducteur, nom, categorie, description) VALUES (2, 'Huile Olive AOP', 'Epicerie', 'Première pression à froid.'); -- ID 3
INSERT INTO Produit (idProducteur, nom, categorie, description) VALUES (3, 'Cidre Brut', 'Boissons', 'Artisanal breton.'); -- ID 4
INSERT INTO Produit (idProducteur, nom, categorie, description) VALUES (4, 'Riesling', 'Boissons', 'Vin blanc sec.'); -- ID 5
INSERT INTO Produit (idProducteur, nom, categorie, description) VALUES (5, 'Gousses Vanille', 'Epicerie', 'Vanille Bourbon de la Réunion.'); -- ID 6
INSERT INTO Produit (idProducteur, nom, categorie, description) VALUES (3, 'Farine Sarrasin', 'Epicerie', 'Pour galettes.'); -- ID 7


-- 7. ARTICLES & ITEMS (Ce qu'on achète)

-- Article 1 : Pommes Vrac (Produit 1) -> ITEM 4
INSERT INTO Article (idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (1, 'Vrac', NULL, 1.00, 2.50);
INSERT INTO Item (typeItem, idArticle) VALUES ('ARTICLE', 1);

-- Article 2 : Pommes Sachet 3kg (Produit 1) -> ITEM 5
INSERT INTO Article (idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (1, 'Sachet 3kg', 3.00, 2.50, 6.00);
INSERT INTO Item (typeItem, idArticle) VALUES ('ARTICLE', 2);

-- Article 3 : Carottes Vrac (Produit 2) -> ITEM 6
INSERT INTO Article (idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (2, 'Vrac', NULL, 0.80, 1.80);
INSERT INTO Item (typeItem, idArticle) VALUES ('ARTICLE', 3);

-- Article 4 : Huile Olive 75cl (Produit 3) -> ITEM 7
INSERT INTO Article (idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (3, 'Bouteille', 0.75, 8.00, 15.00);
INSERT INTO Item (typeItem, idArticle) VALUES ('ARTICLE', 4);

-- Article 5 : Cidre (Produit 4) -> ITEM 8
INSERT INTO Article (idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (4, 'Bouteille', 0.75, 2.50, 4.50);
INSERT INTO Item (typeItem, idArticle) VALUES ('ARTICLE', 5);

-- Article 6 : Vanille (Produit 6) - SUR COMMANDE -> ITEM 9
INSERT INTO Article (idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient, delaiDisponibilite) 
VALUES (6, 'Tube 3 gousses', 0.05, 12.00, 25.00, 15); -- 15 jours de délai
INSERT INTO Item (typeItem, idArticle) VALUES ('ARTICLE', 6);

-- Article 7 : Farine Sarrasin (Produit 7) -> ITEM 10
INSERT INTO Article (idProduit, modeConditionnement, poids, prixAchatProducteur, prixVenteClient) 
VALUES (7, 'Paquet 1kg', 1.00, 2.00, 3.80);
INSERT INTO Item (typeItem, idArticle) VALUES ('ARTICLE', 7);


-- 8. SAISONNALITÉ
-- Tout est de saison pour simplifier le test, sauf le Cidre (dispo toute l'année)
INSERT INTO est_de_saison (idProduit, dateDebut, dateFin) VALUES (1, TO_DATE('01/01/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY'));
INSERT INTO est_de_saison (idProduit, dateDebut, dateFin) VALUES (2, TO_DATE('01/01/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY'));
INSERT INTO est_de_saison (idProduit, dateDebut, dateFin) VALUES (3, TO_DATE('01/01/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY'));
INSERT INTO est_de_saison (idProduit, dateDebut, dateFin) VALUES (4, TO_DATE('01/01/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY'));
INSERT INTO est_de_saison (idProduit, dateDebut, dateFin) VALUES (5, TO_DATE('01/01/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY'));
INSERT INTO est_de_saison (idProduit, dateDebut, dateFin) VALUES (6, TO_DATE('01/01/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY'));
INSERT INTO est_de_saison (idProduit, dateDebut, dateFin) VALUES (7, TO_DATE('01/01/2025','DD/MM/YYYY'), TO_DATE('31/12/2025','DD/MM/YYYY'));


-- 9. LOTS (Stocks)

-- Pommes Vrac (Article 1 / Item 4)
-- Lot 1 : Normal
INSERT INTO Lot (idArticle, dateReception, quantiteInitiale, quantiteDisponible, datePeremption, typePeremption) 
VALUES (1, SYSDATE-5, 100, 80, SYSDATE+20, 'DLUO');
-- Lot 2 : Promo Date Courte (Périme dans 4 jours)
INSERT INTO Lot (idArticle, dateReception, quantiteInitiale, quantiteDisponible, datePeremption, typePeremption, pourcentageReduction) 
VALUES (1, SYSDATE-15, 50, 10, SYSDATE+4, 'DLC', 30); 

-- Pommes Sachet 3kg (Article 2 / Item 5)
INSERT INTO Lot (idArticle, dateReception, quantiteInitiale, quantiteDisponible, datePeremption, typePeremption) 
VALUES (2, SYSDATE-2, 20, 20, SYSDATE+15, 'DLUO');

-- Carottes (Article 3 / Item 6) - Bientôt périmé
INSERT INTO Lot (idArticle, dateReception, quantiteInitiale, quantiteDisponible, datePeremption, typePeremption) 
VALUES (3, SYSDATE-10, 50, 5, SYSDATE+2, 'DLC'); -- Test Alerte Péremption !

-- Huile Olive (Article 4 / Item 7) - Stock confortable
INSERT INTO Lot (idArticle, dateReception, quantiteInitiale, quantiteDisponible, datePeremption, typePeremption) 
VALUES (4, SYSDATE-60, 100, 95, SYSDATE+365, 'DLUO');

-- Cidre (Article 5 / Item 8) - Rupture de stock
INSERT INTO Lot (idArticle, dateReception, quantiteInitiale, quantiteDisponible, datePeremption, typePeremption) 
VALUES (5, SYSDATE-30, 24, 0, SYSDATE+180, 'DLUO');

-- Vanille (Article 6 / Item 9) : PAS DE LOT CAR SUR COMMANDE

-- Farine (Article 7 / Item 10)
INSERT INTO Lot (idArticle, dateReception, quantiteInitiale, quantiteDisponible, datePeremption, typePeremption) 
VALUES (7, SYSDATE-20, 50, 48, SYSDATE, 'DLUO');


COMMIT;
