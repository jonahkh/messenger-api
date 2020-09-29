# Messenger API
Messenger-API is an API platform enabling simple message delivery. Users can send a message from one to another. 

Note that currently there is no concept of a user so for now we work with "recipients" and "senders".

## To build and run the application:

```
git clone https://github.com/jonahkh/messenger-api
cd messenger-api
mvn clean install
docker-compose up
```

The application will run using your local Docker service and be exposed on port 8080. For instructions on how to use Docker
and download image dependencies refer to the following:  https://docs.docker.com/get-docker/

The API exposes the following endpoints:

    1. /messenger/getMessagesWithinThirtyDays
    2. /messenger/getMessages
    3. /messenger/sendMessage
    4. /messenger/getUnreadMessages

The api can be viewed in a friendly format using Swagger UI. Navigate to `http://localhost:8080/swagger-ui.html`.
Alternatively, `curl` commands may be used:

1. Retrieves all messages within the past thirty days for a recipient with sender being optional. If sender is not provided, returns all messages for that recipient.

    Sample invocation to get all messages from sender=Denver to recipient=Colorado within past 30 days
    
    `curl -X GET http://localhost:8080/messenger/getMessagesWithinThirtyDays?recipient=Colorado&sender=Denver`
    
2. Retrieves the most recent 100 messages for a recipient with sender being optional. If sender is not provided, the most recent 100 messages will be returned regardless of sender. 
In future implementations, this limit should be configurable as an environment variable.

    Sample invocation to get 100 most recent messages from sender=Denver to recipient=Colorado
    
    `curl -X GET http://localhost:8080/messenger/getMessages?recipient=Colorado&sender=Denvre`
    
3. Send a Simple Message from a given sender to recipient with a message body. Message will be stored in the UNREAD status.

    Sample invocation to send a message from sender=Denver to recipient=Colorado and text=hello world
    
    `curl -X POST -H 'Content-Type: application/json' http://localhost:8080/messenger/sendMessage -d {"recipient": "Colorado", "sender": "Denver", "text": "hello world"}`
    
4. Get all unread Simple Messages for a given recipient. This will check for any messages that are in the UNREAD state for the recipient
    
    Sample invocation to get the message from (3) for recipient=Colorado
    
    `curl -X GET http://localhost:8080/messenger/getUnreadMessages?recipient=Colorado`
    
## Testing
Mesenger-api has a collection of unit (suffix Test) and integration tests (suffix IT). The unit tests cover 100% of the 
code and the Integration tests cover end-to-end testing with an embedded Mongo server (flapdoodle).

## Usage
A web application would use this platform to manage messages from a web server standpoint. A client would create some 
application with a friendly UI so that a user can send a message to another. This application should periodically poll the
API's `/messenger/getUnreadMessages` endpoint for the current user to check for new messages. A future implementation could
include a server-side scheduler that sends notifications to recipient devices for new messages. 

Future implementations would also cache unread messages for much quicker read access for the recipient. The writes would 
also be put into a buffer/queue to be written once a certain request count threshold has been met i.e. if the server has
1,000 unread messages, write to DB.

Use this API as you would any standard RESTful API. Invoke via SawggerUI, Postman, curl, another application, etc.

Note that this project uses Lombok which is an open source compiler time code generator for common class components i.e. 
getters, setters, equals & hashcode, constructors, etc. More information can be found on their project page: https://projectlombok.org/ 

