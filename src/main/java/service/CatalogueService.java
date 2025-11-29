package service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CatalogueService {
    private Connection conn;

    public CatalogueService(Connection conn){
        this.conn = conn;
    }


    public void consulterCatalogue(){
        String sql = "SELECT Produit.idProduit, Produit.categorie, Produit.nom, Producteur.nom, Produit.description, " +
            "FROM Producteur, Produit" +
            "WHERE Producteur.idProducteur = Produit.idProducteur" +
            "ORDER BY Produit.categorie, Produit.nom";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n--- Catalogue ---");
            while (rs.next()) {
                int idProduit = rs.getInt("Produit.idProduit");
                String categorieProduit = rs.getString("Produit.categorie");
                String nomProduit = rs.getString("Produit.nom");
                String nomProducteur = rs.getString("Producteur.nom");
                String descriptionProduit = rs.getString("Produit.description");
                System.out.printf("Catégorie: %s, Nom: %s, Producteur: %s, Description: %s\n",
                        categorieProduit, nomProduit, nomProducteur, descriptionProduit);
                getArticles_Produit(idProduit);
                System.out.println("");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la consultation du catalogue : " + e.getMessage());
        }
    }


    public void getArticles_Produit(int idProduit){
        String sql = "SELECT idArticle, modeConditionnement, poids, prixVenteClient, stock" +
            "FROM Article WHERE Article.idProduit = ?";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1,idProduit);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                int idArticle = rs.getInt("idArticle");
                String modeConditionnement = rs.getString("modeConditionnement");
                boolean est_vrac = modeConditionnement.equals("Vrac");
                Float prix = rs.getFloat("prixVenteClient");
                Float stock = rs.getFloat("stock");
                if(!est_vrac){
                    Float poids = rs.getFloat("poids");
                    System.out.printf(" - Id: %d, Article: %s de %f, PrixUnitaire: %f, Stock: %f\n",
                        idArticle,modeConditionnement,poids,prix,stock);
                }
                else{
                    System.out.printf(" - Id: %d, En vrac, Prix: %f/kg, Stock: %f\n",
                        idArticle,prix,stock);
                }
            }
        } catch (SQLException e){
            System.err.println("Erreur lors de la consultation des articles : " + e.getMessage());
        }
    }
}
