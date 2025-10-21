package model;

public class Producteur {
    private int id;
    private String nom;
    private String adresse;
    private String email;
    private String typeActivite;
    private double latitude;
    private double longitude;

    // Constructeurs, getters, setters...
    public Producteur() {}

    public Producteur(int id, String nom, String adresse, String email, String typeActivite) {
        this.id = id;
        this.nom = nom;
        this.adresse = adresse;
        this.email = email;
        this.typeActivite = typeActivite;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    // ... etc pour tous les attributs
}