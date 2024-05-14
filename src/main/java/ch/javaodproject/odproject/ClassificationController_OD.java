package ch.javaodproject.odproject;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import ai.djl.modality.Classifications;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@RestController
public class ClassificationController_OD {

    private final List<SseEmitter> emitters = new ArrayList<>();

    private static int fps = 5;
    private static final double YES_PROBABILITY_THRESHOLD = 0.1; // 0.5 = 50% probability
    private static String[] searchObject = { "*" };

    @Autowired
    private ObjectDetection ObjectDetection;

    private static final String TEMP_DIR = "src\\main\\resources\\static\\tempVideos";
    private static final String Frames_DIR = "src\\main\\resources\\static\\Frames_Dir";
    //private static final String outputDir = "src/main/resources/static/predict_img";


    @GetMapping("/reload-image")
    public String reloadImage() {
        String imagePath = "display/display.png";
        System.out.println("Image reloaded from " + imagePath);

        // Notify all connected clients to reload the image
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("reload").data("reload"));
                System.out.println("Sent reload event to client" + emitter);
            } catch (IOException e) {
                deadEmitters.add(emitter);
                e.printStackTrace();
            }
        }

        // Remove all dead emitters
        emitters.removeAll(deadEmitters);

        return "Image reloaded";
    }

    @GetMapping("/sse")
    public SseEmitter streamSseMvc() {
        System.out.println("SSE Triggered \n");

        SseEmitter sseEmitter = new SseEmitter(60000L); // Set timeout to 60 seconds
        emitters.add(sseEmitter);
        

        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        sseEmitter.onTimeout(() -> emitters.remove(sseEmitter));
        sseEmitter.onError((e) -> emitters.remove(sseEmitter));

        return sseEmitter;
    }
    
    @PostMapping(path = "/analyze")
    public ResponseEntity<?> predict_OD(@RequestParam("image") MultipartFile image) {

        // System.out.println("Session ID: " + request.getSession().getId() + " -
        // Current searchObject: " + searchObject);
        if (image.isEmpty()) {
            System.out.println("Picture is empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"Empty file.\"}");

        }

        try {
            DetectionResult result = ObjectDetection.predict(image.getBytes(), searchObject, YES_PROBABILITY_THRESHOLD);
            if (result == null || result.getDetectedObjects() == null || result.getImagePath() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"error\": \"Detection failed.\"}");
            }

            // Since result.getDetectedObjects().toJson() is an array, parse it as JSONArray
            JSONArray detections = new JSONArray(result.getDetectedObjects().toJson());
            JSONObject json = new JSONObject();
            json.put("detections", detections);
            System.out.println("\n");
            json.put("imagePath", result.getImagePath());
            System.out.println("\n");
            System.out.println("detection: " + detections);
            System.out.println("\n");
            System.out.println("imagePath " + result.getImagePath());
            System.out.println("\n");
            System.out.println("Json " + json);

            System.out.println("\n");
            sendNotifyRequest();

            // VorlesungsbeispielApplication.notifyWebSocketServer();
        System.out.println("\n" + "Response Entity: " +ResponseEntity.ok(json.toString()) + "\n");

            return ResponseEntity.ok(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }




    private Inference inference = new Inference();
    @PostMapping(path = "/analyze_Class")
    public ResponseEntity<?> predictClass(@RequestParam("image") MultipartFile image) {
        if (image.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"Empty file.\"}");
        }
    
        try {
            byte[] imageBytes = image.getBytes();
            Object[] results = inference.predict(imageBytes);
            Classifications predictionResult = (Classifications) results[0];
            String path = (String) results[1];
    
            // Convert Classifications to a more appropriate JSON format
            List<Classifications.Classification> classifications = predictionResult.items();
            JSONArray jsonArray = new JSONArray();
            for (Classifications.Classification classification : classifications) {
                JSONObject jsonItem = new JSONObject();
                jsonItem.put("class", classification.getClassName());
                jsonItem.put("probability", classification.getProbability());
                jsonArray.put(jsonItem);
            }
    
            JSONObject json = new JSONObject();
            json.put("detections", jsonArray);
            json.put("imagePath", path); // Update or calculate the image path
    
            return ResponseEntity.ok(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    
    private void sendNotifyRequest() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:3000/notify"))
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
           // logger.info("Notification sent, response status: {}", response.statusCode());
        } catch (Exception e) {
           // logger.error("Failed to send notification", e);
        }
    }

    public String sanitizeFilePath(String path) {
        return path.replaceAll("[<>:\"/\\\\|?*]", "");
    }

}
