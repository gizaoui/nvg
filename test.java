  import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class MainJava8Linux {

    // 1. Une classe interne cachée qui sert uniquement à éveiller proprement JavaFX sous Linux
    public static class LanceurInterne extends Application {
        @Override
        public void start(Stage primaryStage) {
            // On laisse cette méthode vide, on ne l'utilise pas pour notre logique
        }
    }

    public static void main(String[] args) {
        // 2. Débloquer la fermeture de l'application car notre LanceurInterne va se fermer immédiatement
        Platform.setImplicitExit(false);

        // 3. Démarrer le moteur JavaFX de manière standard en tâche de fond
        new Thread(new Runnable() {
            @Override
            public void run() {
                Application.launch(LanceurInterne.class, args);
            }
        }).start();

        // 4. Exécuter l'instanciation de notre Stage sur le thread JavaFX maintenant qu'il est initialisé
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                // 5. Instanciation directe et manuelle du Stage
                Stage monStage = new Stage();
                
                Label message = new Label("JavaFX 8 sous Linux sans extension directe !");
                StackPane racine = new StackPane(message);
                Scene scene = new Scene(racine, 400, 200);
                
                monStage.setTitle("Solution Java 8 Propre");
                monStage.setScene(scene);
                
                // 6. Forcer l'arrêt total du processus lors de la fermeture
                monStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                    @Override
                    public void handle(WindowEvent event) {
                        Platform.exit();
                        System.exit(0);
                    }
                });
                
                monStage.show();
            }
        });
    }
}
