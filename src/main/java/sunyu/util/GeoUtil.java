package sunyu.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;
import com.canna.geodata.GeoData;

import java.io.Closeable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * 记亩工具类
 *
 * @author 孙宇
 */
public enum GeoUtil implements Serializable, Closeable {
    INSTANCE;
    private Log log = LogFactory.get();
    private String userDir = System.getProperty("user.dir");
    private String PPM = "china_desc.ppm";
    private String PPC = "china_desc.ppc";
    private String GEO_DATA = "GeoData.dll";
    private String LIB_GEO_DATA = "libGeoData.so";
    private List<String> resourceFiles = Arrays.asList(PPC, PPM, GEO_DATA, LIB_GEO_DATA);
    private GeoData geoData;
    private int splitNum = 6;

    public GeoUtil build() {
        if (geoData == null) {
            log.debug("userDir {}", userDir);
            for (String resourceFile : resourceFiles) {
                if (resourceFile.equals(PPM)) {//ppm特殊处理，因为这个文件太大了，在resource中是压缩的，要先解压再合并成一个文件
                    for (int i = 0; i <= splitNum; i++) {
                        String splitName = resourceFile + ".part" + StrUtil.fillBefore(Convert.toStr(i), '0', 3) + ".zip";
                        FileUtil.writeFromStream(ResourceUtil.getStream(splitName), userDir + "/" + splitName);
                    }
                    MergeFileUtil.mergeFiles(userDir, userDir, PPM);
                } else {
                    FileUtil.writeFromStream(ResourceUtil.getStream(resourceFile), userDir + "/" + resourceFile);
                }
            }
            if (SystemUtil.getOsInfo().isWindows()) {
                System.load(userDir + "/" + GEO_DATA);
            } else {
                System.load(userDir + "/" + LIB_GEO_DATA);
            }
            geoData = new GeoData();
            geoData.load(userDir + "/" + PPM, userDir + "/" + PPC);
            if (geoData.isLoad()) {
                log.debug("geo文件初始化完毕");
            }
        }
        return INSTANCE;
    }

    /**
     * 通过经纬度获取地址
     *
     * @param lon 经度
     * @param lat 纬度
     * @return 地址
     */
    synchronized public String getAddress(double lon, double lat) {
        return geoData.positionDescript(lon, lat);
    }

    @Override
    public void close() {
        for (String resourceFile : resourceFiles) {
            if (resourceFile.equals(PPM)) {
                for (int i = 0; i <= splitNum; i++) {
                    String splitName = resourceFile + ".part" + StrUtil.fillBefore(Convert.toStr(i), '0', 3) + ".zip";
                    try {
                        FileUtil.del(userDir + "/" + splitName);
                    } catch (IORuntimeException e) {
                    }
                }
            }
            try {
                FileUtil.del(userDir + "/" + resourceFile);
            } catch (Exception e) {
            }
        }
    }
}
