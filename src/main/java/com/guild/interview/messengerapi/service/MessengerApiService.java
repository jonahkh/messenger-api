package com.guild.interview.messengerapi.service;

import com.guild.interview.messengerapi.model.SimpleMessage;

import java.util.List;

public interface MessengerApiService {
    List<SimpleMessage> getUnreadMessages(String recipient);
    List<SimpleMessage> getRecentMessages(String recipient, String sender);
    List<SimpleMessage> getRecentWithinThirtyDays(String recipient, String sender);
    void sendMessage(SimpleMessage simpleMessage);
}
