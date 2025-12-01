package service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ServiceAlerte {

    private Connection conn;

     String cmd_sql = "SELECT idLot, idArticle, quantiteDisponible, " +
            "TRUNC(datePeremption - SYSDATE) AS nbJourRestant " +
            "FROM Lot " +
            "WHERE quantiteDisponible <> 0 " +
            "AND datePeremption < SYSDATE + 7 FOR UPDATE";
            
    public ServiceAlerte(Connection conn){
        this.conn = conn;
    }

    public void consulterAlerte(){
        String sql = cmd_sql;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n--- Produits proches de la péremption ---");
            boolean vide = true;
            while (rs.next()) {
                int idLot = rs.getInt("idLot");
                int idArticle = rs.getInt("idArticle");
                float stock = rs.getFloat("quantiteDisponible");
                int nbJourRestant = rs.getInt("nbJourRestant");
                
                if(nbJourRestant<0){
                    System.out.printf("PERTE péremption : Article = %d, Lot = %d, Quantité = %3.f\n",
                        idArticle,idLot,stock);
                    perte_produit(idLot,stock,"Péremption");   
                }
                else{
                    System.out.printf("Article: %d, Quantité: %.3f : périme dans %d jours\n",
                        idArticle, stock, nbJourRestant);
                    reduction_peremption(idLot,nbJourRestant);
                }
                vide = false;
            }
            if (vide) System.out.println("Aucune alerte pour le moment.");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la consultation des alertes : " + e.getMessage());
        }
    }


    public void perte_produit(int idLot,float quantitePerdue,String nature){
        String updateSql = "UPDATE Lot SET quantiteDisponible = quantiteDisponible - ? WHERE idLot = ?";
        String insertSql = "INSERT INTO Perte (idLot, idContenant, datePerte, naturePerte, quantitePerdue) VALUES (?, NULL, SYSDATE, ?, ?)";
        
        try{
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setFloat(1, quantitePerdue);
                updateStmt.setInt(2, idLot);
                updateStmt.executeUpdate();
            }
            
            // Exécuter l'INSERT
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, idLot);
                insertStmt.setString(2, nature);
                insertStmt.setFloat(3, quantitePerdue);
                insertStmt.executeUpdate();
            }
            conn.commit();
            System.out.println(" Ajout en Perte du produit !");
        } catch (SQLException e){
            System.err.println("Erreur lors de la mise en perte d'un article : " + e.getMessage());
        }
    }

    public void perte_contenant(int idContenant,float quantitePerdue,String nature){
        String updateSql = "UPDATE Contenant SET stock = stock - ? WHERE idContenant = ?";
        String insertSql = "INSERT INTO Perte (idLot, idContenant, datePerte, naturePerte, quantitePerdue) VALUES (NULL, ?, SYSDATE, ?, ?)";
        
        try{
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setFloat(1, quantitePerdue);
                updateStmt.setInt(2, idContenant);
                updateStmt.executeUpdate();
            }
            
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setInt(1, idContenant);
                insertStmt.setString(2, nature);
                insertStmt.setFloat(3, quantitePerdue);
                insertStmt.executeUpdate();
            }
            conn.commit();
            System.out.println(" Ajout en Perte de contenants !");
        } catch (SQLException e){
            System.err.println("Erreur lors de la mise en perte de contenant : " + e.getMessage());
        }
    }

    private void reduction_peremption(int idLot,int nbJourRestant){
        String sql = "UPDATE Lot SET pourcentageReduction = ? WHERE idLot = ?";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)){
            if(nbJourRestant>3){
                pstmt.setInt(1, 30);
            }
            else{
                pstmt.setInt(1, 50);
            }
            pstmt.setInt(2, idLot);
            pstmt.executeUpdate();
            conn.commit();
            System.out.println(" Réduction de prix enregistrée !");
        } catch (SQLException e){
            System.err.println("Erreur lors de la réduction du prix d'un article : " + e.getMessage());
        }
    }
}
