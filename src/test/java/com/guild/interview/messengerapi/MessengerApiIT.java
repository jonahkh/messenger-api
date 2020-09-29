package com.guild.interview.messengerapi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.guild.interview.messengerapi.model.MessageStatus;
import com.guild.interview.messengerapi.model.SimpleMessage;
import com.guild.interview.messengerapi.model.SimpleMessageDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for MessengerAPI. Uses Flapdoodle/fakemongo embedded Mongo server to test queries with live data.
 * This approach requires no actual Mongo instance and will run significantly faster due to the Mongo server running
 * directly in memory.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class MessengerApiIT {
    private static final long SINGLE_DAY_MILLIS = 1000 * 60 * 60 * 24;
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<SimpleMessage>>() {
    }.getType();
    private static boolean isInitialized = false;

    @Autowired
    private MockMvc controller;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    public void setupData() {
        if (!isInitialized) {
            List<SimpleMessageDocument> simpleMessageDocuments = new ArrayList<>();
            // add 105 messages from Denver to Colorado
            // the first ten will be within the past 30 days, so text "hello world0" through "hello world9"
            // "hello world0" through "hello world4" will have sender=lakewood the rest with denver
            // "hello world10" through "hello world50" will have sender=littleton
            // "hello world51" through "hello world104" will have sender=denver

            for (int i = 0; i < 105; i++) {
                final SimpleMessageDocument simpleMessageDocument = new SimpleMessageDocument();
                simpleMessageDocument.setMessageStatus(MessageStatus.UNREAD);
                simpleMessageDocument.setRecipient("colorado");
                simpleMessageDocument.setText("hello world" + i);
                if (i < 10) {
                    // Set timestamp to 20 days ago + position (i) and entries 0-10 are sorted ascending (to test the DB read sorting functionality)
                    simpleMessageDocument.setTimestamp(new Date(System.currentTimeMillis() - (20 * SINGLE_DAY_MILLIS) + (i * SINGLE_DAY_MILLIS)));
                    if (i < 5) {
                        simpleMessageDocument.setSender("lakewood");
                    } else {
                        simpleMessageDocument.setSender("denver");
                    }
                } else {
                    simpleMessageDocument.setTimestamp(new Date(System.currentTimeMillis() - (31 * SINGLE_DAY_MILLIS)));
                    if (i < 51) {
                        simpleMessageDocument.setSender("littleton");
                    } else {
                        simpleMessageDocument.setSender("denver");
                    }
                }
                simpleMessageDocuments.add(simpleMessageDocument);
            }

            mongoTemplate.insertAll(simpleMessageDocuments);
            isInitialized = true;
        }
    }

    @Test
    public void testSendMessageFlow() throws Exception {
        final SimpleMessage requestMessage = new SimpleMessage("stapleton", "highlands", "hello world200");
        controller.perform(post("http://localhost:8080/messenger/sendMessage").contentType("application/json").content(GSON.toJson(requestMessage)))
                .andExpect(status().isOk());
        List<SimpleMessageDocument> simpleMessageDocuments = mongoTemplate.find(Query.query(where("sender").is("stapleton")), SimpleMessageDocument.class);
        assertEquals(1, simpleMessageDocuments.size());
        assertEquals("hello world200", simpleMessageDocuments.get(0).getText());
        assertEquals(MessageStatus.UNREAD, simpleMessageDocuments.get(0).getMessageStatus());

        MvcResult apiResponse = controller.perform(get("http://localhost:8080/messenger/getUnreadMessages?recipient=highlands"))
                .andExpect(status().isOk()).andReturn();
        List<SimpleMessage> message = GSON.fromJson(apiResponse.getResponse().getContentAsString(), LIST_TYPE);

        assertEquals(1, message.size());
        assertEquals(message.get(0), requestMessage);

        // Verify that message was marked as READ
        simpleMessageDocuments = mongoTemplate.find(Query.query(where("sender").is("stapleton")), SimpleMessageDocument.class);
        assertEquals(1, simpleMessageDocuments.size());
        assertEquals(MessageStatus.READ, simpleMessageDocuments.get(0).getMessageStatus());

        // Check that calling getUnreadMessages again returns nothing
        apiResponse = controller.perform(get("http://localhost:8080/messenger/getUnreadMessages?recipient=highlands"))
                .andExpect(status().isOk()).andReturn();
        message = GSON.fromJson(apiResponse.getResponse().getContentAsString(), LIST_TYPE);
        assertEquals(0, message.size());
    }


    @Test
    public void testGetMessages_withSender() throws Exception {
        final MvcResult mvcResult = controller.perform(get("http://localhost:8080/messenger/getMessages?sender=littleton&recipient=colorado")).andExpect(status().isOk()).andReturn();
        List<SimpleMessage> response = GSON.fromJson(mvcResult.getResponse().getContentAsString(), LIST_TYPE);
        assertEquals(41, response.size());
        // Entries 10-50 are from littleton to colorado
        for (int i = 10; i < 51; i++) {
            // Order of insertion should be maintained
            assertEquals("hello world" + i, response.get(i - 10).getText());
            assertEquals("littleton", response.get(i - 10).getSender());
        }
    }

    @Test
    public void testGetMessages_withoutSender() throws Exception {
        final MvcResult mvcResult = controller.perform(get("http://localhost:8080/messenger/getMessages?recipient=colorado")).andExpect(status().isOk()).andReturn();
        List<SimpleMessage> response = GSON.fromJson(mvcResult.getResponse().getContentAsString(), LIST_TYPE);
        assertEquals(100, response.size());
        // We stored the first 10 entries in reverse timestamp order (each entry is closer to current date starting with #9)
        int masterCounter = 0;
        for (int i = 9; i >= 0; i--) {
            assertEquals("hello world" + i, response.get(masterCounter).getText());
            masterCounter++;
        }

        for (int i = 10; i < 100; i++) {
            assertEquals("hello world" + i, response.get(i).getText());
        }
    }

    @Test
    public void testGetRecentWithinThirtyDays_withSender() throws Exception {
        final MvcResult mvcResult = controller.perform(get("http://localhost:8080/messenger/getMessagesWithinThirtyDays?sender=lakewood&recipient=colorado")).andExpect(status().isOk()).andReturn();
        List<SimpleMessage> response = GSON.fromJson(mvcResult.getResponse().getContentAsString(), LIST_TYPE);
        // First 5 entries were sent from lakewood within last 30 days
        assertEquals(5, response.size());
        assertEquals("hello world4", response.get(0).getText());
        assertEquals("hello world3", response.get(1).getText());
        assertEquals("hello world2", response.get(2).getText());
        assertEquals("hello world1", response.get(3).getText());
        assertEquals("hello world0", response.get(4).getText());
    }

    @Test
    public void testGetRecentWithinThirtyDays_withoutSender() throws Exception {
        final MvcResult mvcResult = controller.perform(get("http://localhost:8080/messenger/getMessagesWithinThirtyDays?sende&recipient=colorado")).andExpect(status().isOk()).andReturn();
        List<SimpleMessage> response = GSON.fromJson(mvcResult.getResponse().getContentAsString(), LIST_TYPE);
        assertEquals(10, response.size());
        int position = 0;
        for (int i = 9; i >=0; i--) {
            assertEquals("hello world" + i, response.get(position).getText());
            position++;
        }
    }
}
