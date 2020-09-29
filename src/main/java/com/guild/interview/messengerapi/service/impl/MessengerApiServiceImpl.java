package com.guild.interview.messengerapi.service.impl;

import com.guild.interview.messengerapi.dao.MessengerApiRepository;
import com.guild.interview.messengerapi.model.MessageStatus;
import com.guild.interview.messengerapi.model.SimpleMessage;
import com.guild.interview.messengerapi.model.SimpleMessageDocument;
import com.guild.interview.messengerapi.service.MessengerApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessengerApiServiceImpl implements MessengerApiService {
    private static final long SINGLE_DAY_CONVERSION_MILLIS = 1000 * 60 * 60 * 24;

    @Autowired
    private MessengerApiRepository messengerApiRepository;

    @Override
    public List<SimpleMessage> getUnreadMessages(String recipient) {
        final List<SimpleMessageDocument> dbResponse = messengerApiRepository.findAllByRecipientAndMessageStatus(recipient, MessageStatus.UNREAD);
        log.debug("Found {} unread messages for recipient: {}", dbResponse.size(), recipient);
        return convertSimpleMessageFromDocument(dbResponse);
    }

    @Override
    public List<SimpleMessage> getRecentMessages(String recipient, String sender) {
        List<SimpleMessageDocument> matchingMessages;
        if (StringUtils.isEmpty(sender)) {
            log.debug("Sender is null for fetch top 100 messages for {}", recipient);
            matchingMessages = messengerApiRepository.findTop100ByRecipientOrderByTimestampDesc(recipient);
        } else {
            log.debug("Finding top 100 for recipient: {} and sender: {}", recipient, sender);
            matchingMessages = messengerApiRepository.findTop100ByRecipientAndSenderOrderByTimestampDesc(recipient, sender);
        }
        log.debug("Found {} messages for recipient: {} and sender: {}", matchingMessages.size(), recipient, sender);

        return convertSimpleMessageFromDocument(matchingMessages);
    }

    @Override
    public List<SimpleMessage> getRecentWithinThirtyDays(String recipient, String sender) {
        List<SimpleMessageDocument> matchingMessages;
        final Date thirtyDaysPast = new Date(System.currentTimeMillis() - (SINGLE_DAY_CONVERSION_MILLIS * 30)); // TODO make 30 env variable
        if (StringUtils.isEmpty(sender)) {
            log.debug("Sender is null for fetching all messages in past 30 days for {} ", recipient);
            matchingMessages = messengerApiRepository.findAllByTimestampAfterAndRecipientOrderByTimestampDesc(thirtyDaysPast, recipient);
        } else {
            log.debug("Finding all messages within past 30 days for {} from {}", recipient, sender);
            matchingMessages = messengerApiRepository.findAllByTimestampAfterAndRecipientAndSenderOrderByTimestampDesc(thirtyDaysPast, recipient, sender);
        }
        return convertSimpleMessageFromDocument(matchingMessages);
    }

    @Override
    public void sendMessage(SimpleMessage simpleMessage) {
        final String id = UUID.randomUUID().toString();
        log.debug("Saving simple message with id: {}", id);
        final SimpleMessageDocument messageDocument = new SimpleMessageDocument(id,
                simpleMessage.getText(),
                simpleMessage.getRecipient(),
                simpleMessage.getSender(),
                MessageStatus.UNREAD,
                new Date());
        messengerApiRepository.save(messageDocument);
    }


    // Convert SimpleMessageDocument (dao) to api response SimpleMessage
    private List<SimpleMessage> convertSimpleMessageFromDocument(List<SimpleMessageDocument> matchingMessages) {
        final List<SimpleMessage> mappedResult = matchingMessages.stream()
                .map(document -> {
                    // Mark each document as read
                    document.setMessageStatus(MessageStatus.READ);
                    return new SimpleMessage(document.getSender(), document.getRecipient(), document.getText());
                })
                .collect(Collectors.toList());
        // Update all messages to READ status in db
        log.debug("Updating {} documents to READ status", matchingMessages.size());
        messengerApiRepository.saveAll(matchingMessages);
        return mappedResult;
    }
}
