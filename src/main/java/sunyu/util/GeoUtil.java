package sunyu.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.log.StaticLog;
import cn.hutool.system.SystemUtil;
import com.canna.geodata.GeoData;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 记亩工具类
 *
 * @author 孙宇
 */
public class GeoUtil implements Serializable, Closeable {
    private Log log = LogFactory.get();
    private static final GeoUtil INSTANCE = new GeoUtil();


    private String userDir = System.getProperty("user.dir");
    private String PPM = "china_desc.ppm";
    private String PPC = "china_desc.ppc";
    private String GEO_DATA = "GeoData.dll";
    private String LIB_GEO_DATA = "libGeoData.so";
    private List<String> resourceFiles = Arrays.asList(PPC, PPM, GEO_DATA, LIB_GEO_DATA);
    private GeoData geoData;
    private int splitNum = 331;

    /**
     * 通过经纬度获取地址
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 地址
     */
    synchronized public String getAddress(double lon, double lat) {
        log.debug("参数 {} {}", lon, lat);
        String address = geoData.positionDescript(lon, lat);
        log.debug("响应值 {} {} {}", lon, lat, address);
        return address;
    }


    public void mergeFiles(String inputDirPath, String outputDirPath, String mergeFileName) {
        File inputDir = new File(inputDirPath);
        File[] partFiles = inputDir.listFiles((dir, name) -> name.matches(".*\\.part\\d+\\.zip$"));
        if (partFiles == null || partFiles.length == 0) {
            System.out.println("No part files found in the directory.");
            return;
        }
        // 按文件名中的数字顺序排序
        Arrays.sort(partFiles, (f1, f2) -> {
            String num1 = f1.getName().replaceAll(".*\\.part(\\d+)\\.zip$", "$1");
            String num2 = f2.getName().replaceAll(".*\\.part(\\d+)\\.zip$", "$1");
            return Integer.parseInt(num1) - Integer.parseInt(num2);
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
            StaticLog.debug("文件合并完毕，合并后文件 {} {}", outputDirPath, mergeFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void splitFile(String inputFilePath, String outputDirPath, int chunkSize) {
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
            StaticLog.debug("文件切割完毕，请查看 {} 目录", outputDirPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeQuietly(ZipOutputStream zipOut) {
        if (zipOut != null) {
            try {
                zipOut.close();
            } catch (IOException e) {
                // Ignore IOException on close
            }
        }
    }


    /**
     * 私有构造，避免外部初始化
     */
    private GeoUtil() {
    }

    /**
     * 获得工具类工厂
     *
     * @return
     */
    public static GeoUtil builder() {
        return INSTANCE;
    }

    /**
     * 构建工具类
     *
     * @return
     */
    public GeoUtil build() {
        log.info("构建工具开始");

        if (geoData != null && geoData.isLoad()) {
            log.warn("工具类已构建，请不要重复构建");
            return INSTANCE;
        }

        log.info("释放动态链接库开始");
        for (String resourceFile : resourceFiles) {
            if (resourceFile.equals(PPM)) {//ppm特殊处理，因为这个文件太大了，在resource中是压缩的，要先解压再合并成一个文件
                for (int i = 0; i <= splitNum; i++) {
                    String splitName = resourceFile + ".part" + StrUtil.fillBefore(Convert.toStr(i), '0', 3) + ".zip";
                    FileUtil.writeFromStream(ResourceUtil.getStream(splitName), userDir + "/" + splitName);
                }
                mergeFiles(userDir, userDir, PPM);
            } else {
                FileUtil.writeFromStream(ResourceUtil.getStream(resourceFile), userDir + "/" + resourceFile);
            }
        }
        log.info("释放动态链接库完毕");

        log.info("加载动态链接库开始");
        if (SystemUtil.getOsInfo().isWindows()) {
            System.load(userDir + "/" + GEO_DATA);
        } else {
            System.load(userDir + "/" + LIB_GEO_DATA);
        }
        log.info("加载动态链接库完毕");

        log.info("读取数据开始");
        geoData = new GeoData();
        geoData.load(userDir + "/" + PPM, userDir + "/" + PPC);
        if (geoData.isLoad()) {
            log.info("读取数据成功");
        } else {
            log.error("读取数据失败");
            throw new RuntimeException("读取数据失败");
        }
        log.info("构建工具完毕");
        return INSTANCE;
    }

    /**
     * 回收资源
     */
    @Override
    public void close() {
        log.info("销毁工具开始");
        for (String resourceFile : resourceFiles) {
            if (resourceFile.equals(PPM)) {
                for (int i = 0; i <= splitNum; i++) {
                    String splitName = resourceFile + ".part" + StrUtil.fillBefore(Convert.toStr(i), '0', 3) + ".zip";
                    try {
                        FileUtil.del(userDir + "/" + splitName);
                    } catch (Exception e) {
                        log.warn(e.getMessage());
                    }
                }
            }
            try {
                FileUtil.del(userDir + "/" + resourceFile);
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }
        log.info("销毁工具结束");
    }


}





