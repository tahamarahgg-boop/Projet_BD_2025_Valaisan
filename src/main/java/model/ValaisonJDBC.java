package model;

import java.sql.*;
import java.util.Scanner;

public class ValaisonJDBC {
    
    // --- CONFIGURATION DE LA CONNEXION ---
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USER = "system";     // Utilisateur validé ensemble
    private static final String PASSWORD = "lahmouza"; // Mot de passe validé ensemble

    public static void main(String[] args) {
        try {
            // Chargement du driver Oracle
            Class.forName("oracle.jdbc.OracleDriver");
            
            // Connexion à la base
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[OK] Connecte a la base de donnees.");

            // Initialisation du scanner pour les entrées clavier
            Scanner scanner = new Scanner(System.in);
            
            System.out.print("Veuillez entrer votre ID Client : ");
            if (scanner.hasNextInt()) {
                int idClient = scanner.nextInt();
                scanner.nextLine(); // Consommer le saut de ligne restant
                
                // Lancement du menu principal
                menuPrincipal(conn, scanner, idClient);
            } else {
                System.out.println("[ERREUR] ID invalide (doit etre un nombre).");
            }

            // Fermeture propre
            conn.close();
            scanner.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- MENU PRINCIPAL ---
    private static void menuPrincipal(Connection conn, Scanner scanner, int idClient) {
        // Instanciation des services métiers
        ServiceCommande serviceCreation = new ServiceCommande();
        CloturerCommande serviceGestion = new CloturerCommande();

        int choix;
        do {
            System.out.println("\n==========================================");
            System.out.println("       VALAISON - MENU PRINCIPAL");
            System.out.println("==========================================");
            System.out.println("1. Consulter le catalogue (Articles & Contenants)");
            System.out.println("2. Passer une commande");
            System.out.println("3. Consulter les alertes de peremption");
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
                        // Appel au service de création de commande
                        serviceCreation.passerCommandeDebut(idClient);
                        break;
                    case 3:
                        consulterAlertes(conn);
                        break;
                    case 4:
                        // Appel au service de gestion pour l'annulation
                        menuAnnulation(conn, scanner, serviceGestion);
                        break;
                    case 5:
                        // Sous-menu pour le personnel du magasin
                        menuStaff(conn, scanner, serviceGestion);
                        break;
                    case 0:
                        System.out.println("Au revoir !");
                        break;
                    default:
                        System.out.println("[ERREUR] Choix invalide.");
                }
            } catch (SQLException e) {
                System.out.println("[ERREUR SQL CRITIQUE] " + e.getMessage());
            }
        } while (choix != 0);
    }

    // --- SOUS-MENU : ANNULATION ---
    private static void menuAnnulation(Connection conn, Scanner scanner, CloturerCommande gestion) throws SQLException {
        System.out.println("\n--- ANNULATION DE COMMANDE ---");
        System.out.print("Entrez l'ID de la commande a annuler : ");
        try {
            int idCmd = Integer.parseInt(scanner.nextLine());
            gestion.annulerCommande(conn, idCmd);
        } catch (NumberFormatException e) {
            System.out.println("[ERREUR] ID invalide.");
        }
    }

    // --- SOUS-MENU : STAFF ---
    private static void menuStaff(Connection conn, Scanner scanner, CloturerCommande gestion) throws SQLException {
        System.out.println("\n--- ESPACE STAFF (Gestion des Commandes) ---");
        System.out.print("Entrez l'ID de la commande a traiter : ");
        int idCmd;
        try {
            idCmd = Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("[ERREUR] ID invalide.");
            return;
        }

        System.out.println("Action a effectuer ?");
        System.out.println("1. Preparer la commande (Passe a 'Prete' + Sortie Stock)");
        System.out.println("2. Expedier la commande (Passe a 'En livraison')");
        System.out.println("3. Confirmer la livraison (Passe a 'Livree')");
        System.out.println("4. Confirmer le retrait boutique (Passe a 'Recuperee')");
        System.out.println("0. Retour au menu principal");
        System.out.print("Choix : ");

        try {
            int choixStaff = Integer.parseInt(scanner.nextLine());
            switch (choixStaff) {
                case 1:
                    gestion.commandePrete(conn, idCmd);
                    break;
                case 2:
                    gestion.commandeEnLivraison(conn, idCmd);
                    break;
                case 3:
                    gestion.commandeLivree(conn, idCmd);
                    break;
                case 4:
                    gestion.commandeRecuperee(conn, idCmd);
                    break;
                case 0:
                    break; // Retour
                default:
                    System.out.println("[ERREUR] Choix invalide.");
            }
        } catch (NumberFormatException e) {
            System.out.println("[ERREUR] Entree invalide.");
        }
    }

    // --- FONCTIONNALITE : CATALOGUE ---
    private static void consulterCatalogue(Connection conn) {
        // Requête complexe pour fusionner Articles et Contenants via la table Item
        String sql = 
            "SELECT i.idItem, i.typeItem, " +
            "       p.nom AS nomArticle, a.modeConditionnement, a.prixVenteClient, a.delaiDisponibilite, " +
            "       (SELECT SUM(quantiteDisponible) FROM Lot WHERE idArticle = a.idArticle) as stockArticle, " +
            "       c.typeContenant, c.capacite, c.prixVente as prixContenant, c.stock as stockContenant " +
            "FROM Item i " +
            "LEFT JOIN Article a ON i.idArticle = a.idArticle " +
            "LEFT JOIN Produit p ON a.idProduit = p.idProduit " +
            "LEFT JOIN Contenant c ON i.idContenant = c.idContenant " +
            "ORDER BY i.typeItem DESC, nomArticle, typeContenant";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            String separateur = "+-------+-----------+------------------------------+------------------+----------+------------------------+";
            System.out.println("\n--- CATALOGUE GENERAL (ITEMS) ---");
            System.out.println(separateur);
            // Formatage des colonnes
            System.out.printf("| %-5s | %-9s | %-28s | %-16s | %-8s | %-22s |\n", 
                              "ID", "Type", "Nom / Description", "Condit.", "Prix", "Disponibilite");
            System.out.println(separateur);

            boolean vide = true;
            while (rs.next()) {
                vide = false;
                int idItem = rs.getInt("idItem");
                String type = rs.getString("typeItem");
                
                String nom = "";
                String cond = "-";
                double prix = 0;
                String dispo = "";

                // Logique d'affichage selon si c'est un Article ou un Contenant
                if ("ARTICLE".equals(type)) {
                    nom = rs.getString("nomArticle");
                    cond = rs.getString("modeConditionnement");
                    prix = rs.getDouble("prixVenteClient");
                    
                    double stock = rs.getDouble("stockArticle");
                    int delai = rs.getInt("delaiDisponibilite");
                    
                    if (stock > 0) dispo = "En stock: " + stock;
                    else if (delai > 0) dispo = "Sous " + delai + " jours";
                    else dispo = "Epuise";
                } else {
                    // C'est un CONTENANT
                    nom = rs.getString("typeContenant") + " (" + rs.getDouble("capacite") + ")";
                    cond = "Unite";
                    prix = rs.getDouble("prixContenant");
                    int stock = rs.getInt("stockContenant");
                    dispo = (stock > 0) ? "En stock: " + stock : "Epuise";
                }

                // Affichage de la ligne
                System.out.printf("| %-5d | %-9s | %-28.28s | %-16.16s | %8.2f | %-22.22s |\n",
                        idItem, 
                        type.length() > 9 ? type.substring(0, 9) : type, // Tronque si trop long
                        nom, cond, prix, dispo);
            }
            System.out.println(separateur);
            
            if (vide) {
                System.out.println("[INFO] Le catalogue est vide.");
            }

        } catch (SQLException e) {
            System.err.println("[ERREUR SQL] Impossible de lire le catalogue : " + e.getMessage());
        }
    }

    // --- FONCTIONNALITE : ALERTES ---
    private static void consulterAlertes(Connection conn) {
        // Requête pour trouver les produits périmés dans moins de 7 jours
        String sql = "SELECT p.nom, l.datePeremption, l.quantiteDisponible " +
                     "FROM Lot l " +
                     "JOIN Article a ON l.idArticle = a.idArticle " +
                     "JOIN Produit p ON a.idProduit = p.idProduit " +
                     "WHERE l.datePeremption < SYSDATE + 7 " +
                     "AND l.quantiteDisponible > 0 " +
                     "ORDER BY l.datePeremption ASC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("\n--- ALERTES ANTI-GASPI (Peremption < 7 jours) ---");
            boolean vide = true;
            while (rs.next()) {
                System.out.printf(" - %-20s : Reste %-5s unites (Perime le %s)\n",
                        rs.getString("nom"), 
                        rs.getString("quantiteDisponible"),
                        rs.getDate("datePeremption"));
                vide = false;
            }
            if (vide) System.out.println("[OK] Aucune alerte de peremption.");
            
        } catch (SQLException e) {
            System.err.println("[ERREUR SQL] Impossible de lire les alertes : " + e.getMessage());
        }
    }
}