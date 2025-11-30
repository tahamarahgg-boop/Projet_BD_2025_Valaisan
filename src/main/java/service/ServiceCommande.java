package service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ServiceCommande {

    // Coordonnées de la boutique (Grenoble) pour le calcul de distance
    private static final double[] positionBoutique = {45.192534, 5.770541};
    
    private static final List<String> LISTE_DOM_TOM = Arrays.asList(
        "guadeloupe", "martinique", "guyane", "la réunion", "mayotte", 
        "nouvelle-calédonie", "polynésie"
    );

    // --- POINT D'ENTRÉE UTILISATEUR (IHM Console) ---

    public void passerCommandeDebut(Connection conn, int idClient) {
        Scanner scanner = new Scanner(System.in);
        Map<Integer, Integer> panier = new HashMap<>();

        System.out.println("--- DEBUT DE COMMANDE ---");
        System.out.println("Veuillez saisir les Identifiants (ID ITEM) du catalogue.");
        
        int choix = 0;
        do {
            System.out.print("Entrez l'ID de l'ITEM (ou -1 pour terminer) : ");
            if (scanner.hasNextInt()) {
                choix = scanner.nextInt();
                if (choix != -1) {
                    System.out.print("Saisissez la quantite : ");
                    int quantite = scanner.nextInt();
                    panier.put(choix, panier.getOrDefault(choix, 0) + quantite);
                    System.out.println("-> Item ajoute.");
                }
            } else {
                System.out.println("Veuillez entrer un nombre.");
                scanner.next(); // Vider le buffer
            }
        } while (choix != -1);

        if (panier.isEmpty()) {
            System.out.println("Panier vide, commande annulee.");
            return;
        }
        scanner.nextLine(); // Consommer le reste de la ligne

        // Choix Mode Livraison
        String modeLivraison = choixOption(scanner, "MODE DE RECUPERATION", "Livraison", "Boutique");
        
        // Choix Mode Paiement
        String modePaiement = choixOption(scanner, "MODE DE PAIEMENT", "Carte", "Especes");

        passerCommande(conn, idClient, panier, modeLivraison, modePaiement);
    }

    private String choixOption(Scanner sc, String titre, String opt1, String opt2) {
        while (true) {
            System.out.println("\n--- " + titre + " ---");
            System.out.println(" - " + opt1);
            System.out.println(" - " + opt2);
            System.out.print("Votre choix : ");
            String input = sc.nextLine().trim();
            if (input.equalsIgnoreCase(opt1) || input.equalsIgnoreCase(opt2)) {
                return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
            }
            System.out.println("Choix invalide.");
        }
    }

    // --- CŒUR DE LA TRANSACTION ---

    public void passerCommande(Connection conn, int idClient, Map<Integer, Integer> panier, 
                               String modeLivraison, String modePaiement) {
        // On utilise CloturerCommande pour obtenir les prix de manière cohérente
        CloturerCommande staffOutils = new CloturerCommande();
        
        try {
            conn.setAutoCommit(false);
            // Isolation standard : le client ne bloque pas les autres
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            
            System.out.println("Traitement de la commande en cours...");

            double total = 0;
            double fraisPoids = 0;
            Map<Integer, Double> prixCaptures = new HashMap<>();
            
            for (Map.Entry<Integer, Integer> ligne : panier.entrySet()) {
                int idItem = ligne.getKey();
                int qte = ligne.getValue();

                // 1. Vérification Disponibilité (Lecture Seule - Pas de blocage)
                if (!est_disponible(conn, idItem, qte)) {
                    throw new SQLException("Item " + idItem + " indisponible (Stock/Saison/Délai).");
                }

                // 2. Calcul Prix & Frais
                double prixUnit = staffOutils.getPrixItem(conn, idItem);
                prixCaptures.put(idItem, prixUnit);
                
                total += prixUnit * qte;
                fraisPoids += calculerFraisPoidsItem(conn, idClient, idItem, qte, modeLivraison);
            }
            
            double fraisDist = calculerFraisDistance(conn, idClient, modeLivraison);
            double fraisTotal = ("Boutique".equalsIgnoreCase(modeLivraison)) ? 0 : (fraisPoids + fraisDist);
            double finalTotal = total + fraisTotal;

            // 3. Création Commande en base
            creer_commande(conn, idClient, modeLivraison, modePaiement, finalTotal, fraisTotal, panier, prixCaptures);
            
            conn.commit();
            System.out.printf(" Transaction validée ! Montant total : %.2f € (dont %.2f € de livraison)\n", 
                            finalTotal, fraisTotal);

        } catch (Exception e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) {}
            System.err.println(" Commande annulée : " + e.getMessage());
        }
    }

    // --- VÉRIFICATION DISPONIBILITÉ ---

    private boolean est_disponible(Connection conn, int idItem, int qte) throws SQLException {
        String sql = "SELECT typeItem, idArticle, idContenant FROM Item WHERE idItem = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idItem);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if ("ARTICLE".equals(rs.getString("typeItem"))) {
                    int idArt = rs.getInt("idArticle");
                    
                    // A. Check SAISON
                    String sqlSaison = "SELECT e.dateDebut, e.dateFin FROM est_de_saison e " + 
                                       "JOIN Article a ON e.idProduit=a.idProduit " + 
                                       "WHERE a.idArticle=? AND SYSDATE NOT BETWEEN e.dateDebut AND e.dateFin";
                    try(PreparedStatement s2 = conn.prepareStatement(sqlSaison)) {
                         s2.setInt(1, idArt); 
                         ResultSet rsS = s2.executeQuery();
                         if(rsS.next()) {
                             System.out.println(" [REFUS] Article " + idArt + " hors saison (Dispo : " + rsS.getDate(1) + " au " + rsS.getDate(2) + ")");
                             return false;
                         }
                    }

                    // B. Check STOCK PHYSIQUE
                    int stockPhysique = 0;
                    try(PreparedStatement s3 = conn.prepareStatement("SELECT COALESCE(SUM(quantiteDisponible),0) FROM Lot WHERE idArticle=?")) {
                        s3.setInt(1, idArt); 
                        ResultSet rs3 = s3.executeQuery();
                        if(rs3.next()) stockPhysique = rs3.getInt(1);
                    }
                    if (stockPhysique >= qte) return true;

                    // C. Check SUR COMMANDE
                    try(PreparedStatement s4 = conn.prepareStatement("SELECT delaiDisponibilite FROM Article WHERE idArticle=?")) {
                        s4.setInt(1, idArt); 
                        ResultSet rs4 = s4.executeQuery();
                        if(rs4.next()) {
                            int delai = rs4.getInt(1);
                            if (delai > 0) {
                                System.out.println(" [INFO] Stock épuisé, mais disponible sous " + delai + " jours.");
                                return true;
                            }
                        }
                    }
                    System.out.println(" [REFUS] Stock insuffisant (" + stockPhysique + ") et non commandable.");
                    return false;

                } else {
                    // Contenant
                    try(PreparedStatement sC = conn.prepareStatement("SELECT stock FROM Contenant WHERE idContenant=?")) {
                        sC.setInt(1, rs.getInt("idContenant")); ResultSet rsC = sC.executeQuery();
                        return rsC.next() && rsC.getInt(1) >= qte;
                    }
                }
            }
        }
        return false;
    }

    // --- INSERTION BDD ---

    private void creer_commande(Connection conn, int idClient, String modeLivr, String modePaie, 
                                double total, double frais, Map<Integer, Integer> panier, 
                                Map<Integer, Double> prixCaptures) throws SQLException {
        
        String query = "INSERT INTO Commande (idClient, dateCommande, statut, modeRecuperation, modePaiement, montantTotal, fraisLivraison) "
                     + "VALUES (?, SYSDATE, 'En préparation', ?, ?, ?, ?)";
        
        int idCmd = -1;
        try (PreparedStatement stmt = conn.prepareStatement(query, new String[]{"idCommande"})) {
            stmt.setInt(1, idClient);
            stmt.setString(2, modeLivr);
            stmt.setString(3, modePaie);
            stmt.setDouble(4, total);
            stmt.setDouble(5, frais);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) idCmd = rs.getInt(1);
        }

        if (idCmd == -1) throw new SQLException("Echec création ID Commande");

        String queryLigne = "INSERT INTO LigneCommande (idCommande, idItem, quantite, prixUnitaireApplique) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmtLigne = conn.prepareStatement(queryLigne)) {
            for (Map.Entry<Integer, Integer> entry : panier.entrySet()) {
                stmtLigne.setInt(1, idCmd);
                stmtLigne.setInt(2, entry.getKey());
                stmtLigne.setInt(3, entry.getValue());
                stmtLigne.setDouble(4, prixCaptures.get(entry.getKey()));
                stmtLigne.executeUpdate();
            }
        }
    }

    // --- CALCULS FRAIS DE PORT ---

    private double calculerFraisPoidsItem(Connection conn, int idClient, int idItem, int qte, String mode) throws SQLException {
        if ("Boutique".equalsIgnoreCase(mode)) return 0;
        
        String sql = "SELECT i.typeItem, a.poids, ad.pays FROM Item i " + 
                     "LEFT JOIN Article a ON i.idArticle=a.idArticle " + 
                     "JOIN Adresse ad ON ad.idClient = ? WHERE i.idItem = ?";    
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idClient); ps.setInt(2, idItem);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && "ARTICLE".equals(rs.getString("typeItem"))) {
                double poids = rs.getDouble("poids");
                if (poids == 0) poids = 1.0; // Poids par défaut si vrac/inconnu

                String pays = rs.getString("pays").toLowerCase();
                double tarif = 0.50; // France
                if (LISTE_DOM_TOM.contains(pays)) tarif = 5.00;
                else if (!pays.contains("france")) tarif = 8.00;
                
                return poids * qte * tarif;
            }
        }
        return 0;
    }
    
    private double calculerFraisDistance(Connection conn, int idClient, String mode) throws SQLException {
        if ("Boutique".equalsIgnoreCase(mode)) return 0;
        String sql = "SELECT pays, latitude, longitude FROM Adresse WHERE idClient = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idClient);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double[] posClient = {rs.getDouble("latitude"), rs.getDouble("longitude")};
                int dist = GestionLivraison.calculdistance(posClient, positionBoutique);
                
                String pays = rs.getString("pays").toLowerCase();
                double prixKm = 0.05; // France
                if (LISTE_DOM_TOM.contains(pays)) prixKm = 0.15;
                else if (!pays.contains("france")) prixKm = 0.20;
                
                return dist * prixKm;
            }
        }
        return 5.00; // Forfait sécurité
    }
}