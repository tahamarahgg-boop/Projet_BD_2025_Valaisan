package model;

import java.sql.Connection;
import java.sql.DriverManager;
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

    // --- LA TRANSACTION PRINCIPALE ---
    public void passerCommandeDebut(int idClient) {
        Scanner scanner = new Scanner(System.in);
        int choix;
        Map<Integer, Integer> panier = new HashMap<>(); // Associe IdItem à qte demandée

        System.out.println("--- DÉBUT DE COMMANDE ---");
        System.out.println("Veuillez saisir les Identifiants (ID ITEM) du catalogue.");
        do {
            System.out.print("Entrez l'ID de l'ITEM (ou -1 pour terminer) : ");
            choix = scanner.nextInt();
            if (choix == -1) continue;
            
            System.out.print("Saisissez la quantité : ");
            int quantite = scanner.nextInt();
            
            panier.put(choix, panier.getOrDefault(choix, 0) + quantite);
            System.out.println("-> Item ajouté au panier.");
        } while (choix != -1);

        if (panier.isEmpty()) {
            System.out.println("Panier vide, commande annulée.");
            return;
        }

        scanner.nextLine(); 
        
        System.out.println("\n--- OPTIONS DE Recupération ---");
        System.out.println(" - Tapez 'Livraison' pour domicile");
        System.out.println(" - Tapez 'Boutique' pour retrait");
        System.out.print("Votre choix : ");
        String modeLivraison = scanner.nextLine();

        System.out.println("\n--- MODE DE PAIEMENT ---");
        System.out.println(" - Tapez 'En ligne'");
        System.out.println(" - Tapez 'En boutique'");
        System.out.print("Votre choix : ");
        String modePaiement = scanner.nextLine();

        passerCommande(idClient, panier,modeLivraison,modePaiement);
    }

    public void passerCommande(int idClient, Map<Integer, Integer> panier,String modeLivraison,String modePaiement) {
        Connection conn = null;
        try {
            // Connection Oracle
            String url = "jdbc:oracle:thin:@localhost:1521:XE"; 
            conn = DriverManager.getConnection(url, "system", "lahmouza");
            conn.setAutoCommit(false); 
            System.out.println("⏳ Traitement de la commande en cours...");

            double montantProduits = 0;
            double fraisPoidsTotal = 0;
            for (Map.Entry<Integer, Integer> ligne : panier.entrySet()) {
                int idItem = ligne.getKey();
                int qte = ligne.getValue();

                //  Vérif Stock / Saison
                if (!est_disponible_item(conn, idItem, qte)) {
                    throw new Exception("Item ID " + idItem + " indisponible (Stock insuffisant ou Hors Saison).");
                }


                //  Prix par Item 
                double prixUnitaire = getPrixItem(conn, idItem);
                montantProduits += (prixUnitaire * qte);
                //  Calcul Poids
                fraisPoidsTotal += calculerFraisPoidsItem(conn, idClient, idItem, qte, modeLivraison);
            }
            // Frais Total
            double fraisDistance = calculerFraisDistance(conn, idClient, modeLivraison);
            double fraisLivraisonFinal = fraisPoidsTotal + fraisDistance;
            if ("Boutique".equalsIgnoreCase(modeLivraison)){
                fraisLivraisonFinal=0; // retrait en boutique
            }
            double total = montantProduits + fraisLivraisonFinal;

            creer_commande(conn, idClient, modeLivraison, modePaiement, total, fraisLivraisonFinal, panier);
            conn.commit();
            System.out.printf("✅ Transaction validée ! Montant total : %.2f € (dont %.2f € de livraison)\n", total, fraisLivraisonFinal);

        } catch (Exception e) {
            try { 
                if (conn != null) conn.rollback(); 
                System.err.println("❌ Commande annulée : " + e.getMessage());
            } catch (SQLException ex) { ex.printStackTrace(); }
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }

    // --- MÉTHODES INTERNES ---

    public static boolean est_disponible_item(Connection conn, int idItem, int quantite) throws SQLException {
        // type d'item
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

        // Vérification selon le type
        if ("ARTICLE".equals(type)) {
            return est_disponible_article(conn, refId, quantite);
        } else {
            return est_disponible_contenant(conn, refId, quantite);
        }
    }

    // Vérif spécifique Article (Lot + Saison + Sur commande)
    private static boolean est_disponible_article(Connection conn, int idArticle, int quantite) throws SQLException {
        // Hors Saison
        String querySaison = "SELECT 1 FROM est_de_saison e JOIN Article a ON e.idProduit = a.idProduit " +
                             "WHERE a.idArticle = ? AND SYSDATE NOT BETWEEN e.dateDebut AND e.dateFin";
        try (PreparedStatement s = conn.prepareStatement(querySaison)) {
            s.setInt(1, idArticle);
            if (s.executeQuery().next()) {
                System.out.println("⛔ Article " + idArticle + " hors saison.");
                return false;
            }
        }

        // Stock Lot
        String queryStock = "SELECT COALESCE(SUM(quantiteDisponible), 0) FROM Lot WHERE idArticle=?";
        try (PreparedStatement s = conn.prepareStatement(queryStock)) {
            s.setInt(1, idArticle);
            ResultSet rs = s.executeQuery();
            if (rs.next() && rs.getInt(1) >= quantite) return true;
        }

        // Produit sur commande
        String queryCommande = "SELECT delaiDisponibilite FROM Article WHERE idArticle = ?";
        try (PreparedStatement s = conn.prepareStatement(queryCommande)) {
            s.setInt(1, idArticle);
            ResultSet rs = s.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("ℹ️ Article " + idArticle + " dispo sur commande.");
                return true;
            }
        }
        
        System.out.println("⚠️ Stock insuffisant Article " + idArticle);
        return false;
    }

    // Vérif spécifique Contenant
    private static boolean est_disponible_contenant(Connection conn, int idContenant, int quantite) throws SQLException {
        String sql = "SELECT stock FROM Contenant WHERE idContenant = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idContenant);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int stock = rs.getInt("stock");
                if (stock >= quantite) return true;
            }
        }
        System.out.println("⚠️ Stock insuffisant Contenant " + idContenant);
        return false;
    }

    private static void creer_commande(Connection conn, int idClient, String modeLivraison, String modePaiement, double total, double frais, Map<Integer, Integer> panier) throws SQLException {
        // 1. Insertion Commande
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

        // 2. Insertion Lignes
        String queryLigne = "INSERT INTO LigneCommande (idCommande, idItem, quantite, prixUnitaireApplique) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement stmtLigne = conn.prepareStatement(queryLigne)) {
            for (Map.Entry<Integer, Integer> entry : panier.entrySet()) {
                int idItem = entry.getKey();
                int quant = entry.getValue();
                
                stmtLigne.setInt(1, idCommandeGenere);
                stmtLigne.setInt(2, idItem);
                stmtLigne.setInt(3, quant);
                stmtLigne.setDouble(4, getPrixItem(conn, idItem));
                stmtLigne.executeUpdate();
                
            }
        }
    }

    // --- Outils Polymorphes ---

    private static double getPrixItem(Connection conn, int idItem) throws SQLException {
        // Récupère le prix (Soit Article avec promo, soit Contenant)
        String sql = "SELECT typeItem, idArticle, idContenant FROM Item WHERE idItem = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idItem);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                if ("ARTICLE".equals(rs.getString("typeItem"))) {
                    return getPrixArticle(conn, rs.getInt("idArticle"));
                } else {
                    // Prix contenant
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
        // Prix Article =Base - Promo
        double prixBase = 0;
        String sql = "SELECT prixVenteClient FROM Article WHERE idArticle = ?";
        try (PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, idArticle);
            ResultSet rs = s.executeQuery();
            if (rs.next()) prixBase = rs.getDouble(1);
        }
        // Promo ?
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
/* 
    private void decrementerStockItem(Connection conn, int idItem, int qte) throws SQLException {
        String sql = "SELECT typeItem, idArticle, idContenant FROM Item WHERE idItem = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idItem);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Article 
                if ("ARTICLE".equals(rs.getString("typeItem"))) {
                    decrementerStockArticle(conn, rs.getInt("idArticle"), qte);
                } else {
                    // Contenant
                    String up = "UPDATE Contenant SET stock = stock - ? WHERE idContenant = ?";
                    try(PreparedStatement s = conn.prepareStatement(up)) {
                        s.setInt(1, qte);
                        s.setInt(2, rs.getInt("idContenant"));
                        s.executeUpdate();
                    }
                }
            }
        }
    }

    private void decrementerStockArticle(Connection conn, int idArticle, int qte) throws SQLException {
        String sql = "SELECT idLot, quantiteDisponible FROM Lot WHERE idArticle=? AND quantiteDisponible>0 ORDER BY datePeremption ASC";
        try (PreparedStatement s = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            s.setInt(1, idArticle);
            ResultSet rs = s.executeQuery();
            double reste = qte;
            while (rs.next() && reste > 0) {
                int idLot = rs.getInt(1);
                double dispo = rs.getDouble(2);
                double pris = Math.min(dispo, reste);
                
                try(PreparedStatement up = conn.prepareStatement("UPDATE Lot SET quantiteDisponible = quantiteDisponible - ? WHERE idLot=?")) {
                    up.setDouble(1, pris);
                    up.setInt(2, idLot);
                    up.executeUpdate();
                }
                reste -= pris;
            }
        }
    }

 */
    private double calculerFraisPoidsItem(Connection conn, int idClient, int idItem, int qte, String modeLivraison) throws SQLException {
        if ("Boutique".equalsIgnoreCase(modeLivraison)) return 0;
        
        // On ne calcule le poids que pour les Articles
        String sql = "SELECT typeItem, idArticle FROM Item WHERE idItem = ?";
        try(PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, idItem);
            ResultSet rs = s.executeQuery();
            if (rs.next() && "ARTICLE".equals(rs.getString("typeItem"))) {
                int idArt = rs.getInt("idArticle");
                // Calcul poids article
                String sqlP = "SELECT a.poids, a.modeConditionnement, ad.pays FROM Article a, Adresse ad WHERE a.idArticle=? AND ad.idClient=?";
                try(PreparedStatement s2 = conn.prepareStatement(sqlP)) {
                    s2.setInt(1, idArt);
                    s2.setInt(2, idClient);
                    ResultSet rs2 = s2.executeQuery();
                    if (rs2.next()) {
                        double poidsUnit = "Vrac".equalsIgnoreCase(rs2.getString("modeConditionnement")) ? 1.0 : rs2.getDouble("poids");
                        double poidsTotal = (poidsUnit == 0 ? 1 : poidsUnit) * qte; // Si Vrac, qte est en Kg
                        
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