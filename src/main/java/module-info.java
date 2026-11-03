module com.tuman {
    requires javafx.controls;
    requires javafx.fxml;
    requires dropbox.core.sdk;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires java.sql;
    requires org.xerial.sqlitejdbc;


    opens com.tuman.tableControllers;
    opens com.tuman to javafx.fxml;
    opens com.tuman.cash to com.fasterxml.jackson.databind;

    exports com.tuman;
    exports com.tuman.tableControllers;


}
