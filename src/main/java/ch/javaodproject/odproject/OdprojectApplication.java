package ch.javaodproject.odproject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OdprojectApplication {
    private static WebSocketNotifyClient client;

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(OdprojectApplication.class, args);

        Thread.sleep(5000); // 5000 milliseconds delay

        try {
            client = new WebSocketNotifyClient(new URI("ws://mdm-project-2-server.azurewebsites.net:80"));
            client.setConnectionLostTimeout(10); // Increase timeout (in seconds)
            if (client.connectBlocking(10, TimeUnit.SECONDS)) { // Wait for up to 10 seconds
                System.out.println("WebSocket connected on " + client);
            } else {
                System.out.println("WebSocket connection failed");
            }
        } catch (URISyntaxException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void notifyWebSocketServer(String message) {
        if (client != null && client.isOpen()) {
            try {
                System.out.println("Attempting to notify Server...");
                client.notifyServer(message);
            } catch (Exception e) {
                System.out.println("Failed to notify server. Error: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("WebSocket is not connected. Attempting to reconnect...");
            try {
                client.reconnectBlocking();
                client.notifyServer(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }
}
