package com.guild.interview.messengerapi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.guild.interview.messengerapi.model.SimpleMessage;
import com.guild.interview.messengerapi.service.MessengerApiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(MessengerApiController.class)
public class MessengerApiControllerTest {
    public static final Gson GSON = new Gson();
    @Autowired
    private MockMvc controller;

    @MockBean
    private MessengerApiService messengerApiService;

    @Test
    public void testWriteMessage() throws Exception {
        final SimpleMessage request = new SimpleMessage("sender", "recipient", "hello world!");
        this.controller.perform(post("http://localhost:8080/messenger/sendMessage").contentType("application/json").content(GSON.toJson(request)))
                .andExpect(status().isOk());
        // Check that the sendMessage() method is invoked with our request body
        verify(messengerApiService).sendMessage(request);
    }

    @Test
    public void testGetMessages_withSender() throws Exception {
        testGetMessages("denver", "http://localhost:8080/messenger/getMessages?sender=denver&recipient=colorado");
    }

    @Test
    public void testGetMessages_withoutSender() throws Exception {
        testGetMessages(null, "http://localhost:8080/messenger/getMessages?recipient=colorado");
    }

    @Test
    public void testGetMessagesWithinThirtyDays_withSender() throws Exception {
        testGetMessagesWithinThirtyDays("denver", "http://localhost:8080/messenger/getMessagesWithinThirtyDays?sender=denver&recipient=colorado");
    }

    @Test
    public void testGetMessagesWithinThirtyDays_withoutSender() throws Exception {
        testGetMessagesWithinThirtyDays(null, "http://localhost:8080/messenger/getMessagesWithinThirtyDays?recipient=colorado");
    }

    private void testGetMessagesWithinThirtyDays(String sender, String url) throws Exception {
        final List<SimpleMessage> response = Collections.singletonList(new SimpleMessage("denver", "colorado", "hello world!"));
        when(messengerApiService.getRecentWithinThirtyDays("colorado", sender)).thenReturn(response);
        final MvcResult mvcResult = this.controller.perform(get(url))
                .andExpect(status().isOk()).andReturn();
        // Need to deserialize object of type List<SimpleMessage>. Gson (Google) library provides the Type class to accomplish this
        final String jsonResponse = mvcResult.getResponse().getContentAsString();
        final Type type = new TypeToken<List<SimpleMessage>>() {
        }.getType();
        assertEquals(response, GSON.fromJson(jsonResponse, type));
    }

    private void testGetMessages(String sender, String url) throws Exception {
        final List<SimpleMessage> response = Collections.singletonList(new SimpleMessage("denver", "colorado", "hello world!"));
        when(messengerApiService.getRecentMessages("colorado", sender)).thenReturn(response);
        final MvcResult mvcResult = this.controller.perform(get(url))
                .andExpect(status().isOk()).andReturn();
        // Need to deserialize object of type List<SimpleMessage>. Gson (Google) library provides the Type class to accomplish this
        final String jsonResponse = mvcResult.getResponse().getContentAsString();
        final Type type = new TypeToken<List<SimpleMessage>>() {
        }.getType();
        assertEquals(response, GSON.fromJson(jsonResponse, type));
    }
}
