package com.chatsystem;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;
import java.util.ResourceBundle;

public class ClientController implements Initializable {
    // Fields representing UI elements
    @FXML private Label nameLabel;
    @FXML private Label receiverLabel;
    @FXML private TextField roomNameField;
    @FXML private TextField messageField;
    @FXML private ScrollPane scrollPaneGroups;
    @FXML private ScrollPane scrollPaneMain;
    @FXML private ScrollPane scrollPaneUsers;
    @FXML private VBox vboxGroups;
    @FXML private VBox vboxMessages;
    @FXML private HBox hboxUsers;

    // Other fields for socket and data management
    private Socket socket;
    private String username;
    private String password;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private DataManager dataManager;
    private String currentRoom;
    private String currentRoomType;
    private boolean socketIsOpen;


    public ClientController(Socket socket, String username, String password){
        try{
            this.socket = socket;
            this.socketIsOpen = true;
            this.username = username;
            this.password = password;
            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());
        } catch(IOException e) {
            closeEverything(socket, output, input);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        nameLabel.setText(username);

        // Set vBoxes to scroll to the bottom automatically
        vboxGroups.widthProperty().addListener((observable, oldValue, newValue) -> {
            scrollPaneGroups.setHvalue(scrollPaneGroups.getHmax());
        });
        vboxMessages.widthProperty().addListener((observable, oldValue, newValue) -> {
            scrollPaneMain.setHvalue(scrollPaneMain.getHmax());
        });
        hboxUsers.widthProperty().addListener((observable, oldValue, newValue) -> {
            scrollPaneUsers.setHvalue(scrollPaneUsers.getHmax());
        });

        // Wrap the hboxUsers in a ScrollPane
        ScrollPane usersScrollPane = new ScrollPane(hboxUsers);
        usersScrollPane.setFitToHeight(false);
        usersScrollPane.setFitToWidth(true);
        usersScrollPane.setStyle("-fx-background-color: LIGHTBLUE;");

        // Set up the horizontal scroll for the user list
        hboxUsers.setFillHeight(true);
        hboxUsers.setSpacing(5.0);

        // Send username to the ClientHandler and get the dataManager
        try {
            output.writeObject(username);
            output.flush();

            dataManager = (DataManager) input.readObject();
            // Add the first user to the data manager if it doesn't exist
            if (!dataManager.getUsers().contains(username)) {
                dataManager.addUser(username);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Set up the rooms and users
        ArrayList<String> users = dataManager.getUsers();
        ArrayList<String> rooms = dataManager.getRooms();

        dataManager.addRoom("General");
        changeRoom(rooms.get(0));

        for (String room : rooms) {
            createButton(vboxGroups, room);
        }
        for (String user : users) {
            if (!user.equals(username))
                createUserButton(hboxUsers, user);
        }

        listenForMessage();
    }

    @FXML
    public void sendMessage(KeyEvent keyEvent) {
        try {
            if (socket.isConnected() && keyEvent.getCode() == KeyCode.ENTER) {
                String messageToSend = messageField.getText();
                if (!messageToSend.isEmpty()) {
                    output.writeObject(new Message(username, currentRoomType, currentRoom, messageToSend));
                    output.flush();
                    displayMessageOut(messageToSend);
                    messageField.clear();
                }
            }
        } catch (IOException e) {
            closeEverything(socket, output, input);
        }
    }
    @FXML
    void createRoom() {
        try {
            String roomName = roomNameField.getText();
            if(!roomName.isEmpty() && !dataManager.getRooms().contains(roomName) && !dataManager.getUsers().contains(roomName)){
                createButton(vboxGroups, roomName);
                dataManager.getRooms().add(roomName);
                output.writeObject(new Room(roomName));
                output.flush();
                roomNameField.clear();
            }
        } catch (IOException e) {
            closeEverything(socket, output, input);
        }
    }

    void changeRoom(String roomName) {
        currentRoom = roomName;
        currentRoomType = "Room";
        Platform.runLater(() -> {
            receiverLabel.setText(roomName);
            receiverLabel.setStyle("-fx-font-weight: bold;");
            vboxMessages.getChildren().clear();
        });
        loadMessages();
    }
    void changeUser(String userName) {
        currentRoom = userName;
        currentRoomType = "User";
        Platform.runLater(() -> {
            receiverLabel.setText(userName);
            receiverLabel.setStyle("-fx-font-weight: bold;");
            vboxMessages.getChildren().clear();
        });
        loadMessages();
    }

    void loadMessages() {
        try {
            output.writeObject("Load messages");
            output.flush();
        }catch (IOException e) {
            closeEverything(socket, output, input);
        }
    }

    @FXML
    void exit(ActionEvent event) throws IOException {
        dataManager.exportData("chats.csv");
        closeEverything(socket, output, input);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
        Parent root = loader.load();
        root.setStyle("-fx-background-color: LIGHTBLUE;");
        Scene scene = new Scene(root);
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(scene);
        stage.show();
    }

    public void listenForMessage() {
        new Thread(() -> {
            while(socketIsOpen) {
                try{
                    Object receivedObject = input.readObject();

                    if (receivedObject instanceof Message message) {
                        if(message.receiverType().equals("Room") && currentRoom.equals(message.receiver())){
                            if(username.equals(message.sender()))
                                displayMessageOut(message.text());
                            else
                                displayMessageIn(message.sender(), message.text());
                        }
                        else if(message.receiverType().equals("User")) {
                            if(currentRoom.equals(message.sender()) && username.equals(message.receiver()))
                                displayMessageIn(message.sender(), message.text());
                            else if (currentRoom.equals(message.receiver()) && username.equals(message.sender()))
                                displayMessageOut(message.text());
                        }

                    } else if (receivedObject instanceof Room room) {
                        if(!dataManager.getRooms().contains(room.room())) {
                            createButton(vboxGroups, room.room());
                            dataManager.getRooms().add(room.room());
                        }

                    } else if (receivedObject instanceof User user) {
                        if(!dataManager.getUsers().contains(user.user())) {
                            createUserButton(hboxUsers, user.user());
                            dataManager.getUsers().add(user.user());
                        }

                    } else if (receivedObject instanceof DataManager data) {
                        dataManager = data;
                    }
                } catch (IOException | ClassNotFoundException e) {
                    closeEverything(socket, output, input);
                }
            }
        }).start();
    }

    private void displayUsername(String message) {
        Platform.runLater(() -> {
            HBox hbox = new HBox();
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setPadding(new Insets(0, 0, 0, 10));

            hbox.getChildren().add(new Text(message));
            vboxMessages.getChildren().add(hbox);
        });
    }
    private void displayMessageIn(String sender, String message) {
        displayUsername(sender);
        Platform.runLater(() -> {
            HBox hbox = new HBox();
            hbox.setAlignment(Pos.CENTER_LEFT);
            hbox.setPadding(new Insets(5, 5, 5, 10));

            Text text = new Text(message);
            TextFlow textFlow = new TextFlow(text);
            textFlow.setStyle("-fx-color: rgb(0, 0, 0);" + "-fx-background-color: rgb(212, 207, 207);" + "-fx-background-radius: 20px");
            textFlow.setPadding(new Insets(5, 10, 5, 10));
            text.setFill(Color.color(0, 0, 0));

            hbox.getChildren().add(textFlow);
            vboxMessages.getChildren().add(hbox);
        });
    }

    private void displayMessageOut(String message) {
        Platform.runLater(() -> {
            HBox hbox = new HBox();
            hbox.setAlignment(Pos.CENTER_RIGHT);
            hbox.setPadding(new Insets(5, 5, 5, 10));

            Text text = new Text(message);
            TextFlow textFlow = new TextFlow(text);
            textFlow.setStyle("-fx-color: rgb(239, 242, 255);" +
                    "-fx-background-color: rgb(15, 125, 242);" +
                    "-fx-background-radius: 20px");
            textFlow.setPadding(new Insets(5, 10, 5, 10));
            text.setFill(Color.color(0.934, 0.945, 0.996));

            hbox.getChildren().add(textFlow);
            vboxMessages.getChildren().add(hbox);

            // Scroll to the bottom of the scroll pane
            scrollPaneMain.applyCss();
            scrollPaneMain.layout();
            scrollPaneMain.setVvalue(1.0);
        });
    }


    private void createButton(VBox vbox, String name) {
        Platform.runLater(() -> {
            Button button = new Button(name);
            button.setOnAction(event -> {
                if (vbox.equals(vboxGroups))
                    changeRoom(name);
                if (vbox.equals(hboxUsers))
                    changeUser(name);
            });
            button.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
            button.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-width: 2; -fx-border-color: grey;");
            VBox.setMargin(button, new Insets(5)); // Optional: Add some spacing between buttons
            vbox.getChildren().add(button);
        });
    }

    private void createUserButton(HBox hbox, String name) {
        Platform.runLater(() -> {
            Button button = new Button(name);
            button.setMinWidth(90); // Set a minimum width for the buttons
            button.setMaxWidth(100); // Set a maximum width for the buttons
            button.setMinHeight(30); // Set a minimum height for the buttons
            button.setOnAction(event -> {
                if (hbox.equals(vboxGroups))
                    changeRoom(name);
                if (hbox.equals(hboxUsers))
                    changeUser(name);
            });
            button.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(button, Priority.ALWAYS);
            button.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-width: 2; -fx-border-color: grey; -fx-font-size: 15px;");
            HBox.setMargin(button, new Insets(5)); // Optional: Add some spacing between buttons
            hbox.getChildren().add(button);
        });
    }



    public void closeEverything(Socket socket, ObjectOutputStream output, ObjectInputStream input) {
        try {
            if (output != null)
                output.close();
            if (input != null)
                input.close();
            if (socket != null)
                socket.close();
            socketIsOpen = false;
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}