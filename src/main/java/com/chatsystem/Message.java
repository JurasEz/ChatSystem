package com.chatsystem;

import java.io.Serializable;

public record Message(String sender, String receiverType, String receiver, String text) implements Serializable {
}
