<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.TextField?>
<?import javafx.scene.layout.VBox?>

<VBox alignment="CENTER" spacing="15" xmlns="http://javafx.com/javafx"
      xmlns:fx="http://javafx.com/fxml" fx:controller="com.example.HelloController">
    <padding>
        <Insets top="20" right="20" bottom="20" left="20"/>
    </padding>

    <Label text="Entrez votre nom :"/>
    <TextField fx:id="nameField" promptText="Nom" onAction="#onGreet"/>
    <Button text="Dire bonjour" onAction="#onGreet"/>
    <Label fx:id="greetingLabel"/>
</VBox>



package com.example;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class HelloController {

    @FXML
    private TextField nameField;

    @FXML
    private Label greetingLabel;

    @FXML
    private void onGreet() {
        String name = nameField.getText().trim();
        greetingLabel.setText(name.isEmpty() ? "Bonjour, inconnu !" : "Bonjour, " + name + " !");
    }
}


package com.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApp extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("hello-view.fxml"));
        stage.setTitle("Hello FXML");
        stage.setScene(new Scene(root, 360, 220));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
