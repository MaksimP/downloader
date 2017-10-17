import com.google.common.util.concurrent.RateLimiter;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class Download implements Runnable {

    private URL url;
    private double limitSpeed;
    private String urlFile;
    private String nameFile;
    private String nameDir;
    private int countBytes;
    private int fullnessBuffer;

    public Download(double limitSpeed, String urlFile, String nameFile, String nameDir) {
        this.limitSpeed = limitSpeed;
        this.urlFile = urlFile;
        this.nameFile = nameFile;
        this.nameDir = nameDir;
    }

    public int getCountBytes() {
        return countBytes;
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        final RateLimiter rateLimiter = RateLimiter.create(limitSpeed);

        try {
            byte[] buffer = new byte[4];
            url = new URL(urlFile);
            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();
            fileOutputStream = new FileOutputStream(nameDir + "/" + nameFile);
            connection.connect();
            inputStream = new BufferedInputStream(connection.getInputStream());

            while ((fullnessBuffer = inputStream.read(buffer)) != -1) {
                rateLimiter.acquire(buffer.length);
                fileOutputStream.write(buffer);
                if (fullnessBuffer < buffer.length) {
                    countBytes += fullnessBuffer;
                } else {
                    countBytes += buffer.length;
                }
            }
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
