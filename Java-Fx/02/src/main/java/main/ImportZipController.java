package main;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ImportZipController {

    /** Dossier où sont stockées les images existantes (à adapter). */
    private static final Path IMAGES_DIR = Path.of("images");

    // --- Éléments FXML ---
    @FXML private TextField zipPathField;
    @FXML private Button browseButton;
    @FXML private Button replaceAllArtifactsButton;
    @FXML private Button renameButton;
    @FXML private Button replaceAllImagesButton;
    @FXML private TreeView<ZipItem> artifactsTree;
    @FXML private ListView<String> existingImagesList;
    @FXML private Label noDuplicateLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Button importButton;
    @FXML private Button closeButton;

    private File zipFile;
    private final List<ZipItem> items = new ArrayList<>();

    /** Élément du zip à importer. */
    public static class ZipItem {
        final String entryName;   // chemin dans le zip
        String targetName;        // nom de destination (modifiable via Rename)
        boolean replace;          // true si l'utilisateur accepte l'écrasement

        ZipItem(String entryName) {
            this.entryName = entryName;
            this.targetName = entryName;
        }

        boolean isDuplicate() {
            return Files.exists(IMAGES_DIR.resolve(targetName));
        }

        /** Un conflit non résolu : doublon sans autorisation de remplacer. */
        boolean isUnresolved() {
            return isDuplicate() && !replace;
        }

        @Override
        public String toString() {
            return replace && isDuplicate() ? targetName + "  (sera remplacé)" : targetName;
        }
    }

    @FXML
    private void initialize() {
        artifactsTree.setRoot(new TreeItem<>());

        // Affiche en rouge les noms déjà utilisés et non résolus
        artifactsTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(ZipItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    setStyle(item.isUnresolved() ? "-fx-text-fill: red;" : "");
                }
            }
        });

        // Sélection à gauche -> sélection de l'image existante correspondante à droite
        artifactsTree.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            existingImagesList.getSelectionModel().clearSelection();
            if (sel == null || sel.getValue() == null) return;
            String target = sel.getValue().targetName;
            if (existingImagesList.getItems().contains(target)) {
                existingImagesList.getSelectionModel().select(target);
            }
        });

        // Clic sur une image existante : à brancher sur votre fenêtre de comparaison
        existingImagesList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                compare(sel);
            }
        });

        renameButton.disableProperty().bind(
                artifactsTree.getSelectionModel().selectedItemProperty().isNull());

        refresh();
    }

    // --- Actions ---

    @FXML
    private void onBrowse() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une archive zip");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archives zip", "*.zip"));
        File file = chooser.showOpenDialog(browseButton.getScene().getWindow());
        if (file != null) {
            loadZip(file);
        }
    }

    @FXML
    private void onReplaceAllArtifacts() {
        replaceAllDuplicates();
    }

    @FXML
    private void onReplaceAllImages() {
        replaceAllDuplicates();
    }

    @FXML
    private void onRename() {
        TreeItem<ZipItem> selected = artifactsTree.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) return;

        ZipItem item = selected.getValue();
        TextInputDialog dialog = new TextInputDialog(item.targetName);
        dialog.setTitle("Renommer");
        dialog.setHeaderText("Nouveau nom pour « " + item.entryName + " »");
        dialog.setContentText("Nom :");

        Optional<String> result = dialog.showAndWait();
        result.map(String::trim).filter(s -> !s.isEmpty()).ifPresent(name -> {
            item.targetName = name;
            item.replace = false;   // le nouveau nom est évalué à nouveau
            refresh();
        });
    }

    @FXML
    private void onImport() {
        if (zipFile == null || items.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Aucune archive chargée.");
            return;
        }

        // Liste figée sur le thread JavaFX : les doublons non résolus sont ignorés
        List<ZipItem> toImport = items.stream().filter(i -> !i.isUnresolved()).toList();
        int skipped = items.size() - toImport.size();
        if (toImport.isEmpty()) {
            alert(Alert.AlertType.WARNING, "Tous les éléments sont en conflit : remplacez ou renommez-les.");
            return;
        }

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                Path base = IMAGES_DIR.toAbsolutePath().normalize();
                Files.createDirectories(base);
                int done = 0;
                try (ZipFile zip = new ZipFile(zipFile)) {
                    for (ZipItem item : toImport) {
                        ZipEntry entry = zip.getEntry(item.entryName);
                        Path target = base.resolve(item.targetName).normalize();
                        if (entry == null || !target.startsWith(base)) {   // protection zip-slip
                            throw new IOException("Entrée invalide : " + item.entryName);
                        }
                        Files.createDirectories(target.getParent());
                        try (InputStream in = zip.getInputStream(entry)) {
                            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                        updateProgress(++done, toImport.size());
                    }
                }
                return done;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        setBusy(true);

        task.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1.0);
            setBusy(false);
            refresh();
            alert(Alert.AlertType.INFORMATION,
                    task.getValue() + " élément(s) importé(s)"
                            + (skipped > 0 ? ", " + skipped + " ignoré(s) (conflit)." : "."));
        });
        task.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            setBusy(false);
            alert(Alert.AlertType.ERROR, "Échec de l'import : " + task.getException().getMessage());
        });

        Thread thread = new Thread(task, "import-zip");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onClose() {
        ((Stage) closeButton.getScene().getWindow()).close();
    }

    // --- Logique interne ---

    private void loadZip(File file) {
        List<ZipItem> loaded = new ArrayList<>();
        try (ZipFile zip = new ZipFile(file)) {
            Collections.list(zip.entries()).stream()
                    .filter(e -> !e.isDirectory())
                    .map(ZipEntry::getName)
                    .sorted()
                    .forEach(name -> loaded.add(new ZipItem(name)));
        } catch (IOException e) {
            alert(Alert.AlertType.ERROR, "Impossible de lire le zip : " + e.getMessage());
            return;
        }
        zipFile = file;
        zipPathField.setText(file.getAbsolutePath());
        items.clear();
        items.addAll(loaded);
        progressBar.setProgress(0);
        refresh();
    }

    private void replaceAllDuplicates() {
        items.stream().filter(ZipItem::isDuplicate).forEach(i -> i.replace = true);
        refresh();
    }

    /** Reconstruit les deux arbres à partir de la liste des éléments. */
    private void refresh() {
        TreeItem<ZipItem> left = artifactsTree.getRoot();
        left.getChildren().clear();
        items.forEach(i -> left.getChildren().add(new TreeItem<>(i)));

        existingImagesList.getItems().setAll(
                items.stream()
                        .filter(ZipItem::isDuplicate)
                        .map(i -> i.targetName)
                        .toList());

        noDuplicateLabel.setVisible(existingImagesList.getItems().isEmpty());
        artifactsTree.refresh();   // recalcule la couleur des cellules
    }

    private void compare(String imageName) {
        // À implémenter : ouvrir une fenêtre comparant IMAGES_DIR.resolve(imageName)
        // avec l'entrée correspondante du zip.
    }

    private void setBusy(boolean busy) {
        importButton.setDisable(busy);
        browseButton.setDisable(busy);
        replaceAllArtifactsButton.setDisable(busy);
        replaceAllImagesButton.setDisable(busy);
    }

    private void alert(Alert.AlertType type, String message) {
        new Alert(type, message).showAndWait();
    }
}