package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

import service.CloturerCommande;
import service.ServiceAlerte;
import service.ServiceCatalogue;
import service.ServiceCommande;

public class ValaisonJDBC {
    
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USER = "system";     
    private static final String PASSWORD = "lahmouza"; 

    public static void main(String[] args) {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            conn.setAutoCommit(false);
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
                CloturerCommande serviceStaff = new CloturerCommande();
                switch (choix) {
                    case 1:
                    	ServiceCatalogue catalogue = new ServiceCatalogue();
                        catalogue.consulterCatalogue(conn);
                        break;
                    case 2:
                        // Utilise ServiceCommande
                        ServiceCommande serviceClient = new ServiceCommande();
                        serviceClient.passerCommandeDebut(conn,idClient);
                        break;
                    case 3:
                    	ServiceAlerte alerte = new ServiceAlerte(conn);
                        alerte.consulterAlerte();
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
        System.out.println("\n--- ESPACE CLIENT : ANNULATION ---");
        System.out.print("ID Commande à annuler : ");
        try {
            int idCmd = Integer.parseInt(scanner.nextLine());
            // FALSE car c'est le client
            staff.annulerCommande(conn, idCmd, false); 
        } catch (NumberFormatException e) {
            System.out.println("ID invalide.");
        }
    }

    private static void menuStaff(Connection conn, Scanner scanner, CloturerCommande staff) throws SQLException {
        System.out.println("\n--- ESPACE STAFF ---");
        
        // 1. Saisie ID Commande
        System.out.print("ID Commande : ");
        int idCmd;
        try {
            idCmd = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID invalide.");
            return;
        }

        // 2. Saisie ID Staff (Nécessaire pour la traçabilité de la préparation)
        System.out.print("Veuillez entrer votre ID Staff : ");
        int idStaff = 0;
        try {
            idStaff = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("ID Staff invalide (0 par défaut).");
        }

        System.out.println("-----------------------------------");
        System.out.println("1. Préparer (Sortie Stock -> Prête)");
        System.out.println("2. Expédier (-> En livraison)");
        System.out.println("3. Livrer (-> Livrée)");
        System.out.println("4. Retrait Boutique (-> Récupérée)");
        System.out.println("5. ANNULER une commande (Rupture/Non-retrait)"); // Ajout de l'affichage
        System.out.println("0. Retour");
        System.out.println("-----------------------------------");
        System.out.print("Choix : ");

        try {
            int c = Integer.parseInt(scanner.nextLine());
            switch (c) {
                // On passe idStaff ici
                case 1: staff.commandePrete(conn, idCmd, idStaff); break;
                
                case 2: staff.commandeEnLivraison(conn, idCmd); break;
                case 3: staff.commandeLivree(conn, idCmd); break;
                case 4: staff.commandeRecuperee(conn, idCmd); break;
                
                // Annulation par Staff
                case 5: 
                    System.out.println("Annulation forcée par le Staff...");
                    // true = C'est le staff, donc droit d'annuler même si "Prête"
                    staff.annulerCommande(conn, idCmd, true); 
                    break;
                    
                case 0: break;
                default: System.out.println("Choix inconnu.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Choix invalide.");
        }
    }
    
}
