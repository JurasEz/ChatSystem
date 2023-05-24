// JURAS EZERSKIS IV
package com.chatsystem;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("login.fxml")));
        stage.setTitle("Communication System");
        root.setStyle("-fx-background-color: LIGHTBLUE;");
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) { launch(args); }
}