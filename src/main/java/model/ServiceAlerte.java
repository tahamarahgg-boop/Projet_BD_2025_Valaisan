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
    public enum NaturePerte{
        PERIME, CASSE, VOL
    };

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
                int stock = rs.getInt("quantiteDisponible");
                int nbJourRestant = rs.getInt("nbJourRestant");
                if(nbJourRestant<=0){
                    System.out.printf("PERTE péremption : Article = %d, Lot = %d, Quantité = %d\n",
                        idArticle,idLot,stock);
                    perte_produit(idLot,stock,NaturePerte.PERIME);   
                }
                else{
                    System.out.printf("Article: %d, Quantité: %d — périme dans %d jours\n",
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


    public void perte_produit(int idLot,int quantitePerdue,NaturePerte nature){
        String sql = "UPDATE Lot SET quantiteDisponible = quantiteDisponible - ? WHERE idLot = ?;" +
         "INSERT INTO Perte (idLot,datePerte,naturePerte,quantitePerdue) Values(?,NOW(),?,?;";
        try(PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1,quantitePerdue);
            pstmt.setInt(2,idLot);
            pstmt.setInt(3,idLot);
            pstmt.setString(4,nature_perte(nature));
            pstmt.setInt(5,quantitePerdue);
            pstmt.executeUpdate();
            conn.commit();
            System.out.println(" Ajout en Perte du produit !");
        } catch (SQLException e){
            System.err.println("Erreur lors de la mise en perte d'un article : " + e.getMessage());
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

    private String nature_perte(NaturePerte nature){
        switch(nature){
            case PERIME:
                return "PEREMPTION";
            case CASSE:
                return "CASSE";
            default:
                return "VOL";
        }
        
    }
}