import javax.sound.sampled.*;
import org.jtransforms.fft.DoubleFFT_1D;

public class WhistleC {

    private static final double WHISTLE_MIN_FREQUENCY = 1000;
    private static final double WHISTLE_MAX_FREQUENCY = 3000;
    private static final double DETECTION_THRESHOLD = 200;

    private int whistleCount = 0;
    private boolean isWhistleDetected = false;

    public static void main(String[] args) {
        WhistleC counter = new WhistleC();
        counter.captureAudio();
    }

    public void captureAudio() {
        try {
            AudioFormat format = new AudioFormat(16000, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Microphone not supported.");
                return;
            }

            final TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);

            byte[] buffer = new byte[4096];
            microphone.start();

            System.out.println("Listening for cooker whistles... Press Ctrl+C to stop.");

            while (true) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    double[] audioData = convertToDoubleArray(buffer, bytesRead);
                    analyzeAudio(audioData);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double[] convertToDoubleArray(byte[] buffer, int bytesRead) {
        double[] audioData = new double[bytesRead / 2];
        for (int i = 0; i < audioData.length; i++) {
            int sample = ((buffer[2 * i] << 8) | (buffer[2 * i + 1] & 0xFF));
            audioData[i] = sample / 32768.0;
        }
        return audioData;
    }

    private void analyzeAudio(double[] audioData) {
        DoubleFFT_1D fft = new DoubleFFT_1D(audioData.length);
        fft.realForward(audioData);

        double[] magnitude = new double[audioData.length / 2];
        for (int i = 0; i < magnitude.length; i++) {
            double real = audioData[2 * i];
            double imag = audioData[2 * i + 1];
            magnitude[i] = Math.sqrt(real * real + imag * imag);
        }

        double dominantFrequency = findDominantFrequency(magnitude, 16000);

        if (dominantFrequency >= WHISTLE_MIN_FREQUENCY && dominantFrequency <= WHISTLE_MAX_FREQUENCY) {
            if (!isWhistleDetected) {
                whistleCount++;
                isWhistleDetected = true;
                System.out.println("Whistle detected! Frequency: " + dominantFrequency + " Hz");
                System.out.println("Whistle count: " + whistleCount);
            }
        } else {
            isWhistleDetected = false;
        }
    }

    private double findDominantFrequency(double[] magnitude, int sampleRate) {
        int maxIndex = 0;
        double maxMagnitude = 0;

        for (int i = 0; i < magnitude.length; i++) {
            if (magnitude[i] > maxMagnitude && magnitude[i] > DETECTION_THRESHOLD) {
                maxMagnitude = magnitude[i];
                maxIndex = i;
            }
        }

        return maxIndex * (sampleRate / 2.0) / magnitude.length;
    }
}
