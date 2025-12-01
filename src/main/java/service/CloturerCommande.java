package service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CloturerCommande {

    public void commandePrete(Connection conn, int idCmd,int idStaff) throws SQLException {
        try {
            // 1. Verrouiller la commande et vérifier le statut
            String sqlLock = "SELECT statut FROM Commande WHERE idCommande = ? FOR UPDATE";
            try(PreparedStatement ps = conn.prepareStatement(sqlLock)) {
                ps.setInt(1, idCmd);
                ResultSet rs = ps.executeQuery();
                if(!rs.next()) { 
                    System.out.println(" [ERREUR] Commande introuvable."); 
                    conn.rollback(); return; 
                }
                if(!"En préparation".equalsIgnoreCase(rs.getString(1))) { 
                    System.out.println(" [ERREUR] Statut invalide (Déja traitée ou annulée)."); 
                    conn.rollback(); return; 
                }
            }

            // 2. Récupérer les lignes de la commande
            Map<Integer, Integer> lignes = new HashMap<>();
            String sqlLignes = "SELECT idItem, quantite FROM LigneCommande WHERE idCommande=?";
            try(PreparedStatement ps = conn.prepareStatement(sqlLignes)) {
                ps.setInt(1, idCmd);
                ResultSet rs = ps.executeQuery();
                while(rs.next()) lignes.put(rs.getInt(1), rs.getInt(2));
            }

            // 3. VERROUILLAGE ET DÉCRÉMENTATION
            System.out.println(" Vérification des stocks en cours...");
            for(Map.Entry<Integer, Integer> e : lignes.entrySet()) {
                int idItem = e.getKey();
                int qteDemande = e.getValue();

                // On tente de verrouiller et débité les lots nécessaires
                if (!checkAndDebitStock(conn, idItem, qteDemande)) {
                    System.out.println(" RUPTURE DE STOCK sur l'item " + idItem + " !");
                    System.out.println("    Impossible de finaliser la préparation.");
                    conn.rollback();// permet de reinitialiser les lots
                    return;
                }
            }

            // 4. Tout est bon : on passe à "Prête"
            String sqlUpdate = "UPDATE Commande SET statut='Prête' WHERE idCommande=?";
            try(PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                ps.setInt(1, idCmd);
                ps.executeUpdate();
            }
            
            conn.commit();
            System.out.println(" Commande N°" + idCmd + " PRÊTE (Stocks débités).");

        } catch(SQLException e) {
            if (conn != null) conn.rollback();
            System.err.println("Exception technique : " + e.getMessage());
        }
    }

       // Vérifie la disponibilité ET débite le stock en une seule fois.
    private boolean checkAndDebitStock(Connection conn, int idItem, int qte) throws SQLException {
        // Identifier si c'est un Article ou un Contenant
        String type = "";
        int idRef = 0;
        String sqlInfo = "SELECT typeItem, idArticle, idContenant FROM Item WHERE idItem=?";
        
        try(PreparedStatement ps = conn.prepareStatement(sqlInfo)) {
            ps.setInt(1, idItem); 
            ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                type = rs.getString("typeItem");
                idRef = "ARTICLE".equals(type) ? rs.getInt("idArticle") : rs.getInt("idContenant");
            } else return false; // Item inconnu
        }

        if ("ARTICLE".equals(type)) {
            // --- GESTION ARTICLE  ---
            
            // On sélectionne les lots disponibles, du plus vieux au plus récent
            // FOR UPDATE verrouille ces lignes pour empêcher un autre staff de les prendre
            String sqlLots = "SELECT idLot, quantiteDisponible FROM Lot " +
                             "WHERE idArticle=? AND quantiteDisponible > 0 " +
                             "ORDER BY datePeremption ASC FOR UPDATE";
                             
            try(PreparedStatement ps = conn.prepareStatement(sqlLots)) {
                ps.setInt(1, idRef);
                ResultSet rs = ps.executeQuery();
                
                double resteADebiter = qte;
                
                while(rs.next() && resteADebiter > 0) {
                    int idLot = rs.getInt("idLot");
                    double dispo = rs.getDouble("quantiteDisponible");
                    
                    // On prend soit tout le besoin, soit tout le lot (si pas assez)
                    double aPrendre = Math.min(dispo, resteADebiter);
                    
                    // Mise à jour du lot
                    String upLot = "UPDATE Lot SET quantiteDisponible = quantiteDisponible - ? WHERE idLot=?";
                    try(PreparedStatement up = conn.prepareStatement(upLot)) {
                        up.setDouble(1, aPrendre);
                        up.setInt(2, idLot);
                        up.executeUpdate();
                    }
                    
                    resteADebiter -= aPrendre;
                }
                
                // Si resteADebiter > 0, c'est qu'on n'a pas trouvé assez de stock
                return resteADebiter <= 0; 
            }
            
        } else {
            // --- GESTION CONTENANT (Stock simple) ---
            
            String sqlCont = "SELECT stock FROM Contenant WHERE idContenant=? FOR UPDATE";
            try(PreparedStatement ps = conn.prepareStatement(sqlCont)) {
                ps.setInt(1, idRef); 
                ResultSet rs = ps.executeQuery();
                
                if(rs.next()) {
                    int stockActuel = rs.getInt("stock");
                    if (stockActuel >= qte) {
                        String upCont = "UPDATE Contenant SET stock = stock - ? WHERE idContenant=?";
                        try(PreparedStatement up = conn.prepareStatement(upCont)) {
                            up.setInt(1, qte); 
                            up.setInt(2, idRef); 
                            up.executeUpdate();
                        }
                        return true;
                    }
                }
            }
        }
        return false; // Stock insuffisant ou item non trouvé
    }

    // --- GESTION DES AUTRES STATUTS ---

    public void annulerCommande(Connection conn, int idCmd, boolean isStaff) throws SQLException {
        conn.setAutoCommit(false);
        try {
            // 1. Verrouiller la commande pour vérifier son statut
            String sqlCheck = "SELECT statut FROM Commande WHERE idCommande=? FOR UPDATE";
            
            try (PreparedStatement ps = conn.prepareStatement(sqlCheck)) {
                ps.setInt(1, idCmd);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String statutActuel = rs.getString("statut");

                    // --- RÈGLE CLIENT : Strictement "En préparation" ---
                    if (!isStaff && !"En préparation".equalsIgnoreCase(statutActuel)) {
                        System.out.println(" REFUS : En tant que client, vous ne pouvez plus annuler.");
                        System.out.println("   (Votre commande est déjà : " + statutActuel + ")");
                        conn.rollback();
                        return;
                    }

                    // --- RÈGLE GÉNÉRALE (Staff & Client) : Trop tard si expédié ---
                    if ("En livraison".equalsIgnoreCase(statutActuel) || 
                        "Livrée".equalsIgnoreCase(statutActuel) || 
                        "Récupérée".equalsIgnoreCase(statutActuel)) {
                        System.out.println(" REFUS : Impossible d'annuler, commande déjà expédiée/terminée.");
                        conn.rollback();
                        return;
                    }

                    // --- CAS PARTICULIER STAFF : Annulation d'une commande "Prête" ---
                    if ("Prête".equalsIgnoreCase(statutActuel)) {
                        System.out.println(" Annulation d'une commande PRÊTE par le propriétaire.");
                        System.out.println("   -> Remise en stock des articles...");
                        recrediterStock(conn, idCmd); // On rend le stock !
                    }

                    // 2. Application de l'annulation
                    String sqlUp = "UPDATE Commande SET statut='Annulée' WHERE idCommande=?";
                    try (PreparedStatement up = conn.prepareStatement(sqlUp)) {
                        up.setInt(1, idCmd);
                        up.executeUpdate();
                        conn.commit();
                        System.out.println(" Commande N°" + idCmd + " annulée avec succès.");
                    }

                } else {
                    conn.rollback();
                    System.out.println("Commande introuvable.");
                }
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }

    // Méthode privée pour remettre le stock si le staff annule une commande prête
    private void recrediterStock(Connection conn, int idCmd) throws SQLException {
        // On récupère les lignes de la commande
        String sqlLignes = "SELECT idItem, quantite FROM LigneCommande WHERE idCommande=?";
        Map<Integer, Integer> lignes = new HashMap<>();
        try(PreparedStatement ps = conn.prepareStatement(sqlLignes)) {
            ps.setInt(1, idCmd); ResultSet rs = ps.executeQuery();
            while(rs.next()) lignes.put(rs.getInt(1), rs.getInt(2));
        }

        // On re-crédite
        for(Map.Entry<Integer, Integer> e : lignes.entrySet()) {
            int idItem = e.getKey();
            int qte = e.getValue();
            
            // On simplifie ici : on remet dans le Lot le plus récent ou un lot générique
            String sqlCheckType = "SELECT typeItem, idContenant FROM Item WHERE idItem=?";
            try(PreparedStatement ps = conn.prepareStatement(sqlCheckType)) {
                ps.setInt(1, idItem); ResultSet rsType = ps.executeQuery();
                if(rsType.next() && "CONTENANT".equals(rsType.getString(1))) {
                    int idCont = rsType.getInt(2);
                    try(PreparedStatement up = conn.prepareStatement("UPDATE Contenant SET stock = stock + ? WHERE idContenant=?")) {
                        up.setInt(1, qte); up.setInt(2, idCont); up.executeUpdate();
                        System.out.println("   + Contenant " + idCont + " recrédité.");
                    }
                } else {
                    System.out.println("   (i) Article " + idItem + " à remettre en rayon manuellement (Gestion FIFO complexe).");
                }
            }
        }
    }

    //  transitions de statut
    public void commandeEnLivraison(Connection conn, int id) throws SQLException { 
        updateStatut(conn, id, "En livraison", "Livraison", "Prête"); 
    }
    public void commandeLivree(Connection conn, int id) throws SQLException { 
        updateStatut(conn, id, "Livrée", "Livraison", "En livraison"); 
    }
    public void commandeRecuperee(Connection conn, int id) throws SQLException { 
        updateStatut(conn, id, "Récupérée", "Boutique", "Prête"); 
    }

    private void updateStatut(Connection conn, int id, String newSt, String modeReq, String oldSt) throws SQLException {
        conn.setAutoCommit(false);
        try {
            String sql = "SELECT statut, modeRecuperation FROM Commande WHERE idCommande=? FOR UPDATE";
            try(PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id); ResultSet rs = ps.executeQuery();
                if(rs.next()) {
                    // Vérification logique métier
                    if(!rs.getString("modeRecuperation").equalsIgnoreCase(modeReq)) {
                        System.out.println("Erreur : Mode de récupération incompatible."); 
                        conn.rollback(); return;
                    }
                    // Vérification séquence (ex: on ne passe pas de 'Prête' à 'Livrée' sans passer par 'En livraison')
                    // (Sauf si vous voulez être permissif, ici c'est strict)
                    if(!oldSt.isEmpty() && !rs.getString("statut").equalsIgnoreCase(oldSt)) {
                        System.out.println(" Erreur : Statut actuel incorrect (" + rs.getString("statut") + ").");
                        conn.rollback(); return;
                    }

                    try(PreparedStatement up = conn.prepareStatement("UPDATE Commande SET statut=? WHERE idCommande=?")) {
                        up.setString(1, newSt); up.setInt(2, id); up.executeUpdate(); 
                        conn.commit();
                        System.out.println(" Statut mis à jour : " + newSt);
                    }
                } else { 
                    conn.rollback(); System.out.println("Commande introuvable"); 
                }
            }
        } catch(SQLException e) { conn.rollback(); throw e; }
    }

    // --- UTILITAIRE PRIX  ---
    
    public double getPrixItem(Connection conn, int idItem) throws SQLException {
        String sql = "SELECT typeItem, idArticle, idContenant FROM Item WHERE idItem=?";
        try(PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idItem); ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                if("ARTICLE".equals(rs.getString("typeItem"))) return getPrixArticle(conn, rs.getInt("idArticle"));
                else return getPrixContenant(conn, rs.getInt("idContenant"));
            }
        }
        return 0;
    }

    private double getPrixArticle(Connection conn, int idArt) throws SQLException {
        double prix = 0;
        // Prix de base
        try(PreparedStatement ps = conn.prepareStatement("SELECT prixVenteClient FROM Article WHERE idArticle=? FOR UPDATE")) {
            ps.setInt(1, idArt); ResultSet rs = ps.executeQuery();
            if(rs.next()) prix = rs.getDouble(1);
        }
        // Application Promo
        // On regarde si le premier lot qui sort a une promo
        String sqlPromo = "SELECT pourcentageReduction FROM Lot WHERE idArticle=? AND quantiteDisponible>0 ORDER BY datePeremption ASC";
        try(PreparedStatement ps = conn.prepareStatement(sqlPromo)) {
            ps.setInt(1, idArt); ResultSet rs = ps.executeQuery();
            if(rs.next()) {
                double r = rs.getDouble(1);
                if(r > 0) prix = prix * (1.0 - r/100.0);
            }
        }
        return prix;
    }

    private double getPrixContenant(Connection conn, int idCont) throws SQLException {
        try(PreparedStatement ps = conn.prepareStatement("SELECT prixVente FROM Contenant WHERE idContenant=?")) {
            ps.setInt(1, idCont); ResultSet rs = ps.executeQuery();
            if(rs.next()) return rs.getDouble(1);
        }
        return 0;
    }
}