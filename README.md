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
    GeoUtil geoUtil = GeoUtil.builder().build();
    log.debug("{}", geoUtil.getAddress(118.829303, 36.740818));
    //geoUtil.close();//如果项目不再使用，需要调用这个方法
}
```