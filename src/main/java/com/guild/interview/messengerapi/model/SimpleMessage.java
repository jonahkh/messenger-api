package com.guild.interview.messengerapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SimpleMessage {
    private String sender;
    private String recipient;
    private String text;
}
