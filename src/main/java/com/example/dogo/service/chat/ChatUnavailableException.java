package com.example.dogo.service.chat;

public class ChatUnavailableException extends RuntimeException {

    public ChatUnavailableException(String message) {
        super(message);
    }
}
