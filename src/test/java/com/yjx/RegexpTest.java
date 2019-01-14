package com.yjx;

import com.yjx.commonsimaging.RegexpPropertyLoader;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Version 1.0
 * @Since JDK1.8
 * @Author junxiaoyang
 * @Company 洛阳艾鹿网络有限公司
 * @Date 2019-01-12 14:11
 */
@Slf4j
public class RegexpTest {

    @Test
    public void testRegex() throws IOException {
        Properties properties = RegexpPropertyLoader.init();
        for (String name : properties.stringPropertyNames()) {
            if (name.endsWith("regexp")) {
                String text = properties.getProperty(name + ".testtext");
                String property = properties.getProperty(name);
                Pattern compile = Pattern.compile(property);
                Matcher matcher = compile.matcher(text);
                boolean matches = matcher.matches();
                Assert.assertTrue("regex: ," + property + " text: " + text, matches);
            }
        }
    }
}
