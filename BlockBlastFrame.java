import javax.swing.*;
import java.awt.*;

public class BlockBlastFrame extends JFrame {
    public BlockBlastFrame() {
        setTitle("Block Blast - Java OOP Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        GamePanel gamePanel = new GamePanel();

        JScrollPane scrollPane = new JScrollPane(gamePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        add(scrollPane);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int maxH = (int)(screen.height * 0.92);
        int maxW = (int)(screen.width * 0.95);
        int winW = Math.min(740, maxW);
        int winH = Math.min(880, maxH);
        setSize(winW, winH);
        setLocationRelativeTo(null);
    }
}
