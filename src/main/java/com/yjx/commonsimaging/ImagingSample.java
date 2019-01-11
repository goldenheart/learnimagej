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
import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;

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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

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

    public static void main(String[] args) {
        ArrayList<File> sourceFiles = Lists.newArrayList();
        if (SOURCE_PATH.isDirectory()) {
            sourceFiles.addAll(Arrays.asList(Objects.requireNonNull(SOURCE_PATH.listFiles())));
        }
        for (File sourceFile : sourceFiles) {
            if (!sourceFile.getName().equals("IMG_9359.JPG")) {
                continue;
            }
//            revertName(sourceFile);
//            log.info(sourceFile.getName());
            String dateFormatted = getDate(sourceFile);
//            if (isFile.getName().startsWith(NO_METADATA) || isFile.getName().startsWith(DATA_BLOCK) || isFile.getName().startsWith(NO_DATE)) {
//                continue;
//            }
//            if (isFile.getName().startsWith("IMG")) {
//                try {
//                    writetofile(isFile);
//                } catch (Exception e) {
//                    log.error("file: {} message: {}", isFile.getName(), e.getMessage());
//                }
//            }
        }
        log.info("no metadata files {}, block files {}", noMetaDataIndex.get(), blockFilesIndex.get());
    }

    static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static AtomicInteger noMetaDataIndex = new AtomicInteger(0);
    static AtomicInteger blockFilesIndex = new AtomicInteger(0);
    private static final String NO_METADATA = "NoMetadata_";
    private static final String DATA_BLOCK = "Datablock_";
    private static final String NO_DATE = "NoDate_";

    private static void writetofile(File source) {
        ImageInfo imageInfo;
        IImageMetadata imageMetadata;
        try {
            imageInfo = Sanselan.getImageInfo(source);
            imageMetadata = Sanselan.getMetadata(source);
        } catch (ImageReadException | IOException e) {
            blockFilesIndex.incrementAndGet();
            rename(source, DATA_BLOCK + source.getName());
            return;
        }
        if (imageInfo != null) {
            if (!ImageFormat.IMAGE_FORMAT_JPEG.name.equals(imageInfo.getFormat().name)) {
                log.info("{} image type is {}", source.getName(), imageInfo.getFormatName());
            }
        }
        if (imageMetadata == null) {
            log.warn("{} no metadata", source.getName());
            noMetaDataIndex.incrementAndGet();
            rename(source, NO_METADATA + source.getName());
            return;
            //            moveFile(file.getName());
        }
        try {
            if (imageMetadata instanceof JpegImageMetadata) {
                JpegImageMetadata jpegImageMetadata = (JpegImageMetadata) imageMetadata;
                TiffField createDateField = jpegImageMetadata.findEXIFValue(TiffConstants.EXIF_TAG_CREATE_DATE);
                File osFile = new File(TARGET_PATH.getPath() + "/" + source.getName());
                String date = createDateField.getValue().toString();
                boolean endsWith = date.endsWith("\u0000");
                if (endsWith) {
                    date = date.substring(0, date.length() - 1);
                    LocalDateTime localDateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"));
                    date = localDateTime.format(FORMATTER);
                    log.info("name: {} date: {} after: {}", source.getName(), createDateField.getValue(), date);
                }
                addTextWatermark(source, date, osFile, imageInfo.getFormat().name);
            }
        } catch (ImageReadException | IOException | NullPointerException e) {
            log.warn("no create date {}", source.getName());
            rename(source, NO_DATE + source.getName());
        }
    }


    private static String getDate(File source) {
        String dateFormatted;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(source);
            //1.metadata
            dateFormatted = getDateTimeFromDirectory(metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class));
            if (dateFormatted != null) {
                return dateFormatted;
            }
            log.warn("{} no datetime in exif directory use drew imaging", source.getName());
            //2.file name
            dateFormatted = getDateTimeFromFileName(source);
            //3.modifytime
            dateFormatted = getDateTimeFromDirectory(metadata.getFirstDirectoryOfType(FileSystemDirectory.class));
            if (dateFormatted != null) {
                return dateFormatted;
            }
            log.warn("{} no datetime in file use drew imaging", source.getName());
            //            printAllTags(source);
            log.info("{} create time is {} ", source.getName(), dateFormatted);
            return null;
        } catch (ImageProcessingException | IOException e) {
            log.error("{} get metadata error", source.getName());
        }
        return null;
    }

    private static String getDateTimeFromFileName(File source) {
        //TODO
        return null;
    }

    private static String formatDateTime(Date date) {
        String dateFormatted;
        LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        dateFormatted = localDateTime.format(FORMATTER);
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
        if (StringUtils.endsWithIgnoreCase(source.getName(), "JPG")) {
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
        //draw image
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
//        int imageType = imageSource.getType();
//        BufferedImage watermarked = new BufferedImage(imageSource.getWidth(), imageSource.getHeight(), imageType);

        ColorSpace sRGBColorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ColorModel colorModel = new ComponentColorModel(sRGBColorSpace, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        BufferedImage watermarked = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(imageSource.getWidth(), imageSource.getHeight()), colorModel.isAlphaPremultiplied(), null);

        Graphics2D textGraphics = (Graphics2D) watermarked.getGraphics();
        textGraphics.drawImage(imageSource, 0, 0, null);
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
        textGraphics.setComposite(alphaChannel);
        //微软雅黑 粗体 26号字  投影4像素，或是描边4像素
        textGraphics.setColor(Color.BLACK);
        textGraphics.setFont(getFont("TypoSlab Irregular Shadowed Demo"));
//        textGraphics.setFont(new Font("Here & Not Found", Font.BOLD, 100));
        FontMetrics fontMetrics = textGraphics.getFontMetrics();
        Rectangle2D rect = fontMetrics.getStringBounds(text, textGraphics);

        // calculate center of the imageSource
        int centerX = imageSource.getWidth() - ((int) rect.getWidth() + 20);
        int centerY = imageSource.getHeight() - ((int) rect.getHeight() + 5);

//        setGrayRGB(imageSource, watermarked);
        // add text overlay to the imageSource
        textGraphics.drawString(text, centerX, centerY);
        return watermarked;
    }

    private static Font getFont(String text) {
        Font font = new Font(text, Font.BOLD, 100);
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

//    private static ImageWriter getImageWriter() {
//        Iterator<ImageWriter> jpegWriterIt = ImageIO.getImageWritersByFormatName("jpeg");
//        ImageWriter jpegWriter = jpegWriterIt.next();
//        ImageWriteParam writeParam = jpegWriter.getDefaultWriteParam();
//        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//        writeParam.setCompressionQuality(1.0F);
//        return jpegWriter;
//    }
}
