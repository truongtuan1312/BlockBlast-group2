public class ClassicScoreStrategy extends ScoreStrategy {

    @Override
    public int calculateMoveScore(Piece piece) {
        return piece.getBlockCount() * 10;
    }

    @Override
    public int calculateLineScore(int clearedLines, int comboScore, int lineScore) {
        if (clearedLines <= 0) return 0;
        int bonus = clearedLines * lineScore;
        int combo = clearedLines >= 2 ? clearedLines * comboScore : 0;
        return bonus + combo;
    }
}
