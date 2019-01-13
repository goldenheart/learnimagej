package com.yjx.commonsimaging;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @Version 1.0
 * @Since JDK1.8
 * @Author junxiaoyang
 * @Company 洛阳艾鹿网络有限公司
 * @Date 2019-01-12 13:51
 */
@Slf4j
public class RegexpPropertyLoader {

    public static Properties load() throws IOException {
        InputStream rootPath = Thread.currentThread().getContextClassLoader().getResourceAsStream("regex.properties");
        Properties properties = new Properties();
        properties.load(rootPath);
        return properties;
    }
}
