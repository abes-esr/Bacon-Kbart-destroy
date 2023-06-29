import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.text.SimpleDateFormat; 
import java.util.Date;
import java.util.AbstractMap.SimpleEntry;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.util.Properties;

public class EffacerPackage {

    public static Properties getProperties() throws Exception {
        FileInputStream f;
        Properties prop = new Properties();
        try {
            f = new FileInputStream(new File("/home/devel/MaintenanceBacon/.env"));
            prop.load(f);
        } catch (Exception e) {
            throw e;
        }
        return prop;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String mot1, mot2, mot3, mot4 = "%";
        System.out.print("Entrez une valeur : ");
        String inputVariable = sc.nextLine();
        String[] mots = inputVariable.split("_");
        try {
            if (mots.length < 3) {
                throw new IllegalArgumentException("Je n'ai pas assez d'informations pour retourner un résultat, merci d'affiner votre recherche.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }
        mot1 = mots[0];
        mot2 = mots[1];
        mot3 = mots[2];
        mot4 = "";
        if (mots.length >= 4) {
            mot4 = mots[3];
        }
        List<SimpleEntry<Integer, String>> tab = new ArrayList<>();
        Integer n = 0;
        String url, user, password;
        try {
            Properties prop = getProperties();
            url = prop.getProperty("DB_URL");
            user = prop.getProperty("DB_USER");
            password = prop.getProperty("DB_PASSWORD");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
	// requête SQL avec une variable
        String sql = "SELECT provider, package, idt_provider, date_p FROM provider, provider_package WHERE idt_provider=provider_idt_provider AND provider = '" + mot1 + "' AND package LIKE '" + mot2 + "_" + mot3 + "%'";
	Connection conn = null;
        try {
            // connexion à la base de données
            conn = DriverManager.getConnection(url, user, password);
            System.out.println("Requête : " + sql);
	} catch (SQLException e) {
	System.out.println("Connxion TLS NOK");
	e.printStackTrace();
	return;
	}
	System.out.println("Conn = " + conn);
	if (conn != null) {
	try {
            PreparedStatement pstmt = conn.prepareStatement(sql); 
            // exécution de la requête
            ResultSet rs = pstmt.executeQuery();
	    System.out.println(rs.isBeforeFirst());
	    // vérifier si le résultat est vide
	    if (!rs.isBeforeFirst()) {
		System.out.println("La requête ne renvoie aucun résultat.");
		rs.close();
		pstmt.close();
		conn.close();
		return;
	    }
	    int providerIdG = 0;
            // traitement des résultats
            while (rs.next()) {
                n++;
                String date = rs.getString("date_p");
                String[] dateString = date.split(" ");
                String providerName = rs.getString("provider");
                int providerId = rs.getInt("idt_provider");
                String packageName = rs.getString("package");
                String datePackage = dateString[0];
                System.out.println("package " + n + " : " + providerName + " " + packageName + " " + datePackage);
                tab.add(new SimpleEntry<>(n, providerName + " " + providerId + " " + packageName + " " + datePackage));
		providerIdG = providerId;
            }
            rs.close();
            pstmt.close();
            conn.close();
            Scanner dc = new Scanner(System.in);
            System.out.print("Entrez une valeur à supprimer (0 -> aucune, T -> toutes) : ");
            String supVariable = dc.next();
	    int supVariableInt = -1;
		try {
		    supVariableInt = Integer.parseInt(supVariable);
		    if (supVariableInt <= 0 || supVariableInt > tab.size()) {
 		       throw new Exception("La valeur doit être comprise entre 1 et " + tab.size());
 		   }
		} catch (NumberFormatException e) {
 		   if (!supVariable.equalsIgnoreCase("T")) {
		        System.out.println("Valeur invalide !");
 		       return; // ou utiliser break; si vous êtes dans une boucle
  		  }
		} catch (Exception e) {
		    System.out.println(e.getMessage());
 		   return; // ou utiliser break; si vous êtes dans une boucle
		}
            switch (supVariable) {
	    case "0":  // ne rien supprimer
		System.out.println("Aucune ligne n'est supprimée");
        	break;
            case "T":  // supprimer tout
                Scanner scannerT = new Scanner(System.in);
                int rowsAffectedT = tab.size();
                System.out.println("Êtes-vous sûr de vouloir supprimer " + rowsAffectedT + " lignes du package " + mot2 + "_" + mot3 + "%   de l'éditeur " + mot1);
                System.out.println("Veuillez répondre par 'oui' ou 'non': ");
                String confirmationT = scannerT.nextLine();
                if (confirmationT.equalsIgnoreCase("oui")) {
                        String sqlDelT = "DELETE FROM provider_package WHERE package LIKE '" + mot2 + "_" + mot3 + "% AND provider_idt_provider = " + providerIdG + "'";
                        System.out.println("Requête de suppression : " + sqlDelT);
                        PreparedStatement deleteStmt = null;
                        try (Connection connDT = DriverManager.getConnection(url, user, password);
				PreparedStatement deleteStmtT = connDT.prepareStatement(sqlDelT)) {
                                deleteStmtT.setString(1, mot2 + "_" + mot3 + "%");
				deleteStmt.setInt(2, providerIdG);

                                rowsAffectedT = deleteStmtT.executeUpdate();
                                System.out.println("Vous avez effacé " + rowsAffectedT + " lignes du package " + mot2 + "_" + mot3 + "%   de l'éditeur " + mot1);
                                } catch (SQLException e) {
                                        e.printStackTrace();
                                }
                } break;
		default:
		SimpleEntry<Integer, String> element = tab.get(supVariableInt - 1);
		String[] result = element.getValue().split(" ");
		String providerName = result[0];
		int providerIdg = Integer.parseInt(result[1]);
		String packageName = result[2];
		String datePackage = result[3];
		Scanner scannerL = new Scanner(System.in);
		System.out.println("Êtes-vous sûr de vouloir supprimer la ligne qui a pour date " + datePackage + " du package " + packageName + " de l'éditeur " + providerName + " ?");
		System.out.println("Veuillez répondre par 'oui' ou 'non': ");
		String confirmationL = scannerL.nextLine();
		if (confirmationL.equalsIgnoreCase("oui")) {
			String sqlDel = "DELETE FROM provider_package WHERE package = ? AND date_p = ? AND provider_idt_provider = ?";
			System.out.println("Requête de suppression : " + sqlDel);
			try (Connection connD = DriverManager.getConnection(url, user, password);
				PreparedStatement deleteStmt = connD.prepareStatement(sqlDel)) {
				deleteStmt.setInt(3, providerIdG);
				deleteStmt.setString(1, packageName);
				deleteStmt.setDate(2, java.sql.Date.valueOf(datePackage));
 
				int rowsAffected = deleteStmt.executeUpdate();
				System.out.println("Vous avez effacé la ligne qui a pour date " + datePackage + " du package " + packageName + " de l'éditeur " + providerName);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		break;
	}
	} catch (SQLException e) {
            System.out.println(e.getMessage());
        }

	}
    }
}
