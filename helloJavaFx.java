<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.TextField?>
<?import javafx.scene.layout.HBox?>

<HBox spacing="10.0" xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
      fx:controller="InputController">
    <children>
        <TextField fx:id="textField" promptText="Saisissez du texte..." HBox.hgrow="ALWAYS" />
        <Button text="Copier" onAction="#handleCopy" />
    </children>
</HBox>



<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.TextArea?>
<?import javafx.scene.layout.VBox?>

<VBox xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
      fx:controller="DisplayController">
    <children>
        <TextArea fx:id="textArea" VBox.vgrow="ALWAYS" />
    </children>
</VBox>



import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import java.util.function.Consumer;

public class InputController {

    @FXML
    private TextField textField;

    private Consumer<String> onTextSubmittedAction;

    // Permet de définir l'action à exécuter lors du clic
    public void setOnTextSubmitted(Consumer<String> action) {
        this.onTextSubmittedAction = action;
    }

    @FXML
    private void handleCopy() {
        if (onTextSubmittedAction != null) {
            onTextSubmittedAction.accept(textField.getText());
        }
    }
}



import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class DisplayController {

    @FXML
    private TextArea textArea;

    public void appendText(String text) {
        if (textArea.getText().isEmpty()) {
            textArea.setText(text);
        } else {
            textArea.appendText("\n" + text);
        }
    }
}





<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.geometry.Insets?>
<?import javafx.scene.layout.VBox?>

<VBox spacing="15.0" prefWidth="400.0" prefHeight="300.0" 
      xmlns="http://javafx.com/javafx" xmlns:fx="http://javafx.com/fxml"
      fx:controller="MainController">
    <padding>
        <Insets top="15" right="15" bottom="15" left="15" />
    </padding>
    <children>
        <!-- Inclusions des deux sous-vues -->
        <fx:include fx:id="input" source="input.fxml" />
        <fx:include fx:id="display" source="display.fxml" VBox.vgrow="ALWAYS" />
    </children>
</VBox>



import javafx.fxml.FXML;

public class MainController {

    // JavaFX injecte automatiquement les contrôleurs des sous-vues
    // grâce à la convention de nommage : "fx:id" + "Controller"
    @FXML
    private InputController inputController;

    @FXML
    private DisplayController displayController;

    @FXML
    public void initialize() {
        // On lie la saisie de 'input' à l'affichage dans 'display'
        inputController.setOnTextSubmitted(text -> {
            displayController.appendText(text);
        });
    }
}


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main.fxml"));
        Scene scene = new Scene(loader.load());

        primaryStage.setTitle("Exemple Inclusions FXML");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
