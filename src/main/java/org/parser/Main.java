package org.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;


public class Main {
    private static String url = "http://4pda.to/";
    private static TaskController taskController;
    private static App app;
    private static CreateBD createBD;


    static void SendMsg(String link) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
        factory.setPort(5672);
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.queueDeclare("QUEUE_LINKS", false, false, true, null);
            channel.basicPublish("", "QUEUE_LINKS", null, link.getBytes(StandardCharsets.UTF_8));
            channel.close();
            connection.close();
        } catch (Exception e) {
            return;
        }
    }

    public static void main(String[] args) throws Exception {

        taskController = new TaskController();
        Document doc = taskController.GetUrl(url);


        ProducerConsumer PC = new ProducerConsumer();
        createBD = new CreateBD();

        Thread tMain = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PC.produce(doc);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread tConsume1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PC.consume();
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread tConsume2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PC.consume();
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        });
        Thread TElastic = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    createBD.connect();
                    createBD.SendElastic();

                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        });
        Thread Agr = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    createBD.receive_author();

                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        });

        tMain.start();
        tConsume1.start();
        tConsume2.start();
        TElastic.start();
        Agr.start();

        tMain.join();
        tConsume1.join();
        tConsume2.join();
        TElastic.join();
        Agr.join();

    }

    public static class ProducerConsumer {

        public void produce(Document doc) throws InterruptedException {
            Elements links = doc.getElementsByAttributeValue("itemprop", "url");
            for(Element i : links){
                SendMsg(i.attr("href"));
            }

        }


        public void consume() throws InterruptedException, IOException {
            {
                try {
                    ConnectionFactory factory = new ConnectionFactory();
                    factory.setHost("localhost");
                    factory.setUsername("rabbitmq");
                    factory.setPassword("rabbitmq");
                    factory.setPort(5672);
                    Connection connection = factory.newConnection();
                    Channel channel = connection.createChannel();

                    channel.queueDeclare("QUEUE_LINKS", false, false, true, null);
                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                        String message = new String(delivery.getBody(), "UTF-8");
                        try {
                            String childlink = message;
                            Document newDoc = taskController.GetUrl(childlink);
                            app.ParseNews(newDoc, childlink);
                        } catch (Exception e) {
                            System.out.println(" error downloading page " + e.toString());
                        } finally {
                            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        }

                    };
                    channel.basicConsume("QUEUE_LINKS", false, deliverCallback, consumerTag -> { });
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                    e.printStackTrace();
                }
            }


        }
    }
}


