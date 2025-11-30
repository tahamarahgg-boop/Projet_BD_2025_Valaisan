package service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ServiceCatalogue {

    
    // --- AFFICHAGE (Catalogue/Alertes) ---

    private static void consulterCatalogue(Connection conn) {
        String sql = "SELECT i.idItem, i.typeItem, p.nom, a.modeConditionnement, a.prixVenteClient, c.typeContenant, c.capacite, c.prixVente, c.stock," +
                     "(SELECT SUM(quantiteDisponible) FROM Lot WHERE idArticle = a.idArticle) as stk " +
                     "FROM Item i LEFT JOIN Article a ON i.idArticle=a.idArticle LEFT JOIN Produit p ON a.idProduit=p.idProduit " +
                     "LEFT JOIN Contenant c ON i.idContenant=c.idContenant ORDER BY i.idItem";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            System.out.println("--- CATALOGUE ---");
            while(rs.next()) {
                String desc = "ARTICLE".equals(rs.getString("typeItem")) ? 
                    rs.getString("nom") + " (" + rs.getString("modeConditionnement") + ") - Prix: " + rs.getFloat("prixVenteClient") + "€ - Stock: " + rs.getInt("stk") :
                    rs.getString("typeContenant") + " (" + rs.getDouble("capacite") + ") - Prix: " + rs.getFloat("prixVente") + "€ - Stock: " + rs.getInt("stock");
                System.out.printf("[%d] %s\n", rs.getInt("idItem"), desc);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
