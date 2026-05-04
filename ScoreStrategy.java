public abstract class ScoreStrategy {
    public abstract int calculateMoveScore(Piece piece);
    public abstract int calculateLineScore(int clearedLines, int comboScore, int lineScore);

    public int calculateTotalScore(Piece piece, int clearedLines, int comboScore, int lineScore) {
        int move = calculateMoveScore(piece);
        int lines = calculateLineScore(clearedLines, comboScore, lineScore);
        return move + lines;
    }
}
