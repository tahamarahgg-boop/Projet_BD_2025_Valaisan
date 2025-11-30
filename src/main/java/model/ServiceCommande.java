package model;

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

    private static final double[] positionBoutique = {45.192534, 5.770541};
    
    private static final List<String> LISTE_DOM_TOM = Arrays.asList(
        "guadeloupe", "martinique", "guyane", "la réunion", "mayotte", 
        "nouvelle-calédonie", "polynésie"
    );

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
                scanner.next();
            }
        } while (choix != -1);

        if (panier.isEmpty()) {
            System.out.println("Panier vide, commande annulee.");
            return;
        }

        scanner.nextLine(); 

        String modeLivraison = "";
        while (true) {
            System.out.println("\n--- MODE DE RECUPERATION ---");
            System.out.println(" - Tapez 'Livraison' (Domicile)");
            System.out.println(" - Tapez 'Boutique' (Retrait)");
            System.out.print("Votre choix : ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("Livraison") || input.equalsIgnoreCase("Boutique")) {
                modeLivraison = input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
                break;
            }
            System.out.println("Choix invalide. Vous devez taper 'Livraison' ou 'Boutique'.");
        }

        String modePaiement = "";
        while (true) {
            System.out.println("\n--- MODE DE PAIEMENT ---");
            System.out.println(" - Tapez 'Carte'");
            System.out.println(" - Tapez 'Especes'");
            System.out.print("Votre choix : ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("Carte") || input.equalsIgnoreCase("Especes")) {
                modePaiement = input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
                break;
            }
            System.out.println(" Choix invalide. Tapez 'Carte' ou 'Especes'.");
        }

        passerCommande(conn, idClient, panier, modeLivraison, modePaiement);
    }

    public void passerCommande(Connection conn, int idClient, Map<Integer, Integer> panier, 
                               String modeLivraison, String modePaiement) {
        try {
            // Niveau d'isolation SERIALIZABLE
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            conn.setAutoCommit(false);
            
            System.out.println("⏳ Traitement de la commande en cours...");

            double montantProduits = 0;
            double fraisPoidsTotal = 0;
            
            //  Map pour capturer les prix au moment de la lecture
            Map<Integer, Double> prixCaptures = new HashMap<>();
            
            for (Map.Entry<Integer, Integer> ligne : panier.entrySet()) {
                int idItem = ligne.getKey();
                int qte = ligne.getValue();

                // Vérifier ET verrouiller le stock
                if (!verifierEtVerrouillerStock(conn, idItem, qte)) {
                    throw new Exception("Item ID " + idItem + " indisponible (Stock insuffisant ou Hors Saison).");
                }

                // Capturer le prix avec verrou
                double prixUnitaire = getPrixItemAvecVerrou(conn, idItem);
                prixCaptures.put(idItem, prixUnitaire);
                montantProduits += (prixUnitaire * qte);
                
                fraisPoidsTotal += calculerFraisPoidsItem(conn, idClient, idItem, qte, modeLivraison);
            }
            
            double fraisDistance = calculerFraisDistance(conn, idClient, modeLivraison);
            double fraisLivraisonFinal = fraisPoidsTotal + fraisDistance;
            if ("Boutique".equalsIgnoreCase(modeLivraison)) {
                fraisLivraisonFinal = 0;
            }
            double total = montantProduits + fraisLivraisonFinal;

            // Créer la commande avec les prix capturés
            creer_commande(conn, idClient, modeLivraison, modePaiement, total, fraisLivraisonFinal, panier, prixCaptures);
            
            conn.commit();
            System.out.printf("✓ Transaction validée ! Montant total : %.2f € (dont %.2f € de livraison)\n", 
                            total, fraisLivraisonFinal);

        } catch (Exception e) {
            try { 
                if (conn != null) {
                    conn.rollback();
                    System.err.println("✗ Commande annulée : " + e.getMessage());
                }
            } catch (SQLException ex) { 
                ex.printStackTrace(); 
            }
        }
    }


    private boolean verifierEtVerrouillerStock(Connection conn, int idItem, int quantite) throws SQLException {
        String sqlType = "SELECT typeItem, idArticle, idContenant FROM Item WHERE idItem = ? FOR UPDATE";
        
        try (PreparedStatement stmt = conn.prepareStatement(sqlType)) {
            stmt.setInt(1, idItem);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String type = rs.getString("typeItem");
                
                if ("ARTICLE".equals(type)) {
                    int idArticle = rs.getInt("idArticle");
                    return verifierEtVerrouillerArticle(conn, idArticle, quantite);
                } else {
                    int idContenant = rs.getInt("idContenant");
                    return verifierEtVerrouillerContenant(conn, idContenant, quantite);
                }
            }
        }
        return false;
    }

    private boolean verifierEtVerrouillerArticle(Connection conn, int idArticle, int quantite) throws SQLException {
        // Vérification saison
        String querySaison = "SELECT 1 FROM est_de_saison e JOIN Article a ON e.idProduit = a.idProduit " +
                             "WHERE a.idArticle = ? AND SYSDATE NOT BETWEEN e.dateDebut AND e.dateFin";
        try (PreparedStatement s = conn.prepareStatement(querySaison)) {
            s.setInt(1, idArticle);
            if (s.executeQuery().next()) {
                System.out.println("⚠ Article " + idArticle + " hors saison.");
                return false;
            }
        }

        // Verrouiller les lignes individuelles, PUIS calculer le total
        String queryStock = "SELECT idLot, quantiteDisponible " +
                          "FROM Lot " +
                          "WHERE idArticle = ? AND quantiteDisponible > 0 " +
                          "ORDER BY datePeremption ASC " +
                          "FOR UPDATE";
        
        double stockTotal = 0;
        try (PreparedStatement s = conn.prepareStatement(queryStock)) {
            s.setInt(1, idArticle);
            ResultSet rs = s.executeQuery();
            
            // Calculer le total manuellement après le verrouillage
            while (rs.next()) {
                stockTotal += rs.getDouble("quantiteDisponible");
            }
            
            if (stockTotal >= quantite) {
                return true;
            }
        }

        // Vérifier produit sur commande
        String queryCommande = "SELECT delaiDisponibilite FROM Article WHERE idArticle = ?";
        try (PreparedStatement s = conn.prepareStatement(queryCommande)) {
            s.setInt(1, idArticle);
            ResultSet rs = s.executeQuery();

            if (rs.next()) {
                int delai = rs.getInt(1);
                if (delai > 0) {
                    System.out.println("[INFO] Article " + idArticle + " disponible sur commande (Delai : " + delai + " jours).");
                    return true;
                }
            }
        }
        
        System.out.println("⚠ Stock insuffisant Article " + idArticle + " (Stock: " + stockTotal + ", Demandé: " + quantite + ")");
        return false;
    }

    private boolean verifierEtVerrouillerContenant(Connection conn, int idContenant, int quantite) throws SQLException {
        String sql = "SELECT stock FROM Contenant WHERE idContenant = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idContenant);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int stock = rs.getInt("stock");
                if (stock >= quantite) return true;
            }
        }
        System.out.println("⚠ Stock insuffisant Contenant " + idContenant);
        return false;
    }

    // Capturer le prix avec verrou
    private double getPrixItemAvecVerrou(Connection conn, int idItem) throws SQLException {
        String sql = "SELECT typeItem, idArticle, idContenant FROM Item WHERE idItem = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idItem);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                if ("ARTICLE".equals(rs.getString("typeItem"))) {
                    return getPrixArticleAvecVerrou(conn, rs.getInt("idArticle"));
                } else {
                    String sqlC = "SELECT prixVente FROM Contenant WHERE idContenant = ? FOR UPDATE";
                    try (PreparedStatement s2 = conn.prepareStatement(sqlC)) {
                        s2.setInt(1, rs.getInt("idContenant"));
                        ResultSet rs2 = s2.executeQuery();
                        if (rs2.next()) return rs2.getDouble(1);
                    }
                }
            }
        }
        return 0;
    }

    private double getPrixArticleAvecVerrou(Connection conn, int idArticle) throws SQLException {
        double prixBase = 0;
        String sql = "SELECT prixVenteClient FROM Article WHERE idArticle = ? FOR UPDATE";
        try (PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, idArticle);
            ResultSet rs = s.executeQuery();
            if (rs.next()) prixBase = rs.getDouble(1);
        }
        
        // Vérifier promotions
        String sqlP = "SELECT pourcentageReduction FROM Lot " +
                     "WHERE idArticle=? AND quantiteDisponible>0 " +
                     "ORDER BY datePeremption ASC";
        try (PreparedStatement s = conn.prepareStatement(sqlP)) {
            s.setInt(1, idArticle);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                double reduc = rs.getDouble(1);
                if (reduc > 0) return prixBase * (1.0 - reduc/100.0);
            }
        }
        return prixBase;
    }

    // Méthodes publiques 
    public boolean est_disponible_item(Connection conn, int idItem, int quantite) throws SQLException {
        String sqlIdentify = "SELECT typeItem, idArticle, idContenant FROM Item WHERE idItem = ?";
        String type = null;
        int refId = 0;

        try (PreparedStatement stmt = conn.prepareStatement(sqlIdentify)) {
            stmt.setInt(1, idItem);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                type = rs.getString("typeItem");
                if ("ARTICLE".equals(type)) refId = rs.getInt("idArticle");
                else refId = rs.getInt("idContenant");
            } else {
                throw new SQLException("Item inconnu : " + idItem);
            }
        }

        if ("ARTICLE".equals(type)) {
            return est_disponible_article(conn, refId, quantite);
        } else {
            return est_disponible_contenant(conn, refId, quantite);
        }
    }

    public boolean est_disponible_article(Connection conn, int idArticle, int quantite) throws SQLException {
        String querySaison = "SELECT 1 FROM est_de_saison e JOIN Article a ON e.idProduit = a.idProduit " +
                             "WHERE a.idArticle = ? AND SYSDATE NOT BETWEEN e.dateDebut AND e.dateFin";
        try (PreparedStatement s = conn.prepareStatement(querySaison)) {
            s.setInt(1, idArticle);
            if (s.executeQuery().next()) {
                System.out.println("⚠ Article " + idArticle + " hors saison.");
                return false;
            }
        }

        String queryStock = "SELECT COALESCE(SUM(quantiteDisponible), 0) FROM Lot WHERE idArticle=?";
        try (PreparedStatement s = conn.prepareStatement(queryStock)) {
            s.setInt(1, idArticle);
            ResultSet rs = s.executeQuery();
            if (rs.next() && rs.getInt(1) >= quantite) return true;
        }

        String queryCommande = "SELECT delaiDisponibilite FROM Article WHERE idArticle = ?";
        try (PreparedStatement s = conn.prepareStatement(queryCommande)) {
            s.setInt(1, idArticle);
            ResultSet rs = s.executeQuery();

            if (rs.next()) {
                int delai = rs.getInt(1);
                if (delai > 0) {
                    System.out.println("[INFO] Article disponible sur commande (Delai : " + delai + " jours).");
                    return true;
                }
            }
        }
        
        System.out.println("⚠ Stock insuffisant Article " + idArticle);
        return false;
    }

    public boolean est_disponible_contenant(Connection conn, int idContenant, int quantite) throws SQLException {
        String sql = "SELECT stock FROM Contenant WHERE idContenant = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idContenant);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int stock = rs.getInt("stock");
                if (stock >= quantite) return true;
            }
        }
        System.out.println("⚠ Stock insuffisant Contenant " + idContenant);
        return false;
    }

    public void creer_commande(Connection conn, int idClient, String modeLivraison, String modePaiement, 
                               double total, double frais, Map<Integer, Integer> panier, 
                               Map<Integer, Double> prixCaptures) throws SQLException {
        String query = "INSERT INTO Commande (idClient, dateCommande, statut, modeRecuperation, modePaiement, montantTotal, fraisLivraison) "
                     + "VALUES (?, SYSDATE, 'En préparation', ?, ?, ?, ?)";
        
        int idCommandeGenere = -1;
        try (PreparedStatement stmt = conn.prepareStatement(query, new String[]{"idCommande"})) {
            stmt.setInt(1, idClient);
            stmt.setString(2, modeLivraison);
            stmt.setString(3, modePaiement);
            stmt.setDouble(4, total);
            stmt.setDouble(5, frais);
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) idCommandeGenere = rs.getInt(1);
        }

        if (idCommandeGenere == -1) throw new SQLException("Erreur ID Commande");

        String queryLigne = "INSERT INTO LigneCommande (idCommande, idItem, quantite, prixUnitaireApplique) " +
                          "VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmtLigne = conn.prepareStatement(queryLigne)) {
            for (Map.Entry<Integer, Integer> entry : panier.entrySet()) {
                int idItem = entry.getKey();
                int quant = entry.getValue();
                
                stmtLigne.setInt(1, idCommandeGenere);
                stmtLigne.setInt(2, idItem);
                stmtLigne.setInt(3, quant);
                stmtLigne.setDouble(4, prixCaptures.getOrDefault(idItem, 0.0));
                stmtLigne.executeUpdate();
            }
        }
    }

    private static double getPrixItem(Connection conn, int idItem) throws SQLException {
        String sql = "SELECT typeItem, idArticle, idContenant FROM Item WHERE idItem = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idItem);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                if ("ARTICLE".equals(rs.getString("typeItem"))) {
                    return getPrixArticle(conn, rs.getInt("idArticle"));
                } else {
                    String sqlC = "SELECT prixVente FROM Contenant WHERE idContenant = ?";
                    try (PreparedStatement s2 = conn.prepareStatement(sqlC)) {
                        s2.setInt(1, rs.getInt("idContenant"));
                        ResultSet rs2 = s2.executeQuery();
                        if (rs2.next()) return rs2.getDouble(1);
                    }
                }
            }
        }
        return 0;
    }

    private static double getPrixArticle(Connection conn, int idArticle) throws SQLException {
        double prixBase = 0;
        String sql = "SELECT prixVenteClient FROM Article WHERE idArticle = ?";
        try (PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, idArticle);
            ResultSet rs = s.executeQuery();
            if (rs.next()) prixBase = rs.getDouble(1);
        }
        
        String sqlP = "SELECT pourcentageReduction FROM Lot WHERE idArticle=? AND quantiteDisponible>0 ORDER BY datePeremption ASC";
        try (PreparedStatement s = conn.prepareStatement(sqlP)) {
            s.setInt(1, idArticle);
            ResultSet rs = s.executeQuery();
            if (rs.next()) {
                double reduc = rs.getDouble(1);
                if (reduc > 0) return prixBase * (1.0 - reduc/100.0);
            }
        }
        return prixBase;
    }

    private double calculerFraisPoidsItem(Connection conn, int idClient, int idItem, int qte, 
                                         String modeLivraison) throws SQLException {
        if ("Boutique".equalsIgnoreCase(modeLivraison)) return 0;
        
        String sql = "SELECT typeItem, idArticle FROM Item WHERE idItem = ?";
        try (PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, idItem);
            ResultSet rs = s.executeQuery();
            if (rs.next() && "ARTICLE".equals(rs.getString("typeItem"))) {
                int idArt = rs.getInt("idArticle");
                String sqlP = "SELECT a.poids, a.modeConditionnement, ad.pays " +
                            "FROM Article a, Adresse ad " +
                            "WHERE a.idArticle=? AND ad.idClient=?";
                try (PreparedStatement s2 = conn.prepareStatement(sqlP)) {
                    s2.setInt(1, idArt);
                    s2.setInt(2, idClient);
                    ResultSet rs2 = s2.executeQuery();
                    if (rs2.next()) {
                        double poidsUnit = "Vrac".equalsIgnoreCase(rs2.getString("modeConditionnement")) 
                                         ? 1.0 : rs2.getDouble("poids");
                        double poidsTotal = (poidsUnit == 0 ? 1 : poidsUnit) * qte;
                        
                        String pays = rs2.getString("pays").toLowerCase();
                        double tarif = 0.50;
                        if (LISTE_DOM_TOM.contains(pays)) tarif = 5.00;
                        else if (!pays.contains("france")) tarif = 8.00;
                        
                        return poidsTotal * tarif;
                    }
                }
            }
        }
        return 0;
    }
    
    private double calculerFraisDistance(Connection conn, int idClient, String modeLivraison) throws SQLException {
        if ("Boutique".equalsIgnoreCase(modeLivraison)) return 0;
        String sql = "SELECT pays, latitude, longitude FROM Adresse WHERE idClient = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idClient);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double[] posClient = {rs.getDouble("latitude"), rs.getDouble("longitude")};
                int dist = GestionLivraison.calculdistance(posClient, positionBoutique);
                String pays = rs.getString("pays").toLowerCase();
                double prixKm = 0.05;
                if (LISTE_DOM_TOM.contains(pays)) prixKm = 0.15;
                else if (!pays.contains("france")) prixKm = 0.20;
                return dist * prixKm;
            }
        }
        return 0;
    }
}
