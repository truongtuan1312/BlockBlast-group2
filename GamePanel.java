import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Random;

public class GamePanel extends JPanel implements InputHandler.Callback {
    // --- Responsive layout: calculated from screen size at startup ---
    private int PANEL_WIDTH;
    private int PANEL_HEIGHT;
    private int CELL_SIZE;
    private int GAP;
    private int BOARD_SIZE_PIXEL;
    private int BOARD_X;
    private int BOARD_Y;
    private int PIECE_AREA_Y;
    private int PIECE_CELL_SIZE;
    private int DRAG_CELL_SIZE;

    //Section 1: LAYOUT
    private void recalcLayout() {
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    Insets si = Toolkit.getDefaultToolkit().getScreenInsets(
        GraphicsEnvironment.getLocalGraphicsEnvironment()
        .getDefaultScreenDevice().getDefaultConfiguration());
    int availH = screen.height - si.top - si.bottom - 40;
    int W = getWidth()  > 0 ? getWidth()  : (int)(availH * 0.68);
    int H = getHeight() > 0 ? getHeight() : availH;

    int headerH  = (int)(H * 0.30);
    int dockH    = (int)(H * 0.14);
    int boardAvH = H - headerH - dockH;
    int boardAvW = W - 56;
    int cellH    = boardAvH / Board.SIZE;
    int cellW    = boardAvW / Board.SIZE;
    CELL_SIZE        = Math.max(28, Math.min(cellH, cellW));
    GAP              = Math.max(4, CELL_SIZE / 10);
    BOARD_SIZE_PIXEL = Board.SIZE * CELL_SIZE;
    PANEL_WIDTH      = W;
    PANEL_HEIGHT     = H;
    BOARD_X          = (W - BOARD_SIZE_PIXEL) / 2;
    BOARD_Y          = headerH;
    PIECE_AREA_Y     = BOARD_Y + BOARD_SIZE_PIXEL + 40;
    PIECE_CELL_SIZE  = Math.max(16, CELL_SIZE / 2);
    DRAG_CELL_SIZE   = CELL_SIZE;

    int btnH = Math.max(32, (int)(BOARD_Y * 0.16));
    int btnX  = BOARD_X + BOARD_SIZE_PIXEL - 218; // căn phải theo cạnh phải board
    int btnY  = (int)(BOARD_Y * 0.04);
    menuButton.setBounds(btnX,        btnY, 70, btnH);
    undoButton.setBounds(btnX + 78,   btnY, 70, btnH);
    muteButton.setBounds(btnX + 158,  btnY, 48, btnH);
    int mpY = menuButton.y + menuButton.height + 4;
    menuPanel.setBounds(menuButton.x - 10,  mpY, 200, 140);
    menuNewGameButton.setBounds(menuButton.x + 5, mpY + 30, 170, 38);
    menuExitButton.setBounds(menuButton.x + 5, mpY + 80, 170, 38);
}

    private final Board board;
    private Piece[] pieces;
    private final Rectangle[] pieceBounds;

    private int selectedPieceIndex = -1;
    private boolean dragging = false;
    private int dragMouseX = 0;
    private int dragMouseY = 0;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int hoverRow = -1;
    private int hoverCol = -1;
    private final GameState state = new GameState();

    private InputHandler inputHandler;
    private final SoundManager sound = SoundManager.getInstance();

    private static final Rectangle menuButton            = new Rectangle(0,0,70,30);
    private static final Rectangle undoButton            = new Rectangle(0,0,70,30);
    private static final Rectangle menuPanel             = new Rectangle(0,0,200,140);
    private static final Rectangle menuNewGameButton     = new Rectangle(0,0,170, 38);
    private static final Rectangle menuExitButton        = new Rectangle(0,0, 170, 38);
    private static final Rectangle gameOverNewGameButton = new Rectangle(0,0, 140, 46);
    private static final Rectangle gameOverExitButton    = new Rectangle(0,0, 140, 46);
    private static final Rectangle muteButton            = new Rectangle(0,0,48,30);

    private final Random random = new Random();
    private final GameObjectPool<AtmosphereParticle> atmosphereParticles = new GameObjectPool<AtmosphereParticle>();
    private final GameObjectPool<DebrisParticle> debrisParticles = new GameObjectPool<DebrisParticle>();
    private final GameObjectPool<ScoreText> scoreTexts = new GameObjectPool<ScoreText>();

    private float animationTime = 0f;

    private Font titleFont;
    private Font gameFont;
    private Font normalFont;


    private final Timer animationTimer;

    public GamePanel() {
        board = new Board();
        pieces = PieceFactory.createThreePieces();
        pieceBounds = new Rectangle[3];

        setupFonts();
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(new Color(10, 12, 28));

        inputHandler = new InputHandler(this, state,
            menuButton, undoButton, menuPanel, menuNewGameButton, menuExitButton,
            gameOverNewGameButton, gameOverExitButton, pieceBounds, PANEL_HEIGHT, muteButton);
        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);
        recalcLayout();   
        rebuildAtmosphere();

        animationTimer = new Timer(16, e -> {
            animationTime += 0.026f;
            if (state.getComboPulse() > 0) state.setComboPulse(state.getComboPulse() - 1);
            if (state.isGameOver() && state.getGameOverCurtain() < 1f) {
                state.setGameOverCurtain(Math.min(1f, state.getGameOverCurtain() + 0.035f));
            }

            LevelTheme targetTheme = state.getThemeForScore(state.getScore());
            if (targetTheme != state.getCurrentTheme()) {
                state.setCurrentTheme(targetTheme);
                sound.play(SoundManager.SoundType.LEVEL_UP);
                rebuildAtmosphere();
            }

            atmosphereParticles.updateAll();

            debrisParticles.updateAll();
            debrisParticles.removeDeadObjects();

            scoreTexts.updateAll();
            scoreTexts.removeDeadObjects();

            repaint();
        });

        animationTimer.start();
    }

    private void setupFonts() {
        titleFont = new Font("Cooper Black", Font.BOLD, (int)(CELL_SIZE * 0.78));
        gameFont = new Font("Segoe UI Black", Font.BOLD, 18);
        normalFont = new Font("Segoe UI", Font.PLAIN, 14);

        if (!isFontAvailable("Cooper Black")) {
            titleFont = new Font("Showcard Gothic", Font.BOLD, 42);
        }
        if (!isFontAvailable("Segoe UI Black")) {
            gameFont = new Font("Segoe UI", Font.BOLD, 18);
        }
    }

    private boolean isFontAvailable(String fontName) {
        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String f : fonts) {
            if (f.equalsIgnoreCase(fontName)) return true;
        }
        return false;
    }



    private String getLevelName() {
        return state.getCurrentTheme().displayName;
    }

    private int getNextLevelScore() {
        if (state.getScore() < 300) return 300;
        if (state.getScore() < 700) return 700;
        if (state.getScore() < 1300) return 1300;
        if (state.getScore() < 5200) return 2200;
        return -1;
    }

    private double getProgressToNextLevel() {
        int next = getNextLevelScore();
        if (next == -1) return 1.0;

        int previous;
        if (next == 300) previous = 0;
        else if (next == 700) previous = 300;
        else if (next == 1300) previous = 700;
        else previous = 1300;

        return Math.max(0, Math.min(1, (state.getScore() - previous) / (double) (next - previous)));
    }

    private void rebuildAtmosphere() {
        atmosphereParticles.clear();
        for (int i = 0; i < state.getCurrentTheme().atmosphereCount; i++) {
            atmosphereParticles.add(new AtmosphereParticle(state.getCurrentTheme(), true, random, animationTime, PANEL_WIDTH, PANEL_HEIGHT));
        }
    }

    private void restartGame() {
        sound.play(SoundManager.SoundType.NEW_GAME);
        board.reset();
        pieces = PieceFactory.createThreePieces();
        state.reset();
        selectedPieceIndex = -1;
        dragging = false;
        hoverRow = -1;
        hoverCol = -1;
        debrisParticles.clear();
        scoreTexts.clear();
        rebuildAtmosphere();
        repaint();
    }

    private boolean allPiecesUsed() {
        for (Piece piece : pieces) {
            if (piece != null) return false;
        }
        return true;
    }


    private void saveUndoState() {
        state.saveUndo(board, pieces);
    }

    private void undoLastMove() {
        if (!state.canUndo()) return;
        sound.play(SoundManager.SoundType.UNDO);
        GameStateMemento mem = state.restoreUndo();
        board.restoreCells(mem.getBoardSnapshot());
        pieces = mem.getPiecesSnapshot();
        state.resetScore(mem.getScore());
        state.setCurrentTheme(state.getThemeForScore(state.getScore()));
        rebuildAtmosphere();
        selectedPieceIndex = -1;
        dragging = false;
        hoverRow = -1;
        hoverCol = -1;
        scoreTexts.clear();
        debrisParticles.clear();
        repaint();
    }

    private void startGameOver() {
        sound.play(SoundManager.SoundType.GAME_OVER);
        state.setGameOver(true);
        dragging = false;
        selectedPieceIndex = -1;
        state.setMenuOpen(false);
        state.setGameOverCurtain(0f);
    }

    private void exitGame() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
        System.exit(0);
    }

    private void updateHoverPosition() {
        if (!dragging || selectedPieceIndex == -1 || pieces[selectedPieceIndex] == null) {
            hoverRow = -1;
            hoverCol = -1;
            return;
        }

        int pieceDrawX = dragMouseX - dragOffsetX;
        int pieceDrawY = dragMouseY - dragOffsetY;
        hoverCol = Math.round((pieceDrawX - BOARD_X) / (float) CELL_SIZE);
        hoverRow = Math.round((pieceDrawY - BOARD_Y) / (float) CELL_SIZE);
    }

    //Section 2: Game Logic
    private void placeDraggedPiece() {
        if (selectedPieceIndex == -1 || pieces[selectedPieceIndex] == null) {
            resetDragState();
            return;
        }

        Piece selectedPiece = pieces[selectedPieceIndex];

        if (hoverRow >= 0 && hoverCol >= 0 && board.canPlace(selectedPiece, hoverRow, hoverCol)) {
            saveUndoState();
            state.setMenuOpen(false);
            int moveScore = selectedPiece.getBlockCount() * 10;
            int feedbackX = BOARD_X + hoverCol * CELL_SIZE + selectedPiece.getWidth() * CELL_SIZE / 2;
            int feedbackY = BOARD_Y + hoverRow * CELL_SIZE + selectedPiece.getHeight() * CELL_SIZE / 2;

            try {
                board.placePiece(selectedPiece, hoverRow, hoverCol);
            } catch (InvalidPlacementException e) {
                resetDragState();
                return;
}
            state.addScore(moveScore);
            sound.play(SoundManager.SoundType.PLACE);

            boolean[][] marked = board.getFullLineCells();
            int clearedLines = board.countFullLines();

            if (board.hasMarkedCell(marked)) {
                createBreakingEffect(marked, clearedLines);
                board.clearMarkedCells(marked);

                int lineBonus = clearedLines * state.getCurrentTheme().lineScore;
                int comboBonus = clearedLines >= 2 ? clearedLines * state.getCurrentTheme().comboScore : 0;
                moveScore += lineBonus + comboBonus;
                state.addScore(lineBonus + comboBonus);

                state.setComboPulse(32 + clearedLines * 8);
                sound.play(clearedLines >= 2 ? SoundManager.SoundType.COMBO : SoundManager.SoundType.CLEAR_LINE);
                showMoveFeedback("PERFECT", feedbackX, feedbackY, moveScore, clearedLines);
            } else if (moveScore >= 50) {
                showMoveFeedback("EXCELLENT", feedbackX, feedbackY, moveScore, 0);
            } else {
                showMoveFeedback("GOOD", feedbackX, feedbackY, moveScore, 0);
            }

            // bestScore is managed inside GameState.addScore()
            pieces[selectedPieceIndex] = null;

            if (allPiecesUsed()) pieces = PieceFactory.createThreePieces();
            if (!board.canAnyPieceFit(pieces)) startGameOver();
        }

        resetDragState();
    }

    private void showMoveFeedback(String word, int x, int y, int gainedScore, int clearedLines) {
        Color mainColor;
        Color glowColor;
        float size;

        if ("PERFECT".equals(word)) {
            mainColor = state.getCurrentTheme().scoreAccent;
            glowColor = new Color(255, 255, 255, 230);
            size = 38f;
        } else if ("EXCELLENT".equals(word)) {
            mainColor = state.getCurrentTheme().bestAccent;
            glowColor = new Color(120, 235, 255, 210);
            size = 31f;
        } else {
            mainColor = new Color(255, 255, 255, 235);
            glowColor = state.getCurrentTheme().subText;
            size = 26f;
        }

        int safeX = Math.max(120, Math.min(PANEL_WIDTH - 120, x));
        int safeY = Math.max(230, Math.min(BOARD_Y + BOARD_SIZE_PIXEL - 20, y));

        scoreTexts.add(new ScoreText(word, safeX, safeY, gainedScore, clearedLines, mainColor, glowColor, size, random, titleFont, gameFont));
    }

    private void createBreakingEffect(boolean[][] marked, int clearedLines) {
        int extra = Math.max(0, clearedLines - 1) * 8;

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                if (!marked[r][c]) continue;

                Color base = board.getCellColor(r, c);
                if (base == null) base = Color.WHITE;

                int centerX = BOARD_X + c * CELL_SIZE + CELL_SIZE / 2;
                int centerY = BOARD_Y + r * CELL_SIZE + CELL_SIZE / 2;

                for (int i = 0; i < state.getCurrentTheme().breakParticleCount + extra; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double speed = state.getCurrentTheme().breakSpeedMin + random.nextDouble() * state.getCurrentTheme().breakSpeedRange;
                    float vx = (float) Math.cos(angle) * (float) speed;
                    float vy = (float) Math.sin(angle) * (float) speed - state.getCurrentTheme().breakUpForce;

                    Color particleColor = base;
                    if (state.getCurrentTheme() == LevelTheme.FIRESTORM || state.getCurrentTheme() == LevelTheme.INFERNO) {
                        particleColor = new Color(255, 70 + random.nextInt(170), 10);
                    } else if (state.getCurrentTheme() == LevelTheme.CALM) {
                        particleColor = new Color(235, 240, 245, 220);
                    }

                    debrisParticles.add(new DebrisParticle(
                            centerX, centerY, vx, vy,
                            state.getCurrentTheme().breakMinSize + random.nextInt(state.getCurrentTheme().breakSizeRange),
                            particleColor, state.getCurrentTheme(), random, animationTime
                    ));
                }
            }
        }
    }

    private void resetDragState() {
        selectedPieceIndex = -1;
        dragging = false;
        hoverRow = -1;
        hoverCol = -1;
        repaint();
    }

    @Override
    //Section 3: Rendering
    protected void paintComponent(Graphics g) {
        recalcLayout();
        setupFonts();
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        enableSmoothGraphics(g2);

        drawBackground(g2); //Drawing methods
        drawDynamicOverlay(g2);
        drawAtmosphereParticles(g2);
        drawHeader(g2);
        drawLevelBar(g2);
        drawMenuAndUndoButtons(g2);
        drawBoardShell(g2);
        drawBoard(g2);
        drawPreview(g2);
        drawDebrisParticles(g2);
        drawScoreTexts(g2);
        drawPieceDock(g2);
        drawDraggingPiece(g2);
        drawInstruction(g2);
        if (state.isMenuOpen() && !state.isGameOver()) drawInGameMenu(g2);

        if (state.isGameOver()) drawGameOver(g2);
    }

    private void enableSmoothGraphics(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private void drawBackground(Graphics2D g2) {
        // Bản này không dùng 4 ảnh nền trong assets nữa.
        // Toàn bộ background được vẽ trực tiếp bằng Java2D nhưng vẫn giữ cơ chế
        // chuyển cấp giao diện, particle, fire/neon/sakura/leaf overlay như file cuối cùng.
        drawFallbackGradient(g2);

        drawGeneratedScenery(g2);

        g2.setColor(new Color(0, 0, 0, state.getCurrentTheme().darkOverlayAlpha));
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        RadialGradientPaint glow = new RadialGradientPaint(
                new Point(PANEL_WIDTH / 2, PANEL_HEIGHT / 2),
                520,
                new float[]{0f, 1f},
                new Color[]{state.getCurrentTheme().centerGlow, transparent(state.getCurrentTheme().centerGlow)}
        );
        g2.setPaint(glow);
        g2.fillOval(-120, -80, PANEL_WIDTH + 240, PANEL_HEIGHT + 160);
    }

    private void drawGeneratedScenery(Graphics2D g2) {
        if (state.getCurrentTheme() == LevelTheme.CALM) {
            drawGeneratedSakuraScenery(g2);
        } else if (state.getCurrentTheme() == LevelTheme.GOLDEN_FLOW) {
            drawGeneratedAutumnScenery(g2);
        } else if (state.getCurrentTheme() == LevelTheme.NEON_RUSH) {
            drawGeneratedRobotScenery(g2);
        } else {
            drawGeneratedFireScenery(g2);
        }
    }

    private void drawGeneratedSakuraScenery(Graphics2D g2) {
        g2.setColor(new Color(255, 230, 245, 45));
        g2.fillOval(-80, 95, 270, 140);
        g2.fillOval(470, 105, 310, 150);

        g2.setColor(new Color(105, 145, 185, 55));
        g2.fillOval(-150, 560, 430, 190);
        g2.fillOval(160, 535, 450, 220);
        g2.fillOval(460, 575, 380, 175);

        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 188, 220, 78));
        g2.drawArc(20, 125, 235, 155, 5, 155);
        g2.drawArc(445, 115, 260, 170, 25, 130);
        g2.setStroke(new BasicStroke(1f));

        g2.setColor(new Color(255, 205, 230, 58));
        for (int i = 0; i < 9; i++) {
            g2.fillOval(45 + i * 24, 154 + (i % 2) * 12, 34, 24);
            g2.fillOval(495 + i * 21, 150 + (i % 2) * 10, 34, 24);
        }
    }

    private void drawGeneratedAutumnScenery(Graphics2D g2) {
        g2.setColor(new Color(255, 213, 105, 55));
        g2.fillOval(520, 75, 120, 120);

        g2.setColor(new Color(120, 160, 95, 70));
        g2.fillOval(-95, 555, 365, 180);
        g2.fillOval(150, 535, 430, 210);
        g2.fillOval(475, 575, 360, 170);

        g2.setColor(new Color(84, 60, 38, 92));
        g2.fillRoundRect(80, 145, 20, 250, 18, 18);
        g2.fillRoundRect(595, 135, 20, 260, 18, 18);

        g2.setColor(new Color(105, 150, 85, 68));
        g2.fillOval(15, 95, 165, 125);
        g2.fillOval(520, 85, 175, 135);

        g2.setColor(new Color(255, 180, 80, 45));
        g2.fillOval(40, 120, 65, 42);
        g2.fillOval(585, 115, 65, 42);
    }

    private void drawGeneratedRobotScenery(Graphics2D g2) {
        // Nền robot được vẽ bằng vector Java2D, không cần ảnh robot.jpg.
        drawRobotWorker(g2, 125, 590, 0.80f, new Color(90, 230, 255, 38));
        drawRobotWorker(g2, 590, 420, 0.92f, new Color(120, 230, 255, 48));
        drawRobotWorker(g2, 575, 650, 0.72f, new Color(255, 95, 190, 34));

        g2.setColor(new Color(120, 235, 255, 38));
        g2.drawRoundRect(455, 520, 150, 72, 18, 18);
        g2.drawLine(470, 545, 590, 545);
        g2.drawLine(470, 565, 570, 565);

        g2.setColor(new Color(255, 90, 190, 28));
        g2.drawOval(70, 500, 130, 130);
        g2.drawOval(500, 300, 180, 180);
    }

    private void drawRobotWorker(Graphics2D g2, int cx, int cy, float scale, Color glow) {
        Graphics2D copy = (Graphics2D) g2.create();
        copy.translate(cx, cy);
        copy.scale(scale, scale);

        copy.setColor(glow);
        copy.fillOval(-85, -135, 170, 240);

        copy.setColor(new Color(110, 225, 255, 70));
        copy.fillRoundRect(-42, -110, 84, 66, 22, 22);
        copy.setColor(new Color(8, 18, 35, 170));
        copy.fillOval(-20, -82, 10, 10);
        copy.fillOval(10, -82, 10, 10);
        copy.setColor(new Color(255, 255, 255, 140));
        copy.drawArc(-18, -72, 36, 18, 200, 140);
        copy.setColor(new Color(120, 235, 255, 95));
        copy.drawLine(0, -110, 0, -132);
        copy.fillOval(-6, -140, 12, 12);

        copy.setColor(new Color(120, 235, 255, 40));
        copy.fillRoundRect(-48, -32, 96, 105, 26, 26);
        copy.setColor(new Color(170, 245, 255, 72));
        copy.drawRoundRect(-48, -32, 96, 105, 26, 26);

        copy.setColor(new Color(120, 235, 255, 58));
        copy.drawLine(-48, -4, -92, 32);
        copy.drawLine(48, -4, 92, 32);
        copy.drawLine(-24, 73, -45, 120);
        copy.drawLine(24, 73, 45, 120);

        copy.setColor(new Color(180, 240, 255, 36));
        copy.fillRoundRect(-70, 105, 140, 40, 14, 14);
        copy.dispose();
    }

    private void drawGeneratedFireScenery(Graphics2D g2) {
        g2.setColor(new Color(70, 10, 5, 120));
        g2.fillRect(0, PANEL_HEIGHT - 180, PANEL_WIDTH, 180);

        int flameCount = state.getCurrentTheme() == LevelTheme.INFERNO ? 16 : 12;
        int baseHeight = state.getCurrentTheme() == LevelTheme.INFERNO ? 150 : 120;
        for (int i = 0; i < flameCount; i++) {
            int x = i * 58 - 40;
            int h = baseHeight + (int) (45 * Math.sin(animationTime * 2.2 + i * 0.8));

            Path2D flame = new Path2D.Double();
            flame.moveTo(x, PANEL_HEIGHT);
            flame.curveTo(x + 10, PANEL_HEIGHT - h * 0.45, x + 35, PANEL_HEIGHT - h * 0.95, x + 48, PANEL_HEIGHT - h);
            flame.curveTo(x + 80, PANEL_HEIGHT - h * 0.65, x + 70, PANEL_HEIGHT - h * 0.28, x + 100, PANEL_HEIGHT);
            flame.closePath();

            GradientPaint gp = new GradientPaint(
                    x, PANEL_HEIGHT - h,
                    new Color(255, 235, 60, state.getCurrentTheme() == LevelTheme.INFERNO ? 155 : 125),
                    x + 90, PANEL_HEIGHT,
                    new Color(255, 40, 0, 0)
            );
            g2.setPaint(gp);
            g2.fill(flame);
        }

        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(255, 80, 20, 42));
        for (int i = 0; i < 10; i++) {
            int y = 150 + i * 70;
            g2.drawLine(0, y, PANEL_WIDTH, y - 90);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawFallbackGradient(Graphics2D g2) {
        GradientPaint gp = new GradientPaint(0, 0, state.getCurrentTheme().bgTop, PANEL_WIDTH, PANEL_HEIGHT, state.getCurrentTheme().bgBottom);
        g2.setPaint(gp);
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
    }

    private Color transparent(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
    }

    private void drawDynamicOverlay(Graphics2D g2) {
        if (state.getCurrentTheme() == LevelTheme.CALM) {
            drawSoftSakuraLight(g2);
        } else if (state.getCurrentTheme() == LevelTheme.GOLDEN_FLOW) {
            drawGoldenLeafLight(g2);
        } else if (state.getCurrentTheme() == LevelTheme.NEON_RUSH) {
            drawNeonRush(g2);
        } else {
            drawFireIntensity(g2);
        }

        if (state.getComboPulse() > 0) drawComboFlash(g2);
    }

    private void drawSoftSakuraLight(Graphics2D g2) {
        g2.setColor(new Color(255, 215, 235, 22));
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
    }

    private void drawGoldenLeafLight(Graphics2D g2) {
        g2.setColor(new Color(255, 210, 120, 28));
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
    }

    private void drawNeonRush(Graphics2D g2) {
        int scanY = (int) ((animationTime * 95) % PANEL_HEIGHT);
        g2.setColor(new Color(100, 235, 255, 48));
        g2.fillRect(0, scanY, PANEL_WIDTH, 3);

        g2.setColor(new Color(120, 235, 255, 15));
        for (int y = 0; y < PANEL_HEIGHT; y += 35) g2.drawLine(0, y, PANEL_WIDTH, y);
        for (int x = 0; x < PANEL_WIDTH; x += 35) g2.drawLine(x, 0, x, PANEL_HEIGHT);

        g2.setColor(new Color(255, 90, 190, 28));
        g2.fillOval(PANEL_WIDTH - 360, 180, 420, 420);
    }

    private void drawFireIntensity(Graphics2D g2) {
        int flameCount = state.getCurrentTheme() == LevelTheme.INFERNO ? 24 : 18;
        int baseHeight = state.getCurrentTheme() == LevelTheme.INFERNO ? 155 : 115;

        for (int i = 0; i < flameCount; i++) {
            int x = i * 38 - 50;
            int h = baseHeight + (int) (55 * Math.sin(animationTime * 2.5 + i * 0.7));

            Path2D flame = new Path2D.Double();
            flame.moveTo(x, PANEL_HEIGHT);
            flame.curveTo(x + 10, PANEL_HEIGHT - h * 0.45, x + 25, PANEL_HEIGHT - h * 0.95, x + 48, PANEL_HEIGHT - h);
            flame.curveTo(x + 78, PANEL_HEIGHT - h * 0.60, x + 72, PANEL_HEIGHT - h * 0.22, x + 105, PANEL_HEIGHT);
            flame.closePath();

            GradientPaint paint = new GradientPaint(
                    x, PANEL_HEIGHT - h,
                    new Color(255, 235, 70, state.getCurrentTheme() == LevelTheme.INFERNO ? 190 : 150),
                    x + 100, PANEL_HEIGHT,
                    new Color(255, 40, 0, 0)
            );

            g2.setPaint(paint);
            g2.fill(flame);
        }

        RadialGradientPaint heat = new RadialGradientPaint(
                new Point(PANEL_WIDTH / 2, PANEL_HEIGHT),
                540,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 80, 0, state.getCurrentTheme() == LevelTheme.INFERNO ? 130 : 95), new Color(255, 80, 0, 0)}
        );
        g2.setPaint(heat);
        g2.fillOval(-140, PANEL_HEIGHT - 460, PANEL_WIDTH + 280, 560);
    }

    private void drawComboFlash(Graphics2D g2) {
        int alpha = Math.min(120, state.getComboPulse() * 4);
        g2.setColor(new Color(state.getCurrentTheme().flashColor.getRed(), state.getCurrentTheme().flashColor.getGreen(), state.getCurrentTheme().flashColor.getBlue(), alpha));
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
    }

    private void drawAtmosphereParticles(Graphics2D g2) {
        atmosphereParticles.drawAll(g2);
    }


    private void drawMenuAndUndoButtons(Graphics2D g2) {
        drawIconButton(g2, menuButton, "menu", true);
        drawIconButton(g2, undoButton, "undo", state.canUndo());
        drawMuteButton(g2);
    }

    private void drawIconButton(Graphics2D g2, Rectangle rect, String type, boolean enabled) {
        Graphics2D copy = (Graphics2D) g2.create();

        int glowAlpha = enabled ? 125 : 35;
        int glassAlpha = enabled ? 96 : 38;

        RadialGradientPaint outerGlow = new RadialGradientPaint(
                new Point(rect.x + rect.width / 2, rect.y + rect.height / 2),
                72,
                new float[]{0f, 1f},
                new Color[]{
                        new Color(state.getCurrentTheme().buttonB.getRed(), state.getCurrentTheme().buttonB.getGreen(), state.getCurrentTheme().buttonB.getBlue(), glowAlpha),
                        new Color(state.getCurrentTheme().buttonB.getRed(), state.getCurrentTheme().buttonB.getGreen(), state.getCurrentTheme().buttonB.getBlue(), 0)
                }
        );
        copy.setPaint(outerGlow);
        copy.fillOval(rect.x - 30, rect.y - 30, rect.width + 60, rect.height + 60);

        copy.setColor(new Color(0, 0, 0, enabled ? 150 : 82));
        copy.fillRoundRect(rect.x + 8, rect.y + 10, rect.width, rect.height, 24, 24);

        GradientPaint glass = new GradientPaint(
                rect.x, rect.y,
                enabled ? new Color(255, 255, 255, glassAlpha) : new Color(150, 150, 165, 62),
                rect.x, rect.y + rect.height,
                enabled ? new Color(255, 255, 255, 30) : new Color(80, 80, 92, 54)
        );
        copy.setPaint(glass);
        copy.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 24, 24);

        copy.setColor(new Color(255, 255, 255, enabled ? 60 : 25));
        copy.fillRoundRect(rect.x + 8, rect.y + 7, rect.width - 16, rect.height / 2 - 2, 18, 18);

        copy.setStroke(new BasicStroke(2.2f));
        copy.setColor(enabled ? new Color(255, 255, 255, 160) : new Color(255, 255, 255, 58));
        copy.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 24, 24);

        int cx = rect.x + rect.width / 2;
        int cy = rect.y + rect.height / 2;

        Color iconColor = enabled ? Color.WHITE : new Color(220, 220, 230, 105);
        Color iconShadow = new Color(0, 0, 0, enabled ? 95 : 45);

        if ("menu".equals(type)) {
            copy.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = -12; i <= 12; i += 12) {
                copy.setColor(iconShadow);
                copy.drawLine(cx - 18 + 2, cy + i + 2, cx + 18 + 2, cy + i + 2);
                copy.setColor(iconColor);
                copy.drawLine(cx - 18, cy + i, cx + 18, cy + i);
            }
        } else {
            copy.setStroke(new BasicStroke(4.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            Arc2D shadowArc = new Arc2D.Double(cx - 21 + 2, cy - 20 + 2, 42, 40, 35, 300, Arc2D.OPEN);
            copy.setColor(iconShadow);
            copy.draw(shadowArc);

            Arc2D arc = new Arc2D.Double(cx - 21, cy - 20, 42, 40, 35, 300, Arc2D.OPEN);
            copy.setColor(iconColor);
            copy.draw(arc);

            Path2D arrowShadow = new Path2D.Double();
            arrowShadow.moveTo(cx - 17 + 2, cy - 20 + 2);
            arrowShadow.lineTo(cx - 30 + 2, cy - 18 + 2);
            arrowShadow.lineTo(cx - 22 + 2, cy - 7 + 2);
            arrowShadow.closePath();
            copy.setColor(iconShadow);
            copy.fill(arrowShadow);

            Path2D arrow = new Path2D.Double();
            arrow.moveTo(cx - 17, cy - 20);
            arrow.lineTo(cx - 30, cy - 18);
            arrow.lineTo(cx - 22, cy - 7);
            arrow.closePath();
            copy.setColor(iconColor);
            copy.fill(arrow);

            copy.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            copy.setColor(enabled ? new Color(255, 255, 255, 95) : new Color(255, 255, 255, 35));
            copy.drawArc(cx - 12, cy - 11, 24, 22, 40, 260);
        }

        copy.dispose();
    }

    private void drawMuteButton(Graphics2D g2) {
        Rectangle r = muteButton;
        boolean muted = sound.isMuted();

        // Shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(r.x + 3, r.y + 4, r.width, r.height, 14, 14);

        // Background
        GradientPaint bg = muted
            ? new GradientPaint(r.x, r.y, new Color(80, 30, 30), r.x, r.y + r.height, new Color(50, 15, 15))
            : new GradientPaint(r.x, r.y, state.getCurrentTheme().buttonA, r.x, r.y + r.height, state.getCurrentTheme().buttonB);
        g2.setPaint(bg);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 14, 14);

        // Border
        g2.setColor(muted ? new Color(180, 60, 60, 180) : state.getCurrentTheme().cardBorder);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 14, 14);
        g2.setStroke(new BasicStroke(1f));

        // Icon - speaker shape
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;
        int sz = r.height / 4;
        g2.setColor(muted ? new Color(255, 100, 100) : Color.WHITE);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Speaker body
        int[] spkX = {cx - sz, cx - sz/2, cx + sz/3, cx + sz/3};
        int[] spkY = {cy - sz/3, cy - sz/3, cy - sz*2/3, cy + sz*2/3};
        int[] spkX2 = {cx + sz/3, cx - sz/2, cx - sz, cx - sz};
        int[] spkY2 = {cy + sz*2/3, cy + sz/3, cy + sz/3, cy - sz/3};
        int[] polyX = {cx - sz, cx - sz/2, cx + sz/3, cx + sz/3, cx - sz/2, cx - sz};
        int[] polyY = {cy - sz/3, cy - sz/3, cy - sz*2/3, cy + sz*2/3, cy + sz/3, cy + sz/3};
        g2.fillPolygon(polyX, polyY, polyX.length);

        if (!muted) {
            // Sound waves
            g2.setColor(new Color(255, 255, 255, 180));
            g2.drawArc(cx + sz/3, cy - sz, sz, sz*2, -30, 60);
            g2.drawArc(cx + sz/3 + 4, cy - sz - 4, sz + 8, sz*2 + 8, -40, 80);
        } else {
            // X mark
            g2.setColor(new Color(255, 80, 80));
            g2.drawLine(cx + sz/3 + 2, cy - sz/2, cx + sz + 2, cy + sz/2);
            g2.drawLine(cx + sz/3 + 2, cy + sz/2, cx + sz + 2, cy - sz/2);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawInGameMenu(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRoundRect(menuPanel.x + 8, menuPanel.y + 10, menuPanel.width, menuPanel.height, 28, 28);

        g2.setPaint(new GradientPaint(
                menuPanel.x, menuPanel.y,
                new Color(255, 255, 255, 72),
                menuPanel.x, menuPanel.y + menuPanel.height,
                new Color(255, 255, 255, 22)
        ));
        g2.fillRoundRect(menuPanel.x, menuPanel.y, menuPanel.width, menuPanel.height, 28, 28);
        g2.setColor(new Color(255, 255, 255, 82));
        g2.drawRoundRect(menuPanel.x, menuPanel.y, menuPanel.width, menuPanel.height, 28, 28);

        g2.setFont(gameFont.deriveFont(Font.BOLD, 17f));
        g2.setColor(Color.WHITE);
        g2.drawString("MENU", menuPanel.x + 58, menuPanel.y + 24);

        drawMenuChoiceButton(g2, menuNewGameButton, "NEW GAME", state.getCurrentTheme().buttonA, state.getCurrentTheme().buttonB);
        drawMenuChoiceButton(g2, menuExitButton, "EXIT", new Color(255, 95, 120), new Color(180, 35, 65));
    }

    private void drawMenuChoiceButton(Graphics2D g2, Rectangle rect, String text, Color a, Color b) {
        g2.setColor(new Color(0, 0, 0, 95));
        g2.fillRoundRect(rect.x + 3, rect.y + 5, rect.width, rect.height, 19, 19);
        g2.setPaint(new GradientPaint(rect.x, rect.y, a, rect.x + rect.width, rect.y + rect.height, b));
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 19, 19);
        g2.setColor(new Color(255, 255, 255, 105));
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 19, 19);
        g2.setFont(gameFont.deriveFont(Font.BOLD, 14f));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString(text, rect.x + (rect.width - fm.stringWidth(text)) / 2, rect.y + 25);
    }

    private void drawHeader(Graphics2D g2) {
       int cardW = (int)(BOARD_SIZE_PIXEL * 0.29);
       int titleY    = (int)(BOARD_Y * 0.48);
       int subtitleY = (int)(BOARD_Y * 0.58);
       int cardY     = (int)(BOARD_Y * 0.62);
       int cardH     = Math.min(60, BOARD_Y - cardY - 30);
        g2.setFont(titleFont);
        g2.setColor(new Color(0, 0, 0, 125));
        g2.drawString("BLOCK BLAST", BOARD_X + 4, titleY + 4);
        GradientPaint titleGradient = new GradientPaint(BOARD_X, titleY - 30,
            state.getCurrentTheme().titleA, BOARD_X + 300, titleY + 10, state.getCurrentTheme().titleB);
        g2.setPaint(titleGradient);
        g2.drawString("BLOCK BLAST", BOARD_X, titleY);

        g2.setFont(normalFont.deriveFont(Font.BOLD, (float)(CELL_SIZE * 0.27)));
        g2.setColor(state.getCurrentTheme().subText);
        g2.drawString(getLevelName(), BOARD_X + 2, subtitleY);

        int card2X = BOARD_X + cardW + 16;
        drawScoreCard(g2, BOARD_X, cardY, cardW, cardH, "SCORE",
            String.valueOf(state.getScore()), state.getCurrentTheme().scoreAccent);
        drawScoreCard(g2, card2X, cardY, cardW, cardH, "BEST",
            String.valueOf(state.getBestScore()), state.getCurrentTheme().bestAccent);
    }

    private void drawScoreCard(Graphics2D g2, int x, int y, int w, int h, String label, String value, Color accent) {
        g2.setColor(new Color(0, 0, 0, state.getCurrentTheme().shadowAlpha));
        g2.fillRoundRect(x + 4, y + 5, w, h, 18, 18);
        GradientPaint card = new GradientPaint(x, y, state.getCurrentTheme().cardTop, x, y + h, state.getCurrentTheme().cardBottom);
        g2.setPaint(card);
        g2.fillRoundRect(x, y, w, h, 18, 18);
        g2.setColor(state.getCurrentTheme().cardBorder);
        g2.drawRoundRect(x, y, w, h, 18, 18);

        float labelSz = Math.max(10f, h * 0.28f);
        float valueSz = Math.max(14f, h * 0.52f);
        g2.setFont(gameFont.deriveFont(Font.BOLD, labelSz));
        g2.setColor(state.getCurrentTheme().cardLabel);
        g2.drawString(label, x + 14, y + (int)(h * 0.38));
        g2.setFont(gameFont.deriveFont(Font.BOLD, valueSz));
        g2.setColor(accent);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(value, x + (w - fm.stringWidth(value)) / 2, y + (int)(h * 0.82));
    }

    private void drawSmallButton(Graphics2D g2, Rectangle rect, String text) {
        g2.setColor(new Color(0, 0, 0, state.getCurrentTheme().shadowAlpha + 15));
        g2.fillRoundRect(rect.x + 5, rect.y + 7, rect.width, rect.height, 24, 24);

        GradientPaint button = new GradientPaint(rect.x, rect.y, state.getCurrentTheme().buttonA, rect.x + rect.width, rect.y + rect.height, state.getCurrentTheme().buttonB);
        g2.setPaint(button);
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 24, 24);

        g2.setColor(state.getCurrentTheme().buttonBorder);
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 24, 24);

        g2.setFont(gameFont.deriveFont(Font.BOLD, 15f));
        FontMetrics fm = g2.getFontMetrics();
        int tx = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int ty = rect.y + rect.height / 2 + 6;
        g2.setColor(state.getCurrentTheme().buttonText);
        g2.drawString(text, tx, ty);
    }

    private void drawLevelBar(Graphics2D g2) {
        int x = menuButton.x;
        int y = menuButton.y + menuButton.height + 6;
        int w = muteButton.x + muteButton.width - menuButton.x;
        int h = Math.max(10, (int)(BOARD_Y * 0.06));
        double progress = getProgressToNextLevel();

        g2.setColor(new Color(0, 0, 0, state.getCurrentTheme().shadowAlpha));
        g2.fillRoundRect(x + 3, y + 4, w, h, 15, 15);

        g2.setColor(new Color(255, 255, 255, 28));
        g2.fillRoundRect(x, y, w, h, 15, 15);

        GradientPaint fill = new GradientPaint(x, y, state.getCurrentTheme().buttonA, x + w, y, state.getCurrentTheme().buttonB);
        g2.setPaint(fill);
        g2.fillRoundRect(x, y, (int) (w * progress), h, 15, 15);

        g2.setColor(new Color(255, 255, 255, 70));
        g2.drawRoundRect(x, y, w, h, 15, 15);

        g2.setFont(normalFont.deriveFont(Font.BOLD, 11f));
        String text = getNextLevelScore() == -1 ? "MAX LEVEL" : "NEXT: " + getNextLevelScore();
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(state.getCurrentTheme().mainText);
        g2.drawString(text, x + w + 28, y + h / 2 + fm.getAscent() / 2);
    }

    private void drawBoardShell(Graphics2D g2) {
        int shellX = BOARD_X - 24;
        int shellY = BOARD_Y - 24;
        int shellW = BOARD_SIZE_PIXEL + 48;
        int shellH = BOARD_SIZE_PIXEL + 48;

        g2.setColor(new Color(0, 0, 0, state.getCurrentTheme().shadowAlpha + 25));
        g2.fillRoundRect(shellX + 10, shellY + 14, shellW, shellH, 40, 40);

        GradientPaint outer = new GradientPaint(shellX, shellY, state.getCurrentTheme().boardGlowA, shellX + shellW, shellY + shellH, state.getCurrentTheme().boardGlowB);
        g2.setPaint(outer);
        g2.fillRoundRect(shellX, shellY, shellW, shellH, 40, 40);

        g2.setColor(state.getCurrentTheme().boardInner);
        g2.fillRoundRect(shellX + 4, shellY + 4, shellW - 8, shellH - 8, 36, 36);

        g2.setColor(state.getCurrentTheme().boardLine);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(shellX + 8, shellY + 8, shellW - 16, shellH - 16, 32, 32);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawBoard(Graphics2D g2) {
        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                int x = BOARD_X + c * CELL_SIZE;
                int y = BOARD_Y + r * CELL_SIZE;
                drawEmptyCell(g2, x + GAP / 2, y + GAP / 2, CELL_SIZE - GAP);

                Color cellColor = board.getCellColor(r, c);
                if (cellColor != null) drawPremiumBlock(g2, x + GAP / 2, y + GAP / 2, CELL_SIZE - GAP, cellColor, false);
            }
        }
    }

    private void drawEmptyCell(Graphics2D g2, int x, int y, int size) {
        g2.setColor(state.getCurrentTheme().emptyCell);
        g2.fillRoundRect(x, y, size, size, 15, 15);
        g2.setColor(state.getCurrentTheme().emptyCellBorder);
        g2.drawRoundRect(x, y, size, size, 15, 15);
        g2.setColor(new Color(0, 0, 0, state.getCurrentTheme().innerCellShadow));
        g2.fillRoundRect(x + 4, y + 4, size - 8, size - 8, 13, 13);
    }

    private void drawPreview(Graphics2D g2) {
        if (!dragging || selectedPieceIndex == -1 || pieces[selectedPieceIndex] == null || hoverRow < 0 || hoverCol < 0) return;

        Piece piece = pieces[selectedPieceIndex];
        boolean canPlace = board.canPlace(piece, hoverRow, hoverCol);
        boolean[][] shape = piece.getShape();
        Color previewColor = canPlace ? state.getCurrentTheme().validPreview : state.getCurrentTheme().invalidPreview;
        Color outlineColor = canPlace ? state.getCurrentTheme().validOutline : state.getCurrentTheme().invalidOutline;

        for (int r = 0; r < piece.getHeight(); r++) {
            for (int c = 0; c < piece.getWidth(); c++) {
                if (!shape[r][c]) continue;

                int boardRow = hoverRow + r;
                int boardCol = hoverCol + c;
                if (boardRow >= 0 && boardRow < Board.SIZE && boardCol >= 0 && boardCol < Board.SIZE) {
                    int x = BOARD_X + boardCol * CELL_SIZE + GAP / 2;
                    int y = BOARD_Y + boardRow * CELL_SIZE + GAP / 2;
                    int size = CELL_SIZE - GAP;

                    g2.setColor(previewColor);
                    g2.fillRoundRect(x, y, size, size, 16, 16);
                    g2.setColor(outlineColor);
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(x + 1, y + 1, size - 2, size - 2, 16, 16);
                    g2.setStroke(new BasicStroke(1f));
                }
            }
        }
    }

    private void drawDebrisParticles(Graphics2D g2) {
        debrisParticles.drawAll(g2);
    }

    private void drawScoreTexts(Graphics2D g2) {
        scoreTexts.drawAll(g2);
    }

    private void drawPieceDock(Graphics2D g2) {
        int dockY     = PIECE_AREA_Y;
        int slotW     = (PANEL_WIDTH - 80) / 3;
        int slotH     = PANEL_HEIGHT - dockY - 16;
        slotH         = Math.max(50, Math.min(slotH, (int)(CELL_SIZE * 1.6)));
        int textX = BOARD_X + BOARD_SIZE_PIXEL + 35;
        int labelY    = dockY - (int)(CELL_SIZE * 1.1);
        int hintY     = dockY - (int)(CELL_SIZE * 0.55);

    g2.setColor(state.getCurrentTheme().mainText);
    g2.setFont(gameFont.deriveFont(Font.BOLD, (float)(CELL_SIZE * 0.36)));
    g2.drawString("Drag blocks to board", textX, labelY);
    g2.setFont(normalFont.deriveFont(Font.PLAIN, (float)(CELL_SIZE * 0.24)));
    g2.setColor(state.getCurrentTheme().subText);
    g2.drawString(state.getCurrentTheme().dockHint, textX, hintY);
        for (int i = 0; i < pieces.length; i++) {
            int areaX = 40 + i * slotW;
            int areaY = dockY;
            pieceBounds[i] = new Rectangle(areaX, areaY, slotW - 6, slotH);
            drawDockSlot(g2, areaX, areaY, slotW - 6, slotH, i == selectedPieceIndex && dragging);
            if (pieces[i] != null) {
                if (!(i == selectedPieceIndex && dragging))
                    drawPieceCentered(g2, pieces[i], areaX, areaY, slotW - 6, slotH, PIECE_CELL_SIZE);
            } else {
                g2.setColor(state.getCurrentTheme().usedText);
                g2.setFont(gameFont.deriveFont(Font.BOLD, (float)(CELL_SIZE * 0.35)));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("USED", areaX + (slotW - 6 - fm.stringWidth("USED")) / 2, areaY + slotH / 2 + 7);
            }
        }
    }

    private void drawDockSlot(Graphics2D g2, int x, int y, int w, int h, boolean active) {
        g2.setColor(new Color(0, 0, 0, state.getCurrentTheme().shadowAlpha));
        g2.fillRoundRect(x + 5, y + 8, w, h, 28, 28);

        Color top = active ? state.getCurrentTheme().dockActiveA : state.getCurrentTheme().dockIdleA;
        Color bottom = active ? state.getCurrentTheme().dockActiveB : state.getCurrentTheme().dockIdleB;
        GradientPaint gp = new GradientPaint(x, y, top, x, y + h, bottom);
        g2.setPaint(gp);
        g2.fillRoundRect(x, y, w, h, 28, 28);

        g2.setColor(active ? state.getCurrentTheme().dockActiveBorder : state.getCurrentTheme().dockBorder);
        g2.drawRoundRect(x, y, w, h, 28, 28);
    }

    private void drawPieceCentered(Graphics2D g2, Piece piece, int areaX, int areaY, int areaW, int areaH, int size) {
        int pieceW = piece.getWidth() * size;
        int pieceH = piece.getHeight() * size;
        int startX = areaX + (areaW - pieceW) / 2;
        int startY = areaY + (areaH - pieceH) / 2;
        drawPiece(g2, piece, startX, startY, size, false);
    }

    private void drawDraggingPiece(Graphics2D g2) {
        if (!dragging || selectedPieceIndex == -1 || pieces[selectedPieceIndex] == null) return;

        Piece piece = pieces[selectedPieceIndex];
        int drawX;
        int drawY;
        int drawSize;

        if (hoverRow >= 0 && hoverCol >= 0) {
            drawX = BOARD_X + hoverCol * CELL_SIZE + GAP / 2;
            drawY = BOARD_Y + hoverRow * CELL_SIZE + GAP / 2;
            drawSize = DRAG_CELL_SIZE;
        } else {
            drawX = dragMouseX - dragOffsetX;
            drawY = dragMouseY - dragOffsetY;
            drawSize = PIECE_CELL_SIZE + 7;
        }

        drawPieceShadow(g2, piece, drawX + 10, drawY + 14, drawSize);
        drawPiece(g2, piece, drawX, drawY, drawSize, true);
    }

    private void drawPieceShadow(Graphics2D g2, Piece piece, int startX, int startY, int size) {
        boolean[][] shape = piece.getShape();
        g2.setColor(new Color(0, 0, 0, state.getCurrentTheme().shadowAlpha + 30));
        for (int r = 0; r < piece.getHeight(); r++) {
            for (int c = 0; c < piece.getWidth(); c++) {
                if (shape[r][c]) g2.fillRoundRect(startX + c * size, startY + r * size, size - 5, size - 5, 17, 17);
            }
        }
    }

    private void drawPiece(Graphics2D g2, Piece piece, int startX, int startY, int size, boolean floating) {
        boolean[][] shape = piece.getShape();
        for (int r = 0; r < piece.getHeight(); r++) {
            for (int c = 0; c < piece.getWidth(); c++) {
                if (shape[r][c]) drawPremiumBlock(g2, startX + c * size, startY + r * size, size - 5, piece.getColor(), floating);
            }
        }
    }

    private void drawPremiumBlock(Graphics2D g2, int x, int y, int size, Color baseColor, boolean floating) {
        Color brighter = brighten(baseColor, state.getCurrentTheme().blockBrighten);
        Color darker = darken(baseColor, state.getCurrentTheme().blockDarken);

        if (floating) {
            g2.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), state.getCurrentTheme().floatGlowAlpha));
            g2.fillRoundRect(x - 5, y - 5, size + 10, size + 10, 22, 22);
        }

        GradientPaint block = new GradientPaint(x, y, brighter, x + size, y + size, darker);
        g2.setPaint(block);
        g2.fillRoundRect(x, y, size, size, 16, 16);

        g2.setColor(state.getCurrentTheme().blockHighlight);
        g2.fillRoundRect(x + 6, y + 6, size - 12, Math.max(8, size / 3), 12, 12);
        g2.setColor(state.getCurrentTheme().blockBorderLight);
        g2.drawRoundRect(x + 1, y + 1, size - 2, size - 2, 16, 16);
        g2.setColor(state.getCurrentTheme().blockBorderDark);
        g2.drawRoundRect(x, y, size, size, 16, 16);
    }

    private Color brighten(Color color, float factor) {
        return new Color(
                Math.min(255, (int) (color.getRed() * factor)),
                Math.min(255, (int) (color.getGreen() * factor)),
                Math.min(255, (int) (color.getBlue() * factor))
        );
    }

    private Color darken(Color color, float factor) {
        return new Color(
                Math.max(0, (int) (color.getRed() * factor)),
                Math.max(0, (int) (color.getGreen() * factor)),
                Math.max(0, (int) (color.getBlue() * factor))
        );
    }

    private void drawInstruction(Graphics2D g2) {
        g2.setFont(normalFont.deriveFont(Font.PLAIN, (float)(CELL_SIZE * 0.25)));
        g2.setColor(state.getCurrentTheme().subText);
    }

    private void drawGameOver(Graphics2D g2) {
        float t = Math.max(0f, Math.min(1f, state.getGameOverCurtain()));
        float eased = (float) (1 - Math.pow(1 - t, 3));

        int curtainHeight = (int) (PANEL_HEIGHT * eased);

        GradientPaint curtain = new GradientPaint(
                0, 0,
                new Color(8, 10, 18, 235),
                0, Math.max(1, curtainHeight),
                new Color(38, 28, 48, 242)
        );
        g2.setPaint(curtain);
        g2.fillRect(0, 0, PANEL_WIDTH, curtainHeight);

        if (curtainHeight > 0 && curtainHeight < PANEL_HEIGHT) {
            g2.setColor(new Color(0, 0, 0, 135));
            g2.fillRoundRect(0, curtainHeight - 16, PANEL_WIDTH, 32, 28, 28);
            g2.setColor(new Color(255, 255, 255, 40));
            g2.drawLine(0, curtainHeight - 14, PANEL_WIDTH, curtainHeight - 14);
        }

        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 45; i++) {
            int rx = (int) ((i * 97 + animationTime * 25) % (PANEL_WIDTH + 80)) - 40;
            int ry = (int) ((i * 61 + animationTime * 80) % (PANEL_HEIGHT + 120)) - 60;
            int a = (int) (70 * eased);
            g2.setColor(new Color(170, 195, 230, Math.max(0, Math.min(95, a))));
            g2.drawLine(rx, ry, rx - 8, ry + 18);
        }
        g2.setStroke(new BasicStroke(1f));

        if (t < 0.45f) return;

        float panelIn = Math.min(1f, (t - 0.45f) / 0.55f);
        float panelEase = (float) (1 - Math.pow(1 - panelIn, 3));

        int w = 540;
        int h = 270;
        int x = (PANEL_WIDTH - w) / 2;
        int finalY = (PANEL_HEIGHT - h) / 2;
        int y = (int) (-h + (finalY + h) * panelEase);

        g2.setColor(new Color(0, 0, 0, (int) (150 * panelEase)));
        g2.fillRoundRect(x + 10, y + 14, w, h, 38, 38);

        GradientPaint panel = new GradientPaint(
                x, y,
                new Color(58, 58, 88, (int) (235 * panelEase)),
                x + w, y + h,
                new Color(22, 24, 42, (int) (245 * panelEase))
        );
        g2.setPaint(panel);
        g2.fillRoundRect(x, y, w, h, 38, 38);

        g2.setColor(new Color(210, 220, 255, (int) (80 * panelEase)));
        g2.drawRoundRect(x, y, w, h, 38, 38);

        String gameOverText = "GAME OVER";
        g2.setFont(titleFont.deriveFont(Font.BOLD, 42f));
        FontMetrics titleMetrics = g2.getFontMetrics();
        int titleX = x + (w - titleMetrics.stringWidth(gameOverText)) / 2;

        g2.setColor(new Color(0, 0, 0, (int) (145 * panelEase)));
        g2.drawString(gameOverText, titleX + 4, y + 82);
        g2.setColor(new Color(235, 238, 255, (int) (255 * panelEase)));
        g2.drawString(gameOverText, titleX, y + 76);

        String finalScore = "Final Score: " + state.getScore();
        g2.setFont(gameFont.deriveFont(Font.BOLD, 25f));
        FontMetrics scoreMetrics = g2.getFontMetrics();
        int scoreX = x + (w - scoreMetrics.stringWidth(finalScore)) / 2;
        g2.setColor(new Color(255, 225, 120, (int) (255 * panelEase)));
        g2.drawString(finalScore, scoreX, y + 126);

        String sadLine = "The board is full... take a breath, then try again.";
        g2.setFont(normalFont.deriveFont(Font.PLAIN, 16f));
        FontMetrics sadMetrics = g2.getFontMetrics();
        int sadX = x + (w - sadMetrics.stringWidth(sadLine)) / 2;
        g2.setColor(new Color(225, 230, 245, (int) (225 * panelEase)));
        g2.drawString(sadLine, sadX, y + 160);

        Rectangle newButton = new Rectangle(gameOverNewGameButton.x, y + 194, gameOverNewGameButton.width, gameOverNewGameButton.height);
        Rectangle exitButton = new Rectangle(gameOverExitButton.x, y + 194, gameOverExitButton.width, gameOverExitButton.height);

        drawMenuChoiceButton(g2, newButton, "NEW GAME", state.getCurrentTheme().buttonA, state.getCurrentTheme().buttonB);
        drawMenuChoiceButton(g2, exitButton, "EXIT", new Color(255, 95, 120), new Color(170, 35, 65));
    }

    @Override
    public void onPieceSelected(int index, int mouseX, int mouseY) {
        selectedPieceIndex = index;
        dragging = true;
        dragMouseX = mouseX;
        dragMouseY = mouseY;
        int pw2 = (PANEL_WIDTH - 80) / 3;
        int areaX = 40 + index * pw2;
        int pieceW = pieces[index].getWidth() * PIECE_CELL_SIZE;
        int pieceH = pieces[index].getHeight() * PIECE_CELL_SIZE;
        dragOffsetX = mouseX - (areaX + (175 - pieceW) / 2);
        dragOffsetY = mouseY - (PIECE_AREA_Y + (85 - pieceH) / 2);
        updateHoverPosition();
        repaint();
    }
    @Override
    public void onDrag(int mouseX, int mouseY) {
        dragMouseX = mouseX; dragMouseY = mouseY;
        updateHoverPosition(); repaint();
    }
    @Override
    public void onDrop(int mouseX, int mouseY) {
        dragMouseX = mouseX; dragMouseY = mouseY;
        updateHoverPosition(); placeDraggedPiece(); dragging = false;
    }
    @Override public void onMenuToggle()  { state.setMenuOpen(!state.isMenuOpen()); repaint(); }
    @Override public void onMuteToggle()   { sound.toggleMute(); repaint(); }
    @Override public void onUndo()        { undoLastMove(); }
    @Override public void onNewGame()     { restartGame(); }
    @Override public void onExit()        { exitGame(); }
    @Override public void onMenuClose()   { state.setMenuOpen(false); repaint(); }
    @Override public void onHoverExit()   { hoverRow = -1; hoverCol = -1; repaint(); }
}
