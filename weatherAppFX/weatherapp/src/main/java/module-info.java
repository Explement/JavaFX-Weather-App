module weather.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires json.simple;
    requires javafx.graphics;

    opens weather.app to javafx.fxml;
    exports weather.app;
}
