package com.guild.interview.messengerapi;

import com.guild.interview.messengerapi.model.SimpleMessage;
import com.guild.interview.messengerapi.service.MessengerApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API for the Messenger API application. This API can be used to send and receive simple messages from one person to
 * another. It is assumed that recipient's device will poll the /messenger/getUnreadMessages endpoint for unread messages
 * at an arbitrary interval. Otherwise, the recipient can request the last 100 messages or all messages within past 30
 * days either from a certain sender or from all senders.
 *
 */
@RestController
@Slf4j
public class MessengerApiController {

    @Autowired
    private MessengerApiService messengerApiService;

    /**
     * Return latest 100 messages for a given recipient. If sender is provided, returns list of messages for that recipient
     * from the sender. If it's empty, all sender messages will be returned. Currently, there is a 100 message limit. In
     * future iterations this limit should be configurable.
     *
     * @param recipient Query for messages for this recipient
     * @param sender If provided, only return messages from this sender
     * @return last 100 messages for a given recipient/sender pair
     */
    @RequestMapping(method = RequestMethod.GET, value = "/messenger/getMessages")
    public List<SimpleMessage> getMessages(@RequestParam(value = "recipient") String recipient,
                                           @RequestParam(value = "sender", required = false) String sender) {
        log.debug("getMessages request received from: {}, to: {}", sender, recipient);
        return messengerApiService.getRecentMessages(recipient, sender);
    }

    /**
     * Returns all messages within last 30 days for a given recipient.  If sender is provided, returns list of messages
     * for that recipient from the given sender for the past 30 days. Otherwise, return all. In future iterations, make the
     * time range configurable i.e. give me all messages between X date and Y date.
     *
     * @param recipient Query for messages for this recipient
     * @param sender If provided, only return messages from this sender
     * @return all messages in the past 30 days for a given recipient/sender pair
     */
    @RequestMapping(method = RequestMethod.GET, value = "/messenger/getMessagesWithinThirtyDays")
    public List<SimpleMessage> getMessagesWithinThirtyDays(@RequestParam(value = "recipient") String recipient,
                                                           @RequestParam(value = "sender", required = false) String sender) {

        log.debug("getMessagesWithinThirtyDays request received from: {}, to: {}", sender, recipient);
        return messengerApiService.getRecentWithinThirtyDays(recipient, sender);
    }

    /**
     * Send a simple message. Body should contain recipient, sender, and body. Message will be in UNREAD state until the
     * recipient invokes /messenger/getUnreadMessages.
     *
     * @param message message to send
     */
    @RequestMapping(method = RequestMethod.POST, value = "/messenger/sendMessage")
    public void sendMessage(@RequestBody SimpleMessage message) {
        log.debug("sendMessage request received with body: \n{}", message.toString());
        messengerApiService.sendMessage(message);
    }

    /**
     * Retrieve a list of all unread messages for a given recipient. Consuming this will update each unread message to the
     * READ state.
     *
     * @param recipient recipient to retrieve unread messages for
     */
    @RequestMapping(method = RequestMethod.GET, value = "/messenger/getUnreadMessages")
    public List<SimpleMessage> getUnreadMessages(@RequestParam(value = "recipient") String recipient) {
        log.debug("getUnreadMessages request received for recipient: {}", recipient);
        return messengerApiService.getUnreadMessages(recipient);
    }
}
