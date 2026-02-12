import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class EffacerPackageService {
    private static final String CONFIG_FILE = "config.properties";

    private String dbUrl;
    private String dbUser;
    private String dbPassword;

    public EffacerPackageService() throws Exception {
        loadDatabaseConfig();
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String[] validateAndParseInput(String input) {
        String[] parts = input.split("_");
        if (parts.length < 3) {
            throw new IllegalArgumentException("Information insuffisante. Minimum 3 elements requis.");
        }
        return parts;
    }

    public List<PackageRecord> fetchPackages(String[] parts) throws SQLException {
        String provider = parts[0];
        String packagePattern = parts[1] + "_" + parts[2];
        String sql =
            "SELECT provider, package, idt_provider, date_p " +
            "FROM provider, provider_package " +
            "WHERE idt_provider = provider_idt_provider AND provider = ? AND package LIKE ? " +
            "ORDER BY date_p";

        List<PackageRecord> results = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, provider);
            pstmt.setString(2, packagePattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                int index = 1;
                while (rs.next()) {
                    String providerName = rs.getString("provider");
                    int providerId = rs.getInt("idt_provider");
                    String packageName = rs.getString("package");
                    Date dateValue = rs.getDate("date_p");
                    LocalDate date = dateValue != null ? dateValue.toLocalDate() : null;

                    results.add(new PackageRecord(index, providerName, providerId, packageName, date));
                    index++;
                }
            }
        }

        return results;
    }

    public int deleteAll(List<PackageRecord> results, String[] parts) throws SQLException {
        String packagePattern = parts[1] + "_" + parts[2];
        String provider = parts[0];
        int providerId = getProviderIdFromResults(results);

        String sqlDeletePP = "DELETE FROM provider_package WHERE package LIKE ? AND provider_idt_provider = ?";
        String sqlDeleteLK = "DELETE FROM ligne_kbart WHERE provider_package_package LIKE ? AND provider_package_idt_provider = ?";
        String sqlInsert = "INSERT INTO provider_package_deleted (PACKAGE, PROVIDER) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement deleteStmtPP = conn.prepareStatement(sqlDeletePP);
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
            return rowsDeleted;
        }
    }

    public int deleteSingle(List<PackageRecord> results, int index, String provider) throws SQLException {
        if (index < 1 || index > results.size()) {
            throw new IllegalArgumentException("Numero invalide.");
        }

        PackageRecord selected = results.get(index - 1);
        int providerId = selected.getProviderId();
        String packageName = selected.getPackageName();
        LocalDate date = selected.getDate();

        if (isLastPackage(results, providerId, packageName)) {
            return deleteAllForPackage(providerId, packageName, provider);
        }

        String sql = "DELETE FROM provider_package WHERE package = ? AND date_p = ? AND provider_idt_provider = ?";
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, packageName);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setInt(3, providerId);
            return stmt.executeUpdate();
        }
    }

    public void modifyPackage(List<PackageRecord> results, String[] parts, String newPackage) throws SQLException {
        if (newPackage == null || newPackage.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nouveau package est vide.");
        }

        Set<String> uniquePackages = new HashSet<>();
        for (PackageRecord record : results) {
            uniquePackages.add(record.getPackageName());
        }
        if (uniquePackages.size() > 1) {
            throw new IllegalArgumentException("Plusieurs packages differents trouves. Modifiez un seul package a la fois.");
        }

        String oldPackage = parts[1] + "_" + parts[2];
        int providerId = getProviderIdFromResults(results);

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
            if (packageExists(conn, newPackage, providerId)) {
                updateExistingPackage(conn, oldPackage, newPackage, providerId);
            } else {
                insertNewPackage(conn, oldPackage, newPackage, providerId);
            }
            updateLigneKbart(conn, oldPackage, newPackage, providerId);
        }
    }

    private void loadDatabaseConfig() throws Exception {
        Properties prop = new Properties();
        try (FileInputStream f = new FileInputStream(new File(CONFIG_FILE))) {
            prop.load(f);
            dbUrl = prop.getProperty("DB_URL");
            dbUser = prop.getProperty("DB_USER");
            dbPassword = prop.getProperty("DB_PASSWORD");
        }
    }

    private boolean isLastPackage(List<PackageRecord> results, int providerId, String packageName) {
        int count = 0;
        for (PackageRecord record : results) {
            if (record.getProviderId() == providerId && packageName.equals(record.getPackageName())) {
                count++;
            }
        }
        return count == 1;
    }

    private int deleteAllForPackage(int providerId, String packageName, String provider) throws SQLException {
        String sqlDeletePP = "DELETE FROM provider_package WHERE package = ? AND provider_idt_provider = ?";
        String sqlDeleteLK = "DELETE FROM ligne_kbart WHERE provider_package_package = ? AND provider_package_idt_provider = ?";
        String sqlInsert = "INSERT INTO provider_package_deleted (PACKAGE, PROVIDER) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement deleteStmtPP = conn.prepareStatement(sqlDeletePP);
             PreparedStatement deleteStmtLK = conn.prepareStatement(sqlDeleteLK);
             PreparedStatement insertStmt = conn.prepareStatement(sqlInsert)) {
            deleteStmtPP.setString(1, packageName);
            deleteStmtPP.setInt(2, providerId);

            deleteStmtLK.setString(1, packageName);
            deleteStmtLK.setInt(2, providerId);

            insertStmt.setString(1, packageName);
            insertStmt.setString(2, provider);

            int rowsDeleted = deleteStmtPP.executeUpdate();
            deleteStmtLK.executeUpdate();
            insertStmt.executeUpdate();
            return rowsDeleted;
        }
    }

    private int getProviderIdFromResults(List<PackageRecord> results) {
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Aucun resultat.");
        }
        return results.get(0).getProviderId();
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
        String sql =
            "INSERT INTO PROVIDER_PACKAGE (PACKAGE, DATE_P, LABEL_ABES, PROVIDER_IDT_PROVIDER) " +
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
        String sql = "UPDATE PROVIDER_PACKAGE SET PACKAGE = ? WHERE PROVIDER_IDT_PROVIDER = ? AND PACKAGE = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPackage);
            stmt.setInt(2, providerId);
            stmt.setString(3, oldPackage);
            stmt.executeUpdate();
        }
    }

    private void updateLigneKbart(Connection conn, String oldPackage, String newPackage, int providerId) throws SQLException {
        String sql =
            "UPDATE ligne_kbart SET provider_package_package = ? " +
            "WHERE provider_package_idt_provider = ? AND provider_package_package = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPackage);
            stmt.setInt(2, providerId);
            stmt.setString(3, oldPackage);
            stmt.executeUpdate();
        }
    }
}
