package ch.javaodproject.odproject;

import ai.djl.modality.cv.output.DetectedObjects;

public class DetectionResult {
    private DetectedObjects detectedObjects;
    private String imagePath;

    public DetectionResult(DetectedObjects detectedObjects, String imagePath) {
        this.detectedObjects = detectedObjects;
        this.imagePath = imagePath;
    }

    public DetectedObjects getDetectedObjects() {
        return detectedObjects;
    }

    public String getImagePath() {
        return imagePath;
    }
}
