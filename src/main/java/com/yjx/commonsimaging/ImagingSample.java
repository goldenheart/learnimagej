package com.yjx.commonsimaging;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
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
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * @Version 1.0
 * @Since JDK1.8
 * @Author junxiaoyang
 * @Company 洛阳艾鹿网络有限公司
 * @Date 2019-01-09 10:18
 */
@Slf4j
public class ImagingSample {
    private static File picFolder = new File("/Users/junxiaoyang/Documents/testdata/imageio/");

    public static void main(String[] args) {
        log.info(picFolder.getPath());
        ArrayList<File> isFiles = Lists.newArrayList();
        if (picFolder.isDirectory()) {
            isFiles.addAll(Arrays.asList(Objects.requireNonNull(picFolder.listFiles())));
        }
        for (File isFile : isFiles) {
            log.info(isFile.getName());
            try {
                writetofile(isFile);
            } catch (ImageReadException | IOException e) {
                e.printStackTrace();
            }
        }

    }

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static void writetofile(File file) throws ImageReadException, IOException {
        IImageMetadata imageMetadata = Sanselan.getMetadata(file);
        if (imageMetadata instanceof JpegImageMetadata) {
            JpegImageMetadata jpegImageMetadata = (JpegImageMetadata) imageMetadata;
            TiffField createDateField = jpegImageMetadata.findEXIFValue(TiffConstants.EXIF_TAG_CREATE_DATE);
            log.info("name: {} value: {}", createDateField.getTagName(), createDateField.getValue());
            File osFile = new File(picFolder + "/datewatermark_" + file.getName());
            String date = createDateField.getValue().toString();
            boolean endsWith = date.endsWith("\u0000");
            if (endsWith) {
                String subdate = date.substring(0, date.length() - 1);
                LocalDateTime localDateTime = LocalDateTime.parse(subdate, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"));
                String format = localDateTime.format(formatter);
                System.out.println(format);
                addTextWatermark(file, format, osFile);
            }

        }
    }

    private static void addTextWatermark(File source, String text, File destination) throws IOException {
        //Get Reader
        ImageReader jpgReader = getReader(source);
        BufferedImage imageSource = jpgReader.read(0);
        //draw image
        BufferedImage bufferedImageOutput = drawText(text, imageSource);
        //get and config writer
        ImageWriter jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
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

    private static ImageReader getReader(File file) throws IOException {
        ImageReader jpgReader = ImageIO.getImageReadersByFormatName("jpg").next();
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
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
        textGraphics.setComposite(alphaChannel);
        //微软雅黑 粗体 26号字  投影4像素，或是描边4像素
        textGraphics.setColor(Color.BLACK);
        textGraphics.setFont(new Font("TypoSlab Irregular Shadowed Demo", Font.BOLD, 100));
//        textGraphics.setFont(new Font("Here & Not Found", Font.BOLD, 100));
        FontMetrics fontMetrics = textGraphics.getFontMetrics();
        Rectangle2D rect = fontMetrics.getStringBounds(text, textGraphics);

        // calculate center of the imageSource
        int centerX = imageSource.getWidth() - ((int) rect.getWidth() + 100);
        int centerY = imageSource.getHeight() - ((int) rect.getHeight() + 50);

//        setGrayRGB(imageSource, watermarked);
        // add text overlay to the imageSource
        textGraphics.drawString(text, centerX, centerY);
        return watermarked;
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
