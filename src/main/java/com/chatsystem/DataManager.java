package com.chatsystem;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

public class DataManager implements Serializable {
    private static final DataManager instance = new DataManager();
    private DataManager(){}
    private final ArrayList<Message> messages = new ArrayList<>();
    private final ArrayList<String> rooms = new ArrayList<>();
    private final ArrayList<String> users = new ArrayList<>();

    public static synchronized DataManager getInstance() {
        return instance;
    }
    private DataManager readResolve() {
        return getInstance();
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }
    public ArrayList<String> getRooms() {
        return rooms;
    }
    public ArrayList<String> getUsers() {
        return users;
    }

    public void importData(String filePath) {
        try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
            String[] line;
//            boolean isFirstLine = true; // Flag to handle the first line separately
            while ((line = csvReader.readNext()) != null) {
                if (line.length == 4) { // ensures that the line has exactly 4 fields or columns
                    String sender = line[0];
                    String receiverType = line[1];
                    String receiver = line[2];
                    String text = line[3];

//                    if (isFirstLine) {
//                        addUser(sender); // Add the first user directly without checking if it exists
//                        isFirstLine = false;
//                    }

                    if (receiverType.equals("Room"))
                        addRoom(receiver);
                    if (receiverType.equals("User")) {
                        addUser(receiver);
                        if (!users.contains(sender) && !sender.equals("Server"))
                            addUser(sender);
                    }

                    Message message = new Message(sender, receiverType, receiver, text);
                    messages.add(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
    }

    public void addMessage(Message message){
        messages.add(message);
    }

    public void addUser(String user){
        if(!users.contains(user) && !user.equals("Server"))
            users.add(user);
    }
    public void addRoom(String room){
        if(!rooms.contains(room))
            rooms.add(room);
    }

    public void exportData(String filePath) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath))) {
            for (Message message : messages) {
                String[] data = {message.sender(), message.receiverType(), message.receiver(), message.text()};
                writer.writeNext(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}