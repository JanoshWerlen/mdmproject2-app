package ch.javaodproject.odproject;

import ai.djl.Model;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.translator.ImageClassificationTranslator;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import javax.imageio.ImageIO;



public class Inference {

    Predictor<Image, Classifications> predictor;

    public Inference() {
        try {
            Model model = Models.getModel();
            Path modelDir = Paths.get("models");
            System.out.println("Using mode: " + Models.MODEL_NAME);
            model.load(modelDir, Models.MODEL_NAME);

            // define a translator for pre and post processing
            Translator<Image, Classifications> translator = ImageClassificationTranslator.builder()
                    .addTransform(new Resize(Models.IMAGE_WIDTH, Models.IMAGE_HEIGHT))
                    .addTransform(new ToTensor())
                    .optApplySoftmax(true)
                    .build();
            predictor = model.newPredictor(translator);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
public Object[] predict(byte[] image) throws ModelException, TranslateException, IOException {
    InputStream is = new ByteArrayInputStream(image);
    BufferedImage bi = ImageIO.read(is);
    Image img = ImageFactory.getInstance().fromImage(bi);

    // Save the image and get the path
    String path = saveImage(img);

    // Get the prediction result
    Classifications predictResult = this.predictor.predict(img);

    // Store results in an Object array
    Object[] results = new Object[2];
    results[0] = predictResult;  // First element is the Classifications object
    results[1] = path;           // Second element is the path to the saved image

    return results;
}

    
    synchronized private String saveImage(Image img)
            throws IOException {
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
                        .uri(URI.create("http://mdm-project-2-server.azurewebsites.net/display"))
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
