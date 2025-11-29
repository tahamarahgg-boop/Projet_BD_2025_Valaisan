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

public class ServiceAlerte {

    private Connection conn;

    String cmd_sql = "SELECT idLot, idArticle, quantiteDisponible " +
            "DATEDIFF(datePeremption,DATE(NOW())) AS nbJourRestant " +
            "FROM Lot " +
            "WHERE quantiteDisponible <> 0 " +
            "AND datePeremption < NOW() + INTERVAL 7 DAY";

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
                if(nbJourRestant<=0){
                    System.out.printf("PERTE péremption : Article = %d, Lot = %d, Quantité = %f\n",
                        idArticle,idLot,stock);
                    perte_produit(idLot,idArticle,stock,"Péremption");   
                }
                else{
                    System.out.printf("Article: %d, Quantité: %f — périme dans %d jours\n",
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


    public void perte_produit(int idLot,int idArticle,float quantitePerdue,String nature){
        String sql = "UPDATE Lot SET quantiteDisponible = quantiteDisponible - ? WHERE idLot = ?;" +
            "UPDATE Article SET stock = stock - ? WHERE idArticle = ?;" +
            "INSERT INTO Perte (idLot,idContenant,datePerte,naturePerte,quantitePerdue) Values(?,NULL,NOW(),?,?);";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setFloat(1,quantitePerdue);
            pstmt.setInt(2,idLot);
            pstmt.setFloat(3,quantitePerdue);
            pstmt.setInt(4,idArticle);
            pstmt.setInt(5,idLot);
            pstmt.setString(6,nature);
            pstmt.setFloat(7,quantitePerdue);
            pstmt.executeUpdate();
            conn.commit();
            System.out.println(" Ajout en Perte du produit !");
        } catch (SQLException e){
            System.err.println("Erreur lors de la mise en perte d'un article : " + e.getMessage());
        }
    }

    public void perte_contenant(int idContenant,float quantitePerdue,String nature){
        String sql = "UPDATE Contenant SET stock = stock - ? WHERE idContenant = ?;" +
          "INSERT INTO Perte (idLot,idContenant,datePerte,naturePerte,quantitePerdue) Values(NULL,?,NOW(),?,?);";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setFloat(1,quantitePerdue);
            pstmt.setInt(2,idContenant);
            pstmt.setInt(3,idContenant);
            pstmt.setString(4,nature);
            pstmt.setFloat(5,quantitePerdue);
            pstmt.executeUpdate();
            conn.commit();
            System.out.println(" Ajout en Perte de contenants !");
        } catch (SQLException e){
            System.err.println("Erreur lors de la mise en perte de contenant : " + e.getMessage());
        }
    }

    private void reduction_peremption(int idLot,int nbJourRestant){
        String sql = "UPDATE Lot SET reduction = ? WHERE idLot = ?";
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
