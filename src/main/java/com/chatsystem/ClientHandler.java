package com.chatsystem;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private DataManager dataManager;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private String username;

    public ClientHandler(Socket socket, DataManager dataManager) {
        try {
            this.socket = socket;
            this.dataManager = dataManager;
            this.objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            this.objectInputStream = new ObjectInputStream(socket.getInputStream());

            this.username = (String) objectInputStream.readObject();
            dataManager.addUser(username);

            clientHandlers.add(this);

            objectOutputStream.writeObject(dataManager);
            objectOutputStream.flush();
            broadcastMessage(new Message("Server", "Room", "General", username + " has entered the chat."));
            broadcastNewUser(username);
        } catch (IOException | ClassNotFoundException e) {
            closeEverything(socket, objectOutputStream, objectInputStream);
        }
    }


    @Override
    public void run() {
        while (socket.isConnected()) {
            try {
                Object receivedObject = objectInputStream.readObject();
                if (receivedObject instanceof Message) {
                    broadcastMessage((Message) receivedObject);
                }
                else if (receivedObject instanceof Room) {
                    broadcastNewRoom(((Room) receivedObject).room());
                }
                else if (receivedObject instanceof String) {
                    if (receivedObject.equals("Load messages"))
                        loadMessages();
                }
                else if (receivedObject instanceof DataManager){
                    dataManager = (DataManager) receivedObject;
                }
            } catch(IOException | ClassNotFoundException e){
                closeEverything(socket, objectOutputStream, objectInputStream);
                break;
            }
        }
    }

    public void broadcastMessage(Message message) {
        dataManager.addMessage(message);
        for(ClientHandler clientHandler : clientHandlers) {
            try {
                if(clientHandler != this) {
                    clientHandler.objectOutputStream.writeObject(message);
                    clientHandler.objectOutputStream.flush();
                }
            }catch(IOException e){
                closeEverything(socket, objectOutputStream, objectInputStream);
            }
        }
    }
    public void broadcastNewRoom(String room) {
        dataManager.addRoom(room);
        for(ClientHandler clientHandler : clientHandlers) {
            try {
                if(clientHandler != this) {
                    clientHandler.objectOutputStream.writeObject(new Room(room));
                    clientHandler.objectOutputStream.flush();
                }
            } catch(IOException e){
                closeEverything(socket, objectOutputStream, objectInputStream);
            }
        }
    }
    public void broadcastNewUser(String username) {
        for(ClientHandler clientHandler : clientHandlers) {
            try {
                if(clientHandler != this) {
                    clientHandler.objectOutputStream.writeObject(new User(username));
                    clientHandler.objectOutputStream.flush();
                }
            } catch(IOException e){
                closeEverything(socket, objectOutputStream, objectInputStream);
            }
        }
    }

    public void loadMessages() {
        for (Message message : dataManager.getMessages()) {
            try {
                objectOutputStream.writeObject(message);
                objectOutputStream.flush();
            }catch(IOException e){
                closeEverything(socket, objectOutputStream, objectInputStream);
            }
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        broadcastMessage(new Message("Server", "Room", "General", username + " has left the chat."));
    }
    public void closeEverything(Socket socket, ObjectOutputStream objectOutputStream, ObjectInputStream objectInputStream) {
        removeClientHandler();
        try {
            if (objectOutputStream != null)
                objectOutputStream.close();
            if (objectInputStream != null)
                objectInputStream.close();
            if (socket != null)
                socket.close();
            dataManager.exportData("chats.csv");
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
