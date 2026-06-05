import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

public class BinaryVisualizer {
    public byte[] data;
    public BufferedImage bi;
    public BufferedImage biBackup;

    public BinaryVisualizer(byte[] data, int height) {
        this.data = data;
        this.bi = new BufferedImage(64+4, 48*16, BufferedImage.TYPE_INT_RGB);
    }

    public void Backup() {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        biBackup = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    public void Restore() {
        ColorModel cm = biBackup.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = biBackup.copyData(null);
        bi = new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    public void DrawFrame(int startAddress, int endAddress) {
        int bytesPerLine = data.length / 1024;
        int startLine = 767 - startAddress / bytesPerLine;
        int endLine = 767 - Math.min(768, endAddress / bytesPerLine);
        Color color = new Color(238, 238, 238);
        int colorValue = color.getRGB();
        for (int i=0; i<768; i++) {
            this.bi.setRGB(0, i, colorValue);
            this.bi.setRGB(1, i, colorValue);
            this.bi.setRGB(2, i, colorValue);
            this.bi.setRGB(3, i, colorValue);
        }
        color = Color.RED;
        colorValue = color.getRGB();
        for (int i=startLine; i<endLine; i++) {
            this.bi.setRGB(0, i, colorValue);
            this.bi.setRGB(1, i, colorValue);
            this.bi.setRGB(2, i, colorValue);
            this.bi.setRGB(3, i, colorValue);
        }
    }
}
