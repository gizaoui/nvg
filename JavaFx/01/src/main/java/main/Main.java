package main;


import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main extends Application {

    private static final Logger logger = LogManager.getLogger(Main.class);
    Button button;


    @Override
    public void init() {
        logger.info(OutColor.GREEN.getCode() + "__________  INIT  __________" + OutColor.RESET.getCode());

    }

    @Override
    public void start(Stage stage) {
        logger.info(OutColor.GREEN.getCode() + "__________  START  __________" + OutColor.RESET.getCode());

        stage.setTitle("Title of the Window");

        button = new Button();
        button.setText("Click me");
        button.setOnAction(e ->{
            if(e.getSource()==button){
                logger.info(OutColor.YELLOW.getCode() + "Click" + OutColor.RESET.getCode());
            }
        });

        StackPane layout = new StackPane();
        layout.getChildren().add(button);

        Scene scene = new Scene(layout);
        stage.setScene(scene);
        // stage.setMaximized(true); // fenêtre agrandie au démarrage
        stage.show();
    }




    public static void main(String[] args) {
        logger.info(OutColor.GREEN.getCode() + "__________  01  __________" + OutColor.RESET.getCode());
        launch(args);
    }


}