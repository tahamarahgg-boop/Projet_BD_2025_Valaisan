package model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class CloturerCommande {
    
    // changer le status de En Preparation à Prete par le staff du magasin && decrementer le stock 
    public void commandePrete(Connection conn,int idCommande) throws SQLException{
        conn.setAutoCommit(false);
        Scanner scanner=new Scanner(System.in);
        System.out.println("Entrer votre ID Staff");
        scanner.nextInt();
        /* valider id ( à faire après) */

        // valider l'existence de la commande 
        // empêche les autres sessions de la modifier jusqu'au commit
        String selectVerrou = "SELECT idCommande, statut FROM Commande WHERE idCommande = ? FOR UPDATE";
        try (PreparedStatement lockStmt = conn.prepareStatement(selectVerrou)) {
            lockStmt.setInt(1, idCommande);
            ResultSet rs = lockStmt.executeQuery();
            
            if (rs.next()) {
                String statut = rs.getString("statut");
                System.out.println(" Commande " + idCommande + " verrouillée (Statut actuel: " + statut + ")");
            
                // vérifier si la commande peut être modifiée
                if (statut.equalsIgnoreCase("En préparation")) {

                    // Récupérer les articles de la commande pour décrémenter le stock
                    String sqlLignes = "SELECT idItem, quantite FROM LigneCommande WHERE idCommande = ?";
                    Map<Integer, Integer> articlesADecrementer = new HashMap<>();
                    try (PreparedStatement stmtLignes = conn.prepareStatement(sqlLignes)) {
                        stmtLignes.setInt(1, idCommande);
                        ResultSet rsLignes = stmtLignes.executeQuery();
                        while (rsLignes.next()) {
                            articlesADecrementer.put(rsLignes.getInt("idItem"), rsLignes.getInt("quantite"));
                        }
                    }
                    // Tenter de décrémenter le stock pour chaque ligne
                    for (Map.Entry<Integer, Integer> ligne : articlesADecrementer.entrySet()) {
                        int idItem = ligne.getKey();
                        int qte = ligne.getValue();
                        ServiceCommande serviceCommande=new ServiceCommande();
                        
                        // On vérifie une dernière fois si c'est disponible
                        if (!serviceCommande.est_disponible_item(conn, idItem, qte)) {
                            System.out.println(" Stock insuffisant pour l'item " + idItem + " !");
                            System.out.println("   La commande ne peut pas être préparée (Rupture de stock).");
                            conn.rollback();
                            return; // On arrête tout, la commande reste "En préparation"
                        }
                    
                        decrementerStockItem(conn, idItem, qte);
                    }

                    // Si tous les produits sont disponibles , on change le status
                    String changerStatus = "UPDATE Commande SET statut = 'Prête' WHERE idCommande = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(changerStatus)) {
                        updateStmt.setInt(1, idCommande);
                        int rowsUpdated = updateStmt.executeUpdate();
                        if (rowsUpdated > 0) {
                            System.out.println(" Statut de la commande " + idCommande + " changé en 'Prête'");
                            
                            // valider la transaction et libérer le verrou
                            conn.commit();
                            System.out.println(" Stocks mis à jour et commande N°" + idCommande + " est PRÊTE.");
                        } else {
                            conn.rollback();
                            System.out.println(" Erreur : Commande non mise à jour");
                        }
                    }
                }   
                    else {
                        conn.rollback();
                        System.out.println(" Erreur : Commande en statut '" + statut + "', ne peut pas passer à 'Prête'");
                    }
                }
                else {
                    conn.rollback();
                    System.out.println(" La Commande " + idCommande + " n'existe pas");
                }
        



    }
}




    public void annulerCommande(Connection conn, int idCommande) throws SQLException {
        conn.setAutoCommit(false);
        // Verrouiller la ligne et récupérer le statut actuel
        String selectVerrou = "SELECT statut FROM Commande WHERE idCommande = ? FOR UPDATE";

        try (PreparedStatement lockStmt = conn.prepareStatement(selectVerrou)) {
            lockStmt.setInt(1, idCommande);
            
            try (ResultSet rs = lockStmt.executeQuery()) {
                if (rs.next()) {
                    String statutActuel = rs.getString("statut");

                    // Verifier si la commande peut etre annulée
                    if ("En livraison".equalsIgnoreCase(statutActuel) || 
                        "Livrée".equalsIgnoreCase(statutActuel) || 
                        "Récupérée".equalsIgnoreCase(statutActuel) || 
                        "Prete".equalsIgnoreCase(statutActuel) ) {
                        
                        System.out.println("Impossible d'annuler : La commande est déjà " + statutActuel);
                        conn.rollback(); // On libère le verrou sans rien changer
                        return;
                    }

                    if ("Annulée".equalsIgnoreCase(statutActuel)) {
                        System.out.println(" Commande déjà annulée.");
                        conn.rollback();
                        return;
                    }

                    //Procéder à l'annulation
                    String updateQuery = "UPDATE Commande SET statut = 'Annulée' WHERE idCommande = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                        updateStmt.setInt(1, idCommande);
                        updateStmt.executeUpdate();
                        
                        conn.commit();
                        System.out.println(" Commande N°" + idCommande + " annulée avec succès.");
                    }

                } else {
                    conn.rollback();
                    System.out.println(" La Commande " + idCommande + " n'existe pas.");
                }
            }
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        }
    }


    // Passer la commande à "En livraison" (Le livreur récupère le colis)
    // Remplace votre "commandeRecue"
    public void commandeEnLivraison(Connection conn, int idCommande) throws SQLException {
        conn.setAutoCommit(false);
        String selectVerrou = "SELECT statut, modeRecuperation FROM Commande WHERE idCommande = ? FOR UPDATE";

        try (PreparedStatement lockStmt = conn.prepareStatement(selectVerrou)) {
            lockStmt.setInt(1, idCommande);
            ResultSet rs = lockStmt.executeQuery();

            if (rs.next()) {
                String statut = rs.getString("statut");
                String mode = rs.getString("modeRecuperation");

                // Vérification Métier
                if (!"Livraison".equalsIgnoreCase(mode)) {
                    System.out.println("[ERREUR] Une commande 'Boutique' ne peut pas être mise 'En livraison'.");
                    conn.rollback();
                    return;
                }
                if (!"Prête".equalsIgnoreCase(statut)) {
                    System.out.println("[ERREUR] La commande doit être 'Prête' pour être expédiée (Statut actuel: " + statut + ")");
                    conn.rollback();
                    return;
                }

                // Update
                String updateSql = "UPDATE Commande SET statut = 'En livraison' WHERE idCommande = ?";
                try (PreparedStatement upStmt = conn.prepareStatement(updateSql)) {
                    upStmt.setInt(1, idCommande);
                    upStmt.executeUpdate();
                    conn.commit();
                    System.out.println("[OK] Commande N°" + idCommande + " est maintenant EN LIVRAISON");
                }
            } else {
                conn.rollback();
                System.out.println("[INFO] Commande introuvable.");
            }
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        }
    }

    // Passer la commande à "Livrée" (Le client a reçu le colis chez lui)
    public void commandeLivree(Connection conn, int idCommande) throws SQLException {
        conn.setAutoCommit(false);
        String selectVerrou = "SELECT statut, modeRecuperation FROM Commande WHERE idCommande = ? FOR UPDATE";

        try (PreparedStatement lockStmt = conn.prepareStatement(selectVerrou)) {
            lockStmt.setInt(1, idCommande);
            ResultSet rs = lockStmt.executeQuery();

            if (rs.next()) {
                String statut = rs.getString("statut");
                String mode = rs.getString("modeRecuperation");

                if (!"Livraison".equalsIgnoreCase(mode)) {
                    System.out.println("[ERREUR] Mode incorrect pour ce statut.");
                    conn.rollback();
                    return;
                }
                // On peut accepter de passer de 'Prête' à 'Livrée' si le livreur a oublié de scanner 'En livraison'
                if (!"En livraison".equalsIgnoreCase(statut) && !"Prête".equalsIgnoreCase(statut)) {
                    System.out.println("[ERREUR] Statut incohérent (" + statut + ")");
                    conn.rollback();
                    return;
                }

                // Update
                String updateSql = "UPDATE Commande SET statut = 'Livrée' WHERE idCommande = ?";
                try (PreparedStatement upStmt = conn.prepareStatement(updateSql)) {
                    upStmt.setInt(1, idCommande);
                    upStmt.executeUpdate();
                    conn.commit();
                    System.out.println("[OK] Commande N°" + idCommande + " marquée comme LIVREE");
                }
            } else {
                conn.rollback();
                System.out.println("[INFO] Commande introuvable.");
            }
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        }
    }

    // Passer la commande à "Récupérée" (Le client est venu en boutique)
    public void commandeRecuperee(Connection conn, int idCommande) throws SQLException {
        conn.setAutoCommit(false);
        String selectVerrou = "SELECT statut, modeRecuperation FROM Commande WHERE idCommande = ? FOR UPDATE";

        try (PreparedStatement lockStmt = conn.prepareStatement(selectVerrou)) {
            lockStmt.setInt(1, idCommande);
            ResultSet rs = lockStmt.executeQuery();

            if (rs.next()) {
                String statut = rs.getString("statut");
                String mode = rs.getString("modeRecuperation");

                if (!"Boutique".equalsIgnoreCase(mode)) {
                    System.out.println("[ERREUR] Une commande 'Livraison' ne peut pas être 'Récupérée' au guichet.");
                    conn.rollback();
                    return;
                }
                if (!"Prête".equalsIgnoreCase(statut)) {
                    System.out.println("[ERREUR] La commande n'est pas encore prête (Statut: " + statut + ")");
                    conn.rollback();
                    return;
                }

                // Update
                String updateSql = "UPDATE Commande SET statut = 'Récupérée' WHERE idCommande = ?";
                try (PreparedStatement upStmt = conn.prepareStatement(updateSql)) {
                    upStmt.setInt(1, idCommande);
                    upStmt.executeUpdate();
                    conn.commit();
                    System.out.println("[OK] Commande N°" + idCommande + " a été RECUPEREE par le client");
                }
            } else {
                conn.rollback();
                System.out.println("[INFO] Commande introuvable.");
            }
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        }
    }

    // Gérer le stock 

    private void decrementerStockItem(Connection conn, int idItem, int qte) throws SQLException {
        String sqlIdentify = "SELECT typeItem, idArticle, idContenant FROM Item WHERE idItem = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sqlIdentify)) {
            stmt.setInt(1, idItem);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String type = rs.getString("typeItem");
                
                if ("ARTICLE".equals(type)) {
                    // C'est un article
                    int idArticle = rs.getInt("idArticle");
                    decrementerStockArticle(conn, idArticle, qte);
                } 
                else {
                    // C'est un contenant
                    int idContenant = rs.getInt("idContenant");
                    String up = "UPDATE Contenant SET stock = stock - ? WHERE idContenant = ?";
                    try(PreparedStatement s = conn.prepareStatement(up)) {
                        s.setInt(1, qte);
                        s.setInt(2, idContenant);
                        s.executeUpdate();
                    }
                }
            } else {
                throw new SQLException("Item " + idItem + " introuvable dans la base.");
            }
        }
    }

    private void decrementerStockArticle(Connection conn, int idArticle, int qte) throws SQLException {
        String sql = "SELECT idLot, quantiteDisponible FROM Lot " + 
                     "WHERE idArticle = ? AND quantiteDisponible > 0 " + 
                     "ORDER BY datePeremption ASC " + 
                     "FOR UPDATE";

        try (PreparedStatement s = conn.prepareStatement(sql)) {
            s.setInt(1, idArticle);
            
            try (ResultSet rs = s.executeQuery()) {
                double reste = qte;
                
                while (rs.next() && reste > 0) {
                    int idLot = rs.getInt("idLot");
                    double dispo = rs.getDouble("quantiteDisponible");
                    double pris = Math.min(dispo, reste);
                    
                    String up = "UPDATE Lot SET quantiteDisponible = quantiteDisponible - ? WHERE idLot = ?";
                    try(PreparedStatement p = conn.prepareStatement(up)) {
                        p.setDouble(1, pris);
                        p.setInt(2, idLot);
                        p.executeUpdate();
                    }
                    reste -= pris;
                }
                
                if (reste > 0) {
                    throw new SQLException("Rupture de stock : Il manque " + reste + " unites pour l'article " + idArticle);
                }
            }
        }
    }
}

