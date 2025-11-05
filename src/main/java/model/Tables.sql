CREATE TYPE statut_commande AS ENUM ('EN_PREPARATION', 'PRETE', 'EN_LIVRAISON', 'LIVREE', 'ANNULEE');
CREATE TYPE statut_commande AS ENUM ("EN_LIGNE", "DANS_LA_BOUTIQUE")
CREATE TABLE PRODUIT (
    id NUMBER PRIMARY KEY,
    nom VARCHAR2(100) NOT NULL,
    categorie VARCHAR2(50),
    description VARCHAR2(500),
    idproducteur NUMBER NOT NULL,
    caracteristique VARCHAR2(200),
    modedisponibilite VARCHAR2(50),
    CONSTRAINT fk_produit_producteur FOREIGN KEY (idproducteur) REFERENCES PRODUCTEUR(id)
);

CREATE TABLE PRODUCTEUR (
    id NUMBER PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    adresse VARCHAR(200),
    email VARCHAR(100),
    longitude NUMBER(10,6),
    latitude NUMBER(10,6)
);
CREATE TABLE CLIENT (
    id NUMBER PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    mail VARCHAR(100),
    tel INT
);
CREATE TABLE COMMANDE(
    id NUMBER PRIMARY KEY,
    idclient NUMBER NOT NULL,
    dateCOMMANDE DATE NOT NULL,
    heure HOUR NOT NULL,
    statut statut_commande,
    modepaiment paiment,
    moderecuperation VARCHAR2(200),
);
CREATE TABLE CONTENANT(
    id NUMBER PRIMARY KEY

    )