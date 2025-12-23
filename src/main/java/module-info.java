module com.moriket {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;

    opens com.moriket to javafx.fxml;
    exports com.moriket;
}
