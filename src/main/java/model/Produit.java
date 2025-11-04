package model;

public class Produit {
    private int id;
    private String nom;
    private String categorie;
    private String description;
    private int idproducteur;
    private String caracteristique;
    private String modedisponibilité;


    // Constructeurs, getters, setters...
    public Produit() {}

    public Produit(int id, String nom, String categorie, String description, int idproducteur, String caracteristique, String modedisponibilité){
        this.id = id;
        this.nom = nom;
        this.categorie = categorie;
        this.description = description;
        this.idproducteur = idproducteur;
        this.caracteristique = caracteristique;
        this.modedisponibilité = modedisponibilité;

    }

    //partie des set et des get
    public void setId(int id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }
    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setIdproducteur(int idproducteur) {
        this.idproducteur = idproducteur;
    }
    public void setCaracteristique(String caracteristique) {
        this.caracteristique = caracteristique;
    }
    public void setModedisponibilité(String modedisponibilité){
        this.modedisponibilité=modedisponibilité;
    }
    public String getNom() {
        return nom;
    }
    public String getCategorie() {
        return categorie;
    }
    public String getDescription() {
        return description;
    }
    public int getIdproducteur() {
        return idproducteur;
    }
    public String getCaracteristique() {
        return caracteristique;
    }
    public String getModedisponibilité(){
        return modedisponibilité;
    }
    public int getId() {
        return id;
    }
    @Override
    public String toString() {
        //Produit[id,nom,categorie, etc...]
        return "Produit[" + id +"," + nom +","+ categorie+"," + description + "," + idproducteur +"," + caracteristique +"," + modedisponibilité +"]";
    }
}
