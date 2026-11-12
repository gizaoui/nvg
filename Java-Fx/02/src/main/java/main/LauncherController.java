package main;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class LauncherController {

    @FXML private Button openImportButton;

    /** Ouvre import-zip.fxml comme boîte de dialogue modale. */
    @FXML
    private void onOpenImport() {
        try {
            Stage dialog = new Stage();
            dialog.initOwner(openImportButton.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL); // bloque la fenêtre parente jusqu'à la fermeture

            FXMLLoader loader = new FXMLLoader(getClass().getResource("import-zip.fxml"));
            loader.setRoot(dialog); // le Stage de la boîte de dialogue est la racine du FXML (fx:root)
            loader.load();

            dialog.showAndWait();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir la fenêtre : " + e.getMessage()).showAndWait();
        }
    }
}
