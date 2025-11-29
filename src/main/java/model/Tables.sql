/* ============================================================
   Fichier : tables.sql
   Projet BD 2025-2026 - Compatibilité : Oracle Database
   ============================================================ */

-- Suppression propre des tables (ordre inverse des dépendances)
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE LigneCommande CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE Commande CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE Perte CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE Lot CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE Contenant CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE est_disponible CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE est_de_saison CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE est_de_chara CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE Article CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE Produit CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE Producteur CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE Client CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE Adresse CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE Saison CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE Caracteristique CASCADE CONSTRAINTS';
   EXECUTE IMMEDIATE 'DROP TABLE Disponibilite CASCADE CONSTRAINTS';
EXCEPTION
   WHEN OTHERS THEN NULL; -- Ignore les erreurs si les tables n'existent pas
END;
/

/* ============================================================
   CREATION DES TABLES
   ============================================================ */

CREATE TABLE Producteur (
    idProducteur INTEGER NOT NULL,
    nom VARCHAR2(100),
    adresse VARCHAR2(200),
    longitude NUMBER,
    latitude NUMBER,
    typeActivite VARCHAR2(100), -- Ajouté selon sujet [cite: 15]
    CONSTRAINT Producteur_PK PRIMARY KEY (idProducteur)
);

CREATE TABLE Produit (
    idProduit INTEGER NOT NULL,
    idProducteur INTEGER NOT NULL,
    nom VARCHAR2(100),
    categorie VARCHAR2(50),
    description VARCHAR2(500),
    CONSTRAINT Produit_PK PRIMARY KEY (idProduit)
);

CREATE TABLE Article (
    idArticle INTEGER NOT NULL,
    idProduit INTEGER NOT NULL,
    modeConditionnement VARCHAR2(50),
    poids NUMBER, -- Poids du sachet si pré-conditionné, sinon NULL
    prixAchatProducteur NUMBER(10,2), -- Prix d'achat 
    prixVenteClient NUMBER(10,2),     -- Prix de vente 
    CONSTRAINT Article_PK PRIMARY KEY (idArticle)
);
CREATE TABLE Produit_conditionné(
    idArticle INTEGER UNIQUE
    modeconditionnement VARCHAR2(50) UNIQUE
    poids NUMBER,
    date_Peremption DATE UNIQUE,
    idProduit INTEGER UNIQUE,
    TypedatePeremption VARCHAR2(50),
    CONSTRAINT Produit_conditionné PRIMARY KEY (idArticle, modeConditionnement, poids, date_Peremption, idProduit)

);

CREATE TABLE Contenant (
    idContenant INTEGER NOT NULL,
    type VARCHAR2(50),
    capacite NUMBER,
    reutilisable NUMBER(1) CHECK (reutilisable IN (0,1)), -- Booléen simulé
    stock INTEGER, -- Gestion du stock des contenants
    prixVente NUMBER(10,2),
    CONSTRAINT Contenant_PK PRIMARY KEY (idContenant)
);

CREATE TABLE Lot (
    idArticle INTEGER NOT NULL,
    dateReception DATE DEFAULT SYSDATE NOT NULL,
    quantiteDisponible NUMBER(10,2) NOT NULL,
    datePeremption DATE,
    typePeremption VARCHAR2(4),

    CONSTRAINT Lot_PK PRIMARY KEY (idLot),
    CONSTRAINT Lot_Unique_Livraison UNIQUE (idArticle, dateReception),
    CONSTRAINT Chk_Type_Peremption CHECK (typePeremption IN ('DLC', 'DLUO'))
);

CREATE TABLE Perte (
    idPerte INTEGER NOT NULL, -- ajout idPerte car datePerte n'est pas clé primaire
    idLot INTEGER NOT NULL,
    datePerte DATE DEFAULT SYSDATE,
    naturePerte VARCHAR2(100),
    quantitePerdue NUMBER(10,2),
    CONSTRAINT Perte_PK PRIMARY KEY (idPerte)
);

CREATE TABLE Client (
    idClient INTEGER NOT NULL,
    mail VARCHAR2(100) NOT NULL UNIQUE,
    nom VARCHAR2(100),
    prenom VARCHAR2(100),
    telephone VARCHAR2(20),
    CONSTRAINT Client_PK PRIMARY KEY (idClient)
);

CREATE TABLE Adresse (
    idAdresse INTEGER NOT NULL,
    idClient INTEGER NOT NULL,
    adressePostale VARCHAR2(200),
    pays VARCHAR2(50),
    CONSTRAINT Adresse_PK PRIMARY KEY (idAdresse)
);

CREATE TABLE Commande (
    idCommande INTEGER NOT NULL,
    idClient INTEGER NOT NULL,
    idAdresseLivraison INTEGER, -- Null si retrait boutique
    dateCommande DATE DEFAULT SYSDATE,
    statut VARCHAR2(50), -- En préparation, Prête, etc. [cite: 54]
    modePaiement VARCHAR2(50),
    modeRecuperation VARCHAR2(50), -- Livraison ou Boutique
    fraisLivraison NUMBER(10,2),
    montantTotal NUMBER(10,2),
    CONSTRAINT Commande_PK PRIMARY KEY (idCommande)
);

CREATE TABLE LigneCommande (
    idLigne INTEGER NOT NULL,
    idCommande INTEGER NOT NULL,
    idArticle INTEGER NOT NULL,
    quantite NUMBER(10,2), -- Kg ou Unités [cite: 62]
    prixUnitaireApplique NUMBER(10,2), -- Prix au moment de la commande [cite: 63]
    CONSTRAINT LigneCommande_PK PRIMARY KEY (idLigne)
);

CREATE TABLE Saison (
    dateDebut DATE NOT NULL,
    dateFin DATE NOT NULL,
    CONSTRAINT Saison_PK PRIMARY KEY (dateDebut, dateFin)
);

CREATE TABLE est_de_saison (
    idProduit INTEGER NOT NULL,
    dateDebut DATE NOT NULL,
    dateFin DATE NOT NULL,
    CONSTRAINT est_de_saison_PK PRIMARY KEY (idProduit, dateDebut, dateFin)
);

/* ============================================================
   AJOUT DES CLES ETRANGERES (FOREIGN KEYS)
   ============================================================ */

-- Liens Produits / Producteurs
ALTER TABLE Produit ADD CONSTRAINT Produit_Producteur_FK FOREIGN KEY (idProducteur) REFERENCES Producteur(idProducteur);
ALTER TABLE Article ADD CONSTRAINT Article_Produit_FK FOREIGN KEY (idProduit) REFERENCES Produit(idProduit);

-- Liens Stocks / Lots / Pertes
ALTER TABLE Lot ADD CONSTRAINT Lot_Article_FK FOREIGN KEY (idArticle) REFERENCES Article(idArticle);
ALTER TABLE Perte ADD CONSTRAINT Perte_Lot_FK FOREIGN KEY (idLot) REFERENCES Lot(idLot);

-- Liens Clients / Commandes
ALTER TABLE Adresse ADD CONSTRAINT Adresse_Client_FK FOREIGN KEY (idClient) REFERENCES Client(idClient);
ALTER TABLE Commande ADD CONSTRAINT Commande_Client_FK FOREIGN KEY (idClient) REFERENCES Client(idClient);
ALTER TABLE Commande ADD CONSTRAINT Commande_Adresse_FK FOREIGN KEY (idAdresseLivraison) REFERENCES Adresse(idAdresse);

-- Liens Lignes de Commande
ALTER TABLE LigneCommande ADD CONSTRAINT Ligne_Commande_FK FOREIGN KEY (idCommande) REFERENCES Commande(idCommande);
ALTER TABLE LigneCommande ADD CONSTRAINT Ligne_Article_FK FOREIGN KEY (idArticle) REFERENCES Article(idArticle);

-- Liens Saisonnalité
ALTER TABLE est_de_saison ADD CONSTRAINT Saison_Produit_FK FOREIGN KEY (idProduit) REFERENCES Produit(idProduit);
ALTER TABLE est_de_saison ADD CONSTRAINT Saison_Dates_FK FOREIGN KEY (dateDebut, dateFin) REFERENCES Saison(dateDebut, dateFin);
--Liens Produit_Conditionné
ALTER TABLE Produit_conditionné ADD CONSTRAINT Produit_conditionné_Produit_FK FOREIGN KEY (idProduit) REFERENCES Produit(idProduit);
ALTER TABLE est_de_saison ADD CONSTRAINT Produit_conditionné_Article_FK FOREIGN KEY (idArticle) REFERENCES Produit(idArticle);