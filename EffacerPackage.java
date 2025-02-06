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

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
        	String mot1, mot2, mot3, mot4;
        	System.out.print("Entrez une valeur (exemple : GALLICA_GLOBAL_ALLJOURNALS ou GALLICA_GLOBAL_ALL% pour une recherche plus large): ");
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
        	if (mots.length >= 4) {
           		 mot4 = mots[3];
        	}
        	List<SimpleEntry<Integer, String>> tab = new ArrayList<>();
        	Integer n = 0;
        	String url, user, password;
        	try {
			Properties prop = new Properties();
			FileInputStream f;
			f = new FileInputStream(new File("config.properties"));
			prop.load(f);
			f.close();
			url = prop.getProperty("DB_URL");
			user = prop.getProperty("DB_USER");
			password = prop.getProperty("DB_PASSWORD");
			System.out.println("Connxion TLS : " + url);
        	} catch (Exception e) {
            		e.printStackTrace();
            		return;
        	}
        	String sql = "SELECT provider, package, idt_provider, date_p FROM provider, provider_package WHERE idt_provider=provider_idt_provider AND provider = '" + mot1 + "' AND package LIKE '" + mot2 + "_" + mot3 + "'"; // requête SQL avec une variable
		Connection conn = null;
        	try {
            		conn = DriverManager.getConnection(url, user, password); // Connexion à la base de donnée
            		System.out.println("Requête : " + sql);
		} catch (SQLException e) {
			System.out.println("Connxion TLS NOK : " + url);
			e.printStackTrace();
			return;
		}
		System.out.println("Conn = " + conn);
		if (conn != null) {
			try {   // Préparation, Execution de la Reqête SQL
            			PreparedStatement pstmt = conn.prepareStatement(sql); 
            			ResultSet rs = pstmt.executeQuery();
	    			System.out.println(rs.isBeforeFirst());
	    			if (!rs.isBeforeFirst()) {  // Vérifier si le résultat est vide
					System.out.println("La requête ne renvoie aucun résultat.");
					rs.close();
					pstmt.close();
					conn.close();
					return;
	    			}
	    			int providerIdG = 0;
            			while (rs.next()) { // TRaitement des résultats
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
            			System.out.print("Entrez une valeur à supprimer (0 -> aucune, T -> toutes, M -> modification) : ");
            			String supVariable = dc.next();

            			int supVariableInt = -1;
				try {
		    			supVariableInt = Integer.parseInt(supVariable);
		    			if (supVariableInt <= 0 || supVariableInt > tab.size()) {
 		       				throw new Exception("La valeur doit être comprise entre 1 et " + tab.size());
 		   			}
				} catch (NumberFormatException e) {
					System.out.println("Var : " + supVariable + " valeur : " + supVariable.equalsIgnoreCase("M"));
 		   			if (!supVariable.equalsIgnoreCase("T") && !supVariable.equalsIgnoreCase("M")) {
		        			System.out.println("Valeur invalide !");
 		       				return; // ou utiliser break; si vous êtes dans une boucle
  		  			}
				} catch (Exception e) {
		    			System.out.println(e.getMessage());
		    			System.out.println("Var2 : " + supVariable + " valeur : " + supVariable.equalsIgnoreCase("M"));
 		   			return; // ou utiliser break; si vous êtes dans une boucle
				}
            			switch (supVariable) {
	    			case "0":  // ne rien supprimer
					System.out.println("Aucune ligne n'est supprimée");
        				break;

            			case "T":  // supprimer tout
                			Scanner scannerT = new Scanner(System.in);
                			int rowsAffectedT = tab.size();
                			System.out.println("Êtes-vous sûr de vouloir supprimer " + rowsAffectedT + " lignes du package " + mot2 + "_" + mot3 + "   de l'éditeur " + mot1);
                			System.out.println("Veuillez répondre par 'oui' ou 'non': ");
                			String confirmationT = scannerT.nextLine();
                			if (confirmationT.equalsIgnoreCase("oui")) {
                        			String sqlDelT1 = "DELETE FROM provider_package WHERE package LIKE '" + mot2 + "_" + mot3 + "' AND provider_idt_provider = " + providerIdG ;
						String sqlDelT2 = "DELETE FROM ligne_kabart WHERE provider_package_package LIKE '" + mot2 + "_" + mot3 + "' AND provider_package_idt_provider = " + providerIdG ;
						String sqlInsT = "INSERT INTO provider_package_deleted (PACKAGE, PROVIDER) VALUES ('" + mot2 + "_" + mot3 + "', '" + mot1 + "')";
                        			System.out.println("Requête de suppression : " + sqlDelT1);
                        			PreparedStatement deleteStmt = null;
                        			try (Connection connDT = DriverManager.getConnection(url, user, password);
						PreparedStatement insertStmtT = connDT.prepareStatement(sqlInsT);
						PreparedStatement deleteStmtT1 = connDT.prepareStatement(sqlDelT1);
						PreparedStatement deleteStmtT2 = connDT.prepareStatement(sqlDelT2)) { 
                                			rowsAffectedT = deleteStmtT.executeUpdate();
							insertStmtT.executeUpdate();
                                			System.out.println("Vous avez effacé " + rowsAffectedT + " lignes du package " + mot2 + "_" + mot3 + "   de l'éditeur " + mot1);
                                		} catch (SQLException e) {
                                        		e.printStackTrace();
                                		}
                			} break;

				case "M":
            				Scanner scannerM = new Scanner(System.in);
            				System.out.println("Entré le nouveau nom de Package sans le provider et un seul package à la fois :  ");
            				String newPackageM = scannerM.nextLine();
					 // Vérifier si le nom kabart existe déjà
                    			String sqlCheckExists = "SELECT COUNT(*) FROM PROVIDER_PACKAGE WHERE PACKAGE = '" + newPackageM + "' and PROVIDER_IDT_PROVIDER = " + providerIdG ;
                    			boolean kabartExists = false;
					try (Connection connCheck = DriverManager.getConnection(url, user, password);
					PreparedStatement checkStmt = connCheck.prepareStatement(sqlCheckExists);
					ResultSet rstmp = checkStmt.executeQuery()) {
						if (rstmp.next() && rstmp.getInt(1) > 0) {
							kabartExists = true; // kabart existe déjà
						}       
					} catch (SQLException e) {
						e.printStackTrace();
					}
					if (!kabartExists) {
						System.out.println("le Kabart n'existe pas !!!");
            				String sqlInsertM = "INSERT INTO PROVIDER_PACKAGE (PACKAGE, DATE_P, LABEL_ABES, PROVIDER_IDT_PROVIDER) SELECT '" + newPackageM + "', DATE_P, LABEL_ABES, PROVIDER_IDT_PROVIDER FROM PROVIDER_PACKAGE WHERE PROVIDER_IDT_PROVIDER= " + providerIdG + " AND PACKAGE= '" + mot2 + "_" + mot3 + "'" ;
					try (Connection connM = DriverManager.getConnection(url, user, password);
					PreparedStatement insertStmtM = connM.prepareStatement(sqlInsertM)) {
						insertStmtM.executeUpdate();
					} catch (SQLException e) {
						e.printStackTrace();
					} }
					else {
					String sqlUpdatePPM = "UPDATE PROVIDER_PACKAGE SET PACKAGE = '" + newPackageM + "' WHERE PROVIDER_IDT_PROVIDER= " + providerIdG + " AND PACKAGE= '" + mot2 + "_" + mot3 + "'" ;
					try (Connection connM = DriverManager.getConnection(url, user, password);
					PreparedStatement updatePPStmtM = connM.prepareStatement(sqlUpdatePPM)) {
						updatePPStmtM.executeUpdate();
					} catch (SQLException e) {
						e.printStackTrace();
					}
					}
						System.out.println("le Kabart existe !!!");
            				String sqlUpdateM = "UPDATE ligne_kbart SET provider_package_package = '" + newPackageM + "' WHERE provider_package_idt_provider = " + providerIdG + " AND provider_package_package = '" + mot2 + "_" + mot3 + "'" ;
            				try (Connection connM = DriverManager.getConnection(url, user, password);
                    			PreparedStatement updateStmtM = connM.prepareStatement(sqlUpdateM)) {
						updateStmtM.executeUpdate();
						System.out.println("UPDATE ligne_kbart SET provider_package_package = '" + newPackageM + "' WHERE provider_package_idt_provider = " + providerIdG + " AND provider_package_package = '" + mot2 + "_" + mot3 + "'");
                				System.out.println("Le nouveau package " + newPackageM + " a été ajouté à la base et toutes les lignes de la table ligne_kbart qui ont le package " + mot2 + "_" + mot3 + " et l'éditeur " + mot1 + " ont été mises à jour avec le nouveau package.");
            				} catch (SQLException e) {
                				e.printStackTrace();
            				}
            				break;



				default:
					SimpleEntry<Integer, String> element = tab.get(Integer.parseInt(supVariable) - 1);
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
