package com.yjx.commonsimaging;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.*;

/**
 * @Version 1.0
 * @Since JDK1.8
 * @Author junxiaoyang
 * @Company 洛阳艾鹿网络有限公司
 * @Date 2019-01-09 10:18
 */
@Slf4j
public class ImagingSample {
    private final static File SOURCE_PATH = new File("/Users/junxiaoyang/OneDrive/3寸-无白边-加日期/");
    private final static File TARGET_PATH = new File("/Users/junxiaoyang/Documents/testdata/imageio/adddate/");
    private static List<FileNameDateParser> DATEPARSERS = new ArrayList<>();
    static int hasDate = 0;
    static int noDate = 0;
    static String fileName;

    public static void main(String[] args) throws IOException {
        Properties properties = RegexpPropertyLoader.load();
        for (String propertyName : properties.stringPropertyNames()) {
            if (propertyName.endsWith("regexp")) {
                DATEPARSERS.add(new FileNameDateParser(properties.getProperty(propertyName)));
            }
        }
        ArrayList<File> sourceFiles = Lists.newArrayList();
        if (SOURCE_PATH.isDirectory()) {
            sourceFiles.addAll(Arrays.asList(Objects.requireNonNull(SOURCE_PATH.listFiles())));
        }

        for (File sourceFile : sourceFiles) {
//            revertName(sourceFile);
            try {
                fileName = sourceFile.getName();
//                printAllTags(sourceFile);
                writetofile(sourceFile);
            } catch (Exception e) {
                log.error("file: {} message: {}", sourceFile.getName(), e.getMessage());
            }
        }
        log.info("has date files {}, no date files {}", hasDate, noDate);
    }

    private static final String NO_METADATA = "NoMetadata_";
    private static final String DATA_BLOCK = "Datablock_";
    private static final String NO_DATE = "NoDate_";

    private static void writetofile(File source) {
        try {
            File osFile = new File(TARGET_PATH.getPath() + "/" + source.getName());
            String formatDate = getDate(source);
            if (formatDate == null) {
                noDate++;
                log.warn("{} no date info", source.getName());
                return;
            }
            hasDate++;
            String format = getFormat(source);
            if (format == null) {
                log.warn("{} unknow format", source.getName());
                return;
            }
            addTextWatermark(source, formatDate, osFile, format);
        } catch (IOException | NullPointerException e) {
            log.warn("no create date {}", source.getName());
        }
    }


    private static String getDate(File source) {
        String formatDate;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(source);
            //1.metadata
            formatDate = getDateTimeFromDirectory(metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class));
            if (formatDate != null) {
                return formatDate;
            }
            log.debug("{} no datetime in exif directory use drew imaging", source.getName());
            //2.file name
            formatDate = getDateTimeFromFileName(source);
            if (formatDate != null) {
                return formatDate;
            }
            log.debug("{} no datetime in filename", source.getName());
            //3.modifytime
            formatDate = getDateTimeFromDirectory(metadata.getFirstDirectoryOfType(FileSystemDirectory.class));
            if (formatDate != null) {
                return formatDate;
            }
            log.warn("{} no datetime in file use drew imaging", source.getName());
            return null;
        } catch (ImageProcessingException | IOException e) {
            log.error("{} get metadata error", source.getName());
        }
        return null;
    }

    private static String getDateTimeFromFileName(File source) {
        for (FileNameDateParser dateparser : DATEPARSERS) {
            boolean match = dateparser.isMatch(source.getName());
            if (!match) {
                continue;
            }
            return dateparser.getDate();
        }
        return null;
    }

    private static String formatDateTime(Date date) {
        String dateFormatted;
        LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        dateFormatted = localDateTime.format(FileNameDateParser.FORMATTER);
        return dateFormatted;
    }

    private static String getDateTimeFromDirectory(ExifSubIFDDirectory directory) {
        if (directory == null) {
            return null;
        }
        String dateTime = null;
        Date date = directory.getDateOriginal();
        if (date != null) {
            dateTime = formatDateTime(date);
        }
        return dateTime;
    }

    private static String getDateTimeFromDirectory(FileSystemDirectory directory) {
        String dateTime = null;
        if (directory == null) {
            return null;
        }
        Date date = directory.getDate(FileSystemDirectory.TAG_FILE_MODIFIED_DATE);
        if (date != null) {
            dateTime = formatDateTime(date);
        }
        return dateTime;
    }

    private static void printAllTags(File file) {
        try {
            IImageMetadata metadata = Sanselan.getMetadata(file);
            if (metadata == null) {
                log.warn("{} not fount metadata use imaging ", file.getName());
                return;
            }
            for (Object item : metadata.getItems()) {
                log.info(item.toString());
            }
        } catch (ImageReadException | IOException e) {
            log.warn("{} not fount metadata use imaging ", file.getName());
        }
    }

    private static void printAllTags(Iterable<Directory> directories) {
        for (Directory directory : directories) {
            for (Tag tag : directory.getTags()) {
                log.info("{}, {}, {}", tag.getDirectoryName(), tag.getTagName(), tag.toString());
            }
        }
    }

    private static String getFormat(File source) {
        if (StringUtils.endsWithIgnoreCase(source.getName(), "JPG") || StringUtils.endsWithIgnoreCase(source.getName(), "JPEG")) {
            return "JPEG";
        }
        if (StringUtils.endsWithIgnoreCase(source.getName(), "PNG")) {
            return "PNG";
        }
        return null;
    }

    private static void revertName(File file) {
        String newName = null;
        if (StringUtils.startsWith(file.getName(), NO_DATE)) {
            newName = StringUtils.substring(file.getName(), NO_DATE.length());
        }
        if (StringUtils.startsWith(file.getName(), NO_METADATA)) {
            newName = StringUtils.substring(file.getName(), NO_METADATA.length());
        }
        if (StringUtils.startsWith(file.getName(), DATA_BLOCK)) {
            newName = StringUtils.substring(file.getName(), DATA_BLOCK.length());
        }
        if (newName == null) {
            return;
        }
        rename(file, newName);
    }

    private static void rename(File file, String newName) {
        log.info("rename {} to {}", file.getName(), newName);
        boolean b = file.renameTo(new File(SOURCE_PATH.getPath() + "/" + newName));
        if (!b) {
            log.error("{} rename fail", file.getName());
        }
    }

    private static void moveFile(String name) throws IOException {
        log.info("move file {}", name);
        Files.move(Paths.get(SOURCE_PATH + name), Paths.get(TARGET_PATH + name));
    }

    private static void addTextWatermark(File source, String text, File destination, String imageFormat) throws IOException {
        //Get Reader
        ImageReader jpgReader = getReader(source, imageFormat);
        BufferedImage imageSource = jpgReader.read(0);
//        draw image
        BufferedImage bufferedImageOutput = drawText(text, imageSource);
        //get and config writer
        ImageWriter jpegWriter = getImageWriter(imageFormat);
        ImageWriteParam writeParam = jpegWriter.getDefaultWriteParam();
        writeParam.setProgressiveMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
        writeParam.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
        //set output
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(destination)) {
            jpegWriter.setOutput(imageOutputStream);
            //write image to output file
            jpegWriter.write(jpgReader.getStreamMetadata(), new IIOImage(bufferedImageOutput, null, null), writeParam);
            jpegWriter.dispose();
        }
    }

    private static ImageWriter getImageWriter(String imageFormat) {
        return ImageIO.getImageWritersByFormatName(imageFormat).next();
    }

    private static ImageReader getReader(File file, String imageFormat) throws IOException {
        ImageReader jpgReader = ImageIO.getImageReadersByFormatName(imageFormat).next();
        jpgReader.setInput(ImageIO.createImageInputStream(file));
        return jpgReader;
    }

    private static BufferedImage drawText(String text, BufferedImage imageSource) {
        ColorSpace sRGBColorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel colorModel = new ComponentColorModel(sRGBColorSpace, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        BufferedImage watermarked = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(imageSource.getWidth(), imageSource.getHeight()), colorModel.isAlphaPremultiplied(), null);

        Graphics2D textGraphics = (Graphics2D) watermarked.getGraphics();
        textGraphics.drawImage(imageSource, 0, 0, null);
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
        textGraphics.setComposite(alphaChannel);
        //微软雅黑 粗体 26号字  投影4像素，或是描边4像素
        textGraphics.setColor(Color.WHITE);
        int size = calculateFontSize(imageSource);
        textGraphics.setFont(getFont("TypoSlab Irregular Shadowed Demo", size));

        FontMetrics fontMetrics = textGraphics.getFontMetrics();
        Rectangle2D rect = fontMetrics.getStringBounds(text, textGraphics);
        log.debug("rect h {} w {}", rect.getHeight(), rect.getWidth());
        // calculate center of the imageSource
        int centerX = imageSource.getWidth() - ((int) rect.getWidth() + 70);
        int centerY = imageSource.getHeight() - ((int) rect.getHeight());

//        setGrayRGB(imageSource, watermarked);
        // add text overlay to the imageSource
        textGraphics.drawString(text, centerX, centerY);
        return watermarked;
    }

    private static int calculateFontSize(BufferedImage imageSource) {
        int size = imageSource.getWidth() / 25;
        log.debug("{} font size is {}", fileName, size);
        return size;
    }

    private static Font getFont(String text, int size) {
        Font font = new Font(text, Font.PLAIN, size);
        return font;
    }

    private static void setGrayRGB(BufferedImage imageSource, BufferedImage watermarked) {
        for (int y = 0; y < imageSource.getHeight(); y++) {
            for (int x = 0; x < imageSource.getWidth(); x++) {
                int p = imageSource.getRGB(x, y);

//                int a = (p >> 24) & 0xff;
//                int r = (p >> 16) & 0xff;
//                int g = (p >> 8) & 0xff;
//                int b = p & 0xff;
//
//                calculate average
//                int avg = (r + g + b) / 3;
//
//                replace RGB value with avg
//                p = (a << 24) | (avg << 16) | (avg << 8) | avg;
                watermarked.setRGB(x, y, p);
            }
        }
    }
}
