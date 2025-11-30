package service;

public class GestionLivraison {
    /**
     * Prend une ville en entrée et retourne l'approximation de {latitude, longitude}.
     */
    public static double[] obtenirCoordonnees(String ville) {
        // Nettoyage de l'entrée (minuscules, sans espaces inutiles)
        String villeNettoyee = ville.toLowerCase();

        switch (villeNettoyee) {
            case "grenoble":
                return new double[]{45.1885, 5.7245};
            case "paris":
                return new double[]{48.8566, 2.3522};
            case "lyon":
                return new double[]{45.7640, 4.8357};
            case "marseille":
                return new double[]{43.2965, 5.3698};
            case "bordeaux":
                return new double[]{44.8378, -0.5792};
            default:
                // Valeur par défaut (Centre de la France) si la ville n'est pas connue
                System.out.println("⚠️ Ville non reconnue dans la simulation, utilisation des coordonnées par défaut.");
                return new double[]{46.603354, 1.888334};
        }
    }
    /**
     * Calcule la distance en kilomètres entre deux points .
     * @param pos1 Tableau [latitude, longitude] du point A
     * @param pos2 Tableau [latitude, longitude] du point B
     * @return La distance arrondie en kilomètres (int)
     */
    public static int calculdistance(double[] pos1, double[] pos2) {
        final int R = 6371; 
        double lat1 = pos1[0];
        double lon1 = pos1[1];
        double lat2 = pos2[0];
        double lon2 = pos2[1];
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        // Formule de Haversine
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Calcul final et arrondissement à l'entier le plus proche
        double distance = R * c;

        return (int) Math.round(distance);
    }

}

