package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class ValaisonJDBC {
    
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USER = "system";     
    private static final String PASSWORD = "lahmouza"; 

    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[OK] Connecte a la base de donnees.");

            Scanner scanner = new Scanner(System.in);
            
            System.out.print("Veuillez entrer votre ID Client : ");
            if (scanner.hasNextInt()) {
                int idClient = scanner.nextInt();
                scanner.nextLine(); 
                
                menuPrincipal(conn, scanner, idClient);
            } else {
                System.out.println("[ERREUR] ID invalide.");
            }

            conn.close();
            scanner.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void menuPrincipal(Connection conn, Scanner scanner, int idClient) {
        // On instancie les deux services
        ServiceCommande serviceClient = new ServiceCommande();
        CloturerCommande serviceStaff = new CloturerCommande();

        int choix;
        do {
            System.out.println("\n==========================================");
            System.out.println("       VALAISON - MENU PRINCIPAL");
            System.out.println("==========================================");
            System.out.println("1. Consulter le catalogue");
            System.out.println("2. Passer une commande (Client)");
            System.out.println("3. Consulter les alertes");
            System.out.println("4. Annuler une commande (Client)");
            System.out.println("5. Espace STAFF (Gestion des statuts)");
            System.out.println("0. Quitter");
            System.out.println("------------------------------------------");
            System.out.print("Votre choix : ");
            
            try {
                String input = scanner.nextLine();
                choix = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                choix = -1;
            }

            try {
                switch (choix) {
                    case 1:
                        consulterCatalogue(conn);
                        break;
                    case 2:
                        // Utilise ServiceCommande
                        serviceClient.passerCommandeDebut(conn,idClient);
                        break;
                    case 3:
                        consulterAlertes(conn);
                        break;
                    case 4:
                        // Utilise CloturerCommande
                        menuAnnulation(conn, scanner, serviceStaff);
                        break;
                    case 5:
                        // Utilise CloturerCommande
                        menuStaff(conn, scanner, serviceStaff);
                        break;
                    case 0:
                        System.out.println("Au revoir !");
                        break;
                    default:
                        System.out.println("[ERREUR] Choix invalide.");
                }
            } catch (SQLException e) {
                System.out.println("[ERREUR SQL] " + e.getMessage());
            }
        } while (choix != 0);
    }

    // --- SOUS-MENUS ---

    private static void menuAnnulation(Connection conn, Scanner scanner, CloturerCommande staff) throws SQLException {
        System.out.println("\n--- ANNULATION ---");
        System.out.print("ID Commande : ");
        try {
            int idCmd = Integer.parseInt(scanner.nextLine());
            staff.annulerCommande(conn, idCmd);
        } catch (NumberFormatException e) {
            System.out.println("ID invalide.");
        }
    }

    private static void menuStaff(Connection conn, Scanner scanner, CloturerCommande staff) throws SQLException {
        System.out.println("\n--- ESPACE STAFF ---");
        System.out.print("ID Commande : ");
        int idCmd;
        try {
            idCmd = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID invalide.");
            return;
        }

        /* System.out.print("Veuillez entrer votre ID Staff : ");
        int idStaff = Integer.parseInt(scanner.nextLine()); */

        System.out.println("1. Preparer (Sortie Stock -> Prete)");
        System.out.println("2. Expedier (-> En livraison)");
        System.out.println("3. Livrer (-> Livree)");
        System.out.println("4. Retrait Boutique (-> Recuperee)");
        System.out.println("0. Retour");
        System.out.print("Choix : ");

        try {
            int c = Integer.parseInt(scanner.nextLine());
            switch (c) {
                case 1: staff.commandePrete(conn, idCmd); break;
                case 2: staff.commandeEnLivraison(conn, idCmd); break;
                case 3: staff.commandeLivree(conn, idCmd); break;
                case 4: staff.commandeRecuperee(conn, idCmd); break;
                case 0: break;
            }
        } catch (NumberFormatException e) {
            System.out.println("Choix invalide.");
        }
    }

    // --- AFFICHAGE (Catalogue/Alertes) ---

    private static void consulterCatalogue(Connection conn) {
        String sql = "SELECT i.idItem, i.typeItem, p.nom, a.modeConditionnement, a.prixVenteClient, c.typeContenant, c.capacite, c.prixVente, " +
                     "(SELECT SUM(quantiteDisponible) FROM Lot WHERE idArticle = a.idArticle) as stk " +
                     "FROM Item i LEFT JOIN Article a ON i.idArticle=a.idArticle LEFT JOIN Produit p ON a.idProduit=p.idProduit " +
                     "LEFT JOIN Contenant c ON i.idContenant=c.idContenant ORDER BY i.idItem";
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            System.out.println("--- CATALOGUE ---");
            while(rs.next()) {
                String desc = "ARTICLE".equals(rs.getString("typeItem")) ? 
                    rs.getString("nom") + " (" + rs.getString("modeConditionnement") + ") - Stock: " + rs.getInt("stk") :
                    rs.getString("typeContenant") + " (" + rs.getDouble("capacite") + ")";
                System.out.printf("[%d] %s\n", rs.getInt("idItem"), desc);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private static void consulterAlertes(Connection conn) {
        try (Statement s = conn.createStatement(); 
             ResultSet rs = s.executeQuery("SELECT p.nom, l.datePeremption FROM Lot l JOIN Article a ON l.idArticle=a.idArticle JOIN Produit p ON a.idProduit=p.idProduit WHERE l.datePeremption < SYSDATE+7 AND l.quantiteDisponible>0")) {
            System.out.println("--- ALERTES ---");
            while(rs.next()) System.out.println("PERIME BIENTOT : " + rs.getString(1) + " le " + rs.getDate(2));
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
