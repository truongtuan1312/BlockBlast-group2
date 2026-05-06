/**
 * GameState — Single Responsibility: only manages game state.
 * Does not draw, does not handle input.
 */
public class GameState {
    private int score = 0;
    private int bestScore = 0;
    private boolean gameOver = false;
    private boolean menuOpen = false;
    private float gameOverCurtain = 0f;
    private int comboPulse = 0;
    private LevelTheme currentTheme = LevelTheme.CALM;

    private final UndoManager undoManager = new UndoManager();
    private final ScoreStrategy scoreStrategy = new ClassicScoreStrategy();

    public void reset() {
        score = 0;
        gameOver = false;
        menuOpen = false;
        gameOverCurtain = 0f;
        comboPulse = 0;
        currentTheme = LevelTheme.CALM;
    }

    public void resetScore(int value) {
        score = value;
        if (score > bestScore) bestScore = score;
        currentTheme = getThemeForScore(score);
    }

    public void addScore(int delta) {
        score += delta;
        if (score > bestScore) bestScore = score;
    }

    public int calculateMoveScore(Piece piece) {
        return scoreStrategy.calculateMoveScore(piece);
    }

    public int calculateLineScore(int clearedLines, int comboScore, int lineScore) {
        return scoreStrategy.calculateLineScore(clearedLines, comboScore, lineScore);
    }

    public void saveUndo(Board board, Piece[] pieces) {
        undoManager.save(new GameStateMemento(board.copyCells(), pieces, score));
    }

    public GameStateMemento restoreUndo() {
        return undoManager.restore();
    }

    public boolean canUndo() {
        return undoManager.canUndo();
    }

    public LevelTheme getThemeForScore(int value) {
        if (value >= 5200) return LevelTheme.INFERNO;
        if (value >= 3200) return LevelTheme.FIRESTORM;
        if (value >= 1800) return LevelTheme.NEON_RUSH;
        if (value >= 800)  return LevelTheme.GOLDEN_FLOW;
        return LevelTheme.CALM;
    }

    public int getNextLevelScore() {
        if (score < 800)  return 800;
        if (score < 1800) return 1800;
        if (score < 3200) return 3200;
        if (score < 5200) return 5200;
        return -1;
    }

    public double getProgressToNextLevel() {
        int next = getNextLevelScore();
        if (next == -1) return 1.0;
        int prev = next == 800 ? 0 : next == 1800 ? 300 : next == 3200 ? 1800 : 3200;
        return Math.max(0, Math.min(1, (score - prev) / (double)(next - prev)));
    }

    public int getScore()                   { return score; }
    public int getBestScore()               { return bestScore; }
    public boolean isGameOver()             { return gameOver; }
    public void setGameOver(boolean v)      { gameOver = v; }
    public boolean isMenuOpen()             { return menuOpen; }
    public void setMenuOpen(boolean v)      { menuOpen = v; }
    public float getGameOverCurtain()       { return gameOverCurtain; }
    public void setGameOverCurtain(float v) { gameOverCurtain = v; }
    public int getComboPulse()              { return comboPulse; }
    public void setComboPulse(int v)        { comboPulse = v; }
    public LevelTheme getCurrentTheme()     { return currentTheme; }
    public void setCurrentTheme(LevelTheme t) { currentTheme = t; }
}
