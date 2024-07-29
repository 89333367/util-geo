package com.canna.geodata;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

public class GeoData {
    private Log log = LogFactory.get();

    private final byte[] VIEW_AREA = "0000000007".getBytes();
    private final byte[] VIEW_LINE = "00000001F8".getBytes();
    private final byte[] VIEW_POINT = "FFFFFFFE".getBytes();
    private int handle = 0;

    public synchronized String positionDescript(double lon, double lat) {
        try {
            int x = (int) (lon * 3600000.0D);
            int y = (int) (lat * 3600000.0D);
            byte[] res = this.npositionDescript(this.handle, x, y, VIEW_AREA, VIEW_LINE, 10000, VIEW_POINT, 10000);
            return new String(res, CharsetUtil.GBK);
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }

    public boolean load(String ppmFile, String ppcFile) {
        this.handle = this.nload(ppmFile, ppcFile);
        return this.handle != 0;
    }

    public boolean isLoad() {
        return this.handle != 0;
    }

    private native byte[] npositionDescript(int var1, int var2, int var3, byte[] var4, byte[] var5, int var6, byte[] var7, int var8);

    private native int nload(String var1, String var2);
}
