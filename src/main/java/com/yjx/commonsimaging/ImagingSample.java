package com.yjx.commonsimaging;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.file.FileSystemDirectory;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * @Version 1.0
 * @Since JDK1.8
 * @Author junxiaoyang
 * @Company 洛阳艾鹿网络有限公司
 * @Date 2019-01-09 10:18
 */
@Slf4j
public class ImagingSample {
    public final static File SOURCE_PATH = new File("/Users/junxiaoyang/OneDrive/3寸-无白边-加日期/");
    public final static File TARGET_PATH = new File("/Users/junxiaoyang/Documents/testdata/imageio/adddate/");
    public static List<FileNameDateParser> DATEPARSERS = new ArrayList<>();
    static int hasDate = 0;
    static List<String> noDate = Lists.newArrayList();
    static String fileName;
    //    static FilenameFilter filenameFilter = (dir, name) -> name.startsWith("IMG_5187");
    static FilenameFilter filenameFilter = (dir, name) -> true;
    static Metadata metadata;

    public static void main(String[] args) throws IOException {
        DATEPARSERS = RegexpPropertyLoader.load();

        for (File sourceFile : SOURCE_PATH.listFiles(filenameFilter)) {
            fileName = sourceFile.getName();
            try {
                writetofile(sourceFile);
            } catch (Exception e) {
                log.error("file: {} message: {}", sourceFile.getName(), e.getMessage());
            }
        }
        log.info("has date files {}, no date files {}", hasDate, noDate);
        noDate.forEach(s -> log.info("no date image: {}", s));
    }

    private static final String NO_METADATA = "NoMetadata_";
    private static final String DATA_BLOCK = "Datablock_";
    private static final String NO_DATE = "NoDate_";

    private static void writetofile(File source) {
        try {
            File osFile = new File(TARGET_PATH.getPath() + "/" + source.getName());
            metadata = ImageMetadataReader.readMetadata(source);
            String formatDate = getDate(source);
            if (formatDate == null) {
                noDate.add(source.getName());
                Files.copy(source.toPath(), osFile.toPath());
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
        } catch (IOException | NullPointerException | ImageProcessingException e) {
            log.warn("no create date {}", source.getName());
        }
    }


    public static String getDate(File source) {
        String formatDate;
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
//            formatDate = getDateTimeFromDirectory(metadata.getFirstDirectoryOfType(FileSystemDirectory.class));
//            if (formatDate != null) {
//                return formatDate;
//            }
        log.warn("{} no datetime in file use drew imaging", source.getName());
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
        Date date = directory.getDateOriginal(TimeZone.getDefault());
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
        BufferedImage bufferedImageOutput = draw(text, imageSource);
        //get and config writer
        ImageWriter jpegWriter = getImageWriter(imageFormat);
        ImageWriteParam writeParam = jpegWriter.getDefaultWriteParam();
        writeParam.setProgressiveMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionQuality(1.0f);
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

    private static BufferedImage draw(String text, BufferedImage imageSource) {
        ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        int orientation = 0;
        try {
            orientation = exifIFD0Directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (MetadataException e) {
            log.debug("{} no orientation tag", fileName);
        }
        WidthAndHeight widthAndHeight = translateWidthAndHeight(orientation, imageSource.getWidth(), imageSource.getHeight());
        BufferedImage watermarked = new BufferedImage(widthAndHeight.getWidth(), widthAndHeight.getHeight(), BufferedImage.TYPE_INT_RGB);

        Graphics2D textGraphics = watermarked.createGraphics();

        //set composite
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
        textGraphics.setComposite(alphaChannel);

        AffineTransform oringinalTransform = textGraphics.getTransform();
        log.info("w: {}, h: {}", imageSource.getWidth(), imageSource.getHeight());
        //draw image
        configAffineTransform(widthAndHeight, textGraphics, orientation);
        textGraphics.drawImage(imageSource, 0, 0, null);

        //recover transform
        textGraphics.setTransform(oringinalTransform);

        //config font
        textGraphics.setColor(new Color(135, 206, 235));
        textGraphics.setColor(Color.WHITE);
        int size = calculateFontSize(widthAndHeight.getWidth());
        textGraphics.setFont(getFont("TypoSlab Irregular Shadowed Demo", size));

        Coordinate fontCoordinate = calculateCoordinate(textGraphics, text, widthAndHeight.getWidth(), widthAndHeight.getHeight());
        //draw text
        textGraphics.drawString(text, fontCoordinate.getX(), fontCoordinate.getY());
        return watermarked;
    }

    //see http://sylvana.net/jpegcrop/exif_orientation.html
    private static void configAffineTransform(WidthAndHeight widthAndHeight, Graphics2D textGraphics, int orientation) {
        AffineTransform affineTransform = new AffineTransform();
        switch (orientation) {
            case 0:
            case 1:
                return;
            case 2://flip horizon
                affineTransform.scale(-1, 1);
                affineTransform.translate(-widthAndHeight.getWidth(), 0);
                break;
            case 3://flip horizon and vertical
                affineTransform.scale(-1, -1);
                affineTransform.translate(-widthAndHeight.getWidth(), -widthAndHeight.getHeight());
                break;
            case 4://flip vertical
                affineTransform.scale(1, -1);
                affineTransform.translate(0, -widthAndHeight.getHeight());
                break;
//            case 5://flip horizon and rotate
//                affineTransform.scale(-1, 1);
//                affineTransform.rotate(-Math.toRadians(90));
//                affineTransform.translate(0, -widthAndHeight.getWidth());
//                break;
            case 6:
                affineTransform.rotate(Math.toRadians(90));
                affineTransform.translate(0, -widthAndHeight.getWidth());
                break;
            case 8://rotate -90
                affineTransform.rotate(-Math.toRadians(90));
                affineTransform.translate(-widthAndHeight.getHeight(), 0);
                break;
        }
        textGraphics.setTransform(affineTransform);
    }

    //只处理0,90,180,270
    private static WidthAndHeight translateWidthAndHeight(int orientation, int width, int height) {
        WidthAndHeight widthAndHeight = new WidthAndHeight();
        switch (orientation) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                widthAndHeight.setWidth(width);
                widthAndHeight.setHeight(height);
                break;
            case 5:
            case 6:
            case 7:
            case 8:
                widthAndHeight.setWidth(height);
                widthAndHeight.setHeight(width);
                break;
        }
        return widthAndHeight;
    }

    @Data
    static class WidthAndHeight {
        private int width;
        private int height;
    }

    @Data
    static class Coordinate {
        private int x;
        private int y;

    }

    private static Coordinate calculateCoordinate(Graphics2D graphics2D, String text, int imageWidth, int imageHeight) {
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        Rectangle2D rect = fontMetrics.getStringBounds(text, graphics2D);
        int width = (int) rect.getWidth();
        int height = (int) rect.getHeight();
        log.debug("rect h {} w {}", rect.getHeight(), rect.getWidth());
        int centerX = imageWidth - (width + width / 30);
        int centerY = imageHeight - height / 3;
        Coordinate coordinate = new Coordinate();
        coordinate.setX(centerX);
        coordinate.setY(centerY);
        return coordinate;
    }

    private static int calculateFontSize(int width) {
        int size = width / 22;
        log.info("{} font size is {}", fileName, size);
        return size;
    }

    private static Font getFont(String text, int size) {
        Font font = new Font(text, Font.BOLD, size);
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
