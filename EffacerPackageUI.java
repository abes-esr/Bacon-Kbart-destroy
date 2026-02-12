import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class EffacerPackageUI extends JFrame {
    private final JTextField searchField = new JTextField(40);
    private final JTextField newPackageField = new JTextField(25);
    private final JButton searchButton = new JButton("Rechercher");
    private final JButton deleteSelectedButton = new JButton("Supprimer la ligne selectionnee");
    private final JButton deleteAllButton = new JButton("Supprimer toutes les lignes");
    private final JButton renameButton = new JButton("Renommer le package");
    private final DefaultTableModel tableModel = new DefaultTableModel(
        new Object[]{"#", "Provider", "Provider ID", "Package", "Date"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable resultTable = new JTable(tableModel);

    private EffacerPackageService service;
    private List<PackageRecord> currentResults = new ArrayList<>();
    private String[] currentParts;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                EffacerPackageUI app = new EffacerPackageUI();
                app.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Erreur au demarrage : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public EffacerPackageUI() throws Exception {
        super("EffacerPackage - Interface visuelle");
        this.service = new EffacerPackageService();
        initUi();
        bindActions();
    }

    private void initUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(1100, 600));
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBorder(BorderFactory.createTitledBorder("Recherche"));
        topPanel.add(new JLabel("Valeur provider_package :"));
        topPanel.add(searchField);
        topPanel.add(searchButton);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actionsPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        actionsPanel.add(deleteSelectedButton);
        actionsPanel.add(deleteAllButton);
        actionsPanel.add(new JLabel("Nouveau package :"));
        actionsPanel.add(newPackageField);
        actionsPanel.add(renameButton);

        resultTable.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Resultats"));

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(topPanel, BorderLayout.NORTH);
        northPanel.add(actionsPanel, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void bindActions() {
        searchButton.addActionListener(e -> onSearch());
        deleteSelectedButton.addActionListener(e -> onDeleteSelected());
        deleteAllButton.addActionListener(e -> onDeleteAll());
        renameButton.addActionListener(e -> onRename());
    }

    private void onSearch() {
        String input = searchField.getText().trim();
        if (input.isEmpty()) {
            showInfo("Renseigne une valeur de recherche.");
            return;
        }

        try {
            currentParts = service.validateAndParseInput(input);
            currentResults = service.fetchPackages(currentParts);
            refreshTable();

            if (currentResults.isEmpty()) {
                showInfo("Aucun resultat.");
            } else {
                showInfo(currentResults.size() + " ligne(s) trouvee(s).");
            }
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void onDeleteSelected() {
        if (currentResults.isEmpty()) {
            showInfo("Aucun resultat a supprimer.");
            return;
        }

        int selectedRow = resultTable.getSelectedRow();
        if (selectedRow < 0) {
            showInfo("Selectionne une ligne.");
            return;
        }

        int modelRow = resultTable.convertRowIndexToModel(selectedRow);
        int index = (int) tableModel.getValueAt(modelRow, 0);
        PackageRecord selected = currentResults.get(index - 1);

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Supprimer la ligne du " + selected.getDate() + " ?",
            "Confirmation",
            JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int rowsDeleted = service.deleteSingle(currentResults, index, currentParts[0]);
            showInfo(rowsDeleted + " ligne(s) supprimee(s).");
            onSearch();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void onDeleteAll() {
        if (currentResults.isEmpty()) {
            showInfo("Aucun resultat a supprimer.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Supprimer toutes les lignes affichees ?",
            "Confirmation",
            JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int rowsDeleted = service.deleteAll(currentResults, currentParts);
            showInfo(rowsDeleted + " ligne(s) supprimee(s) et archivee(s).");
            onSearch();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void onRename() {
        if (currentResults.isEmpty()) {
            showInfo("Aucun resultat a renommer.");
            return;
        }

        String newPackage = newPackageField.getText().trim();
        if (newPackage.isEmpty()) {
            showInfo("Renseigne un nouveau nom de package.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Renommer le package selectionne en '" + newPackage + "' ?",
            "Confirmation",
            JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            service.modifyPackage(currentResults, currentParts, newPackage);
            showInfo("Package renomme.");
            onSearch();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (PackageRecord record : currentResults) {
            tableModel.addRow(new Object[]{
                record.getIndex(),
                record.getProvider(),
                record.getProviderId(),
                record.getPackageName(),
                record.getDate()
            });
        }
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }
}
