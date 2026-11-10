package main;


import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main extends Application {

    private static final Logger logger = LogManager.getLogger(Main.class);

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("import-zip.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Hello FXML");
        stage.setMinWidth(1100);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        logger.info(OutColor.GREEN.getCode() + "__________  02  __________" + OutColor.RESET.getCode());
        launch(args);
    }


}