import java.awt.Color;

public class Hilbert {
    int x;
    int y;
    int d;
    BinaryVisualizer binviz;
    int step;
    int pixelSize;
    int tmp_x;
    int tmp_y;

    public Hilbert(BinaryVisualizer binviz, int step, int pixelSize) {
        this.x = 0;
        this.y = 0;
        this.d = 0;
        this.binviz = binviz;
        this.step = step;
        this.pixelSize = pixelSize;
    }
    
    public void draw() {
        Color color = getPixelColor(binviz.data[d]);
        int colorValue = color.getRGB();
        if (y < 768) {
        binviz.bi.setRGB(x+4, 767-y, colorValue);
        }
        d += step;
    }
    
    public void up() {
        y += 1;
        draw();
    }
    
    public void down() {
        y -= 1;
        draw();
    }

    public void right() {
        x += 1;
        draw();
    }
    
    public void left() {
        x -= 1;
        draw();
    }
    
    public void curveA(int level) {
        if (level > 1) {
            curveD(level-1);
            y += 1;
            curveA(level-1);
            x += 1;
            curveA(level-1);
            y -= 1;
            curveB(level-1);
            return;
        }

        draw();
        up();
        right();
        down();
    }
    
    public void curveB(int level) {
        if (level > 1) {
            curveC(level-1);
            x -= 1;
            curveB(level-1);
            y -= 1;
            curveB(level-1);
            x += 1;
            curveA(level-1);
            return;
        }

        draw();
        left();
        down();
        right();
    }
    
    public void curveC(int level) {
        if (level > 1) {
            curveB(level-1);
            y -= 1;
            curveC(level-1);
            x -= 1;
            curveC(level-1);
            y += 1;
            curveD(level-1);
            return;
        }

        draw();
        down();
        left();
        up();
    }
    
    public void curveD(int level) {
        if (level > 1) {
            curveA(level-1);
            x += 1;
            curveD(level-1);
            y += 1;
            curveD(level-1);
            x -= 1;
            curveC(level-1);
            return;
        }

        draw();
        right();
        up();
        left();
    }
    
    public int xy2d (int n, int x, int y) {
        int rx = 0, ry = 0, s = 0, d=0;
//        var x = x
//        var y = y
        s = n/2;
        while (s > 0) {
            rx = (x & s) > 0 ? 1 : 0;
            ry = (y & s) > 0 ? 1 : 0;
            d += s * s * ((3 * rx) ^ ry);
            rot(n, x, y, rx, ry);
            x = tmp_x;
            y = tmp_y;
            s /= 2;
        }
        return d;
    }

    public void rot(int n, int x, int y, int rx, int ry) {
        if (ry == 0) {
            if (rx == 1) {
                x = n-1 - x;
                y = n-1 - y;
            }

            //Swap x and y
            int t  = x;
            x = y;
            y = t;
        }
        tmp_x = x;
        tmp_y = y;
    }
    
    Color getPixelColor(int value) {
        if (value < 0) value += 256;
        if (value == 0) return Color.BLACK;
        if (value == 9 || value == 10 || value == 13 || (value >= 32 && value <= 126)) return Color.BLUE;
        if (value >= 1 && value <= 31) return Color.GREEN;
        if (value >= 127 && value <= 254) return Color.RED;
        if (value == 255) return Color.WHITE;
        return Color.BLACK;
    }
}
