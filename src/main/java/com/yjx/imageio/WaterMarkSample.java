package com.yjx.imageio;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @Version 1.0
 * @Since JDK1.8
 * @Author junxiaoyang
 * @Company 洛阳艾鹿网络有限公司
 * @Date 2019-01-09 14:04
 */
@Slf4j
public class WaterMarkSample {
    public static void main(String[] args) throws IOException {
        // overlay settings
        File input = new File("/Users/junxiaoyang/Pictures/IMG_5497.jpg");
        File output = new File("/Users/junxiaoyang/Pictures/IMG_5497_watermarked.jpg");

        ImageReader jpgImageReader = ImageIO.getImageReadersBySuffix("jpg").next();
        jpgImageReader.setInput(ImageIO.createImageInputStream(input));
        jpgImageReader.getStreamMetadata();
        BufferedImage bufferedImage = jpgImageReader.read(0);

        // adding text as overlay to an image
//        addTextWatermark(text, "jpg", input, output);
    }

//    private static void addTextWatermark(String text, String type, File source, File destination) throws IOException {
//        BufferedImage image = ImageIO.read(source);
//        // determine image type and handle correct transparency
//        int imageType = "png".equalsIgnoreCase(type) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
//        BufferedImage watermarked = new BufferedImage(image.getWidth(), image.getHeight(), imageType);
//        // initializes necessary graphic properties
//        Graphics2D w = (Graphics2D) watermarked.getGraphics();
//        w.drawImage(image, 0, 0, null);
//        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f);
//        w.setComposite(alphaChannel);
//        w.setColor(Color.RED);
//        w.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 100));
//        FontMetrics fontMetrics = w.getFontMetrics();
//        Rectangle2D rect = fontMetrics.getStringBounds(text, w);
//
//        // calculate center of the image
//        int centerX = (image.getWidth() - (int) rect.getWidth()) / 2;
//        int centerY = image.getHeight() / 2;
//
//        // add text overlay to the image
//        w.drawString(text, centerX, centerY);
//        ImageIO.write(watermarked, type, destination);
//        w.dispose();
//    }
}
