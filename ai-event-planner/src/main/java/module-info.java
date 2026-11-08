module com.eventplanner {
    requires java.net.http;
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires com.google.genai;
    requires com.fasterxml.jackson.databind;

    opens com.eventplanner to javafx.fxml;
    exports com.eventplanner;
}
