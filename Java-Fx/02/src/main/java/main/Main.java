package main;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main extends Application {

    private static final Logger logger = LogManager.getLogger(Main.class);

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("launcher.fxml"));
        loader.setRoot(stage);   // le Stage est la racine du FXML (fx:root)
        loader.load();
        stage.show();
    }

    public static void main(String[] args) {
        logger.info(OutColor.GREEN.getCode() + "__________  02  __________" + OutColor.RESET.getCode());
        launch(args);
    }


}