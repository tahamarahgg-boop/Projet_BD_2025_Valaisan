package model;

import java.sql.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.Scanner;

public class ValaisonJDBC {
    private static final String URL = "jdbc:mysql://localhost:3306/valaison";
    private static final String USER = "marahm";
    private static final String PASSWORD = "marahm";

    private static Connection conn;

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println(" Connecté à la base de données.");

            initialiserBase("Tables.sql");

            menuPrincipal();

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
            System.err.println("Erreur lors de l’exécution du script SQL : " + e.getMessage());
        }
    }

    // Interface texte principale
    private static void menuPrincipal() {
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
                case 1 -> consulterCatalogue();
                case 2 -> passerCommande(scanner);
                case 3 -> consulterAlertes();
                case 4 -> cloturerCommande(scanner);
                case 0 -> System.out.println(" Au revoir !");
                default -> System.out.println(" Choix invalide.");
            }
        } while (choix != 0);
    }

    // Consultation du catalogue
    private static void consulterCatalogue() {
        String sql = "SELECT * FROM produits";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n--- Catalogue ---");
            while (rs.next()) {
                System.out.printf("",
                        rs.getInt(), rs.getString(),
                        rs.getDouble(), rs.getInt());
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la consultation du catalogue : " + e.getMessage());
        }
    }

    // Passer une commande
    private static void passerCommande(Scanner scanner) {
        try {
            System.out.print("Entrez l’ID du produit : ");
            int id = scanner.nextInt();
            System.out.print("Quantité : ");
            int qte = scanner.nextInt();
            scanner.nextLine();
            System.out.print("Mode de paiement (CB, espèces...) : ");
            String paiement = scanner.nextLine();
            System.out.print("Mode de récupération (livraison, retrait...) : ");
            String recup = scanner.nextLine();

            String sql = "INSERT INTO commandes (produit_id, quantite, paiement, mode_recuperation, statut) VALUES (?, ?, ?, ?, 'EN_COURS')";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                pstmt.setInt(2, qte);
                pstmt.setString(3, paiement);
                pstmt.setString(4, recup);
                pstmt.executeUpdate();
                System.out.println(" Commande enregistrée !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la commande : " + e.getMessage());
        }
    }

    // Alertes de péremption
    private static void consulterAlertes() {
        String sql = "SELECT nom, date_peremption FROM produits WHERE date_peremption < NOW() + INTERVAL 7 DAY";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\n--- Produits proches de la péremption ---");
            boolean vide = true;
            while (rs.next()) {
                System.out.printf(" %s — périme le %s\n",
                        rs.getString("nom"), rs.getDate("date_peremption"));
                vide = false;
            }
            if (vide) System.out.println("Aucune alerte pour le moment.");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la consultation des alertes : " + e.getMessage());
        }
    }

    // Clôturer une commande
    private static void cloturerCommande(Scanner scanner) {
        System.out.print("Entrez l’ID de la commande à clôturer : ");
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
