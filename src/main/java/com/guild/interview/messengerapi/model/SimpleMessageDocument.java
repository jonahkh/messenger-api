package com.guild.interview.messengerapi.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Data
@Document(collection = "messages")
public class SimpleMessageDocument {
    @Id
    private String id;
    private String text;
    private String recipient;
    private String sender;
    private MessageStatus messageStatus;
    private Date timestamp;
}
