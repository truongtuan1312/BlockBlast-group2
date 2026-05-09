import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class BlockBlastFrame extends JFrame {
    private final GamePanel gamePanel;
    private boolean isFullscreen = false;

    public BlockBlastFrame() {
        setTitle("Block Blast - Java OOP Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gamePanel = new GamePanel();
        add(gamePanel);
        addComponentListener(new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
        gamePanel.revalidate();
        gamePanel.repaint();
    }
});

        // F11 = toggle fullscreen, Escape = exit fullscreen
        gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "toggleFullscreen");
        gamePanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exitFullscreen");
        gamePanel.getActionMap().put("toggleFullscreen", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { toggleFullscreen(); }
        });
        gamePanel.getActionMap().put("exitFullscreen", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { if (isFullscreen) toggleFullscreen(); }
        });

        pack();
        Insets fi = getInsets();
        setSize(gamePanel.getPreferredSize().width  + fi.left + fi.right,
        gamePanel.getPreferredSize().height + fi.top  + fi.bottom);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(400, 500));
        setResizable(true);
    }

    private void toggleFullscreen() {
        GraphicsDevice gd = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getDefaultScreenDevice();
        dispose();
        if (!isFullscreen) {
            setUndecorated(true);
            gd.setFullScreenWindow(this);
        } else {
            gd.setFullScreenWindow(null);
            setUndecorated(false);
            pack();
            setLocationRelativeTo(null);
        }
        isFullscreen = !isFullscreen;
        setVisible(true);
    }
}
