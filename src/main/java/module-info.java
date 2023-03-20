module com.fastfile.fastfile {
    requires javafx.controls;
    requires javafx.fxml;
    requires lombok;


    opens com.fastfile to javafx.fxml;
    exports com.fastfile;
    exports com.fastfile.controller;
}