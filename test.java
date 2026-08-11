 import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainJava8SansExtension {

    public static void main(String[] args) {
        // 1. Force l'initialisation du toolkit JavaFX sous Java 8
        new JFXPanel();

        // 2. Exécute le code sur le thread JavaFX Application
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                // 3. Instanciation directe de la classe Stage
                Stage monStage = new Stage();
                
                // 4. Création de l'interface graphique
                Label message = new Label("Application JavaFX en Java 8 sans extension !");
                StackPane racine = new StackPane(message);
                Scene scene = new Scene(racine, 450, 200);
                
                // 5. Configuration et affichage
                monStage.setTitle("Exemple JavaFX 8");
                monStage.setScene(scene);
                
                // Assure la fermeture propre du processus
                monStage.setOnCloseRequest(event -> Platform.exit());
                
                monStage.show();
            }
        });
    }
}
