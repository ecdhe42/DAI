import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.image.BufferedImage;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class ExportDialog extends JPanel {
    MemView parent;
    JFrame frame;
    Integer start;
    Integer end;
    Integer oldStart;
    Integer oldEnd;
    JTextField value1;
    JTextField value2;

    public ExportDialog(MemView parent) {
        this.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        this.parent = parent;
        this.start = null;
        this.end = null;
        GridLayout layout = new GridLayout(3, 3);
        this.setLayout(layout);

        this.add(new JLabel("Adresse de depart:"));
        this.add(new JLabel("0x", SwingConstants.RIGHT));
        this.value1 = new JTextField();
        this.add(value1);
        this.add(new JLabel("Adresse de fin:"));
        this.add(new JLabel("0x", SwingConstants.RIGHT));
        this.value2 = new JTextField();
        this.add(value2);
        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String startString = value1.getText();
                String endString = value2.getText();
                if (startString != null && !startString.equals("")) start = (int)Long.parseLong(startString, 16);
                if (endString != null && !endString.equals("")) end = (int)Long.parseLong(endString, 16);
                if (start == null || end == null) return;

                Save();
                JComponent comp = (JComponent) e.getSource();
                SwingUtilities.getWindowAncestor(comp).dispose();
            }
        });
        this.add(ok);
        JButton clear = new JButton("Effacer");
        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                value1.setText("");
                value2.setText("");
                start = null;
                end = null;
            }
        });
        this.add(clear);
        JButton cancel = new JButton("Annuler");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                start = oldStart;
                end = oldEnd;
                JComponent comp = (JComponent) e.getSource();
                SwingUtilities.getWindowAncestor(comp).dispose();
            }
        });
        this.add(cancel);
    }

    public void Show(int address) {
        oldStart = start;
        oldEnd = end;
        if (start == null) {
            start = address;
        } else if (end == null) {
            end = address;
        }

        value1.setText(String.format("%x", start));
        value2.setText(end == null ? "" : String.format("%x", end));
        frame = new JFrame("Export as PNG");
        frame.add(this);
        frame.setLocationByPlatform( true );
        frame.pack();
        frame.setVisible( true );
    }

    private void Save() {
        BufferedImage bi = parent.getImageFromMemory(start, 0, end, false);
        File outputfile = new File("sauvegarde.png");
        try {
            ImageIO.write(bi, "png", outputfile);
        } catch (Exception ex) {}
        setVisible(false);
    }
}
