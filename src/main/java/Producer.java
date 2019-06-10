import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;


import java.util.Properties;


public class Producer extends Thread {
    private final KafkaProducer<Integer, String> producer;
    private final String topic;
    private final Boolean isAsync;

    public Producer(String topic, Boolean isAsync) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaProperties.KAFKA_SERVER_URL + ":" + KafkaProperties.KAFKA_SERVER_PORT);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "DemoProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producer = new KafkaProducer<>(props);
        this.topic = topic;
        this.isAsync = isAsync;
    }

    public void run() {

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(KafkaProperties.CONSUMER_KEY)
                .setOAuthConsumerSecret(KafkaProperties.CONSUMER_SECRET)
                .setOAuthAccessToken(KafkaProperties.ACCESS_TOKEN)
                .setOAuthAccessTokenSecret(KafkaProperties.ACCESS_TOKEN_SECRET);
        TwitterStream twitterStream =  new TwitterStreamFactory(cb.build()).getInstance();

//        while (messageNo<3)
//            try {
//            Paging paging = new Paging(1, 3);
//            List<Status> statuses = twitter.getHomeTimeline(paging);
//            System.out.println("Showing home timeline.");
//            for (Status status : statuses) {
//                String messageStr = status.getUser().getName() + ":" + status.getText();
//                System.out.println(messageNo + " - " + messageStr);
//                long startTime = System.currentTimeMillis();
//                if (isAsync) { // Send asynchronously
//                    producer.send(new ProducerRecord<>(topic,
//                            messageNo,
//                            messageStr), new DemoCallBack(startTime, messageNo, messageStr));
//                    ++messageNo;
//                } else { // Send synchronously
//                    try {
//                        producer.send(new ProducerRecord<>(topic,
//                                messageNo,
//                                messageStr)).get();
//                        System.out.println("Sent message: (" + messageNo + ", " + messageStr + ")");
//                        ++messageNo;
//                    } catch (InterruptedException | ExecutionException e) {
//                        e.printStackTrace();
//                        ++messageNo;
//                    }
//                }
//
//            }
                StatusListener listener = new StatusListener(){
                    int messageNo = 1;
                    public void onStatus(Status status) {
                        String messageInfo = status.getText() + "-----BY-----" + status.getUser().getName();
                        if (status.getLang().equalsIgnoreCase("en")) {
                            System.out.println(messageNo + " - " + status.getText());
                            producer.send(new ProducerRecord<>(topic,
                                    messageNo,
                                    messageInfo), new DemoCallBack(messageNo, status.getText()));
                            messageNo++;
                        }
                    }
                    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                    }

                    public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                    }

                    @Override
                    public void onScrubGeo(long l, long l1) {
                    }

                    @Override
                    public void onStallWarning(StallWarning stallWarning) {
                    }

                    public void onException(Exception ex) {
                        ex.printStackTrace();
                    }
                };

                twitterStream.addListener(listener);
                // sample() method internally creates a thread which manipulates TwitterStream and calls these adequate listener methods continuously.
                twitterStream.sample();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }
}

class DemoCallBack implements Callback {

    private final int key;
    private final String message;

    public DemoCallBack( int key, String message) {
        this.key = key;
        this.message = message;
    }

    /**
     * A callback method the user can implement to provide asynchronous handling of request completion. This method will
     * be called when the record sent to the server has been acknowledged. When exception is not null in the callback,
     * metadata will contain the special -1 value for all fields except for topicPartition, which will be valid.
     *
     * @param metadata  The metadata for the record that was sent (i.e. the partition and offset). Null if an error
     *                  occurred.
     * @param exception The exception thrown during processing of this record. Null if no error occurred.
     */
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        if (metadata != null) {
            System.out.println(
                    "message(" + key + ", " + message + ") sent to partition(" + metadata.partition() +
                            "), " + "offset(" + metadata.offset() + ")  " );
        } else {
            exception.printStackTrace();
        }
    }

}
