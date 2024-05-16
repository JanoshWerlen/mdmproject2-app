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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.io.File;

import ai.djl.modality.Classifications;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
    private static final String outputDir = "src/main/resources/static/predict_img";

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

            JSONArray detections = new JSONArray(result.getDetectedObjects().toJson());
            JSONObject json = new JSONObject();
            json.put("detections", detections);
            json.put("imagePath", result.getImagePath());

            System.out.println("detection: " + detections);
            System.out.println("imagePath: " + result.getImagePath());
            System.out.println("Json: " + json);


             System.out.println("TEST: " + json.toString());   
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

            JSONArray jsonArray = new JSONArray();
            for (Classifications.Classification classification : predictionResult.items()) {
                if (classification != null && classification.getClassName() != null && !classification.getClassName().isEmpty()) {
                    JSONObject jsonItem = new JSONObject();
                    jsonItem.put("class", classification.getClassName());
                    jsonItem.put("probability", classification.getProbability());
                    jsonArray.put(jsonItem);
                }
            }

            JSONObject json = new JSONObject();
            json.put("detections", jsonArray);
            json.put("imagePath", path);

            return ResponseEntity.ok(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private FrameExtractor extractor = new FrameExtractor();
    @PostMapping("/upload_video")
    public Map<String, Object> handleFileUpload(@RequestParam("video") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Empty file provided.");
        }
    
        Path tempFile = null;
        try {
            System.out.println("Received video file " + file.getSize());
            Path tempDirPath = Paths.get(TEMP_DIR);
            Files.createDirectories(tempDirPath);
            tempFile = Files.createTempFile(tempDirPath, null, ".mp4");
            file.transferTo(tempFile);
    
            List<Path> frames = extractor.extractFrames(tempFile.toString(), fps, Frames_DIR);
            JSONArray resultsForHighProb = new JSONArray();
            String imagePath = null;
            for (Path framePath : frames) {
                byte[] imageData = Files.readAllBytes(framePath);
                try {
                    DetectionResult detectionResult = ObjectDetection.predict(imageData, searchObject, YES_PROBABILITY_THRESHOLD);
                    if (detectionResult == null || detectionResult.getDetectedObjects() == null || detectionResult.getImagePath() == null) {
                        continue;
                    }
    
                    JSONArray detections = new JSONArray(detectionResult.getDetectedObjects().toJson());
                    JSONObject frameResult = new JSONObject();
                    frameResult.put("detections", detections);
                    frameResult.put("imagePath", detectionResult.getImagePath());
                    resultsForHighProb.put(frameResult);
                    imagePath = detectionResult.getImagePath();
    
                    // Notify WebSocket server for each frame
                    OdprojectApplication.notifyWebSocketServer(imagePath);
    
                    // Introduce a small delay to ensure the message is processed by the client
                    Thread.sleep(100); // Adjust the sleep time as needed
                } catch (Exception e) {
                    cleanUpResources(tempFile);
                    e.printStackTrace();
                    continue;
                }
            }
    
            System.out.println("Amount of detections: " + resultsForHighProb.length());
    
            Map<String, Integer> classNameCounts = new HashMap<>();
            for (int i = 0; i < resultsForHighProb.length(); i++) {
                JSONObject detectionResult = resultsForHighProb.getJSONObject(i);
                JSONArray detections = detectionResult.getJSONArray("detections");
                for (int j = 0; j < detections.length(); j++) {
                    JSONObject detectionItem = detections.getJSONObject(j);
                    String className = detectionItem.getString("className");
                    classNameCounts.put(className, classNameCounts.getOrDefault(className, 0) + 1);
                }
            }
    
            for (Map.Entry<String, Integer> entry : classNameCounts.entrySet()) {
                System.out.println("ClassName: " + entry.getKey() + ", Amount: " + entry.getValue());
            }
    
            cleanUpResources(tempFile);
    
            Map<String, Object> response = new HashMap<>();
            response.put("classNameCounts", classNameCounts);
            response.put("imagePath", imagePath);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to process video file.", e);
        }
    }
    

    private void cleanUpResources(Path tempFile) throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
        deleteDirectoryRecursively(Paths.get(TEMP_DIR));
        deleteDirectoryRecursively(Paths.get(Frames_DIR));
    }

    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder())
                     .map(Path::toFile)
                     .forEach(File::delete);
            }
            System.out.println("Deleted directory: " + directory);
        }
    }
    

    public String sanitizeFilePath(String path) {
        return path.replaceAll("[<>:\"/\\\\|?*]", "");
    }
}
