import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.Properties;


public class EffacerPackage {
    private static final String CONFIG_FILE = "config.properties";
    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    public static void main(String[] args) {
        EffacerPackage app = new EffacerPackage();
        app.run();
    }

    private void run() {
        try {
            loadDatabaseConfig();
            String input = getUserInput("Entrez une valeur (exemple : GALLICA_GLOBAL_ALLJOURNALS ou GALLICA_GLOBAL_ALL%): ");
            String[] parts = validateAndParseInput(input);
            
            List<SimpleEntry<Integer, String>> results = fetchPackages(parts);
            if (results.isEmpty()) {
                System.out.println("La requête ne renvoie aucun résultat.");
                return;
            }
            
            displayResults(results);
            handleUserAction(results, parts);
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadDatabaseConfig() throws Exception {
        Properties prop = new Properties();
        try (FileInputStream f = new FileInputStream(new File(CONFIG_FILE))) {
            prop.load(f);
            dbUrl = prop.getProperty("DB_URL");
            dbUser = prop.getProperty("DB_USER");
            dbPassword = prop.getProperty("DB_PASSWORD");
            System.out.println("Connexion TLS : " + dbUrl);
        }
    }

    private String[] validateAndParseInput(String input) throws IllegalArgumentException {
        String[] parts = input.split("_");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Information insuffisante. Minimum 3 éléments requis.");
        }
        return parts;
    }

    private List<SimpleEntry<Integer, String>> fetchPackages(String[] parts) throws SQLException {
        String provider = parts[0];
        String packagePattern = parts[1] + "_" + parts[2];
        
        String sql = "SELECT provider, package, idt_provider, date_p FROM provider, provider_package " +
                     "WHERE idt_provider = provider_idt_provider AND provider = ? AND package LIKE ?";
        
        List<SimpleEntry<Integer, String>> results = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, provider);
            pstmt.setString(2, packagePattern);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                int index = 1;
                while (rs.next()) {
                    String date = rs.getString("date_p").split(" ")[0];
                    String providerName = rs.getString("provider");
                    int providerId = rs.getInt("idt_provider");
                    String packageName = rs.getString("package");
                    
                    System.out.println("Package " + index + " : " + providerName + " " + packageName + " " + date);
                    results.add(new SimpleEntry<>(index, 
                        providerName + " " + providerId + " " + packageName + " " + date));
                    index++;
                }
            }
        }
        
        return results;
    }

    private void displayResults(List<SimpleEntry<Integer, String>> results) {
        System.out.println("\n=== Résultats trouvés ===");
        for (SimpleEntry<Integer, String> entry : results) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }

    private void handleUserAction(List<SimpleEntry<Integer, String>> results, String[] parts) {
        String action = getUserInput("\nEntrez une action (0->aucune, T->toutes, M->modification, ou numéro) : ");
        
        switch (action.toUpperCase()) {
            case "0":
                System.out.println("Aucune action effectuée.");
                break;
            case "T":
                deleteAll(results, parts);
                break;
            case "M":
                modifyPackage(results, parts);
                break;
            default:
                deleteSingle(results, parts, action);
        }
    }

    private void deleteAll(List<SimpleEntry<Integer, String>> results, String[] parts) {
        String packagePattern = parts[1] + "_" + parts[2];
        String provider = parts[0];
        
        if (!confirmAction("Êtes-vous sûr de vouloir supprimer " + results.size() + " lignes ?")) {
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            int providerId = getProviderIdFromResults(results);
            
            String sqlDeletePP = "DELETE FROM provider_package WHERE package LIKE ? AND provider_idt_provider = ?";
            String sqlDeleteLK = "DELETE FROM ligne_kbart WHERE provider_package_package LIKE ? AND provider_package_idt_provider = ?";
            String sqlInsert = "INSERT INTO provider_package_deleted (PACKAGE, PROVIDER) VALUES (?, ?)";
            
            try (PreparedStatement deleteStmtPP = conn.prepareStatement(sqlDeletePP);
                 PreparedStatement deleteStmtLK = conn.prepareStatement(sqlDeleteLK);
                 PreparedStatement insertStmt = conn.prepareStatement(sqlInsert)) {
                
                deleteStmtPP.setString(1, packagePattern);
                deleteStmtPP.setInt(2, providerId);
                
                deleteStmtLK.setString(1, packagePattern);
                deleteStmtLK.setInt(2, providerId);
                
                insertStmt.setString(1, packagePattern);
                insertStmt.setString(2, provider);
                
                int rowsDeleted = deleteStmtPP.executeUpdate();
                deleteStmtLK.executeUpdate();
                insertStmt.executeUpdate();
                
                System.out.println("✓ " + rowsDeleted + " lignes supprimées et archivées.");
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la suppression : " + e.getMessage());
        }
    }

    private void deleteSingle(List<SimpleEntry<Integer, String>> results, String[] parts, String actionStr) {
        try {
            int index = Integer.parseInt(actionStr);
            if (index < 1 || index > results.size()) {
                System.out.println("Numéro invalide.");
                return;
            }
            
            SimpleEntry<Integer, String> entry = results.get(index - 1);
            String[] data = entry.getValue().split(" ");
            String packageName = data[2];
            String date = data[3];
            int providerId = Integer.parseInt(data[1]);
            
            if (!confirmAction("Supprimer la ligne du " + date + " ?")) {
                return;
            }
            
            String sql = "DELETE FROM provider_package WHERE package = ? AND date_p = ? AND provider_idt_provider = ?";
            
            try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, packageName);
                stmt.setDate(2, java.sql.Date.valueOf(date));
                stmt.setInt(3, providerId);
                
                int rowsDeleted = stmt.executeUpdate();
                System.out.println("✓ " + rowsDeleted + " ligne(s) supprimée(s).");
            }
        } catch (NumberFormatException e) {
            System.out.println("Veuillez entrer un numéro valide.");
        } catch (SQLException e) {
            System.out.println("Erreur SQL : " + e.getMessage());
        }
    }

    private void modifyPackage(List<SimpleEntry<Integer, String>> results, String[] parts) {
        String oldPackage = parts[1] + "_" + parts[2];
        String newPackage = getUserInput("Entrez le nouveau nom du package : ");
        int providerId = getProviderIdFromResults(results);
        
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            // Vérifier si le nouveau package existe déjà
            if (packageExists(conn, newPackage, providerId)) {
                System.out.println("Le package '" + newPackage + "' existe déjà.");
                updateExistingPackage(conn, oldPackage, newPackage, providerId);
            } else {
                System.out.println("Le package n'existe pas, création en cours...");
                insertNewPackage(conn, oldPackage, newPackage, providerId);
            }
            
            // Mettre à jour la table ligne_kbart
            updateLigneKbart(conn, oldPackage, newPackage, providerId);
            System.out.println("✓ Package renommé de '" + oldPackage + "' à '" + newPackage + "'.");
        } catch (SQLException e) {
            System.out.println("Erreur lors de la modification : " + e.getMessage());
        }
    }

    private boolean packageExists(Connection conn, String packageName, int providerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM PROVIDER_PACKAGE WHERE PACKAGE = ? AND PROVIDER_IDT_PROVIDER = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, packageName);
            stmt.setInt(2, providerId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void insertNewPackage(Connection conn, String oldPackage, String newPackage, int providerId) throws SQLException {
        String sql = "INSERT INTO PROVIDER_PACKAGE (PACKAGE, DATE_P, LABEL_ABES, PROVIDER_IDT_PROVIDER) " +
                     "SELECT ?, DATE_P, LABEL_ABES, PROVIDER_IDT_PROVIDER FROM PROVIDER_PACKAGE " +
                     "WHERE PROVIDER_IDT_PROVIDER = ? AND PACKAGE = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPackage);
            stmt.setInt(2, providerId);
            stmt.setString(3, oldPackage);
            stmt.executeUpdate();
        }
    }

    private void updateExistingPackage(Connection conn, String oldPackage, String newPackage, int providerId) throws SQLException {
        String sql = "UPDATE PROVIDER_PACKAGE SET PACKAGE = ? " +
                     "WHERE PROVIDER_IDT_PROVIDER = ? AND PACKAGE = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPackage);
            stmt.setInt(2, providerId);
            stmt.setString(3, oldPackage);
            stmt.executeUpdate();
        }
    }

    private void updateLigneKbart(Connection conn, String oldPackage, String newPackage, int providerId) throws SQLException {
        String sql = "UPDATE ligne_kbart SET provider_package_package = ? " +
                     "WHERE provider_package_idt_provider = ? AND provider_package_package = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPackage);
            stmt.setInt(2, providerId);
            stmt.setString(3, oldPackage);
            stmt.executeUpdate();
        }
    }

    private int getProviderIdFromResults(List<SimpleEntry<Integer, String>> results) {
        if (!results.isEmpty()) {
            String[] data = results.get(0).getValue().split(" ");
            return Integer.parseInt(data[1]);
        }
        return 0;
    }

    private String getUserInput(String prompt) {
        Scanner sc = new Scanner(System.in);
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    private boolean confirmAction(String message) {
        String response = getUserInput(message + " (oui/non) : ");
        return response.equalsIgnoreCase("oui");
    }
}


