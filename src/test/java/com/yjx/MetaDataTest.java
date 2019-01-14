package com.yjx;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.yjx.commonsimaging.ImagingSample;
import com.yjx.commonsimaging.RegexpPropertyLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringTokenizer;

/**
 * @Version 1.0
 * @Since JDK1.8
 * @Author junxiaoyang
 * @Company 洛阳艾鹿网络有限公司
 * @Date 2019-01-14 13:59
 */
@Slf4j
public class MetaDataTest {
    final static File SOURCE_PATH = ImagingSample.SOURCE_PATH;
    FilenameFilter filenameFilter = (dir, name) -> true;
//    FilenameFilter filenameFilter = (dir, name) -> name.startsWith("IMG_20180925_");
//    FilenameFilter filenameFilter = (dir, name) -> name.startsWith("CIMG");

    @Test
    public void testGetdate() throws IOException {
        ImagingSample.DATEPARSERS = RegexpPropertyLoader.load();
        for (File file : SOURCE_PATH.listFiles(filenameFilter)) {
            String date = ImagingSample.getDate(file);
            if (date == null || LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).isAfter(LocalDateTime.of(2019, 01, 01, 00, 00))) {
                log.info("{} : {}", file.getName(), date);
            }
        }
    }

    @Test
    public void testImageIO() throws IOException {
        for (File file : SOURCE_PATH.listFiles(filenameFilter)) {
            BufferedImage read = ImageIO.read(file);
            String[] propertyNames = read.getPropertyNames();
            if (propertyNames == null) {
                continue;
            }
            for (String propertyName : propertyNames) {
                log.info("name: {}, value: {}", propertyName, read.getProperty(propertyName));
            }
        }
    }


    @Test
    public void testSanselan() throws IOException, ImageReadException {
        for (File file : SOURCE_PATH.listFiles(filenameFilter)) {
            IImageMetadata metadata = Sanselan.getMetadata(file);
            log.info(metadata.toString(file.getName()));
        }
    }

    @Test
    public void testImaging() throws IOException, ImageReadException, ImageProcessingException {
        for (File file : SOURCE_PATH.listFiles(filenameFilter)) {
            log.info("-------------------------- {} --------------------------", file.getName());
            Metadata metadata = ImageMetadataReader.readMetadata(file);
//            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
//
//            Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
//
//            if (date == null) {
//                date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED);
//            }
//            if (date == null) {
//                date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME);
//            }
//            if (date == null) {
//                continue;
//            }
//            log.info(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            metadata.getDirectories().forEach(directory -> {
                directory.getTags().forEach(tag -> {
                    log.info(tag.toString());
                });
            });
        }
    }

    @Test
    public void testBufferedReader() throws IOException {
        for (File file : SOURCE_PATH.listFiles(filenameFilter)) {
            FileInputStream fileInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String data = "";
            for (int i = 0; i < 6; i++) {
                data = bufferedReader.readLine();
            }
            log.info("extracted value: {}", data);
            StringTokenizer stringTokenizer = new StringTokenizer(data);
            if (stringTokenizer.hasMoreTokens()) {
                String token = stringTokenizer.nextToken();
                log.info("file: {}, token: {}", file.getName(), token);
            }
        }
    }

}
