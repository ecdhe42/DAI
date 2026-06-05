import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.image.BufferedImage;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.File;
import java.io.FileReader;
import java.awt.event.*;

public class MemView extends JPanel implements KeyListener
{
    class Game {
        int[] palette;
        int screenLines;
    }

    class Position {
        String name;
        int offset;
        int width;

        @Override
        public String toString() {
            return name;
        }

        public Position(String name, int offset, int width) {
            this.name = name;
            this.offset = offset;
            this.width = width;
        }
    }

    int[][] screen;
    int[] screenLineAddr;

    byte[] data;
    Color[] paletteDAI = new Color[16];
    Color[] palette = new Color[16];
    Color[] gameDefaultPalette = new Color[4];
    int gameScreenLines;
    int offset = 0xBFFF;
    JFrame frame;
    HelpDialog helpDialog;
    JLabel displayLabel;
    JLabel filenameLabel;
    JLabel vizLabel;
    JLabel bitmapLabel;
    JLabel addressLabel;
    JLabel widthLabel;
    JPanel bookmarksContainer;
    int width;
    int height;
    int screenMode;
    String filename;
    File file;
    String path;
    Position[] positions;
    JPanel controlPanel;
    BinaryVisualizer binViz;
    ExportDialog dialog;
    int offsetUp;
    int offsetDown;
    int offsetLineUp;
    int offsetLineDown;
    int offsetScreenBottom;
    int offsetBottom;

    int getIntensity(int st) {
        return st * (255/7);
    }

    private void LoadScreen(int nbLines) {
        int palette[] = { 0, 1, 2, 15 };
        screen = new int[352][nbLines];
        screenLineAddr = new int[nbLines+1];

        int finalColor;
        int memOffset = 0xBFFF;
        int lineMemOffset = 0;
        int skip = 0;
        int textSkip = 0;
        int ctrl = 0;
        int mode_ctrl = 0;
        int col_ctrl = 0;
        int startOfLineOffset = 0;

        for (int y = 0; y < nbLines; y++)
        {
            if (textSkip > 0) {
                screenLineAddr[y] = lineMemOffset;
                textSkip--;
                continue;
            }

            lineMemOffset = memOffset;
            screenLineAddr[y] = memOffset;

            if (skip == 0) {
                ctrl = data[memOffset] & 0xff;
                skip = ctrl & 0x0F;
                // res_ctrl = (ctrl & 0x30) >> 4;
                mode_ctrl = (ctrl & 0xC0) >> 6;
                col_ctrl = data[memOffset-1] & 0xff;
                memOffset -= 2;
                startOfLineOffset = memOffset;

            if (((col_ctrl & 0x80) != 0) && (y < 300)) {
                int color = col_ctrl & 0x0F;
                int color_idx = (col_ctrl & 0x30) >> 4;
                palette[color_idx] = color;
            }

            } else {
                skip--;
            }

            if (((col_ctrl & 0x40) == 0) && (y < 300)) {
                memOffset -= 2;
                if (skip > 0) {
                memOffset = startOfLineOffset;
            }
                continue;
            }

            switch (mode_ctrl) {
            // 4-color mode
            case 0:
                for (int x = 0; x < 352; x+=8) {
                    int value2 = data[memOffset] & 0xff;
                    int value1 = data[memOffset-1] & 0xff;
                    memOffset -= 2;
                    int mask = 128;

                    for (int pixel = 0; pixel < 8; pixel++) {
                        int value = 0;
                        if ((value1 & mask) != 0) {
                            value += 1;
                        }
                        if ((value2 & mask) != 0) {
                            value += 2;
                        }
                        screen[x+pixel][y] = palette[value];
                        mask /= 2;
                    }
                }
                break;
            // 16-color mode
            case 2:
                for (int x = 0; x < 352; x+=8) {
                    int value2 = data[memOffset] & 0xff;
                    int value1 = data[memOffset-1] & 0xff;
                    memOffset -= 2;
                    int color1 = value1 >> 4;
                    int color2 = value1 & 0x0F;
                    int mask = 128;

                    for (int pixel = 0; pixel < 8; pixel++) {
                        if ((value2 & mask) != 0) {
                            finalColor = color1;
                        } else {
                            finalColor = color2;
                        }

                        screen[x+pixel][y] = finalColor;
                        mask = mask >> 1;
                    }
                }
                break;
            case 1:
            case 3:
                for (int x=0; x<352; x+= 8) {
                    int ch = data[memOffset] & 0xFF;
                    int ch_ctrl = data[memOffset-1] & 0xFF;
                    memOffset -= 2;
                    int ascii_offset = ch * 11;
                    finalColor = ch_ctrl != 0 ? 2 : 1;
                    int color1 = ch_ctrl >> 4;
                    int color2 = ch_ctrl & 0x0F;
                    for (int text_y=0; text_y<11; text_y++) {
                        if (ascii_offset >= ascii.length) {
                            System.out.printf("Unknown char %02X [%c]\n", ch, ch);
                        }
                        int value = ascii[ascii_offset];
                        int mask = 0x80;
                        for (int text_x=0; text_x<8; text_x++) {
                            if ((value & mask) == 0) {
                                screen[x+text_x][y+text_y] = finalColor;
                            }
                            mask = mask >> 1;
                        }
                        ascii_offset++;
                    }
                }
                textSkip = 10;
                break;
            default:
                System.out.printf("Control mode %d\n", mode_ctrl);
            }

            if (skip > 0) {
                memOffset = startOfLineOffset;
            }
        }

        screenLineAddr[nbLines] = memOffset;
        offsetScreenBottom = memOffset;
        System.out.printf("Screen size: %d bytes (0x%04X)\n", 0xBFFF - memOffset, 0xBFFF - memOffset);
    }

    Color getColor(int rST, int gST, int bST) {
        return new Color(getIntensity(rST), getIntensity(gST), getIntensity(bST));
    }

    public boolean ReadJson() throws Exception {
        Matcher matcher = ParseFilename();
        if (matcher == null) return false;
        String filename = "";

        try {
            filename = matcher.group(1).toLowerCase() + ".json";

            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new FileReader(filename));
            JSONObject root = (JSONObject)obj;
            JSONArray palette = (JSONArray)root.get("palette");
            for (int i=0; i<palette.size(); i++) {
                int color = (int)(long)palette.get(i);
                gameDefaultPalette[i] = paletteDAI[color];
            }
            int screenLines = (int)(long)root.get("screenLines");
            gameScreenLines = screenLines;
            JSONArray positionsJson = (JSONArray)root.get("positions");
            positions = new Position[positionsJson.size()];
            System.out.printf("JSON found: %d screen lines, %d positions\n", screenLines, positionsJson.size());

            for (int i=0; i<positionsJson.size(); i++) {
                JSONObject position = (JSONObject)positionsJson.get(i);
                String name = (String)position.get("name");
                int offset = Integer.decode((String)position.get("offset"));
                int width = (int)(long)position.get("width");
                positions[i] = new Position(name, offset, width);
            }
            if (positions.length > 0) {
                offset = positions[0].offset;
                width = positions[0].width;
            }
            return true;
        } catch (Exception e) {
            System.out.printf("Error reading %s: %s\n", filename, e.getMessage());
            return false;
        }
    }

    private void LoadData(File file) throws Exception {
        this.width = 352;
        this.height = 900;
        this.screenMode = 0;
        this.offset = 0xBFFF;
        this.offsetScreenBottom = 0;
        this.filename = file.getName();
        this.path = file.getPath();
        this.dialog = new ExportDialog(this);
        data = Files.readAllBytes(file.toPath());
//        int nbBytesToCopy = Math.min(48*1024, rawData.length);
//        data = new byte[nbBytesToCopy];
//        System.arraycopy(rawData, 0, data, 0, nbBytesToCopy);
        positions = new Position[] {};

        int[] paletteDAIint = new int[] { 0x000000, 0x000080, 0xdb3a38, 0xFF0000, 0x424200, 0x2ec625, 0xdb3a38, 0xc02eeb,
                                          0xc2c1c7, 0x4b3cf5, 0xffa503, 0xff99fe, 0x8080ff, 0x39fc82, 0xffff28, 0xffffff };
        for (int i=0; i<16; i++) {
            int rST = paletteDAIint[i] >> 16;
            int gST = (paletteDAIint[i] >> 8) & 0xFF;
            int bST = paletteDAIint[i] & 0xFF;
            palette[i] = new Color(rST, gST, bST);
            paletteDAI[i] = palette[i];
        }

        if (!ReadJson()) {
            gameDefaultPalette[0] = palette[0];
            gameDefaultPalette[1] = palette[3];
            gameDefaultPalette[2] = palette[10];
            gameDefaultPalette[3] = palette[15];
            gameScreenLines = 260;
        }

        updateBookmarks();
        LoadScreen(gameScreenLines);

        binViz = new BinaryVisualizer(data, height);
        // Round down to the highest power of 2
        int nbBits = 31 - Integer.numberOfLeadingZeros(data.length);
        int roundedDown = (1 << nbBits);
        int step = roundedDown / 64 / 64 / 16; // There are 16 64x64 tiles. step = how many bytes per pixel?
        Hilbert hilbert = new Hilbert(binViz, step, 1);
        for (int i=0; i<16; i++) {
            hilbert.curveD(6);
            hilbert.y += 1;
//            System.out.printf("Offset: %d\n", hilbert.d);
        }
//        binViz.Backup();
        ImageIcon bitmap = new ImageIcon( binViz.bi );
        vizLabel.setIcon(bitmap);
    }

    private void updateBookmarks() {
        bookmarksContainer.removeAll();
        bookmarksContainer.setLayout(new GridLayout(positions.length, 1));
        for (Position pos : positions) {
            JButton button = new JButton(pos.name);
            button.addKeyListener(this);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    width = pos.width;
                    offset = pos.offset;
                    try { Refresh();
                        frame.pack();
                        frame.setVisible( true );
                    } catch (Exception ex) {
                        System.out.println("Error in Update(): " + ex.getMessage());
                        for (StackTraceElement ste : ex.getStackTrace()) {
                            System.out.println(ste);
                        }
                    }
                }
            });
            bookmarksContainer.add(button);
        }

    }

    public Matcher ParseFilename() {
        Pattern pattern = Pattern.compile("([a-z_]+)([0-9]*)\\.(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(filename);
        boolean matchFound = matcher.find();
        if (matchFound) return matcher;
        return null;
    }

    public void LoadNext(int inc) {
        Matcher matcher = ParseFilename();
        if (matcher == null) {
            return;
        }
        String dir = path.substring(0, path.length() - filename.length());
        int imageNb = Integer.parseInt(matcher.group(2));
        String newFilename = String.format("%s%d.%s", matcher.group(1), imageNb+inc, matcher.group(3));
        String newPath = dir + newFilename;
        System.out.printf("Loading %s (%s)\n", newPath, newFilename);
        try {
            data = Files.readAllBytes(Paths.get(newPath));
            path = newPath;
            filename = newFilename;
            filenameLabel.setText(filename.substring(0, filename.length()-5));
            LoadScreen(gameScreenLines);
        } catch (Exception e) {
            System.out.println("Erreur: " + e.getMessage());
            System.out.println(path);
        }
    }

    public JLabel GetAddress() {
        JLabel label = new JLabel();
        label.setText(String.format(" 0x%04X", offset));
        label.setFont(new Font("Courier New", Font.BOLD, 16));
        return label;
    }

    public void Refresh() throws Exception
    {
        switch(screenMode) {
        case 1:
            displayLabel.setText("Affichage: 16 Couleurs");
            break;
        default:
            displayLabel.setText("Affichage: 4 Couleurs");
        }
        filenameLabel.setText(filename.substring(0, filename.length()-5));
        addressLabel.setText(String.format(" 0x%04X", offset));
        widthLabel.setText(String.format("Largeur: %d px", width));

        BufferedImage bi = getImageFromMemory(offset, height, 0, true);
        binViz.DrawFrame(offset, offsetBottom);
        vizLabel.setIcon(new ImageIcon(binViz.bi));
        ImageIcon bitmap = new ImageIcon(bi);
        bitmap.getImage().flush();
        bitmapLabel.setIcon(bitmap);
    }

    BufferedImage getImageFromMemory(int offset, int height, int end, boolean largePixels) {
        if (height == 0) {
            height = (end - offset) / (width / 2);
        }

        BufferedImage bi = new BufferedImage(1408, (int) Math.round(height * 1.5), BufferedImage.TYPE_INT_RGB);
        Color finalColor;
        int memOffset = offset;
        int screenLineNb = 0;
        if (memOffset > offsetScreenBottom) {
            for (int y=0; y<screenLineAddr.length; y++) {
                if (screenLineAddr[y] <= memOffset) {
                    break;
                }
                screenLineNb++;
            }
        }
        int nbCols = 352 / width;
        int x;
//        System.out.printf("Offset=0x%04X\n", memOffset);

        for (int col = 0; col < nbCols; col++) {

        for (int y = 0; y < height/2; y++)
        {
            if (memOffset > offsetScreenBottom) {
                offsetLineDown = screenLineAddr[screenLineNb+1];
                offsetLineUp = screenLineAddr[screenLineNb > 0 ? screenLineNb-1 : 0];
                for (x = 0; x < 352; x++) {
                    finalColor = paletteDAI[screen[x][y+screenLineNb]];
                    Graphics2D graph = bi.createGraphics();
                    graph.setColor(finalColor);
                    graph.fill(new Rectangle(x*4, y*3, 4, 3));
                    graph.dispose();
                }
                memOffset = screenLineAddr[y+screenLineNb+1];

                if (y == 130) {
                    offsetDown = memOffset;
//                    System.out.printf("Next page: 0x%04X\n", offsetDown);
                }                
                continue;
            }

            if (width == 352) {
                memOffset -= 2;
                if (memOffset < 2) { offsetBottom = 0; return bi; }
                x = 0;
            } else {
                x = width-8;
            }

            if (y == 0) {
                offsetLineDown = Math.max(0, offset - (width >> 2));
                offsetLineUp = offset + (width >> 2);
            }

            while (true) {
                // 16-color mode
                if (screenMode == 1) {
                    int value2 = data[memOffset] & 0xff;
                    int value1 = data[memOffset-1] & 0xff;
                    memOffset -= 2;
                    if (memOffset < 2) { offsetBottom = 0; return bi; }
                    int color1 = value1 >> 4;
                    int color2 = value1 & 0x0F;
                    int mask = 128;

                    for (int pixel = 0; pixel < 8; pixel++) {
                        if ((value2 & mask) != 0) {
                            finalColor = paletteDAI[color1];
                        } else {
                            finalColor = paletteDAI[color2];
                        }

                        Graphics2D graph = bi.createGraphics();
                        graph.setColor(finalColor);
                        graph.fill(new Rectangle((x+pixel+col*width)*4, y*3, 4, 3));
                        graph.dispose();
                        mask = mask >> 1;
                    }
                // 4-color mode
                } else {
                    int value2 = data[width == 352 ? memOffset - 1 : memOffset] & 0xff;
                    int value1 = data[width == 352 ? memOffset : memOffset-1] & 0xff;
                    memOffset -= 2;
                    if (memOffset < 2) { offsetBottom = 0; return bi; }
                    int mask = 128;

                    for (int pixel = 0; pixel < 8; pixel++) {
                        int value = 0;
                        if ((value1 & mask) != 0) {
                            value += 1;
                        }
                        if ((value2 & mask) != 0) {
                            value += 2;
                        }
                        finalColor = gameDefaultPalette[value];

                        Graphics2D graph = bi.createGraphics();
                        graph.setColor(finalColor);
                        graph.fill(new Rectangle((x+pixel+col*width)*4, y*3, 4, 3));
                        graph.dispose();
                        mask = mask >> 1;
                    }
                }
                if (memOffset < 2) { offsetBottom = 0; return bi; }

                if (width == 352) {
                    x += 8;
                    if (x == width) break;
                } else {
                    x -= 8;
                    if (x < 0) break;
                }
            }

            if (y == 130) {
                offsetDown = memOffset;
            }
        }
        }

        if (offsetScreenBottom == 0) {
            offsetScreenBottom = memOffset;
        }

        offsetBottom = memOffset;
        return bi;
    }

    private static File openFile() {
        final JFileChooser fc = new JFileChooser(".");
        fc.setFileFilter(new FileNameExtensionFilter("Dump Memoire", "dump"));
        fc.setPreferredSize(new Dimension(800, 400));
        int returnVal = fc.showOpenDialog(null);
        if (returnVal != 0) return null;
        return fc.getSelectedFile();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            offset = offsetLineUp;
            try {
                Refresh();
            } catch (Exception ex) {

            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        int nbCols = 40 / width;
        // Jump one screen up
        if (keyCode == KeyEvent.VK_PAGE_UP) {
            if (e.isControlDown()) {
                LoadNext(-1);
            } else {
                if (offset < 0xBFFF) {
                    offsetDown = offset;
                    offset = offsetUp;
                }
            }
        // Jump one screen down
        } else if (keyCode == KeyEvent.VK_PAGE_DOWN) {
            if (e.isControlDown()) {
                LoadNext(1);
            } else {
            System.out.printf("Offset=0x%04X, OffsetBottom=0x%04X,\n", offset, offsetBottom);
                if (offsetBottom > 0) {
                    offsetUp = offset;
                    offset = offsetDown;
                }
            }
        // Jump one line up, 16 bytes or 2 bytes up
        } else if (keyCode == KeyEvent.VK_UP) {
            offset = offsetLineUp;
        // Jump one line up, 16 bytes or 2 bytes down
        } else if (keyCode == KeyEvent.VK_DOWN) {
            if (offsetBottom > 0) {
                offset = offsetLineDown;
            }
        } else if (keyCode == KeyEvent.VK_LEFT) {
            if (e.isShiftDown()) {
                if (offsetBottom > 0) {
                    offset -= 1;
                }
            }
            else width = Math.max(8, width - 8);
        } else if (keyCode == KeyEvent.VK_RIGHT) {
            if (e.isShiftDown()) {
                if (offset < 0xBFFF) {
                    offset += 1;
                }
            }
            else width = Math.min(width + 8, 352);
        } else if (keyCode >= 97 && keyCode <= 101) {
            screenMode = keyCode - 97;
        } else if (keyCode >= 49 && keyCode <= 53) {
            screenMode = keyCode - 49;
        } else if (keyCode == KeyEvent.VK_HOME) {
            offset = 0xBFFF;
            width = 352;
        } else {
//            System.out.printf("Unknown code: %d\n", keyCode);
            return;
        }

        try {
            Refresh();
            frame.pack();
            frame.setVisible( true );
        } catch (Exception ex) {

        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // CREATE MEMVIEW WINDOW
    ////////////////////////////////////////////////////////////////////////////////////////

    public MemView(JFrame frame, File file) throws Exception {
        this.frame = frame;
        SetComponents();

        LoadData(file);
    }

    public void SetComponents() {
        removeAll();
        controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.PAGE_AXIS));
        JPanel infoPanel = new JPanel(new GridLayout(8, 1));
        controlPanel.add(infoPanel);
        displayLabel = new JLabel("Affichage: 4 couleurs");
        infoPanel.add(displayLabel);
        JLabel dummy = new JLabel("Affichage: Couleur de l'encre");
        dummy.setForeground(getBackground());
        infoPanel.add(dummy);
        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.LINE_AXIS));
        controlPanel.add(filePanel);
        filenameLabel = new JLabel(filename);
        filePanel.add(filenameLabel);
        filePanel.add(new JLabel(" "));

        Icon icon = UIManager.getIcon("Tree.openIcon");
        JButton loadButton = new JButton(icon);
        loadButton.setMargin(new Insets(0, 0, 0, 0));
        loadButton.addKeyListener(this);
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                File file = openFile();
                if (file == null) return;
                try {
                    LoadData(file);
                    Refresh();
                    frame.pack();
                } catch (Exception ex) {
                    System.out.printf("Erreur: %s\n", ex.getMessage());
                }
            }
            
        });
        filePanel.add(loadButton);
        infoPanel.add(filePanel);

        JPanel addressPanel = new JPanel();
        addressPanel.setLayout(new BoxLayout(addressPanel, BoxLayout.LINE_AXIS));
        addressPanel.add(new JLabel("Adresse:"));
        addressLabel = GetAddress();
        addressPanel.add(addressLabel);
        infoPanel.add(addressPanel);

        widthLabel = new JLabel(String.format("Largeur: %d px", width));
        infoPanel.add(widthLabel);

        JButton exportButton = new JButton("Export en PNG");
        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.Show(offset);
            }
        });
        exportButton.addKeyListener(this);
        infoPanel.add(exportButton);

        JPanel positionsContainer = new JPanel();
        positionsContainer.setBorder(BorderFactory.createTitledBorder("Bookmarks"));
        BoxLayout positionsLayout = new BoxLayout(positionsContainer, BoxLayout.Y_AXIS);
        positionsContainer.setLayout(positionsLayout);
        controlPanel.add(positionsContainer);
        GridLayout bookmarksLayout = new GridLayout(1, 1);
        bookmarksContainer = new JPanel();
        bookmarksContainer.setLayout(bookmarksLayout);
        bookmarksLayout.minimumLayoutSize(bookmarksContainer);
        positionsContainer.add(bookmarksContainer);

        helpDialog = new HelpDialog();
        icon = UIManager.getIcon("OptionPane.informationIcon");
        JButton helpButton = new JButton(icon);
        helpButton.setMargin(new Insets(0, 0, 0, 0));
        helpButton.addKeyListener(this);
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                helpDialog.Show();
                helpDialog.setVisible(true);
            }
        });
        controlPanel.add(new JLabel(" "));
        controlPanel.add(new JLabel(" "));
        controlPanel.add(helpButton);
        add(controlPanel);

        ImageIcon vizmap = new ImageIcon();
        vizLabel = new JLabel(vizmap);
        add(vizLabel);

        ImageIcon bitmap = new ImageIcon();
        bitmapLabel = new JLabel(bitmap);
        add(bitmapLabel);
    }

    ////////////////////////////////////////////////////////////////////////////////////////
    // AT STARTUP
    ////////////////////////////////////////////////////////////////////////////////////////

    private static void createAndShowGUI() throws Exception
    {
        File file = openFile();
        if (file == null) return;
        
        JFrame frame = new JFrame("DAI MemView");
        MemView memView = new MemView(frame, file);
        memView.Refresh();

        JScrollPane scrollPane = new JScrollPane(memView);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBounds(50, 30, 320, 1000);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(scrollPane);
        frame.setLocationByPlatform( true );
        frame.pack();
        frame.setVisible( true );
        frame.addKeyListener(memView);
    }

    public static void main(String[] args)
    {
        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                try {
                    createAndShowGUI();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    for (StackTraceElement ste : e.getStackTrace()) {
                        System.out.println(ste);
                    }
                }
            }
        });
    }

    int ascii[] = {
0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 239, 239, 239, 239, 255, 239, 239, 255, 255, 255, 255, 219, 219, 219, 255, 255, 255, 255, 255, 255, 255, 255, 219, 219, 129, 219, 129, 219, 219, 255, 255, 255, 255, 247, 193, 183, 195, 245, 131, 247, 255, 255, 255, 255, 93, 91, 247, 239, 217, 181, 255, 255, 255, 255, 255, 207, 183, 207, 207, 181, 187, 197, 255, 255, 255, 255, 247, 239, 223, 255, 255, 255, 255, 255, 255, 255, 255, 239, 223, 191, 191, 191, 223, 239, 255, 255, 255, 255, 239, 247, 251, 251, 251, 247, 239, 255, 255, 255, 255, 255, 255, 171, 239, 131, 239, 171, 255, 255, 255, 255, 255, 255, 239, 239, 131, 239, 239, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 239, 239, 223, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 239, 239, 255, 255, 255, 255, 253, 251, 247, 239, 223, 191, 127, 255, 255, 255, 255, 195, 185, 181, 173, 173, 157, 195, 255, 255, 255, 255, 247, 231, 215, 247, 247, 247, 129, 255, 255, 255, 255, 195, 189, 253, 195, 191, 191, 129, 255, 255, 255, 255, 131, 253, 253, 195, 253, 253, 131, 255, 255, 255, 255, 247, 231, 215, 183, 129, 247, 247, 255, 255, 255, 255, 129, 191, 191, 131, 253, 189, 195, 255, 255, 255, 255, 195, 189, 191, 131, 189, 189, 195, 255, 255, 255, 255, 129, 189, 251, 247, 239, 239, 239, 255, 255, 255, 255, 195, 189, 189, 195, 189, 189, 195, 255, 255, 255, 255, 195, 189, 189, 193, 253, 189, 195, 255, 255, 255, 255, 255, 255, 239, 239, 255, 239, 239, 255, 255, 255, 255, 255, 255, 239, 239, 255, 255, 239, 239, 223, 255, 255, 251, 247, 239, 223, 239, 247, 251, 255, 255, 255, 255, 255, 255, 255, 193, 255, 193, 255, 255, 255, 255, 255, 223, 239, 247, 251, 247, 239, 223, 255, 255, 255, 255, 195, 189, 189, 251, 247, 255, 247, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 195, 189, 189, 189, 129, 189, 189, 255, 255, 255, 255, 131, 221, 221, 195, 221, 221, 131, 255, 255, 255, 255, 195, 189, 191, 191, 191, 189, 195, 255, 255, 255, 255, 131, 221, 221, 221, 221, 221, 131, 255, 255, 255, 255, 129, 191, 191, 135, 191, 191, 129, 255, 255, 255, 255, 129, 191, 191, 135, 191, 191, 191, 255, 255, 255, 255, 195, 189, 191, 177, 189, 189, 193, 255, 255, 255, 255, 189, 189, 189, 129, 189, 189, 189, 255, 255, 255, 255, 227, 247, 247, 247, 247, 247, 227, 255, 255, 255, 255, 193, 251, 251, 251, 187, 187, 207, 255, 255, 255, 255, 157, 219, 215, 207, 215, 219, 157, 255, 255, 255, 255, 191, 191, 191, 191, 191, 191, 129, 255, 255, 255, 255, 189, 153, 165, 189, 189, 189, 189, 255, 255, 255, 255, 189, 157, 173, 181, 185, 189, 189, 255, 255, 255, 255, 195, 189, 189, 189, 189, 189, 195, 255, 255, 255, 255, 131, 221, 221, 195, 223, 223, 143, 255, 255, 255, 255, 195, 189, 189, 173, 181, 187, 197, 255, 255, 255, 255, 131, 221, 221, 195, 215, 219, 157, 255, 255, 255, 255, 195, 189, 191, 195, 253, 189, 195, 255, 255, 255, 255, 131, 171, 239, 239, 239, 239, 239, 255, 255, 255, 255, 189, 189, 189, 189, 189, 189, 195, 255, 255, 255, 255, 189, 189, 189, 189, 219, 219, 231, 255, 255, 255, 255, 189, 189, 189, 189, 165, 153, 189, 255, 255, 255, 255, 189, 219, 215, 239, 215, 219, 189, 255, 255, 255, 255, 189, 219, 231, 239, 239, 239, 239, 255, 255, 255, 255, 129, 253, 251, 247, 239, 223, 129, 255, 255, 255, 255, 195, 223, 223, 223, 223, 223, 195, 255, 255, 255, 255, 127, 191, 223, 239, 247, 251, 253, 255, 255, 255, 255, 135, 247, 247, 247, 247, 247, 135, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 195, 253, 195, 187, 197, 255, 255, 255, 255, 191, 191, 163, 157, 189, 157, 163, 255, 255, 255, 255, 255, 255, 193, 191, 191, 191, 193, 255, 255, 255, 255, 253, 253, 197, 185, 189, 185, 197, 255, 255, 255, 255, 255, 255, 195, 189, 129, 191, 193, 255, 255, 255, 255, 227, 221, 223, 135, 223, 223, 223, 255, 255, 255, 255, 255, 255, 197, 185, 189, 185, 197, 253, 189, 195, 255, 191, 191, 163, 157, 189, 189, 189, 255, 255, 255, 255, 239, 255, 207, 239, 239, 239, 199, 255, 255, 255, 255, 251, 255, 251, 251, 251, 251, 251, 187, 187, 199, 255, 191, 191, 177, 175, 159, 175, 177, 255, 255, 255, 255, 207, 239, 239, 239, 239, 239, 199, 255, 255, 255, 255, 255, 255, 185, 149, 173, 189, 189, 255, 255, 255, 255, 255, 255, 163, 157, 189, 189, 189, 255, 255, 255, 255, 255, 255, 195, 189, 189, 189, 195, 255, 255, 255, 255, 255, 255, 163, 157, 189, 157, 163, 191, 191, 191, 255, 255, 255, 197, 185, 189, 185, 197, 253, 253, 253, 255, 255, 255, 163, 157, 191, 191, 191, 255, 255, 255, 255, 255, 255, 193, 191, 195, 253, 131, 255, 255, 255, 255, 223, 223, 135, 223, 221, 221, 227, 255, 255, 255, 255, 255, 255, 187, 187, 187, 187, 197, 255, 255, 255, 255, 255, 255, 189, 189, 219, 219, 231, 255, 255, 255, 255, 255, 255, 189, 189, 173, 149, 185, 255, 255, 255, 255, 255, 255, 189, 219, 231, 219, 189, 255, 255, 255, 255, 255, 255, 189, 189, 189, 185, 197, 253, 189, 195, 255, 255, 255, 129, 251, 231, 223, 129, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255
    };
}
