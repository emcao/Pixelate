package com.emcao.pixelate;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.Arrays;

public class PixelateImage {
    private Bitmap originalBitmap;
    private Bitmap scaledDownBitmap;
    private int bitmapWidth;
    private int bitmapHeight;
    private int scaledWidth;
    private int scaledHeight;
    private int maxPixelSize;

    public PixelateImage(Bitmap bitmap) {
        originalBitmap = bitmap;
        bitmapWidth = originalBitmap.getWidth();
        bitmapHeight = originalBitmap.getHeight();
        float scaleFactor = getScaleFactor();
        scaledDownBitmap = scaleDown(originalBitmap, scaleFactor);
        scaledWidth = scaledDownBitmap.getWidth();
        scaledHeight = scaledDownBitmap.getHeight();
        maxPixelSize = Math.min(scaledWidth, scaledHeight) / 5;
    }

    public Bitmap getImageView(int pixelSize) {
        if (pixelSize == 0) {
            return originalBitmap;
        }

        Bitmap pixelatedBitmap = pixelate(pixelSize);
        return scaleUp(pixelatedBitmap);
    }

    public int getMaxPixelSize() {
        return maxPixelSize;
    }

    private Bitmap pixelate(int pixelSize) {
        Bitmap pixelatedBitmap = scaledDownBitmap.copy(scaledDownBitmap.getConfig(), true);

        int[] pixels = new int[scaledWidth * scaledHeight];
        pixelatedBitmap.getPixels(pixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight);

        int[] pixelArea = new int[pixelSize * pixelSize];

        for (int y = 0; y < scaledHeight; y += pixelSize) {
            for (int x = 0; x < scaledWidth; x += pixelSize) {
                int pixel;
                int r = 0;
                int g = 0;
                int b = 0;
                int count = 0;

                int maxX = Math.min(x + pixelSize, scaledWidth);
                int maxY = Math.min(y + pixelSize, scaledHeight);

                for (int j = y; j < maxY; j++) {
                    for (int i = x; i < maxX; i++) {
                        pixel = pixels[j * scaledWidth + i];
                        r += Color.red(pixel);
                        g += Color.green(pixel);
                        b += Color.blue(pixel);
                        count++;
                    }
                }

                pixel = Color.rgb(r / count, g / count, b / count);
                Arrays.fill(pixelArea, pixel);

                int width = Math.min(pixelSize, scaledWidth - x);
                int height = Math.min(pixelSize, scaledHeight - y);

                pixelatedBitmap.setPixels(pixelArea, 0, width, x, y, width, height);
            }
        }

        return pixelatedBitmap;
    }

    private float getScaleFactor() {
        if (bitmapWidth >= 2000 || bitmapHeight >= 2000) {
            return 0.1f;
        }
        else if (bitmapWidth >= 1000 || bitmapHeight >= 1000) {
            return 0.5f;
        }

        return 1;
    }

    private Bitmap scaleDown(Bitmap bitmap, float scaleFactor) {
        int downScaledWidth = (int) (scaleFactor * bitmapWidth);
        int downScaledHeight = (int) (scaleFactor * bitmapHeight);

        return Bitmap.createScaledBitmap(bitmap, downScaledWidth, downScaledHeight, false);
    }

    private Bitmap scaleUp(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, false);
    }
}