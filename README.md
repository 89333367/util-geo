# GEO工具类

> 通过经纬度可以转换出中文地址
> 
> 要求jdk8 x86版本，本工具类使用jdk8 x86版本编译

```xml
<dependency>
    <groupId>sunyu.util</groupId>
    <artifactId>util-geo</artifactId>
    <version>jdk8_x86_v1.0</version>
</dependency>
```


```java
@Test
void t001() {
    GeoUtil geoUtil = GeoUtil.INSTANCE.build();//全局只需要定义一次
    log.debug("{}", geoUtil.getAddress(118.829303, 36.740818));
    //geoUtil.close();如果项目不在使用，可以调用这个方法
}
```