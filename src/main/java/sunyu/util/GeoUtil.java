package sunyu.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
import com.canna.geodata.GeoData;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
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
public class GeoUtil implements AutoCloseable {
    private final Log log = LogFactory.get();
    private final Config config;

    public static Builder builder() {
        return new Builder();
    }

    private GeoUtil(Config config) {
        log.info("[构建GeoUtil] 开始");

        log.info("释放动态链接库 开始");
        for (String resourceFile : config.resourceFiles) {
            if (resourceFile.equals(config.PPM)) {//ppm特殊处理，因为这个文件太大了，在resource中是压缩的，要先解压再合并成一个文件
                for (int i = 0; i <= config.splitNum; i++) {
                    String splitName = resourceFile + ".part" + StrUtil.fillBefore(Convert.toStr(i), '0', 3) + ".zip";
                    FileUtil.writeFromStream(ResourceUtil.getStream(splitName), config.userDir + "/" + splitName);
                }
                mergeFiles(config.userDir, config.userDir, config.PPM);
            } else {
                FileUtil.writeFromStream(ResourceUtil.getStream(resourceFile), config.userDir + "/" + resourceFile);
            }
        }
        log.info("释放动态链接库 结束");

        log.info("加载动态链接库开始");
        if (SystemUtil.getOsInfo().isWindows()) {
            System.load(config.userDir + "/" + config.GEO_DATA);
        } else {
            System.load(config.userDir + "/" + config.LIB_GEO_DATA);
        }
        log.info("加载动态链接库 结束");

        log.info("读取数据开始");
        config.geoData = new GeoData();
        config.geoData.load(config.userDir + "/" + config.PPM, config.userDir + "/" + config.PPC);
        if (config.geoData.isLoad()) {
            log.info("读取数据成功");
        } else {
            log.error("读取数据失败");
            throw new RuntimeException("读取数据失败");
        }
        log.info("[构建GeoUtil] 结束");

        this.config = config;
    }

    private static class Config {
        private final String userDir = System.getProperty("user.dir");
        private final String PPM = "china_desc.ppm";
        private final String PPC = "china_desc.ppc";
        private final String GEO_DATA = "GeoData.dll";
        private final String LIB_GEO_DATA = "libGeoData.so";
        private final List<String> resourceFiles = Arrays.asList(PPC, PPM, GEO_DATA, LIB_GEO_DATA);
        private GeoData geoData;
        private final int splitNum = 331;
    }

    public static class Builder {
        private final Config config = new Config();

        public GeoUtil build() {
            return new GeoUtil(config);
        }
    }

    /**
     * 回收资源
     */
    @Override
    public void close() {
        log.info("[销毁GeoUtil] 开始");
        for (String resourceFile : config.resourceFiles) {
            if (resourceFile.equals(config.PPM)) {
                for (int i = 0; i <= config.splitNum; i++) {
                    String splitName = resourceFile + ".part" + StrUtil.fillBefore(Convert.toStr(i), '0', 3) + ".zip";
                    try {
                        FileUtil.del(config.userDir + "/" + splitName);
                    } catch (Exception e) {
                        log.warn("清理资源异常 {}", ExceptionUtil.stacktraceToString(e));
                    }
                }
            }
            try {
                FileUtil.del(config.userDir + "/" + resourceFile);
            } catch (Exception e) {
                log.warn("清理资源异常 {}", ExceptionUtil.stacktraceToString(e));
            }
        }
        log.info("[销毁GeoUtil] 结束");
    }

    /**
     * 通过经纬度获取地址
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 地址
     */
    synchronized public String getAddress(double lon, double lat) {
        log.debug("参数 {} {}", lon, lat);
        String address = config.geoData.positionDescript(lon, lat);
        log.debug("响应值 {} {} {}", lon, lat, address);
        return address;
    }


    public void mergeFiles(String inputDirPath, String outputDirPath, String mergeFileName) {
        File inputDir = new File(inputDirPath);
        File[] partFiles = inputDir.listFiles((dir, name) -> name.matches(".*\\.part\\d+\\.zip$"));
        if (partFiles == null || partFiles.length == 0) {
            log.error("在目录中没有找到分片文件");
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
        try (BufferedOutputStream writer = new BufferedOutputStream(Files.newOutputStream(outputFile.toPath()))) {
            for (File partFile : partFiles) {
                try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(partFile.toPath()))) {
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
                    log.error("压缩文件异常 {}", ExceptionUtil.stacktraceToString(e));
                }
            }
            log.debug("文件合并完毕，合并后文件 {} {}", outputDirPath, mergeFileName);
        } catch (IOException e) {
            log.error("合并文件异常 {}", ExceptionUtil.stacktraceToString(e));
        }
    }

    public void splitFile(String inputFilePath, String outputDirPath, int chunkSize) {
        File inputFile = new File(inputFilePath);
        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        try (BufferedInputStream reader = new BufferedInputStream(Files.newInputStream(inputFile.toPath()))) {
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
                    zipOut = new ZipOutputStream(Files.newOutputStream(chunkFile.toPath()));
                    zipOut.putNextEntry(new ZipEntry(inputFile.getName()));
                    currentSize = 0;
                }
                zipOut.write(buffer, 0, bytesRead);
                currentSize += bytesRead;
            }
            closeQuietly(zipOut);
            log.debug("文件切割完毕，请查看 {} 目录", outputDirPath);
        } catch (IOException e) {
            log.error("切割文件异常 {}", ExceptionUtil.stacktraceToString(e));
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

}