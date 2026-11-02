module com.eventplanner {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.eventplanner to javafx.fxml;
    exports com.eventplanner;
}
