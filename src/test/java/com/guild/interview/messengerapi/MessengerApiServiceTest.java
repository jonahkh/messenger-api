package com.guild.interview.messengerapi;

import com.guild.interview.messengerapi.dao.MessengerApiRepository;
import com.guild.interview.messengerapi.model.MessageStatus;
import com.guild.interview.messengerapi.model.SimpleMessage;
import com.guild.interview.messengerapi.model.SimpleMessageDocument;
import com.guild.interview.messengerapi.service.impl.MessengerApiServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class MessengerApiServiceTest {
    @Autowired
    private MessengerApiServiceImpl messengerApiService;

    @MockBean
    private MessengerApiRepository messengerApiRepository;

    @Test
    public void testGetRecentMessages_withSender() {
        final List<SimpleMessageDocument> dbResponse = Collections.singletonList(new SimpleMessageDocument("id", "hello world", "denver", "colorado", MessageStatus.UNREAD, new Date()));

        when(messengerApiRepository.findTop100ByRecipientAndSenderOrderByTimestampDesc("denver", "colorado"))
                .thenReturn(dbResponse);
        final List<SimpleMessage> recentMessages = messengerApiService.getRecentMessages("denver", "colorado");
        assertEquals("hello world", recentMessages.get(0).getText());
    }

    @Test
    public void testGetRecentMessages_withoutSender() {
        final List<SimpleMessageDocument> dbResponse = Collections.singletonList(new SimpleMessageDocument("id", "hello world", "denver", "colorado", MessageStatus.UNREAD, new Date()));

        when(messengerApiRepository.findTop100ByRecipientOrderByTimestampDesc("denver"))
                .thenReturn(dbResponse);
        final List<SimpleMessage> recentMessages = messengerApiService.getRecentMessages("denver", null);
        assertEquals("hello world", recentMessages.get(0).getText());
    }

    @Test
    public void testGetRecentWithinThirtyDays_withSender() {
        final List<SimpleMessageDocument> dbResponse = Collections.singletonList(new SimpleMessageDocument("id", "hello world", "denver", "colorado", MessageStatus.UNREAD, new Date()));
        when(messengerApiRepository.findAllByTimestampAfterAndRecipientAndSenderOrderByTimestampDesc(any(), eq("denver"), eq("colorado")))
                .thenReturn(dbResponse);
        final List<SimpleMessage> recentWithinThirtyDays = messengerApiService.getRecentWithinThirtyDays("denver", "colorado");
        assertEquals("hello world", recentWithinThirtyDays.get(0).getText());
    }

    @Test
    public void testGetRecentWithinThirtyDays_withoutSender() {
        final List<SimpleMessageDocument> dbResponse = Collections.singletonList(new SimpleMessageDocument("id", "hello world", "denver", "colorado", MessageStatus.UNREAD, new Date()));
        when(messengerApiRepository.findAllByTimestampAfterAndRecipientOrderByTimestampDesc(any(), eq("denver")))
                .thenReturn(dbResponse);
        final List<SimpleMessage> recentWithinThirtyDays = messengerApiService.getRecentWithinThirtyDays("denver", null);
        assertEquals("hello world", recentWithinThirtyDays.get(0).getText());
    }

    @Test
    public void testSendMessage() {
        final SimpleMessage simpleMessage = new SimpleMessage("denver", "colorado", "hello world");

        // This is used to verify that the save CRUD repository method was invoked with correct input
        final ArgumentCaptor<SimpleMessageDocument> simpleMessageArgumentCaptor = ArgumentCaptor.forClass(SimpleMessageDocument.class);
        messengerApiService.sendMessage(simpleMessage);
        verify(messengerApiRepository).save(simpleMessageArgumentCaptor.capture());

        final SimpleMessageDocument result = simpleMessageArgumentCaptor.getValue();

        assertEquals(simpleMessage.getText(), result.getText());
        assertEquals(MessageStatus.UNREAD, result.getMessageStatus());
        assertEquals("hello world", result.getText());
        assertEquals("denver", result.getSender());
        assertEquals("colorado", result.getRecipient());
        assertNotNull(result.getTimestamp());
    }
}
