package com.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelloApp extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("Hello, World!");
        Button button = new Button("Cliquez-moi");
        button.setOnAction(e -> label.setText("Bonjour depuis JavaFX !"));

        VBox root = new VBox(15, label, button);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        stage.setTitle("Hello JavaFX");
        stage.setScene(new Scene(root, 320, 200));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
