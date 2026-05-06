import java.awt.Rectangle;
import java.awt.event.*;

/**
 * InputHandler - Single Responsibility: chỉ xử lý input chuột.
 * Không biết vẽ, không biết state — chỉ nhận event và gọi callback.
 */
public class InputHandler implements MouseListener, MouseMotionListener {

    public interface Callback {
        void onPieceSelected(int index, int mouseX, int mouseY);
        void onDrag(int mouseX, int mouseY);
        void onDrop(int mouseX, int mouseY);
        void onMenuToggle();
        void onUndo();
        void onNewGame();
        void onExit();
        void onMenuClose();
        void onHoverExit();
    }

    private final Callback cb;
    private final Rectangle menuButton;
    private final Rectangle undoButton;
    private final Rectangle menuPanel;
    private final Rectangle menuNewGameButton;
    private final Rectangle menuExitButton;
    private final Rectangle gameOverNewGameButton;
    private final Rectangle gameOverExitButton;
    private final Rectangle[] pieceBounds;

    private boolean dragging = false;
    private final GameState state;
    private final int panelHeight;

    public InputHandler(Callback cb, GameState state,
                        Rectangle menuButton, Rectangle undoButton,
                        Rectangle menuPanel, Rectangle menuNewGameButton, Rectangle menuExitButton,
                        Rectangle gameOverNewGameButton, Rectangle gameOverExitButton,
                        Rectangle[] pieceBounds, int panelHeight) {
        this.cb = cb;
        this.state = state;
        this.menuButton = menuButton;
        this.undoButton = undoButton;
        this.menuPanel = menuPanel;
        this.menuNewGameButton = menuNewGameButton;
        this.menuExitButton = menuExitButton;
        this.gameOverNewGameButton = gameOverNewGameButton;
        this.gameOverExitButton = gameOverExitButton;
        this.pieceBounds = pieceBounds;
        this.panelHeight = panelHeight;
    }

    public boolean isDragging() { return dragging; }
    public void setDragging(boolean v) { dragging = v; }

    @Override
    public void mousePressed(MouseEvent e) {
        if (state.isGameOver()) {
            if (state.getGameOverCurtain() >= 0.72f) {
                int panelY = (panelHeight - 270) / 2;
                Rectangle nb = new Rectangle(gameOverNewGameButton.x, panelY + 194, gameOverNewGameButton.width, gameOverNewGameButton.height);
                Rectangle eb = new Rectangle(gameOverExitButton.x,    panelY + 194, gameOverExitButton.width,    gameOverExitButton.height);
                if (nb.contains(e.getPoint())) cb.onNewGame();
                else if (eb.contains(e.getPoint())) cb.onExit();
            }
            return;
        }

        if (state.isMenuOpen()) {
            if (menuNewGameButton.contains(e.getPoint()))  { cb.onNewGame(); return; }
            if (menuExitButton.contains(e.getPoint()))     { cb.onExit();    return; }
            if (!menuPanel.contains(e.getPoint()) && !menuButton.contains(e.getPoint())) {
                cb.onMenuClose();
            }
        }

        if (menuButton.contains(e.getPoint())) { cb.onMenuToggle(); return; }
        if (undoButton.contains(e.getPoint())) { cb.onUndo();       return; }

        for (int i = 0; i < pieceBounds.length; i++) {
            if (pieceBounds[i] != null && pieceBounds[i].contains(e.getPoint())) {
                dragging = true;
                cb.onPieceSelected(i, e.getX(), e.getY());
                return;
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging) return;
        cb.onDrag(e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!dragging) return;
        cb.onDrop(e.getX(), e.getY());
        dragging = false;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (!dragging) cb.onHoverExit();
    }

    @Override public void mouseClicked(MouseEvent e)  {}
    @Override public void mouseMoved(MouseEvent e)    {}
    @Override public void mouseEntered(MouseEvent e)  {}
}
