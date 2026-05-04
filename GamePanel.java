import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GamePanel extends JPanel implements MouseListener, MouseMotionListener {
    private static final int PANEL_WIDTH = 720;
    private static final int PANEL_HEIGHT = 840;

    private static final int CELL_SIZE = 52;
    private static final int GAP = 7;
    private static final int BOARD_SIZE_PIXEL = Board.SIZE * CELL_SIZE;
    private static final int BOARD_X = (PANEL_WIDTH - BOARD_SIZE_PIXEL) / 2;
    private static final int BOARD_Y = 230;

    private static final int PIECE_AREA_Y = 720;
    private static final int PIECE_CELL_SIZE = 27;
    private static final int DRAG_CELL_SIZE = CELL_SIZE;

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

    private int score = 0;
    private int bestScore = 0;
    private final UndoManager undoManager = new UndoManager();
    private final ScoreStrategy scoreStrategy = new ClassicScoreStrategy();
    private boolean gameOver = false;
    private boolean menuOpen = false;
    private boolean canUndo = false;
    private float gameOverCurtain = 0f;
    private Color[][] previousBoardState;
    private Piece[] previousPiecesState;
    private int previousScore = 0;

    private final Rectangle menuButton = new Rectangle(486, 66, 68, 56);
    private final Rectangle undoButton = new Rectangle(602, 66, 68, 56);
    private final Rectangle menuPanel = new Rectangle(480, 142, 190, 136);
    private final Rectangle menuNewGameButton = new Rectangle(510, 167, 130, 38);
    private final Rectangle menuExitButton = new Rectangle(510, 220, 130, 38);
    private final Rectangle gameOverNewGameButton = new Rectangle(205, 500, 140, 46);
    private final Rectangle gameOverExitButton = new Rectangle(375, 500, 140, 46);

    private final Random random = new Random();
    private final GameObjectPool<AtmosphereParticle> atmosphereParticles = new GameObjectPool<AtmosphereParticle>();
    private final GameObjectPool<DebrisParticle> debrisParticles = new GameObjectPool<DebrisParticle>();
    private final GameObjectPool<ScoreText> scoreTexts = new GameObjectPool<ScoreText>();

    private float animationTime = 0f;
    private int comboPulse = 0;
    private LevelTheme currentTheme = LevelTheme.CALM;

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

        addMouseListener(this);
        addMouseMotionListener(this);
        rebuildAtmosphere();

        animationTimer = new Timer(16, e -> {
            animationTime += 0.026f;
            if (comboPulse > 0) comboPulse--;
            if (gameOver && gameOverCurtain < 1f) {
                gameOverCurtain = Math.min(1f, gameOverCurtain + 0.035f);
            }

            LevelTheme targetTheme = getThemeForScore(score);
            if (targetTheme != currentTheme) {
                currentTheme = targetTheme;
                rebuildAtmosphere();
            }

            for (AtmosphereParticle p : atmosphereParticles.getAll()) { p.setAnimationTime(animationTime); p.update(); }

            for (DebrisParticle p : debrisParticles.getAll()) p.setAnimationTime(animationTime);
            debrisParticles.updateAll();
            debrisParticles.removeDeadObjects();

            scoreTexts.updateAll();
            scoreTexts.removeDeadObjects();

            repaint();
        });

        animationTimer.start();
    }

    private void setupFonts() {
        titleFont = new Font("Cooper Black", Font.BOLD, 44);
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

    private LevelTheme getThemeForScore(int value) {
        if (value >= 5200) return LevelTheme.INFERNO;
        if (value >= 3200) return LevelTheme.FIRESTORM;
        if (value >= 1800) return LevelTheme.NEON_RUSH;
        if (value >= 800) return LevelTheme.GOLDEN_FLOW;
        return LevelTheme.CALM;
    }

    private String getLevelName() {
        return currentTheme.displayName;
    }

    private int getNextLevelScore() {
        if (score < 800) return 800;
        if (score < 1800) return 1800;
        if (score < 3200) return 3200;
        if (score < 5200) return 5200;
        return -1;
    }

    private double getProgressToNextLevel() {
        int next = getNextLevelScore();
        if (next == -1) return 1.0;

        int previous;
        if (next == 800) previous = 0;
        else if (next == 1800) previous = 800;
        else if (next == 3200) previous = 1800;
        else previous = 3200;

        return Math.max(0, Math.min(1, (score - previous) / (double) (next - previous)));
    }

    private void rebuildAtmosphere() {
        atmosphereParticles.clear();
        for (int i = 0; i < currentTheme.atmosphereCount; i++) {
            atmosphereParticles.add(new AtmosphereParticle(currentTheme, true, random, animationTime, PANEL_WIDTH, PANEL_HEIGHT));
        }
    }

    private void restartGame() {
        board.reset();
        pieces = PieceFactory.createThreePieces();
        score = 0;
        selectedPieceIndex = -1;
        dragging = false;
        hoverRow = -1;
        hoverCol = -1;
        gameOver = false;
        gameOverCurtain = 0f;
        menuOpen = false;
        canUndo = false;
        previousBoardState = null;
        previousPiecesState = null;
        comboPulse = 0;
        debrisParticles.clear();
        scoreTexts.clear();
        currentTheme = LevelTheme.CALM;
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
        previousBoardState = board.copyCells();
        previousPiecesState = pieces.clone();
        previousScore = score;
        canUndo = true;
    }

    private void undoLastMove() {
        if (!canUndo || previousBoardState == null || previousPiecesState == null) return;

        board.restoreCells(previousBoardState);
        pieces = previousPiecesState.clone();
        score = previousScore;
        currentTheme = getThemeForScore(score);
        rebuildAtmosphere();
        selectedPieceIndex = -1;
        dragging = false;
        hoverRow = -1;
        hoverCol = -1;
        gameOver = false;
        gameOverCurtain = 0f;
        menuOpen = false;
        scoreTexts.clear();
        debrisParticles.clear();
        canUndo = false;
        repaint();
    }

    private void startGameOver() {
        gameOver = true;
        dragging = false;
        selectedPieceIndex = -1;
        menuOpen = false;
        gameOverCurtain = 0f;
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

    private void placeDraggedPiece() {
        if (selectedPieceIndex == -1 || pieces[selectedPieceIndex] == null) {
            resetDragState();
            return;
        }

        Piece selectedPiece = pieces[selectedPieceIndex];

        if (hoverRow >= 0 && hoverCol >= 0 && board.canPlace(selectedPiece, hoverRow, hoverCol)) {
            saveUndoState();
            menuOpen = false;
            int moveScore = selectedPiece.getBlockCount() * 10;
            int feedbackX = BOARD_X + hoverCol * CELL_SIZE + selectedPiece.getWidth() * CELL_SIZE / 2;
            int feedbackY = BOARD_Y + hoverRow * CELL_SIZE + selectedPiece.getHeight() * CELL_SIZE / 2;

            try {
                board.placePiece(selectedPiece, hoverRow, hoverCol);
            } catch (InvalidPlacementException e) {
                resetDragState();
                return;
            }
            score += moveScore;

            boolean[][] marked = board.getFullLineCells();
            int clearedLines = board.countFullLines();

            if (board.hasMarkedCell(marked)) {
                createBreakingEffect(marked, clearedLines);
                board.clearMarkedCells(marked);

                int lineBonus = clearedLines * currentTheme.lineScore;
                int comboBonus = clearedLines >= 2 ? clearedLines * currentTheme.comboScore : 0;
                moveScore += lineBonus + comboBonus;
                score += lineBonus + comboBonus;

                comboPulse = 32 + clearedLines * 8;
                showMoveFeedback("PERFECT", feedbackX, feedbackY, moveScore, clearedLines);
            } else if (moveScore >= 50) {
                showMoveFeedback("EXCELLENT", feedbackX, feedbackY, moveScore, 0);
            } else {
                showMoveFeedback("GOOD", feedbackX, feedbackY, moveScore, 0);
            }

            bestScore = Math.max(bestScore, score);
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
            mainColor = currentTheme.scoreAccent;
            glowColor = new Color(255, 255, 255, 230);
            size = 38f;
        } else if ("EXCELLENT".equals(word)) {
            mainColor = currentTheme.bestAccent;
            glowColor = new Color(120, 235, 255, 210);
            size = 31f;
        } else {
            mainColor = new Color(255, 255, 255, 235);
            glowColor = currentTheme.subText;
            size = 26f;
        }

        int safeX = Math.max(120, Math.min(PANEL_WIDTH - 120, x));
        int safeY = Math.max(230, Math.min(BOARD_Y + BOARD_SIZE_PIXEL - 20, y));

        scoreTexts.add(new ScoreText(word, safeX, safeY, gainedScore, clearedLines, mainColor, glowColor, size));
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

                for (int i = 0; i < currentTheme.breakParticleCount + extra; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double speed = currentTheme.breakSpeedMin + random.nextDouble() * currentTheme.breakSpeedRange;
                    float vx = (float) Math.cos(angle) * (float) speed;
                    float vy = (float) Math.sin(angle) * (float) speed - currentTheme.breakUpForce;

                    Color particleColor = base;
                    if (currentTheme == LevelTheme.FIRESTORM || currentTheme == LevelTheme.INFERNO) {
                        particleColor = new Color(255, 70 + random.nextInt(170), 10);
                    } else if (currentTheme == LevelTheme.CALM) {
                        particleColor = new Color(235, 240, 245, 220);
                    }

                    debrisParticles.add(new DebrisParticle(
                            centerX, centerY, vx, vy,
                            currentTheme.breakMinSize + random.nextInt(currentTheme.breakSizeRange),
                            particleColor, currentTheme, random, animationTime
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
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        enableSmoothGraphics(g2);

        drawBackground(g2);
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
        if (menuOpen && !gameOver) drawInGameMenu(g2);

        if (gameOver) drawGameOver(g2);
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

        g2.setColor(new Color(0, 0, 0, currentTheme.darkOverlayAlpha));
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        RadialGradientPaint glow = new RadialGradientPaint(
                new Point(PANEL_WIDTH / 2, PANEL_HEIGHT / 2),
                520,
                new float[]{0f, 1f},
                new Color[]{currentTheme.centerGlow, transparent(currentTheme.centerGlow)}
        );
        g2.setPaint(glow);
        g2.fillOval(-120, -80, PANEL_WIDTH + 240, PANEL_HEIGHT + 160);
    }

    private void drawGeneratedScenery(Graphics2D g2) {
        if (currentTheme == LevelTheme.CALM) {
            drawGeneratedSakuraScenery(g2);
        } else if (currentTheme == LevelTheme.GOLDEN_FLOW) {
            drawGeneratedAutumnScenery(g2);
        } else if (currentTheme == LevelTheme.NEON_RUSH) {
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

        int flameCount = currentTheme == LevelTheme.INFERNO ? 16 : 12;
        int baseHeight = currentTheme == LevelTheme.INFERNO ? 150 : 120;
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
                    new Color(255, 235, 60, currentTheme == LevelTheme.INFERNO ? 155 : 125),
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
        GradientPaint gp = new GradientPaint(0, 0, currentTheme.bgTop, PANEL_WIDTH, PANEL_HEIGHT, currentTheme.bgBottom);
        g2.setPaint(gp);
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
    }

    private Color transparent(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
    }

    private void drawDynamicOverlay(Graphics2D g2) {
        if (currentTheme == LevelTheme.CALM) {
            drawSoftSakuraLight(g2);
        } else if (currentTheme == LevelTheme.GOLDEN_FLOW) {
            drawGoldenLeafLight(g2);
        } else if (currentTheme == LevelTheme.NEON_RUSH) {
            drawNeonRush(g2);
        } else {
            drawFireIntensity(g2);
        }

        if (comboPulse > 0) drawComboFlash(g2);
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
        int flameCount = currentTheme == LevelTheme.INFERNO ? 24 : 18;
        int baseHeight = currentTheme == LevelTheme.INFERNO ? 155 : 115;

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
                    new Color(255, 235, 70, currentTheme == LevelTheme.INFERNO ? 190 : 150),
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
                new Color[]{new Color(255, 80, 0, currentTheme == LevelTheme.INFERNO ? 130 : 95), new Color(255, 80, 0, 0)}
        );
        g2.setPaint(heat);
        g2.fillOval(-140, PANEL_HEIGHT - 460, PANEL_WIDTH + 280, 560);
    }

    private void drawComboFlash(Graphics2D g2) {
        int alpha = Math.min(120, comboPulse * 4);
        g2.setColor(new Color(currentTheme.flashColor.getRed(), currentTheme.flashColor.getGreen(), currentTheme.flashColor.getBlue(), alpha));
        g2.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
    }

    private void drawAtmosphereParticles(Graphics2D g2) {
        atmosphereParticles.drawAll(g2);
    }


    private void drawMenuAndUndoButtons(Graphics2D g2) {
        drawIconButton(g2, menuButton, "menu", true);
        drawIconButton(g2, undoButton, "undo", undoManager.canUndo());
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
                        new Color(currentTheme.buttonB.getRed(), currentTheme.buttonB.getGreen(), currentTheme.buttonB.getBlue(), glowAlpha),
                        new Color(currentTheme.buttonB.getRed(), currentTheme.buttonB.getGreen(), currentTheme.buttonB.getBlue(), 0)
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

        drawMenuChoiceButton(g2, menuNewGameButton, "NEW GAME", currentTheme.buttonA, currentTheme.buttonB);
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
        g2.setFont(titleFont);
        g2.setColor(new Color(0, 0, 0, 125));
        g2.drawString("BLOCK BLAST", 54, 70);

        GradientPaint titleGradient = new GradientPaint(52, 25, currentTheme.titleA, 355, 85, currentTheme.titleB);
        g2.setPaint(titleGradient);
        g2.drawString("BLOCK BLAST", 50, 66);

        g2.setFont(normalFont.deriveFont(Font.BOLD, 15f));
        g2.setColor(currentTheme.subText);
        g2.drawString(getLevelName(), 58, 96);

        drawScoreCard(g2, 58, 110, 155, 48, "SCORE", String.valueOf(score), currentTheme.scoreAccent);
        drawScoreCard(g2, 230, 110, 145, 48, "BEST", String.valueOf(bestScore), currentTheme.bestAccent);
    }

    private void drawScoreCard(Graphics2D g2, int x, int y, int w, int h, String label, String value, Color accent) {
        g2.setColor(new Color(0, 0, 0, currentTheme.shadowAlpha));
        g2.fillRoundRect(x + 5, y + 6, w, h, 22, 22);

        GradientPaint card = new GradientPaint(x, y, currentTheme.cardTop, x, y + h, currentTheme.cardBottom);
        g2.setPaint(card);
        g2.fillRoundRect(x, y, w, h, 22, 22);

        g2.setColor(currentTheme.cardBorder);
        g2.drawRoundRect(x, y, w, h, 22, 22);

        g2.setFont(gameFont.deriveFont(Font.BOLD, 12f));
        g2.setColor(currentTheme.cardLabel);
        g2.drawString(label, x + 18, y + 19);

        g2.setFont(gameFont.deriveFont(Font.BOLD, 22f));
        g2.setColor(accent);
        g2.drawString(value, x + 70, y + 37);
    }

    private void drawSmallButton(Graphics2D g2, Rectangle rect, String text) {
        g2.setColor(new Color(0, 0, 0, currentTheme.shadowAlpha + 15));
        g2.fillRoundRect(rect.x + 5, rect.y + 7, rect.width, rect.height, 24, 24);

        GradientPaint button = new GradientPaint(rect.x, rect.y, currentTheme.buttonA, rect.x + rect.width, rect.y + rect.height, currentTheme.buttonB);
        g2.setPaint(button);
        g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 24, 24);

        g2.setColor(currentTheme.buttonBorder);
        g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 24, 24);

        g2.setFont(gameFont.deriveFont(Font.BOLD, 15f));
        FontMetrics fm = g2.getFontMetrics();
        int tx = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int ty = rect.y + rect.height / 2 + 6;
        g2.setColor(currentTheme.buttonText);
        g2.drawString(text, tx, ty);
    }

    private void drawLevelBar(Graphics2D g2) {
        int x = 398;
        int y = 148;
        int w = 270;
        int h = 18;
        double progress = getProgressToNextLevel();

        g2.setColor(new Color(0, 0, 0, currentTheme.shadowAlpha));
        g2.fillRoundRect(x + 3, y + 4, w, h, 15, 15);

        g2.setColor(new Color(255, 255, 255, 28));
        g2.fillRoundRect(x, y, w, h, 15, 15);

        GradientPaint fill = new GradientPaint(x, y, currentTheme.buttonA, x + w, y, currentTheme.buttonB);
        g2.setPaint(fill);
        g2.fillRoundRect(x, y, (int) (w * progress), h, 15, 15);

        g2.setColor(new Color(255, 255, 255, 70));
        g2.drawRoundRect(x, y, w, h, 15, 15);

        g2.setFont(normalFont.deriveFont(Font.BOLD, 11f));
        String text = getNextLevelScore() == -1 ? "MAX LEVEL" : "NEXT: " + getNextLevelScore();
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(currentTheme.mainText);
        g2.drawString(text, x + (w - fm.stringWidth(text)) / 2, y - 5);
    }

    private void drawBoardShell(Graphics2D g2) {
        int shellX = BOARD_X - 24;
        int shellY = BOARD_Y - 24;
        int shellW = BOARD_SIZE_PIXEL + 48;
        int shellH = BOARD_SIZE_PIXEL + 48;

        g2.setColor(new Color(0, 0, 0, currentTheme.shadowAlpha + 25));
        g2.fillRoundRect(shellX + 10, shellY + 14, shellW, shellH, 40, 40);

        GradientPaint outer = new GradientPaint(shellX, shellY, currentTheme.boardGlowA, shellX + shellW, shellY + shellH, currentTheme.boardGlowB);
        g2.setPaint(outer);
        g2.fillRoundRect(shellX, shellY, shellW, shellH, 40, 40);

        g2.setColor(currentTheme.boardInner);
        g2.fillRoundRect(shellX + 4, shellY + 4, shellW - 8, shellH - 8, 36, 36);

        g2.setColor(currentTheme.boardLine);
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
        g2.setColor(currentTheme.emptyCell);
        g2.fillRoundRect(x, y, size, size, 15, 15);
        g2.setColor(currentTheme.emptyCellBorder);
        g2.drawRoundRect(x, y, size, size, 15, 15);
        g2.setColor(new Color(0, 0, 0, currentTheme.innerCellShadow));
        g2.fillRoundRect(x + 4, y + 4, size - 8, size - 8, 13, 13);
    }

    private void drawPreview(Graphics2D g2) {
        if (!dragging || selectedPieceIndex == -1 || pieces[selectedPieceIndex] == null || hoverRow < 0 || hoverCol < 0) return;

        Piece piece = pieces[selectedPieceIndex];
        boolean canPlace = board.canPlace(piece, hoverRow, hoverCol);
        boolean[][] shape = piece.getShape();
        Color previewColor = canPlace ? currentTheme.validPreview : currentTheme.invalidPreview;
        Color outlineColor = canPlace ? currentTheme.validOutline : currentTheme.invalidOutline;

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
        g2.setColor(currentTheme.mainText);
        g2.setFont(gameFont.deriveFont(Font.BOLD, 20f));
        g2.drawString("Drag blocks to board", 55, 690);

        g2.setFont(normalFont.deriveFont(Font.PLAIN, 13f));
        g2.setColor(currentTheme.subText);
        g2.drawString(currentTheme.dockHint, 55, 710);

        for (int i = 0; i < pieces.length; i++) {
            int areaX = 58 + i * 210;
            int areaY = PIECE_AREA_Y;
            pieceBounds[i] = new Rectangle(areaX, areaY, 175, 85);
            drawDockSlot(g2, areaX, areaY, 175, 85, i == selectedPieceIndex && dragging);

            if (pieces[i] != null) {
                if (!(i == selectedPieceIndex && dragging)) drawPieceCentered(g2, pieces[i], areaX, areaY, 175, 85, PIECE_CELL_SIZE);
            } else {
                g2.setColor(currentTheme.usedText);
                g2.setFont(gameFont.deriveFont(Font.BOLD, 18f));
                g2.drawString("USED", areaX + 61, areaY + 51);
            }
        }
    }

    private void drawDockSlot(Graphics2D g2, int x, int y, int w, int h, boolean active) {
        g2.setColor(new Color(0, 0, 0, currentTheme.shadowAlpha));
        g2.fillRoundRect(x + 5, y + 8, w, h, 28, 28);

        Color top = active ? currentTheme.dockActiveA : currentTheme.dockIdleA;
        Color bottom = active ? currentTheme.dockActiveB : currentTheme.dockIdleB;
        GradientPaint gp = new GradientPaint(x, y, top, x, y + h, bottom);
        g2.setPaint(gp);
        g2.fillRoundRect(x, y, w, h, 28, 28);

        g2.setColor(active ? currentTheme.dockActiveBorder : currentTheme.dockBorder);
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
        g2.setColor(new Color(0, 0, 0, currentTheme.shadowAlpha + 30));
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
        Color brighter = brighten(baseColor, currentTheme.blockBrighten);
        Color darker = darken(baseColor, currentTheme.blockDarken);

        if (floating) {
            g2.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), currentTheme.floatGlowAlpha));
            g2.fillRoundRect(x - 5, y - 5, size + 10, size + 10, 22, 22);
        }

        GradientPaint block = new GradientPaint(x, y, brighter, x + size, y + size, darker);
        g2.setPaint(block);
        g2.fillRoundRect(x, y, size, size, 16, 16);

        g2.setColor(currentTheme.blockHighlight);
        g2.fillRoundRect(x + 6, y + 6, size - 12, Math.max(8, size / 3), 12, 12);
        g2.setColor(currentTheme.blockBorderLight);
        g2.drawRoundRect(x + 1, y + 1, size - 2, size - 2, 16, 16);
        g2.setColor(currentTheme.blockBorderDark);
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
        g2.setFont(normalFont.deriveFont(Font.PLAIN, 13f));
        g2.setColor(currentTheme.subText);
        g2.drawString("The higher your score, the stronger the world becomes.", 182, 830);
    }

    private void drawGameOver(Graphics2D g2) {
        float t = Math.max(0f, Math.min(1f, gameOverCurtain));
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

        String finalScore = "Final Score: " + score;
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

        drawMenuChoiceButton(g2, newButton, "NEW GAME", currentTheme.buttonA, currentTheme.buttonB);
        drawMenuChoiceButton(g2, exitButton, "EXIT", new Color(255, 95, 120), new Color(170, 35, 65));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (gameOver) {
            if (gameOverCurtain >= 0.72f) {
                int panelY = (PANEL_HEIGHT - 270) / 2;
                Rectangle newButton = new Rectangle(gameOverNewGameButton.x, panelY + 194, gameOverNewGameButton.width, gameOverNewGameButton.height);
                Rectangle exitButton = new Rectangle(gameOverExitButton.x, panelY + 194, gameOverExitButton.width, gameOverExitButton.height);
                if (newButton.contains(e.getPoint())) {
                    restartGame();
                } else if (exitButton.contains(e.getPoint())) {
                    exitGame();
                }
            }
            return;
        }

        if (menuOpen) {
            if (menuNewGameButton.contains(e.getPoint())) {
                restartGame();
                return;
            }
            if (menuExitButton.contains(e.getPoint())) {
                exitGame();
                return;
            }
            if (!menuPanel.contains(e.getPoint()) && !menuButton.contains(e.getPoint())) {
                menuOpen = false;
                repaint();
            }
        }

        if (menuButton.contains(e.getPoint())) {
            menuOpen = !menuOpen;
            repaint();
            return;
        }

        if (undoButton.contains(e.getPoint())) {
            undoLastMove();
            return;
        }


        for (int i = 0; i < pieces.length; i++) {
            if (pieces[i] != null && pieceBounds[i] != null && pieceBounds[i].contains(e.getPoint())) {
                selectedPieceIndex = i;
                dragging = true;
                dragMouseX = e.getX();
                dragMouseY = e.getY();

                int areaX = 58 + i * 210;
                int areaY = PIECE_AREA_Y;
                int pieceW = pieces[i].getWidth() * PIECE_CELL_SIZE;
                int pieceH = pieces[i].getHeight() * PIECE_CELL_SIZE;
                int pieceDrawX = areaX + (175 - pieceW) / 2;
                int pieceDrawY = areaY + (85 - pieceH) / 2;

                dragOffsetX = e.getX() - pieceDrawX;
                dragOffsetY = e.getY() - pieceDrawY;
                updateHoverPosition();
                repaint();
                return;
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (!dragging) return;
        dragMouseX = e.getX();
        dragMouseY = e.getY();
        updateHoverPosition();
        repaint();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!dragging) return;
        dragMouseX = e.getX();
        dragMouseY = e.getY();
        updateHoverPosition();
        placeDraggedPiece();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (!dragging) {
            hoverRow = -1;
            hoverCol = -1;
            repaint();
        }
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}


    private class ScoreText implements Drawable {
        private final String word;
        private final int gainedScore;
        private final int clearedLines;
        private final Color mainColor;
        private final Color glowColor;
        private final float baseSize;
        private final float startX;
        private final float startY;
        private final float direction;
        private float x;
        private float y;
        private float vx;
        private float vy;
        private float life;
        private float age;
        private float rotation;

        public ScoreText(String word, int x, int y, int gainedScore, int clearedLines, Color mainColor, Color glowColor, float baseSize) {
            this.word = word;
            this.x = x;
            this.y = y;
            this.startX = x;
            this.startY = y;
            this.gainedScore = gainedScore;
            this.clearedLines = clearedLines;
            this.mainColor = mainColor;
            this.glowColor = glowColor;
            this.baseSize = baseSize;
            this.direction = random.nextBoolean() ? 1f : -1f;
            this.vx = direction * (0.20f + random.nextFloat() * 0.25f);
            this.vy = -2.35f;
            this.life = 1.0f;
            this.age = 0f;
            this.rotation = direction * (-0.05f + random.nextFloat() * 0.08f);
        }

        public void update() {
            age += 0.055f;

            // Chữ bay lên, hơi lướt ngang và chậm dần như hiệu ứng arcade/fighting game.
            x += vx;
            y += vy;
            vx *= 0.970f;
            vy *= 0.955f;

            // PERFECT giữ lâu hơn một chút, GOOD biến mất nhanh hơn để không che board.
            float fade = "PERFECT".equals(word) ? 0.016f : "EXCELLENT".equals(word) ? 0.019f : 0.022f;
            life -= fade;
        }

        public boolean isDead() {
            return life <= 0f;
        }

        public void draw(Graphics2D g2) {
            if (life <= 0) return;

            Graphics2D copy = (Graphics2D) g2.create();
            copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            copy.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            float intro = Math.min(1f, age / 0.32f);
            float outro = Math.max(0f, Math.min(1f, life / 0.42f));
            float punch = (float) Math.sin(intro * Math.PI);

            float scale;
            if (intro < 1f) {
                // Bật mạnh lúc vừa hiện ra.
                scale = 0.45f + intro * 0.88f + punch * 0.28f;
            } else {
                // Sau đó thu nhỏ rất nhẹ khi tan biến.
                scale = 1.0f + Math.max(0f, 0.16f * life);
            }

            int alpha = Math.max(0, Math.min(255, (int) (255 * Math.min(1f, life * 1.45f))));
            int glowAlpha = Math.max(0, Math.min(220, (int) (alpha * 0.72f)));

            Font wordFont = titleFont.deriveFont(Font.BOLD, baseSize * scale);
            copy.setFont(wordFont);
            FontMetrics fm = copy.getFontMetrics();
            int textW = fm.stringWidth(word);
            int textH = fm.getAscent();

            float centerX = x + (float) Math.sin(age * 9.0f) * 2.0f * life;
            float centerY = y;
            int textX = (int) (centerX - textW / 2.0f);
            int textY = (int) centerY;

            copy.translate(centerX, centerY - textH / 2.0f);
            copy.rotate(rotation * life);
            copy.translate(-centerX, -(centerY - textH / 2.0f));

            // Vệt sáng quét ngang phía sau chữ, nhìn giống chữ combo trong game đối kháng.
            int slashW = textW + 96;
            int slashH = Math.max(32, textH + 8);
            int slashX = (int) (centerX - slashW / 2.0f);
            int slashY = (int) (centerY - textH + 3);

            float sweep = Math.min(1f, age / 0.55f);
            int sweepOffset = (int) ((sweep - 0.5f) * 80);

            Path2D slash = new Path2D.Double();
            slash.moveTo(slashX + 34 + sweepOffset, slashY);
            slash.lineTo(slashX + slashW, slashY + 3);
            slash.lineTo(slashX + slashW - 34, slashY + slashH);
            slash.lineTo(slashX, slashY + slashH - 3);
            slash.closePath();

            GradientPaint slashPaint = new GradientPaint(
                    slashX, slashY,
                    new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), Math.min(105, glowAlpha / 2)),
                    slashX + slashW, slashY + slashH,
                    new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), Math.min(55, glowAlpha / 3))
            );
            copy.setPaint(slashPaint);
            copy.fill(slash);

            copy.setStroke(new BasicStroke(2.2f));
            copy.setColor(new Color(255, 255, 255, Math.min(145, glowAlpha / 2)));
            copy.draw(slash);
            copy.setStroke(new BasicStroke(1f));

            // Shockwave vòng ngoài khi vừa xuất hiện.
            if (age < 0.55f) {
                float ring = age / 0.55f;
                int rw = (int) ((textW + 30) * (0.78f + ring * 0.55f));
                int rh = (int) ((textH + 24) * (0.82f + ring * 0.40f));
                int ra = (int) ((1f - ring) * 150);
                copy.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), Math.max(0, ra)));
                copy.setStroke(new BasicStroke(3.2f));
                copy.drawOval((int) centerX - rw / 2, (int) centerY - textH / 2 - rh / 2 + 10, rw, rh);
                copy.setStroke(new BasicStroke(1f));
            }

            // Các tia năng lượng nhỏ bung ra rồi tan dần.
            int sparkCount = "PERFECT".equals(word) ? 18 : "EXCELLENT".equals(word) ? 12 : 8;
            for (int i = 0; i < sparkCount; i++) {
                double angle = (Math.PI * 2 * i / sparkCount) + age * 0.9;
                float dist = (26f + (i % 4) * 9f) * (0.35f + intro) * life;
                int sx = (int) (centerX + Math.cos(angle) * dist);
                int sy = (int) (centerY - textH / 2 + Math.sin(angle) * dist);
                int sparkA = Math.max(0, Math.min(180, (int) (alpha * 0.65f * outro)));
                copy.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), sparkA));
                copy.fillOval(sx - 3, sy - 3, 6, 6);
            }

            // Glow nhiều lớp sau chữ.
            copy.setFont(wordFont);
            for (int i = 10; i >= 2; i -= 2) {
                int a = Math.max(0, Math.min(75, glowAlpha / (i / 2 + 1)));
                copy.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), a));
                copy.drawString(word, textX - i / 2, textY);
                copy.drawString(word, textX + i / 2, textY);
                copy.drawString(word, textX, textY - i / 2);
                copy.drawString(word, textX, textY + i / 2);
            }

            // Bóng đen để chữ nổi khối.
            copy.setColor(new Color(0, 0, 0, Math.min(185, alpha)));
            copy.drawString(word, textX + 5, textY + 6);

            // Viền trắng dày giả lập bằng nhiều lần vẽ lệch.
            copy.setColor(new Color(255, 255, 255, alpha));
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    if (Math.abs(dx) + Math.abs(dy) <= 3) {
                        copy.drawString(word, textX + dx, textY + dy);
                    }
                }
            }

            // Màu chính ở giữa.
            GradientPaint textPaint = new GradientPaint(
                    textX, textY - textH,
                    new Color(255, 255, 255, alpha),
                    textX, textY,
                    new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), alpha)
            );
            copy.setPaint(textPaint);
            copy.drawString(word, textX, textY);

            // Điểm cộng nhỏ bay theo bên dưới, tan nhanh hơn chữ chính.
            String bonus = "+" + gainedScore;
            if (clearedLines > 1) bonus += "  COMBO x" + clearedLines;

            copy.setFont(gameFont.deriveFont(Font.BOLD, 16f * Math.max(0.75f, scale * 0.82f)));
            FontMetrics bfm = copy.getFontMetrics();
            int bonusX = (int) (centerX - bfm.stringWidth(bonus) / 2.0f);
            int bonusY = (int) (centerY + 24 + (1f - life) * 18);

            copy.setColor(new Color(0, 0, 0, Math.min(150, alpha)));
            copy.drawString(bonus, bonusX + 2, bonusY + 2);
            copy.setColor(new Color(255, 255, 255, Math.min(240, alpha)));
            copy.drawString(bonus, bonusX, bonusY);

            copy.dispose();
        }
    }


    public enum LevelTheme {
        CALM(
                "Calm Bloom", "Soft start. Relax and build your score.",
                new Color(28, 34, 56), new Color(78, 65, 95), new Color(255, 190, 225, 90),
                new Color(255, 245, 255), new Color(185, 232, 255), new Color(255, 236, 166), new Color(185, 224, 255),
                new Color(245, 245, 255), new Color(220, 225, 255),
                new Color(255,255,255,42), new Color(255,255,255,16), new Color(255,255,255,48), new Color(225,232,255),
                new Color(255,148,190), new Color(170,218,255), Color.WHITE, new Color(255,255,255,80),
                new Color(166,215,255,105), new Color(255,160,215,90), new Color(22,28,52,240),
                new Color(180,215,255,65), new Color(255,180,215,55), new Color(210,230,255,100),
                new Color(255,255,255,16), new Color(255,255,255,32),
                new Color(120,255,190,125), new Color(255,120,150,120), new Color(165,255,210,205), new Color(255,150,170,205),
                new Color(255,255,255,92), new Color(255,255,255,105), new Color(0,0,0,45),
                new Color(70,72,130,240), new Color(42,45,82,245), new Color(255,255,255,75), Color.WHITE,
                new Color(255, 235, 245), 85, 80, 30, 1.22f, 0.72f, 70,
                10, 1.4, 2.6, 0.6f, 7, 11, 0.06f, 0.988f, 0.018f, 140, 110, 70
        ),
        GOLDEN_FLOW(
                "Golden Flow", "The board warms up as your rhythm grows.",
                new Color(29,38,48), new Color(54,72,62), new Color(255, 220, 125, 75),
                new Color(255,255,235), new Color(146,232,200), new Color(255,235,125), new Color(150,225,195),
                new Color(245,250,235), new Color(230,240,225),
                new Color(255,255,255,40), new Color(255,255,255,16), new Color(255,255,255,48), new Color(230,240,225),
                new Color(255,175,95), new Color(125,205,175), new Color(255,255,240), new Color(255,255,255,75),
                new Color(130,210,185,95), new Color(255,210,100,80), new Color(21,32,39,242),
                new Color(140,205,180,60), new Color(255,220,120,50), new Color(220,238,225,100),
                new Color(255,255,255,16), new Color(255,255,255,34),
                new Color(110,245,175,120), new Color(255,105,120,125), new Color(165,255,205,205), new Color(255,150,150,205),
                new Color(255,255,255,90), new Color(255,255,255,100), new Color(0,0,0,42),
                new Color(64,88,78,240), new Color(40,57,55,245), new Color(255,255,240,75), Color.WHITE,
                new Color(255, 220, 130), 90, 85, 28, 1.20f, 0.74f, 75,
                12, 1.6, 3.0, 0.9f, 7, 10, 0.12f, 0.982f, 0.023f, 150, 120, 88
        ),
        NEON_RUSH(
                "Neon Rush", "Energy rises. The world shifts into high speed.",
                new Color(8,10,27), new Color(22,22,57), new Color(90, 220, 255, 90),
                Color.WHITE, new Color(116,229,255), new Color(255,225,91), new Color(116,229,255),
                new Color(225,232,255), new Color(120,235,255),
                new Color(255,255,255,48), new Color(255,255,255,16), new Color(255,255,255,45), new Color(205,214,246),
                new Color(255,107,121), new Color(255,68,169), Color.WHITE, new Color(255,255,255,90),
                new Color(80,172,255,125), new Color(255,78,168,115), new Color(15,19,42,245),
                new Color(95,180,255,78), new Color(255,90,170,68), new Color(150,220,255,125),
                new Color(255,255,255,12), new Color(255,255,255,22),
                new Color(70,255,170,135), new Color(255,70,100,140), new Color(115,255,190,225), new Color(255,95,120,225),
                new Color(255,255,255,95), new Color(255,255,255,105), new Color(0,0,0,60),
                new Color(65,67,140,240), new Color(37,31,78,245), new Color(255,255,255,55), Color.WHITE,
                new Color(140, 235, 255), 105, 115, 30, 1.28f, 0.68f, 85,
                15, 2.4, 4.2, 1.5f, 7, 10, 0.18f, 0.976f, 0.026f, 165, 135, 95
        ),
        FIRESTORM(
                "Firestorm", "Every combo hits harder. The board starts to burn.",
                new Color(20,6,5), new Color(95,18,7), new Color(255, 80, 0, 105),
                new Color(255,245,190), new Color(255,75,20), new Color(255,225,90), new Color(255,130,55),
                new Color(255,245,225), new Color(255,220,145),
                new Color(255,255,255,48), new Color(255,255,255,18), new Color(255,255,255,58), new Color(255,230,180),
                new Color(255,65,20), new Color(255,170,35), Color.WHITE, new Color(255,255,255,95),
                new Color(255,65,20,170), new Color(255,175,25,145), new Color(28,10,12,248),
                new Color(255,100,25,115), new Color(255,190,50,95), new Color(255,180,80,145),
                new Color(255,255,255,13), new Color(255,255,255,32),
                new Color(60,255,140,135), new Color(255,55,55,150), new Color(110,255,185,225), new Color(255,115,80,235),
                new Color(255,255,255,100), new Color(255,255,255,125), new Color(0,0,0,80),
                new Color(130,34,25,245), new Color(48,8,10,245), new Color(255,210,100,100), Color.WHITE,
                new Color(255, 120, 35), 145, 105, 50, 1.38f, 0.56f, 120,
                26, 4.2, 7.5, 3.0f, 9, 13, 0.28f, 0.960f, 0.036f, 185, 170, 130
        ),
        INFERNO(
                "Inferno Burst", "Maximum power. Every clear erupts.",
                new Color(10,0,0), new Color(120,10,0), new Color(255, 50, 0, 135),
                new Color(255,245,180), new Color(255,45,10), new Color(255,230,65), new Color(255,85,35),
                new Color(255,245,225), new Color(255,210,120),
                new Color(255,255,255,55), new Color(255,255,255,20), new Color(255,255,255,65), new Color(255,230,180),
                new Color(255,45,20), new Color(255,200,35), Color.WHITE, new Color(255,255,255,105),
                new Color(255,35,10,190), new Color(255,205,30,160), new Color(24,4,6,250),
                new Color(255,70,15,140), new Color(255,210,35,120), new Color(255,190,60,170),
                new Color(255,255,255,15), new Color(255,255,255,36),
                new Color(90,255,150,150), new Color(255,30,35,165), new Color(130,255,190,240), new Color(255,110,70,245),
                new Color(255,255,255,112), new Color(255,255,255,135), new Color(0,0,0,90),
                new Color(145,20,20,250), new Color(45,0,0,250), new Color(255,230,90,120), Color.WHITE,
                new Color(255, 165, 30), 170, 120, 56, 1.45f, 0.50f, 135,
                36, 5.2, 9.0, 4.0f, 10, 16, 0.34f, 0.950f, 0.040f, 210, 195, 150
        );

        final String displayName;
        final String dockHint;
        final Color bgTop;
        final Color bgBottom;
        final Color centerGlow;
        final Color titleA;
        final Color titleB;
        final Color scoreAccent;
        final Color bestAccent;
        final Color mainText;
        final Color subText;
        final Color cardTop;
        final Color cardBottom;
        final Color cardBorder;
        final Color cardLabel;
        final Color buttonA;
        final Color buttonB;
        final Color buttonText;
        final Color buttonBorder;
        final Color boardGlowA;
        final Color boardGlowB;
        final Color boardInner;
        final Color dockActiveA;
        final Color dockActiveB;
        final Color dockActiveBorder;
        final Color emptyCell;
        final Color emptyCellBorder;
        final Color validPreview;
        final Color invalidPreview;
        final Color validOutline;
        final Color invalidOutline;
        final Color blockHighlight;
        final Color blockBorderLight;
        final Color blockBorderDark;
        final Color gameOverA;
        final Color gameOverB;
        final Color gameOverBorder;
        final Color gameOverTitle;
        final Color flashColor;
        final int shadowAlpha;
        final int darkOverlayAlpha;
        final int innerCellShadow;
        final float blockBrighten;
        final float blockDarken;
        final int floatGlowAlpha;
        final int breakParticleCount;
        final double breakSpeedMin;
        final double breakSpeedRange;
        final float breakUpForce;
        final int breakMinSize;
        final int breakSizeRange;
        final float gravity;
        final float friction;
        final float fadeSpeed;
        final int lineScore;
        final int comboScore;
        final int atmosphereCount;
        final Color boardLine;
        final Color dockIdleA;
        final Color dockIdleB;
        final Color dockBorder;
        final Color usedText;

        LevelTheme(
                String displayName,
                String dockHint,
                Color bgTop,
                Color bgBottom,
                Color centerGlow,
                Color titleA,
                Color titleB,
                Color scoreAccent,
                Color bestAccent,
                Color mainText,
                Color subText,
                Color cardTop,
                Color cardBottom,
                Color cardBorder,
                Color cardLabel,
                Color buttonA,
                Color buttonB,
                Color buttonText,
                Color buttonBorder,
                Color boardGlowA,
                Color boardGlowB,
                Color boardInner,
                Color dockActiveA,
                Color dockActiveB,
                Color dockActiveBorder,
                Color emptyCell,
                Color emptyCellBorder,
                Color validPreview,
                Color invalidPreview,
                Color validOutline,
                Color invalidOutline,
                Color blockHighlight,
                Color blockBorderLight,
                Color blockBorderDark,
                Color gameOverA,
                Color gameOverB,
                Color gameOverBorder,
                Color gameOverTitle,
                Color flashColor,
                int shadowAlpha,
                int darkOverlayAlpha,
                int innerCellShadow,
                float blockBrighten,
                float blockDarken,
                int floatGlowAlpha,
                int breakParticleCount,
                double breakSpeedMin,
                double breakSpeedRange,
                float breakUpForce,
                int breakMinSize,
                int breakSizeRange,
                float gravity,
                float friction,
                float fadeSpeed,
                int lineScore,
                int comboScore,
                int atmosphereCount
        ) {
            this.displayName = displayName;
            this.dockHint = dockHint;
            this.bgTop = bgTop;
            this.bgBottom = bgBottom;
            this.centerGlow = centerGlow;
            this.titleA = titleA;
            this.titleB = titleB;
            this.scoreAccent = scoreAccent;
            this.bestAccent = bestAccent;
            this.mainText = mainText;
            this.subText = subText;
            this.cardTop = cardTop;
            this.cardBottom = cardBottom;
            this.cardBorder = cardBorder;
            this.cardLabel = cardLabel;
            this.buttonA = buttonA;
            this.buttonB = buttonB;
            this.buttonText = buttonText;
            this.buttonBorder = buttonBorder;
            this.boardGlowA = boardGlowA;
            this.boardGlowB = boardGlowB;
            this.boardInner = boardInner;
            this.dockActiveA = dockActiveA;
            this.dockActiveB = dockActiveB;
            this.dockActiveBorder = dockActiveBorder;
            this.emptyCell = emptyCell;
            this.emptyCellBorder = emptyCellBorder;
            this.validPreview = validPreview;
            this.invalidPreview = invalidPreview;
            this.validOutline = validOutline;
            this.invalidOutline = invalidOutline;
            this.blockHighlight = blockHighlight;
            this.blockBorderLight = blockBorderLight;
            this.blockBorderDark = blockBorderDark;
            this.gameOverA = gameOverA;
            this.gameOverB = gameOverB;
            this.gameOverBorder = gameOverBorder;
            this.gameOverTitle = gameOverTitle;
            this.flashColor = flashColor;
            this.shadowAlpha = shadowAlpha;
            this.darkOverlayAlpha = darkOverlayAlpha;
            this.innerCellShadow = innerCellShadow;
            this.blockBrighten = blockBrighten;
            this.blockDarken = blockDarken;
            this.floatGlowAlpha = floatGlowAlpha;
            this.breakParticleCount = breakParticleCount;
            this.breakSpeedMin = breakSpeedMin;
            this.breakSpeedRange = breakSpeedRange;
            this.breakUpForce = breakUpForce;
            this.breakMinSize = breakMinSize;
            this.breakSizeRange = breakSizeRange;
            this.gravity = gravity;
            this.friction = friction;
            this.fadeSpeed = fadeSpeed;
            this.lineScore = lineScore;
            this.comboScore = comboScore;
            this.atmosphereCount = atmosphereCount;

            this.boardLine = cardBorder;
            this.dockIdleA = cardTop;
            this.dockIdleB = cardBottom;
            this.dockBorder = cardBorder;
            this.usedText = subText;
        }
    }
}

