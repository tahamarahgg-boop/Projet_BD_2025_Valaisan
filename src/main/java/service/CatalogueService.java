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
        String sql = "SELECT idArticle, modeConditionnement, poids, prixVenteClient" +
            "FROM Article WHERE Article.idProduit = ?";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1,idProduit);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                int idArticle = rs.getInt("idArticle");
                String modeConditionnement = rs.getString("modeConditionnement");
                boolean est_vrac = modeConditionnement.equals("Vrac");
                float prix = rs.getFloat("prixVenteClient");
                float stock = getStock_Article(idArticle);
                if(!est_vrac){
                    float poids = rs.getFloat("poids");
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

    private float getStock_Article(int idArticle){
        String sql = "SELECT SUM(quantiteDisponible) AS stock FROM Lot WHERE idArticle = ? AND quantiteDisponible <> 0";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1,idArticle);
            ResultSet rs = pstmt.executeQuery();
            if(rs.next()){
                return rs.getFloat("stock");
            }
            System.out.println("Erreur lors du calcul du stock");
            return -1;
        } catch (SQLException e){
            System.err.println("Erreur lors du calcul du stock : " + e.getMessage());
            return -1;
        }
    }
}
