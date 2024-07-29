package sunyu.util;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MergeFileUtil {
    public static void mergeFiles(String inputDirPath, String outputDirPath, String mergeFileName) {
        File inputDir = new File(inputDirPath);
        File[] partFiles = inputDir.listFiles((dir, name) -> name.matches(".*\\.part\\d+\\.zip$"));
        if (partFiles == null || partFiles.length == 0) {
            System.out.println("No part files found in the directory.");
            return;
        }
        // 按文件名中的数字顺序排序
        Arrays.sort(partFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                String num1 = f1.getName().replaceAll(".*\\.part(\\d+)\\.zip$", "$1");
                String num2 = f2.getName().replaceAll(".*\\.part(\\d+)\\.zip$", "$1");
                return Integer.parseInt(num1) - Integer.parseInt(num2);
            }
        });
        // 确保输出目录存在
        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        // 合并文件名为 china_desc.ppm
        File outputFile = new File(outputDir, mergeFileName);
        try (BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            for (File partFile : partFiles) {
                try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(partFile))) {
                    ZipEntry entry = zipIn.getNextEntry();
                    if (entry != null) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = zipIn.read(buffer)) > 0) {
                            writer.write(buffer, 0, bytesRead);
                        }
                        zipIn.closeEntry();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("File merge and unzip completed.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
