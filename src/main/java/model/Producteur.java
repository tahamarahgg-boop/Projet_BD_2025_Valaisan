package model;

public class Producteur {
    private int id;
    private String nom;
    private String adresse;
    private String email;
    private float longitude;
    private float latitude;

    // Constructeurs, getters, setters...
    public Producteur() {}

    public Producteur(int id, String nom, String adresse, String email, String typeActivite) {
        this.id = id;
        this.nom = nom;
        this.adresse = adresse;
        this.email = email;
        this.longitude = 0;
        this.latitude = 0;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public  String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public float getLongitude() { return longitude; }
    public void setLongitude(float longitude) { this.longitude = longitude; }
    public float getLatitude() { return latitude; }
    public void setLatitude(float latitude) { this.latitude = latitude; }
    public void setGeolocalisation(float longitude, float latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }
    @Override
    public String toString() {
        //ecrire la table sous forme Producteur[Id,Nom,Adresse,Email, (longitude, latitude)]
        return "Producteur["+ this.id + "," + this.nom + "," + this.adresse + "," + this.email + ",(" + this.longitude + "," + this.latitude + ")]";

    }
}