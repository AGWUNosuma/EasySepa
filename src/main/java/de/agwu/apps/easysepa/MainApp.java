package de.agwu.apps.easysepa;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("fxml/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);

        // Load CSS stylesheet
        String css = MainApp.class.getResource("css/styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("EasySepa - SEPA XML Generator");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
