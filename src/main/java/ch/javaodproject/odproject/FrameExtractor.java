package ch.javaodproject.odproject;

import com.github.kokorin.jaffree.ffmpeg.FFmpeg;
import com.github.kokorin.jaffree.ffmpeg.UrlInput;
import com.github.kokorin.jaffree.ffmpeg.UrlOutput;
import com.github.kokorin.jaffree.StreamType;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
public class FrameExtractor {

    public List<Path> extractFrames(String videoFile, double frameIntervalSeconds, String outputDirectory) {
        List<Path> framePaths = new ArrayList<>();
        System.out.println("Attempting to process video file at: " + videoFile);

        File video = new File(videoFile);
        if (!video.exists()) {
            System.err.println("Video file does not exist at the specified path: " + videoFile);
            return framePaths;
        }

        File dir = new File(outputDirectory);
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("Could not create directory for frames at: " + outputDirectory);
            return framePaths;
        }

        String frameOutputPattern = outputDirectory + File.separator + "frame_%d.jpg";

        FFmpeg.atPath()
                .addInput(UrlInput.fromPath(Paths.get(videoFile)))
                .addOutput(UrlOutput.toPath(Paths.get(frameOutputPattern))
                        .setFrameRate(1 / frameIntervalSeconds)
                        .setCodec(StreamType.VIDEO, "mjpeg"))
                .execute();

        // Assuming frames are saved successfully, list them
        File[] files = new File(outputDirectory).listFiles();
        if (files != null) {
            for (File file : files) {
                framePaths.add(file.toPath());
                System.out.println("Frame has been extracted and saved as " + file.toPath());
            }
        }

        System.out.println("Frame extraction completed. Total frames extracted: " + framePaths.size());

        return framePaths;
    }
}
