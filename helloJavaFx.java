<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.control.Button?>
<?import javafx.scene.control.TextField?>
<?import javafx.scene.layout.HBox?>

<HBox spacing="10.0" 
      xmlns="http://javafx.com/javafx" 
      xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.exemple.InputController">
   <children>
      <TextField fx:id="textField" promptText="Saisissez votre texte..." HBox.hgrow="ALWAYS" />
      <Button text="Copier" onAction="#handleCopy" />
   </children>
</HBox>





<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.scene.control.TextArea?>
<?import javafx.scene.layout.VBox?>

<VBox xmlns="http://javafx.com/javafx" 
      xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.exemple.DisplayController">
   <children>
      <TextArea fx:id="textArea" VBox.vgrow="ALWAYS" />
   </children>
</VBox>



<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.layout.VBox?>

<VBox spacing="15.0" prefHeight="300.0" prefWidth="450.0" 
      xmlns="http://javafx.com/javafx" 
      xmlns:fx="http://javafx.com/fxml"
      fx:controller="com.exemple.MainController">
   <padding>
      <Insets bottom="15.0" left="15.0" right="15.0" top="15.0" />
   </padding>
   <children>
      <!-- Inclusion de la vue de saisie -->
      <fx:include fx:id="input" source="input.fxml" />
      
      <!-- Inclusion de la vue d'affichage -->
      <fx:include fx:id="display" source="display.fxml" VBox.vgrow="ALWAYS" />
   </children>
</VBox>




package com.exemple;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class InputController {

    @FXML
    private TextField textField;

    private TextSubmitListener listener;

    // Interface fonctionnelle pour remplacer le Consumer
    @FunctionalInterface
    public interface TextSubmitListener {
        void onTextSubmitted(String text);
    }

    public void setOnTextSubmitted(TextSubmitListener listener) {
        this.listener = listener;
    }

    @FXML
    private void handleCopy() {
        if (listener != null) {
            listener.onTextSubmitted(textField.getText());
        }
    }
}




package com.exemple;

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




package com.exemple;

import javafx.fxml.FXML;

public class MainController {

    // Injection automatique des contrôleurs des vues incluses
    // Convention de nommage : fx:id ("input" / "display") + "Controller"
    @FXML
    private InputController inputController;

    @FXML
    private DisplayController displayController;

    @FXML
    public void initialize() {
        // Liaison de l'événement de saisie avec le composant d'affichage
        inputController.setOnTextSubmitted(text -> {
            displayController.appendText(text);
        });
    }
}



package com.exemple;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/exemple/main.fxml"));
        Scene scene = new Scene(loader.load());

        primaryStage.setTitle("Exemple JavaFX - Inclusions FXML");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}






