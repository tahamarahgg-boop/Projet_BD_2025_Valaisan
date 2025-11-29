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
   EXECUTE IMMEDIATE 'DROP TABLE est_de_saison CASCADE CONSTRAINTS';
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
    idProducteur INTEGER GENERATED ALWAYS AS IDENTITY,
    nom VARCHAR2(100),
    adresse VARCHAR2(200),
    longitude NUMBER,
    latitude NUMBER,
    typeActivite VARCHAR2(100),
    mail VARCHAR2(200),
    telephone VARCHAR2(20),
    CONSTRAINT Producteur_PK PRIMARY KEY (idProducteur)
);

CREATE TABLE Produit (
    idProduit INTEGER GENERATED ALWAYS AS IDENTITY,
    idProducteur INTEGER NOT NULL,
    nom VARCHAR2(100) NOT NULL,
    categorie VARCHAR2(50),
    description VARCHAR2(500),
    CONSTRAINT Produit_PK PRIMARY KEY (idProduit)
);
CREATE TABLE Contenant (
    idContenant INTEGER GENERATED ALWAYS AS IDENTITY,
    typeContenant VARCHAR2(50) NOT NULL,
    capacite NUMBER(10,3),--litres ou grammes selon le type
    reutilisable NUMBER(1) CHECK (reutilisable IN (0,1)), -- Booléen simulé
    stock INTEGER, -- Gestion du stock des contenants
    prixVente NUMBER(10,2),
    CONSTRAINT Contenant_PK PRIMARY KEY (idContenant)
);


CREATE TABLE Article (
    idArticle INTEGER GENERATED ALWAYS AS IDENTITY,
    idProduit INTEGER NOT NULL,
    modeConditionnement VARCHAR2(50), -- certains articles n'ont pas de conditionnement (produits en vrac)
    poids NUMBER,                     -- Poids du sachet si pré-conditionné, sinon NULL
    prixAchatProducteur NUMBER(10,2), -- Prix d'achat 
    prixVenteClient NUMBER(10,2),  -- Prix de vente 
    delaiDisponibilite NUMBER(5) DEFAULT NULL  ,--pour les articles sur-commande qui ont un delai de disponibilite 
    CONSTRAINT Article_PK PRIMARY KEY (idArticle)
);

CREATE TABLE Item (
    idItem INTEGER GENERATED ALWAYS AS IDENTITY,
    typeItem VARCHAR2(20) NOT NULL, -- 'ARTICLE' ou 'CONTENANT'
    idArticle INTEGER,               -- NULL si typeItem = 'CONTENANT'
    idContenant INTEGER,             -- NULL si typeItem = 'ARTICLE'
    prixVente NUMBER(10,2),
    CONSTRAINT Item_PK PRIMARY KEY (idItem)
);

CREATE TABLE Lot (
    idLot INTEGER GENERATED ALWAYS AS IDENTITY,
    idArticle INTEGER NOT NULL,
    dateReception DATE DEFAULT SYSDATE NOT NULL,
    quantiteInitiale NUMBER(10,2) NOT NULL CHECK (quantiteInitiale >= 0),
    quantiteDisponible NUMBER(10,2) NOT NULL CHECK (quantiteDisponible >= 0),
    datePeremption DATE,
    typePeremption VARCHAR2(4),
    prixLot NUMBER(10,2),               -- prix spécifique au lot
    pourcentageReduction NUMBER(5,2) DEFAULT 0,  -- % de réduction automatique
    CONSTRAINT Lot_PK PRIMARY KEY (idLot),
    CONSTRAINT Lot_Unique_Livraison UNIQUE (idArticle, dateReception),
    CONSTRAINT Chk_Type_Peremption CHECK (typePeremption IN ('DLC', 'DLUO'))
);

CREATE TABLE Perte (
    idPerte INTEGER GENERATED ALWAYS AS IDENTITY, -- ajout idPerte car datePerte n'est pas clé primaire
    idLot INTEGER NOT NULL,
    datePerte DATE DEFAULT SYSDATE,
    naturePerte VARCHAR2(100),
    quantitePerdue NUMBER(10,2) CHECK (quantitePerdue >= 0),
    CONSTRAINT Perte_PK PRIMARY KEY (idPerte)
);

CREATE TABLE Client (
    idClient INTEGER GENERATED ALWAYS AS IDENTITY,
    mail VARCHAR2(100) NOT NULL UNIQUE,
    nom VARCHAR2(100),
    prenom VARCHAR2(100),
    telephone VARCHAR2(20),
    CONSTRAINT Client_PK PRIMARY KEY (idClient)
);

CREATE TABLE Adresse (
    idAdresse INTEGER GENERATED ALWAYS AS IDENTITY,
    idClient INTEGER NOT NULL,
    adressePostale VARCHAR2(200),
    pays VARCHAR2(50),
    CONSTRAINT Adresse_PK PRIMARY KEY (idAdresse)
);

CREATE TABLE Commande (
    idCommande INTEGER GENERATED ALWAYS AS IDENTITY,
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
    idLigne INTEGER GENERATED ALWAYS AS IDENTITY,
    idCommande INTEGER NOT NULL,
    idItem INTEGER NOT NULL,
    quantite NUMBER(10,2) CHECK (quantite >= 0), -- Kg ou Unités [cite: 62]
    prixUnitaireApplique NUMBER(10,2), -- Prix au moment de la commande [cite: 63]
    CONSTRAINT LigneCommande_PK PRIMARY KEY (idLigne),
    CONSTRAINT UQ_LigneCommande_CommandeItem UNIQUE (idCommande, idItem) --une commande ne peut avoir qu'une seule ligne par article
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

-- Liens Item/Article/Contenant
ALTER TABLE Item ADD CONSTRAINT Item_Article_FK FOREIGN KEY (idArticle) REFERENCES Article(idArticle);
ALTER TABLE Item ADD CONSTRAINT Item_Contenant_FK FOREIGN KEY (idContenant) REFERENCES Contenant(idContenant);


-- Liens Clients / Commandes
ALTER TABLE Adresse ADD CONSTRAINT Adresse_Client_FK FOREIGN KEY (idClient) REFERENCES Client(idClient);
ALTER TABLE Commande ADD CONSTRAINT Commande_Client_FK FOREIGN KEY (idClient) REFERENCES Client(idClient);
ALTER TABLE Commande ADD CONSTRAINT Commande_Adresse_FK FOREIGN KEY (idAdresseLivraison) REFERENCES Adresse(idAdresse);

-- Liens Lignes de Commande
ALTER TABLE LigneCommande ADD CONSTRAINT Ligne_Commande_FK FOREIGN KEY (idCommande) REFERENCES Commande(idCommande);
ALTER TABLE LigneCommande ADD CONSTRAINT Ligne_Item_FK FOREIGN KEY (idItem) REFERENCES Item(idItem);

-- Liens Saisonnalité
ALTER TABLE est_de_saison ADD CONSTRAINT Saison_Produit_FK FOREIGN KEY (idProduit) REFERENCES Produit(idProduit);
ALTER TABLE est_de_saison ADD CONSTRAINT Saison_Dates_FK FOREIGN KEY (dateDebut, dateFin) REFERENCES Saison(dateDebut, dateFin);





