package ch.javaodproject.odproject;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OdprojectApplication {
    private static WebSocketNotifyClient client;

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(OdprojectApplication.class, args);

        Thread.sleep(5000); // 5000 milliseconds delay

        try {//mdm-project-2-server.azurewebsites.net
            client = new WebSocketNotifyClient(new URI("ws://localhost:8081"));
            client.connect();
            System.out.println("WebSocket connected on " + client);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void notifyWebSocketServer() {
        if (client == null || !client.isOpen()) {
            // Attempt to reconnect
            try {
                System.out.println("Attempting to reconnect WebSocket...");
                client = new WebSocketNotifyClient(new URI("ws://localhost:8081"));
                client.connectBlocking(); 
            } catch (URISyntaxException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (client != null && client.isOpen()) {
            System.out.println("Attempting to notify Server...");
            client.send("update");
        } else {
            System.out.println("WebSocket is still not connected.");
        }
    }
}
