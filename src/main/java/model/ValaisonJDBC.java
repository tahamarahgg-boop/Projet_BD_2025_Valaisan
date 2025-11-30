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

            // Correction : Chemin complet vers le fichier DDL
            initialiserBase("Tables.sql");
            initialiserBase("Données.sql");

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
