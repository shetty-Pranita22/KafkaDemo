

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import kafka.utils.ShutdownableThread;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class Consumer extends ShutdownableThread {
    private final KafkaConsumer<Integer, String> consumer;
    private final String topic;
    private final MongoDatabase db;

    public Consumer(String topic, MongoDatabase db) {
        super("KafkaConsumerExample", false);
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaProperties.KAFKA_SERVER_URL + ":" + KafkaProperties.KAFKA_SERVER_PORT);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "DemoConsumer");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.IntegerDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumer = new KafkaConsumer<>(props);
        this.topic = topic;
        this.db = db;


    }

    @Override
    public void doWork() {
        consumer.subscribe(Collections.singletonList(this.topic));
        ConsumerRecords<Integer, String> records = consumer.poll(2);
        for (ConsumerRecord<Integer, String> record : records) {
            System.out.println("Received message: (" + record.key() + ", " + record.value() + ") at offset " + record.offset());

            String value = record.value();
            String[] strArr = value.trim().split("-----BY-----");
            String tweet = strArr[0];
            String user = strArr[1];

            MongoCollection<Document> collection = db.getCollection("tweets");

            String mention = "";
            String hashtag = "";
            List<String> mentions = new ArrayList<>();
            List<String> hashtags = new ArrayList<>();

            String[] arr = tweet.trim().split(" ");
            for (int i = 0; i < arr.length; i++) {
                if (arr[i].startsWith("@")) {
                    String newMention = arr[i];
                    if (!newMention.equals(mention)) {
                        mention = newMention;
                        mentions.add(mention);
                    }
                }
                if (arr[i].startsWith("#")) {
                    String newHashtag = arr[i];
                    if (!newHashtag.equals(hashtag)) {
                        hashtag = newHashtag;
                        hashtags.add(hashtag);
                    }
                }
            }

            Document document = new Document("title", "MongoDB")
                    .append("_id", record.key())
                    .append("tweet", tweet)
                    .append("username", user)
                    .append("Mentions", mentions)
                    .append("Hashtags", hashtags);
            collection.insertOne(document);
            System.out.println("inserted successfully");


        }
//        List<Document> documents = collection.find().into(
//                new ArrayList<>());
//
//        for (Document doc : documents) {
//            System.out.println(doc);
//        }
//
//            FindIterable<Document> iterDoc = collection.find();
//            MongoCursor<Document> dbc = iterDoc.iterator();
//
//            while(dbc.hasNext()){
//                try {
//                    JsonParser jsonParser = new JsonFactory().createParser(dbc.next().toJson());
//                    ObjectMapper mapper = new ObjectMapper();
//
//                    Person person = mapper.readValue(jsonParser, Person.class);
//                    String name = person.get("Name");
//                    String age = person.get("Age");
//
//                } catch (Exception e){
//                    e.printStackTrace();
//                }
//        }
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }


}