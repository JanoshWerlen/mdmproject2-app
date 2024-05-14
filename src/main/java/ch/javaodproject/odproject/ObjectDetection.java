package ch.javaodproject.odproject;

import ai.djl.Application;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public final class ObjectDetection {

    private static final Logger logger = LoggerFactory.getLogger(ObjectDetection.class);

    private final SimpMessagingTemplate template;

    public ObjectDetection(SimpMessagingTemplate template) {
        this.template = template;
    }

    public DetectionResult predict(byte[] imageData, String[] targetClass, double probabilityThreshold)
            throws IOException, ModelException, TranslateException {

        for (int i = 0; i < targetClass.length; i++) {
            System.out.println("looking for " + targetClass[i] + "\n");
        }

        InputStream is = new ByteArrayInputStream(imageData);
        BufferedImage bi = ImageIO.read(is);
        Image img = ImageFactory.getInstance().fromImage(bi);

        Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                .optApplication(Application.CV.OBJECT_DETECTION)
                .setTypes(Image.class, DetectedObjects.class)
                .optFilter("backbone", "resnet50")
                .optProgress(new ProgressBar())
                .build();

        try (ZooModel<Image, DetectedObjects> model = ModelZoo.loadModel(criteria);
                Predictor<Image, DetectedObjects> predictor = model.newPredictor()) {
            DetectedObjects detection = predictor.predict(img);
            String imagePath = saveBoundingBoxImage(img, detection, targetClass, probabilityThreshold);
            logger.info("Object Detection processing completed. \n " +
                    "detection : " + detection + "\n" +
                    "imagePath: " + imagePath);
            return new DetectionResult(detection, imagePath);
        } catch (Exception e) {
            System.out.println("Error loading model: " + e);
        }
        return null;
    }

    public synchronized String saveBoundingBoxImage(Image img, DetectedObjects detection, String[] targetClass,
            double probabilityThreshold) throws IOException {

        // Draw bounding boxes on the image
        img.drawBoundingBoxes(detection);

        // Convert Image to BufferedImage
        BufferedImage bufferedImage = (BufferedImage) img.getWrappedImage();

        // Convert BufferedImage to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        byte[] imageBytes = baos.toByteArray();

        // Define the boundary
        String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString();

        // Construct the multipart form data
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"image\"; filename=\"display.png\"\r\n");
        sb.append("Content-Type: image/png\r\n\r\n");

        byte[] headerBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

        // Concatenate header, image bytes, and footer
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(headerBytes);
        byteArrayOutputStream.write(imageBytes);
        byteArrayOutputStream.write(footerBytes);

        byte[] multipartBytes = byteArrayOutputStream.toByteArray();

        // Create HttpClient and HttpRequest
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:3000/display"))
                .timeout(Duration.ofMinutes(1))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBytes))
                .build();

        // Send request and handle response
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while sending image", e);
        }

        if (response.statusCode() == 200) {
            String webPath = "/display/display.png";
            System.out.println("Image successfully sent to Node.js server, saved at: " + webPath);
            return webPath;
        } else {
            throw new IOException("Failed to send image to Node.js server: " + response.body());
        }
    }
}
