package service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ServiceCatalogue {

    
        // --- AFFICHAGE (Catalogue/Alertes) ---
    public static void consulterCatalogue(Connection conn) {
        // Requête simplifiée pour le débogage
        String sql = 
            "SELECT i.idItem, i.typeItem, " +
            "       p.nom AS nomProduit, a.modeConditionnement, a.prixVenteClient, " +
            "       c.typeContenant, c.capacite, c.prixVente AS prixContenant, " +
            "       (SELECT SUM(quantiteDisponible) FROM Lot WHERE idArticle = a.idArticle) as stockArticle, " +
            "       c.stock as stockContenant " +
            "FROM Item i " +
            "LEFT JOIN Article a ON i.idArticle = a.idArticle " +
            "LEFT JOIN Produit p ON a.idProduit = p.idProduit " +
            "LEFT JOIN Contenant c ON i.idContenant = c.idContenant " +
            "ORDER BY i.idItem";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n--- CATALOGUE GENERAL ---");
            // En-tête propre
            System.out.printf("%-5s | %-10s | %-30s | %-10s | %-10s\n", "ID", "TYPE", "NOM / DESCRIPTION", "PRIX", "STOCK");
            System.out.println("------------------------------------------------------------------------------");

            boolean vide = true;
            while (rs.next()) {
                vide = false;
                int id = rs.getInt("idItem");
                String type = rs.getString("typeItem");
                
                String nomAffichage = "-";
                String prixAffichage = "-";
                String stockAffichage = "-";

                if ("ARTICLE".equals(type)) {
                    // C'est un Article
                    String nomProd = rs.getString("nomProduit");
                    String cond = rs.getString("modeConditionnement");
                    nomAffichage = (nomProd != null ? nomProd : "Inconnu") + " (" + (cond != null ? cond : "?") + ")";
                    
                    double prix = rs.getDouble("prixVenteClient");
                    prixAffichage = String.format("%.2f €", prix);
                    
                    // Gestion du NULL pour le stock (si pas de lot)
                    int stock = rs.getInt("stockArticle");
                    if (rs.wasNull()) stock = 0; // Important si le SUM retourne NULL
                    stockAffichage = String.valueOf(stock);

                } else if ("CONTENANT".equals(type)) {
                    // C'est un Contenant
                    String typeCont = rs.getString("typeContenant");
                    double cap = rs.getDouble("capacite");
                    nomAffichage = (typeCont != null ? typeCont : "Inconnu") + " (" + cap + ")";
                    
                    double prix = rs.getDouble("prixContenant");
                    prixAffichage = String.format("%.2f €", prix);
                    
                    int stock = rs.getInt("stockContenant");
                    stockAffichage = String.valueOf(stock);
                }

                System.out.printf("%-5d | %-10s | %-30.30s | %-10s | %-10s\n", 
                                  id, type, nomAffichage, prixAffichage, stockAffichage);
            }

            if (vide) {
                System.out.println("(!) Le catalogue est vide. Vérifiez que la table ITEM contient des données.");
            }
            System.out.println("------------------------------------------------------------------------------");

        } catch (SQLException e) {
            System.err.println("[ERREUR SQL Catalogue] " + e.getMessage());
            e.printStackTrace();
        }
    } 
 
}
