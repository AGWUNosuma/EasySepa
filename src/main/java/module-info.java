module de.agwu.apps.easysepa {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.xml.bind;
    requires com.opencsv;
    requires com.google.gson;

    opens de.agwu.apps.easysepa.controller to javafx.fxml;

    exports de.agwu.apps.easysepa;
}