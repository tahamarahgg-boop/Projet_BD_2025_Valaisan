import java.sql.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.Scanner;

import javax.print.DocFlavor.STRING;

import java.util.ArrayList;

public class ValaisonJDBC {
    private static final String URL = "jdbc:oracle:thin:@oracle1:1521:oracle1";
    private static final String USER = "lahmouza";
    private static final String PASSWORD = "lahmouza";

    private static Connection conn;

    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println(" Connecté à la base de données.");

            System.out.print("🔹 Veuillez entrer votre ID Client : ");
            Scanner scanner=new Scanner(System.in);
            String input = scanner.nextLine();
            int id = Integer.parseInt(input);

            
            menuPrincipal(conn,id);

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
/* 

    private boolean est_disponible(int idArticle,int quantite,String cond){
        // STOCK 
        String query="SELECT QUANTITE FROM Article a JOIN Lot l WHERE idArticle = ? AND a.modeConditionnement=?";

        PreparedStatement stmt=conn.prepareStatement(query);
        stmt.setInt(1,idArticle);
        stmt.setString(2,cond);
        ResultSet res=stmt.executeQuery();
        if (res.next()){
            if (res.getInt("quantite")>quantite){
                return true;
            }
            else{
                System.out.println("stock insuffisant");
                return false;
            }
        }
        // Saisonalité
        else{
            String query2="SELECT Saison_dateDebut , Saison_dateFin FROM est_de_saison e JOIN Article a WHERE e.idProduit=a.idProduit and a.idProduit=? and a.modeConditionnement=? ";
            PreparedStatement stmt2=conn.prepareStatement(query2);
            stmt2.setInt(prod);
            stmt2.setString(2,cond);
            ResultSet res=stmt2.executeQuery();
            System.out.println("la prochaine période de dispo est"+(res.next()).getDate("Data_debut") +":"+(res.next()).getDate("Date_fin"));
            return false;
    }
    }
    private int frais_livraison(int distance, int poids, String pays) {
        int frais = 5; // frais de base

        // coût en fonction de la distance et du poids
        frais += (int)(distance * 0.5);
        frais += (int)(poids * 0.2);

        // supplément pour l'international
        if (!pays.equalsIgnoreCase("France")) {
            frais += 10;
        }

        return frais;
    }
    public int montant_commande(ArrayList<Integer> id_produits){
        /*
        String query ="SELECT LIGNE_COMMANDES FROM COMMANDE WHERE COMMANDE_ID=?";
        // calcule frais de livraison si c'est dans la table
        PreparedStatement stmt=conn.prepareStatement(query);
        stmt.setInt(1,id);
        ResultSet res=stmt.executeQuery();
        while (res.next()){
            int prod_id=res.getInt("prod_id");
            int quant=res.getInt("quantite");
            // mode cnd à partir de prod_id
            String mode_cnd;
            if (est_disponible(prod_id,quant,mode_cnd)){

            }
        }
        */
       
    /*     int montant_total=0;
        for (int i=0;i<id_produits.size();i++){
            int id=id_produits.get(i);
            // obtenir le prix total du produit commandé
            String query="SELECT prixUnitaire from Produit p JOIN Article a where p.idProduit=a.idProduit and a.idProduit=?";
            PreparedStatement stmt=conn.prepareStatement(query);
            stmt.setInt(1,id);
            ResultSet res=stmt.executeQuery();
            montant_total+=res.next().getInt("prixUnitaire");
            // ajouter les frais de livraison qui dependent de la localisation 
            // et la quantite commandée
            montant_total+=frais_livraison(i, id, query);
        }
        return montant_total;
    } */
    // pour la gestion du stock on fait un rollback au debut pour annuler les modifications 
    // si on veut
/* *\ */

/*    int c=1;
    public void creer_commande(int id_client, String moderecup, String mode_livr, int montant,int frais_livraison){

        String query = "INSERT INTO COMMANDE (idCommande,date_commande, id_client, montant, mode_recup, mode_livr,status,fraisLivraison) "
                    + "VALUES (NOW(), ?, ?, ?, ?,?,?)";

        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, id_client);
        stmt.setInt(2, montant);
        stmt.setString(3, moderecup);
        stmt.setString(4, mode_livr);
        stmt.setString(status, "Préparation");
        stmt.setInt(6, frais_livraison);

        stmt.executeUpdate();
        c+=1;

        System.out.println("Commande créée !");
}
 */
    // ️ Lecture et exécution du script SQL
    private static void initialiserBase(String cheminFichier) {
        try (Statement stmt = conn.createStatement()) {
            String contenu = Files.readString(Path.of(cheminFichier));
            String[] requetes = contenu.split(";");
            for (String requete : requetes) {
                requete = requete.trim();
                if (!requete.isEmpty()) {
                    stmt.execute(requete);
                }
            }
            System.out.println("Tables créer succès !");
        } catch (SQLException | IOException e) {
            System.err.println("Erreur lors de l'exécution du script SQL : " + e.getMessage());
        }
    }

    // Interface texte principale
    private static void menuPrincipal(Connection conn,int id) {
        Scanner scanner = new Scanner(System.in);
        int choix;

        do {
            System.out.println("\n===== MENU PRINCIPAL =====");
            System.out.println("1. Consulter le catalogue de produits");
            System.out.println("2. Passer une commande");
            System.out.println("3. Consulter les alertes de péremption");
            System.out.println("4. Clôturer une commande");
            System.out.println("0. Quitter");
            System.out.print("Votre choix : ");
            choix = scanner.nextInt();
            scanner.nextLine(); // consomme le retour à la ligne

            switch (choix) {
                case 1 -> {
                    CatalogueService catalogue = new CatalogueService(conn);
                    catalogue.consulterCatalogue();
                    }
                case 2 -> {
                    ServiceCommande comm=new ServiceCommande();
                    comm.passerCommandeDebut(id);
                    }
                case 3 -> {
                    ServiceAlerte alerte = new ServiceAlerte(conn);
                    alerte.consulterAlerte();
                    }
                case 4 -> cloturerCommande(scanner);
                case 0 -> System.out.println(" Au revoir !");
                default -> System.out.println(" Choix invalide.");
            }
        } while (choix != 0);
    }

    // Clôturer une commande
    private static void cloturerCommande(Scanner scanner) {
        System.out.print("Entrez l'ID de la commande à clôturer : ");
        int id = scanner.nextInt();
        String sql = "UPDATE commandes SET statut = 'TERMINEE' WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            int n = pstmt.executeUpdate();
            if (n > 0) System.out.println(" Commande clôturée avec succès !");
            else System.out.println(" Commande introuvable.");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la clôture : " + e.getMessage());
        }
    }


    }
