# GEO工具类

## 描述

* 通过经纬度可以转换出中文地址

## 环境

* jdk8 x86版本，本工具类使用jdk8 x86版本编译，只能在这个版本使用

```xml

<dependency>
    <groupId>sunyu.util</groupId>
    <artifactId>util-geo</artifactId>
    <!-- {util.version}_{jdk.version}_{architecture.version} -->
    <version>1.0_jdk8_x86</version>
</dependency>
```

```java

@Test
void t001() {
    GeoUtil geoUtil = GeoUtil.builder().build();
    log.debug("{}", geoUtil.getAddress(118.829303, 36.740818));
    
    //geoUtil.close();//如果项目不再使用，需要调用这个方法
}
```