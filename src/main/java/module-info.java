module com.chatsystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.opencsv;

    opens com.chatsystem to javafx.fxml;
    exports com.chatsystem;
}