import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

public class ConsumerMain {
    public static void main(String args[]){
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase database = mongoClient.getDatabase("twitterDB");
//        database.createCollection("tweets",null);

        Consumer consumerThread = new Consumer(KafkaProperties.TOPIC, database);
        consumerThread.start();
    }
}
