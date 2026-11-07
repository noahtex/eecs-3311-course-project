module com.eventplanner {
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;

    opens com.eventplanner to javafx.fxml;
    exports com.eventplanner;
}
