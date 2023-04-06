module com.tuman {
    requires javafx.controls;
    requires javafx.fxml;
    requires dropbox.core.sdk;
    requires java.net.http;


    opens com.tuman.tableControllers;
    opens com.tuman to javafx.fxml;

    exports com.tuman;
    exports com.tuman.tableControllers;


}