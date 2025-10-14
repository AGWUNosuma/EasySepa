module de.agwu.apps.easysepa {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.xml.bind;
    requires com.opencsv;
    requires com.google.gson;

    opens de.agwu.apps.easysepa.controller to javafx.fxml;
    opens de.agwu.apps.easysepa.model.config to com.google.gson;

    exports de.agwu.apps.easysepa;
    exports de.agwu.apps.easysepa.model.sepa;
    exports de.agwu.apps.easysepa.model.sepa.definition;
}