import java.util.List;
import java.util.Scanner;

public class EffacerPackage {
    private final Scanner scanner = new Scanner(System.in);
    private EffacerPackageService service;

    public static void main(String[] args) {
        new EffacerPackage().run();
    }

    private void run() {
        try {
            service = new EffacerPackageService();
            System.out.println("Connexion TLS : " + service.getDbUrl());

            String input = getUserInput("Entrez une valeur (exemple : GALLICA_GLOBAL_ALLJOURNALS ou GALLICA_GLOBAL_ALL%) : ");
            String[] parts = service.validateAndParseInput(input);
            List<PackageRecord> results = service.fetchPackages(parts);

            if (results.isEmpty()) {
                System.out.println("La requete ne renvoie aucun resultat.");
                return;
            }

            displayResults(results);
            handleUserAction(results, parts);
        } catch (Exception e) {
            System.out.println("Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayResults(List<PackageRecord> results) {
        System.out.println();
        System.out.println("=== Resultats trouves ===");
        for (PackageRecord record : results) {
            System.out.println(
                record.getIndex() + " : " +
                record.getProvider() + " " +
                record.getProviderId() + " " +
                record.getPackageName() + " " +
                record.getDate()
            );
        }
    }

    private void handleUserAction(List<PackageRecord> results, String[] parts) {
        String action = getUserInput("Entrez une action (0->aucune, T->toutes, M->modification, ou numero) : ");

        try {
            switch (action.toUpperCase()) {
                case "0":
                    System.out.println("Aucune action effectuee.");
                    break;
                case "T":
                    if (!confirmAction("Etes-vous sur de vouloir supprimer " + results.size() + " lignes ?")) {
                        return;
                    }
                    int deletedCount = service.deleteAll(results, parts);
                    System.out.println("OK " + deletedCount + " lignes supprimees et archivees.");
                    break;
                case "M":
                    String newPackage = getUserInput("Entrez le nouveau nom du package : ");
                    service.modifyPackage(results, parts, newPackage);
                    System.out.println("OK Package renomme.");
                    break;
                default:
                    int index = Integer.parseInt(action);
                    PackageRecord selected = results.get(index - 1);
                    if (!confirmAction("Supprimer la ligne du " + selected.getDate() + " ?")) {
                        return;
                    }
                    int rowsDeleted = service.deleteSingle(results, index, parts[0]);
                    System.out.println("OK " + rowsDeleted + " ligne(s) supprimee(s).");
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de l'action : " + e.getMessage());
        }
    }

    private String getUserInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private boolean confirmAction(String message) {
        String response = getUserInput(message + " (oui/non) : ");
        return response.equalsIgnoreCase("oui");
    }
}
