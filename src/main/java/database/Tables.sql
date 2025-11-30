/* ============================================================
   Fichier : tables_corriges.sql
   ============================================================ */

BEGIN
   -- Nettoyage (Ordre inverse des dépendances)
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE LigneCommande CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE Commande CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE Perte CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE Lot CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE Item CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE est_de_saison CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE Article CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE Contenant CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE Produit CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE ActiviteProducteur CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE CaracteristiqueProduit CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE Producteur CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE Adresse CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE Client CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
   BEGIN EXECUTE IMMEDIATE 'DROP TABLE Saison CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
END;
/

-- Tables Indépendantes
CREATE TABLE Producteur (
    idProducteur INTEGER GENERATED ALWAYS AS IDENTITY,
    nom VARCHAR2(100),
    adresse VARCHAR2(200),
    longitude NUMBER(10,6),
    latitude NUMBER(10,6),
    mail VARCHAR2(200),
    telephone VARCHAR2(20),
    CONSTRAINT Producteur_PK PRIMARY KEY (idProducteur)
);

CREATE TABLE Saison (
    dateDebut DATE NOT NULL,
    dateFin DATE NOT NULL,
    CONSTRAINT Saison_PK PRIMARY KEY (dateDebut, dateFin)
);

CREATE TABLE Client (
    idClient INTEGER GENERATED ALWAYS AS IDENTITY,
    mail VARCHAR2(100) NOT NULL UNIQUE,
    nom VARCHAR2(100),
    prenom VARCHAR2(100),
    telephone VARCHAR2(20),
    CONSTRAINT Client_PK PRIMARY KEY (idClient)
);

CREATE TABLE Contenant (
    idContenant INTEGER GENERATED ALWAYS AS IDENTITY,
    typeContenant VARCHAR2(50) NOT NULL,
    capacite NUMBER(10,3),
    unite VARCHAR2(10), 
    reutilisable NUMBER(1) CHECK (reutilisable IN (0,1)),
    stock INTEGER,
    prixVente NUMBER(10,2),
    CONSTRAINT Contenant_PK PRIMARY KEY (idContenant)
);

-- Tables Dépendantes Niveau 1
CREATE TABLE ActiviteProducteur (
    idProducteur INTEGER NOT NULL,
    nomActivite VARCHAR2(50) NOT NULL,
    CONSTRAINT Activite_PK PRIMARY KEY (idProducteur, nomActivite),
    CONSTRAINT Activite_FK FOREIGN KEY (idProducteur) REFERENCES Producteur(idProducteur)
);

CREATE TABLE Produit (
    idProduit INTEGER GENERATED ALWAYS AS IDENTITY,
    idProducteur INTEGER NOT NULL,
    nom VARCHAR2(100) NOT NULL,
    categorie VARCHAR2(50),
    description VARCHAR2(500),
    CONSTRAINT Produit_PK PRIMARY KEY (idProduit),
    CONSTRAINT Produit_FK FOREIGN KEY (idProducteur) REFERENCES Producteur(idProducteur)
);

CREATE TABLE CaracteristiqueProduit (
    idProduit INTEGER NOT NULL,
    caracteristique VARCHAR2(50) NOT NULL,
    CONSTRAINT Caracteristique_PK PRIMARY KEY (idProduit, caracteristique),
    CONSTRAINT Caracteristique_FK FOREIGN KEY (idProduit) REFERENCES Produit(idProduit)
);

CREATE TABLE Adresse (
    idAdresse INTEGER GENERATED ALWAYS AS IDENTITY,
    idClient INTEGER NOT NULL,
    adressePostale VARCHAR2(200),
    pays VARCHAR2(50),
    latitude NUMBER(10, 6),
    longitude NUMBER(10, 6),
    CONSTRAINT Adresse_PK PRIMARY KEY (idAdresse),
    CONSTRAINT Adresse_Client_FK FOREIGN KEY (idClient) REFERENCES Client(idClient)
);

-- Tables Dépendantes Niveau 2
CREATE TABLE Article (
    idArticle INTEGER GENERATED ALWAYS AS IDENTITY,
    idProduit INTEGER NOT NULL,
    modeConditionnement VARCHAR2(50),
    poids NUMBER(10,3),
    prixAchatProducteur NUMBER(10,2),
    prixVenteClient NUMBER(10,2),
    delaiDisponibilite INTEGER DEFAULT 0,
    CONSTRAINT Article_PK PRIMARY KEY (idArticle),
    CONSTRAINT Article_Produit_FK FOREIGN KEY (idProduit) REFERENCES Produit(idProduit)
);

CREATE TABLE Commande (
    idCommande INTEGER GENERATED ALWAYS AS IDENTITY,
    idClient INTEGER NOT NULL,
    idAdresseLivraison INTEGER,
    dateCommande DATE DEFAULT SYSDATE,
    statut VARCHAR2(50),
    modePaiement VARCHAR2(50),
    modeRecuperation VARCHAR2(50),
    fraisLivraison NUMBER(10,2),
    montantTotal NUMBER(10,2),
    CONSTRAINT Commande_PK PRIMARY KEY (idCommande),
    CONSTRAINT Cmd_Client_FK FOREIGN KEY (idClient) REFERENCES Client(idClient),
    CONSTRAINT Cmd_Adresse_FK FOREIGN KEY (idAdresseLivraison) REFERENCES Adresse(idAdresse)
);

CREATE TABLE est_de_saison (
    idProduit INTEGER NOT NULL,
    dateDebut DATE NOT NULL,
    dateFin DATE NOT NULL,
    CONSTRAINT est_de_saison_PK PRIMARY KEY (idProduit, dateDebut, dateFin),
    CONSTRAINT Saison_Prod_FK FOREIGN KEY (idProduit) REFERENCES Produit(idProduit),
    CONSTRAINT Saison_Date_FK FOREIGN KEY (dateDebut, dateFin) REFERENCES Saison(dateDebut, dateFin)
);

-- Tables Dépendantes Niveau 3 (Item, Lot, LigneCommande)
CREATE TABLE Item (
    idItem INTEGER GENERATED ALWAYS AS IDENTITY,
    typeItem VARCHAR2(20) NOT NULL,	-- 'ARTICLE' ou 'CONTENANT'
    idArticle INTEGER,		-- NULL si typeItem = 'CONTENANT'
    idContenant INTEGER,	-- NULL si typeItem = 'ARTICLE'
    CONSTRAINT Item_PK PRIMARY KEY (idItem),
    CONSTRAINT Item_Art_FK FOREIGN KEY (idArticle) REFERENCES Article(idArticle),
    CONSTRAINT Item_Cont_FK FOREIGN KEY (idContenant) REFERENCES Contenant(idContenant),
    CONSTRAINT chk_Item_Type CHECK (
        (typeItem = 'ARTICLE' AND idArticle IS NOT NULL AND idContenant IS NULL) OR
        (typeItem = 'CONTENANT' AND idContenant IS NOT NULL AND idArticle IS NULL)
    )
);

CREATE TABLE Lot (
    idLot INTEGER GENERATED ALWAYS AS IDENTITY,
    idArticle INTEGER NOT NULL,
    dateReception DATE DEFAULT SYSDATE NOT NULL,
    quantiteInitiale NUMBER(10,2) NOT NULL CHECK (quantiteInitiale >= 0),
    quantiteDisponible NUMBER(10,2) NOT NULL CHECK (quantiteDisponible >= 0),
    datePeremption DATE,
    typePeremption VARCHAR2(4) CHECK (typePeremption IN ('DLC', 'DLUO')),
    pourcentageReduction NUMBER(5,2) DEFAULT 0,
    CONSTRAINT Lot_PK PRIMARY KEY (idLot),
    CONSTRAINT Lot_Art_FK FOREIGN KEY (idArticle) REFERENCES Article(idArticle)
);

CREATE TABLE LigneCommande (
    idLigne INTEGER GENERATED ALWAYS AS IDENTITY,
    idCommande INTEGER NOT NULL,
    idItem INTEGER NOT NULL,
    quantite NUMBER(10,2) CHECK (quantite >= 0),
    prixUnitaireApplique NUMBER(10,2),
    CONSTRAINT Ligne_PK PRIMARY KEY (idLigne),
    CONSTRAINT Ligne_Cmd_FK FOREIGN KEY (idCommande) REFERENCES Commande(idCommande),
    CONSTRAINT Ligne_Item_FK FOREIGN KEY (idItem) REFERENCES Item(idItem)
);

CREATE TABLE Perte (
    idPerte INTEGER GENERATED ALWAYS AS IDENTITY,
    idLot INTEGER,		-- NULL si perte sur contenant
    idContenant INTEGER,	-- NULL si perte sur lot
    datePerte DATE DEFAULT SYSDATE,
    naturePerte VARCHAR2(100),
    quantitePerdue NUMBER(10,2) CHECK (quantitePerdue >= 0),
    CONSTRAINT Perte_PK PRIMARY KEY (idPerte),
    CONSTRAINT Perte_Lot_FK FOREIGN KEY (idLot) REFERENCES Lot(idLot),
    CONSTRAINT Perte_Cont_FK FOREIGN KEY (idContenant) REFERENCES Contenant(idContenant)
);
