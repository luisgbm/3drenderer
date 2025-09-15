package br.com.simbasoft.renderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageTexture {
    private final int width;
    private final int height;
    private final int[] meshTexture;

    public ImageTexture(final String filePath) throws IOException {
        BufferedImage image = ImageIO.read(new File(filePath));
        width = image.getWidth();
        height = image.getHeight();
        meshTexture = new int[width * height];
        image.getRGB(0, 0, width, height, meshTexture, 0, width);
    }

    public int[] getMeshTexture() {
        return meshTexture;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}