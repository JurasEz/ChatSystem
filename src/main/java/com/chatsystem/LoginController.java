package com.chatsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public class LoginController {
    @FXML private TextField usernameInput;
    @FXML private TextField passwordInput;
    @FXML private Text passwordError;

    @FXML
    void logIn(ActionEvent event) {
        try {
            String username = usernameInput.getText();
            String password = passwordInput.getText();
            if (!password.isEmpty()) {
                System.out.println("Client started");
                Socket socket = new Socket("localhost", 9806);

                ClientController clientController = new ClientController(socket, username, password);
                FXMLLoader loader = new FXMLLoader(getClass().getResource("client.fxml"));
                loader.setController(clientController);
                Parent root = loader.load();
                root.setStyle("-fx-background-color: LIGHTBLUE;");
                Scene scene = new Scene(root);
                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.setScene(scene);
                stage.show();
            } else {
                passwordError.setText("Please enter a password");
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception or display an error message to the user
        }
    }
}
