package de.agwu.apps.easysepa;

import de.agwu.apps.easysepa.controller.MainController;
import de.agwu.apps.easysepa.service.ConfigService;
import de.agwu.apps.easysepa.service.FieldMappingService;
import de.agwu.apps.easysepa.service.SepaTransactionBuilder;
import de.agwu.apps.easysepa.service.SepaXmlGenerator;
import de.agwu.apps.easysepa.service.XsdValidationService;
import de.agwu.apps.easysepa.util.CsvUtil;
import de.agwu.apps.easysepa.util.UiUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApp.class.getResource("fxml/main-view.fxml"));
        fxmlLoader.setControllerFactory(type -> {
            if (type == MainController.class) {
                return new MainController(
                        new CsvUtil(),
                        new FieldMappingService(),
                        new UiUtil(),
                        new ConfigService(),
                        new SepaTransactionBuilder(),
                        new SepaXmlGenerator(),
                        new XsdValidationService()
                );
            }
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to instantiate controller: " + type.getName(), e);
            }
        });
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
