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
            System.out.println("\n-- Produits --");
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
            System.err.println("Erreur lors de la consultation du catalogue des produits : " + e.getMessage());
        }

        System.out.println("\n-- Contenants --");
        String sql2 = "SELECT Item.idItem, Contenant.typeContenant, Contenant.capacite, Contenant.unite, Contenant.reutilisable, Contenant.stock, Contenant.prixVente " +
            "FROM Item, Contenant WHERE Item.idContenant = Contenant.idContenant";
        try (Statement stmt2 = conn.createStatement();
             ResultSet rs2 = stmt2.executeQuery(sql)){
            while(rs2.next()){
                int reutilisable = rs2.getInt("Contenant.reutilisable");
                String est_reutilisable;
                if(reutilisable == 1){
                    est_reutilisable = "Reutilisable";
                }
                else{
                    est_reutilisable = "Non Reutilisable";
                }
                System.out.printf("Id: %d, Type: %s, Capacite: %f %s, %s --> Prix: %f, Stock: %f\n",
                    rs2.getInt("Item.idItem"),
                    rs2.getString("Contenant.typeContenant"),
                    rs2.getFloat("Contenant.capacite"),
                    rs2.getString("Contenant.unite"),
                    est_reutilisable,
                    rs2.getFloat("Contenant.prixVente"),
                    rs2.getFloat("Contenant.stock"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la consultation du catalogue des contenants : " + e.getMessage());
        }
    }


    public void getArticles_Produit(int idProduit){
        String sql = "SELECT Item.idItem, Article.idArticle, Article.modeConditionnement, Article.poids, Article.prixVenteClient" +
            "FROM Article, Item WHERE Item.idArticle = Article.idArticle AND Article.idProduit = ?";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1,idProduit);
            ResultSet rs = pstmt.executeQuery();
            while(rs.next()) {
                int idItem = rs.getInt("Item.idItem");
                int idArticle = rs.getInt("Article.idArticle");
                String modeConditionnement = rs.getString("Article.modeConditionnement");
                boolean est_vrac = modeConditionnement.equals("Vrac");
                float prix = rs.getFloat("Article.prixVenteClient");
                float stock = getStock_Article(idArticle);
                if(!est_vrac){
                    float poids = rs.getFloat("Article.poids");
                    System.out.printf(" - Id: %d, Article: %s de %f --> PrixUnitaire: %f, Stock: %f\n",
                        idItem,modeConditionnement,poids,prix,stock);
                }
                else{
                    System.out.printf(" - Id: %d, En vrac --> Prix: %f/kg, Stock: %f\n",
                        idItem,prix,stock);
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
