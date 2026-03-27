package com.tumuyan.ncnn.realsr;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class GifUntils {

    public static int getGifFrameCount(String filePath) {
        return parseGif(filePath, false);
    }

    public static boolean isGifAnimation(String filePath) {
        return parseGif(filePath, true) > 1;
    }

    private static int parseGif(String filePath, boolean quickCheck) {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(filePath)))) {
            byte[] header = new byte[6];
            if (bis.read(header) != 6 || header[0] != 'G' || header[1] != 'I' || header[2] != 'F') {
                return -1;
            }

            byte[] lsd = new byte[7];
            if (bis.read(lsd) != 7) return -1;
            int flags = lsd[4] & 0xFF;
            if ((flags & 0x80) != 0) {
                int tableSize = 3 * (int) Math.pow(2, (flags & 0x07) + 1);
                bis.skip(tableSize);
            }

            int frameCount = 0;
            int block;
            while ((block = bis.read()) != -1) {
                if (block == 0x2C) {
                    frameCount++;
                    if (quickCheck && frameCount > 1) return frameCount;
                    byte[] id = new byte[9];
                    if (bis.read(id) != 9) return frameCount;
                    if ((id[8] & 0x80) != 0) {
                        bis.skip(3 * (int) Math.pow(2, (id[8] & 0x07) + 1));
                    }
                    bis.read(); // LZW Minimum Code Size
                    skipSubBlocks(bis);
                } else if (block == 0x21) { // Extension
                    bis.read(); // Label
                    skipSubBlocks(bis);
                } else if (block == 0x3B) { // Trailer
                    break;
                }
            }
            return frameCount;
        } catch (IOException e) {
            return -1;
        }
    }

    private static void skipSubBlocks(BufferedInputStream bis) throws IOException {
        int size;
        while ((size = bis.read()) > 0) {
            bis.skip(size);
        }
    }
}
