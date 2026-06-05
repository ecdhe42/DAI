import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridLayout;

public class HelpDialog extends JPanel {
    JFrame frame;

    public HelpDialog() {
        this.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        GridLayout layout = new GridLayout(17, 2);
        this.setLayout(layout);

        this.add(new JLabel("1-5"));
        this.add(new JLabel("Changement de mode d'affichage de l'écran"));
        this.add(new JLabel("Aller au début"));
        this.add(new JLabel("Retour a l'adresse 0x0000 et largeur 320px"));

        this.add(new JLabel("Page Suivante"));
        this.add(new JLabel("Déplacement d'une page vers le haut"));
        this.add(new JLabel("Page Précédente"));
        this.add(new JLabel("Déplacement d'une page vers le bas"));

        this.add(new JLabel("Touche du haut"));
        this.add(new JLabel("Déplacement d'une ligne vers le haut"));
        this.add(new JLabel("Touche du bas"));
        this.add(new JLabel("Déplacement d'une ligne vers le bas"));

        this.add(new JLabel("Shift + Touche du haut"));
        this.add(new JLabel("Déplacement de 8 octets vers le haut"));
        this.add(new JLabel("Shift + Touche du bas"));
        this.add(new JLabel("Déplacement de 8 octets vers le bas"));

        this.add(new JLabel("Ctrl + Up key"));
        this.add(new JLabel("Déplacement de 2 octets vers le haut"));
        this.add(new JLabel("Ctrl + Down key"));
        this.add(new JLabel("Déplacement de 2 octets vers le bas"));

        this.add(new JLabel(" "));
        this.add(new JLabel(" "));

        this.add(new JLabel("Touche de gauche"));
        this.add(new JLabel("Diminue la largeur"));
        this.add(new JLabel("Touche de droite"));
        this.add(new JLabel("Augmente la largeur"));

        this.add(new JLabel(" "));
        this.add(new JLabel(" "));

        this.add(new JLabel("Ctrl + Page Précédente"));
        this.add(new JLabel("Charge le dump memoire suivant"));
        this.add(new JLabel("Ctrl + Page Suivante"));
        this.add(new JLabel("Charge le dump memoire précédent"));
    }

    public void Show() {
        frame = new JFrame("Aide");
        frame.add(this);
        frame.setLocationByPlatform(true);
        frame.pack();
        frame.setVisible(true);
    }
}
