package com.guild.interview.messengerapi.dao;

import com.guild.interview.messengerapi.model.MessageStatus;
import com.guild.interview.messengerapi.model.SimpleMessageDocument;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface MessengerApiRepository extends PagingAndSortingRepository<SimpleMessageDocument, String> {

    List<SimpleMessageDocument> findAllByRecipientAndMessageStatus(String recipient, MessageStatus messageStatus);

    // Top 100 from all senders
    List<SimpleMessageDocument> findTop100ByRecipientOrderByTimestampDesc(String recipient);

    // Top 100 from specific sender
    List<SimpleMessageDocument> findTop100ByRecipientAndSenderOrderByTimestampDesc(String recipient, String sender);

    // All messages within last 30 days from all senders
    List<SimpleMessageDocument> findAllByTimestampAfterAndRecipientOrderByTimestampDesc(Date timestamp, String recipient);

    // All messages within last 30 days from specific sender
    List<SimpleMessageDocument> findAllByTimestampAfterAndRecipientAndSenderOrderByTimestampDesc(Date timestamp, String recipient, String sender);
}
