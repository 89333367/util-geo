package sunyu.util;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SplitFileUtil {
    public static void splitFile(String inputFilePath, String outputDirPath, int chunkSize) {
        File inputFile = new File(inputFilePath);
        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        try (BufferedInputStream reader = new BufferedInputStream(new FileInputStream(inputFile))) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            int count = 0;
            int currentSize = 0;
            ZipOutputStream zipOut = null;
            while ((bytesRead = reader.read(buffer)) > 0) {
                if (zipOut == null || currentSize >= chunkSize) {
                    closeQuietly(zipOut);
                    String chunkFileName = String.format("%s.part%03d.zip", inputFile.getName(), count++);
                    File chunkFile = new File(outputDir, chunkFileName);
                    zipOut = new ZipOutputStream(new FileOutputStream(chunkFile));
                    zipOut.putNextEntry(new ZipEntry(inputFile.getName()));
                    currentSize = 0;
                }
                zipOut.write(buffer, 0, bytesRead);
                currentSize += bytesRead;
            }
            closeQuietly(zipOut);
            System.out.println("File split into parts and zipped completed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void closeQuietly(ZipOutputStream zipOut) {
        if (zipOut != null) {
            try {
                zipOut.close();
            } catch (IOException e) {
                // Ignore IOException on close
            }
        }
    }
}
