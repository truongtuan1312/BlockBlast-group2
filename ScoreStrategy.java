/**
 * Open for extension (add new scoring modes by subclassing),
 * closed for modification (GameState never needs to change).
 * OCP-Open/Closed Principle.
 */ 
public abstract class ScoreStrategy {
    public abstract int calculateMoveScore(Piece piece);
    public abstract int calculateLineScore(int clearedLines, int comboScore, int lineScore);

    public int calculateTotalScore(Piece piece, int clearedLines, int comboScore, int lineScore) {
        int move = calculateMoveScore(piece);
        int lines = calculateLineScore(clearedLines, comboScore, lineScore);
        return move + lines;
    }
}
