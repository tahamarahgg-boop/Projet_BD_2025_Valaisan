import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ServiceCommande {

    // --- LA TRANSACTION PRINCIPALE ---
    public void passerCommandeDebut(int idClient){
        // construction Map<Integer, Integer> panier
        Scanner scanner=new Scanner(System.in);
        int choix;
        Map<Integer,Integer> panier=new HashMap<>();
        do{
            System.out.println("Veuillez entrez l'Id de votre Article ou -1 pour sortir ");
            choix=scanner.nextInt();
            if (choix==-1) continue;
            System.out.println("Saisissez la quantité ");
            int quantite=scanner.nextInt();
            panier.put(choix, panier.getOrDefault(choix, 0) + quantite);
        }while(choix!=-1);

        System.out.println("Entrez le mode de Livraison ");
        System.out.println(" - Tapez 'Livraison' pour livraison à domicile");
        System.out.println(" - Tapez 'Boutique' pour retrait en magasin");
        System.out.print("Votre choix : ");
        scanner.nextLine();
        String modeLivraison = scanner.nextLine();
        System.out.println("Entrez le mode de Paiement ");
        System.out.println(" - Tapez 'Carte' pour paiement en ligne");
        System.out.println(" - Tapez 'Especes' pour paiement au retrait");
        System.out.print("Votre choix : ");
        String modePaiement = scanner.nextLine();

        passerCommande(idClient,panier,modeLivraison,modePaiement);


    }
    public void passerCommande(int idClient, Map<Integer, Integer> panier, String modeLivraison, String modePaiement) {
        Connection conn = null;
        try {
            // 1. Connexion ORACLE (Mettez vos infos ici)
            String url = "jdbc:oracle:thin:@oracle1:1521:oracle1"; 
            conn = DriverManager.getConnection(url, "lahmouza", "lahmouza");

            // 2. DÉBUT TRANSACTION 
            conn.setAutoCommit(false); 
            System.out.println("--- Début de la transaction ---");

            int montantProduits = 0;
            int fraisLivraison = 0;

            // Étape A : Vérification Stocks et Calcul Montant
            for (Map.Entry<Integer, Integer> ligne : panier.entrySet()) {
                System.out.println("--- Début de la transaction ---");
                int idArticle = ligne.getKey();
                int qte = ligne.getValue();

                // Appel avec la connexion partagée
                if (!est_disponible(conn, idArticle, qte)) {
                    throw new Exception("Stock insuffisant ou hors saison pour l'article " + idArticle);
                }

                // Récupération prix unitaire
                int prixUnitaire = getPrixArticle(conn, idArticle);
                montantProduits += (prixUnitaire * qte);
                
                // Simulation calcul poids pour frais (simplifié ici à 1kg par unité)
                fraisLivraison += calculerFraisLigne(conn, idArticle, qte, modeLivraison);
            }

            // Étape B : Calcul Total
            int total = montantProduits + fraisLivraison;

            // Étape C : Création Commande et Lignes
            creer_commande(conn, idClient, modeLivraison, modePaiement, total, fraisLivraison, panier);

            // 4. VALIDATION
            conn.commit();
            System.out.println("✅ Transaction validée ! Montant total : " + total);

        } catch (Exception e) {
            // 5. ANNULATION 
            try { 
                if (conn != null) conn.rollback(); 
                System.err.println("❌ Annulation de la transaction : " + e.getMessage());
            } catch (SQLException ex) { ex.printStackTrace(); }
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) {}
        }
    }

    

    // --- MÉTHODES INTERNES (Modifiées pour recevoir 'Connection conn') ---

    private boolean est_disponible(Connection conn, int idArticle, int quantite) throws SQLException {
        // Vérification Stock (Somme des lots)
        System.out.println("est_dipo func");
        String query = "SELECT SUM(quantiteDisponible) as sumQuant FROM Lot WHERE idArticle=?";
        
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, idArticle);
            ResultSet res = stmt.executeQuery();
            
            if (res.next()) {
                int stock = res.getInt("sumQuant");
                if (stock >= quantite) return true;
                else System.out.println("Stock insuffisant (" + stock + " disponible).");
            }
            stmt.close();
            res.close();

            //  Hors Saison
            String querySaison = "SELECT e.dateDebut, e.dateFin " +
                             "FROM est_de_saison e " +
                             "JOIN Article a ON e.idProduit = a.idProduit " +
                             "WHERE a.idArticle = ? " +
                             "AND SYSDATE NOT BETWEEN e.dateDebut AND e.dateFin";
            PreparedStatement stmts=conn.prepareStatement(querySaison);
            stmts.setInt(1,idArticle);
            ResultSet ress=stmts.executeQuery();
            if(ress.next()){
                System.out.println("Ce produit sera disponible entre "+ress.getDate("dateDebut")+" et "+ress.getDate("dateFin"));
            }
            ress.close();
            stmts.close();
            return false;

        }
        catch (SQLException e) {
            System.err.println("Erreur SQL lors de la vérification disponibilité : " + e.getMessage());
            return false;
        }   
        
    }

    private void creer_commande(Connection conn, int idClient, String modeLivraison, String modePaiement, int total, int frais, Map<Integer, Integer> panier) throws SQLException {
        // 1. Insertion Commande (SYSDATE pour Oracle)
        // On récupère d'abord un ID (car pas d'auto-increment simple en Oracle parfois, sinon utiliser sequence)
        System.out.println("--- creer commande ---");
        int idCmd = 0;
        try(
            Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("SELECT MAX(idCommande) FROM Commande")) {
            if(rs.next()) idCmd = rs.getInt(1) + 1;
        }

        String query = "Insert into Commande (idCommande, idClient, dateCommande, statut, modeRecuperation, modePaiement, montantTotal, fraisLivraison) "
                     + "VALUES (?, ?, SYSDATE, 'En préparation', ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, idCmd);
            stmt.setInt(2, idClient);
            stmt.setString(3, modeLivraison);
            stmt.setString(4, modePaiement);
            stmt.setInt(5, total);
            stmt.setInt(6, frais);
            stmt.executeUpdate();
        }
        
        String queryLigne="Insert into LigneCommande (idLigne,idCommande,idArticle,quantite,prixUnitaireApplique)"
                        + "Values (?,?,?,?,?)";
        PreparedStatement stmtLigne=conn.prepareStatement(queryLigne);
        int idLigne = 1;
        try (Statement s = conn.createStatement();
            ResultSet rs = s.executeQuery("SELECT MAX(idLigne) FROM LigneCommande")) {
            if (rs.next()) {
                int max = rs.getInt(1);
                if (!rs.wasNull()) idLigne = max + 1;
            }
        }

        for (Map.Entry<Integer, Integer> entry : panier.entrySet()) {
            Integer idArticle = entry.getKey();
            Integer quant = entry.getValue();
            
            stmtLigne.setInt(1,idLigne);
            stmtLigne.setInt(2,idCmd);
            stmtLigne.setInt(3,idArticle);
            stmtLigne.setInt(4,quant);
            stmtLigne.setInt(5,getPrixArticle(conn,idArticle));
            stmtLigne.executeUpdate();
            stmtLigne.close();

            idLigne++;
        }
    }

    // --- Outils (Frais, Prix) ---
    
    private int getPrixArticle(Connection conn, int idArticle) throws SQLException {
        String sql = "SELECT prixVenteClient FROM Article WHERE idArticle = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idArticle);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private int calculerFraisLigne(Connection conn, int idArticle, int qte, String modeLivraison) {
        if (!modeLivraison.equalsIgnoreCase("Livraison")) return 0;
        // Simplification : 1€ par article pour la livraison
        return qte * 1; 
    }
}
