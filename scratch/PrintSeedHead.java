package scratch;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class PrintSeedHead {
    public static void main(String[] args) {
        File gzFile = new File("seed/dogo_seed_recent15.sql.gz");
        if (!gzFile.exists()) {
            System.out.println("GZ file not found.");
            return;
        }

        try (InputStream fileStream = new FileInputStream(gzFile);
             InputStream gzipStream = new GZIPInputStream(fileStream);
             Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(decoder)) {

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 100) {
                if (line.trim().length() > 0 && !line.startsWith("--") && !line.startsWith("/*")) {
                    System.out.println(line.substring(0, Math.min(line.length(), 200)));
                    count++;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
