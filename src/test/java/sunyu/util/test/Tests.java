package sunyu.util.test;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.junit.jupiter.api.Test;
import sunyu.util.GeoUtil;

public class Tests {
    private Log log = LogFactory.get();

    @Test
    void t001() {
        GeoUtil geoUtil = GeoUtil.builder().build();
        log.debug("{}", geoUtil.getAddress(118.829303, 36.740818));
        //geoUtil.close();//如果项目不再使用，需要调用这个方法
    }

    @Test
    void t002() {
        String inputFilePath = "D:\\GitLab\\dapr-service-geo\\src\\main\\resources\\china_desc.ppm"; // 大文件路径
        String outputDirPath = "d:/tmp/ppm"; // 拆分文件输出目录
        int chunkSize = 1 * 1024 * 1024; // 拆分文件大小，这里设置为不大于1M
        GeoUtil.builder().splitFile(inputFilePath, outputDirPath, chunkSize);
    }


    @Test
    void t003() {
        String inputDirPath = "d:/tmp/ppm"; // 修改为实际分割文件所在目录路径
        String outputDirPath = "d:/tmp/ppm2"; // 修改为实际输出文件所在目录路径
        GeoUtil.builder().mergeFiles(inputDirPath, outputDirPath, "china_desc.ppm");
    }

    @Test
    void t004() {
        for (int i = 0; i <= 331; i++) {
            String splitName = "china_desc.ppm.part" + StrUtil.fillBefore(Convert.toStr(i), '0', 3) + ".zip";
            log.debug("{}", splitName);
        }
    }

}
