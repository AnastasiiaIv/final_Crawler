package org.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;



public class App {

    public static void ParseNews(Document doc,String link) {

        PrintNews printNews = new PrintNews();

        printNews.setDetailsLink(link);
        printNews.setTitle(doc.title());

        try {
            Elements text = doc.select("div.content-box > p");
            Elements text_2 = doc.select("div.article > p");
            printNews.setText(text.text() + text_2.text());
            Element authorElement = doc.getElementsByClass("name").first().child(0);
            printNews.setAuthor(authorElement.text());

        } catch (NullPointerException e) {
            Element authorElement2 = doc.getElementsByClass("article-footer-item").last();
            if (authorElement2 != null) {
                // проверка, если есть в начале "Автор"
                String author_edit = authorElement2.text();
                author_edit = author_edit.replaceAll("Автор: ","");
                printNews.setAuthor(author_edit);
            } else {
                printNews.setAuthor("Аноним :)");
            }
        }

        try {
            Element dateElement = doc.getElementsByClass("date").first();
            printNews.setDateOfCreated(dateElement.text());

        } catch (NullPointerException e) {
            Element dateElement2 = doc.getElementsByClass("article-meta-time").first();
            if (dateElement2 != null) {
                printNews.setAuthor(dateElement2.text());
            } else {
                printNews.setDateOfCreated("Дата создания не определена");
            }
            printNews.setText("Текст публикации не определился");
        }

        StringWriter writer = new StringWriter();
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(writer, printNews);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String result = writer.toString();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
        factory.setPort(5672);
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.queueDeclare("JSON_DATA", false, false, true, null);
            channel.basicPublish("", "JSON_DATA", null, result.getBytes(StandardCharsets.UTF_8));
            channel.close();
            connection.close();
        } catch (Exception e) {
            return;

        }
    }
}
