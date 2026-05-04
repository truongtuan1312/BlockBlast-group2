import java.awt.Color;

public class GameStateMemento {
    private final Color[][] boardSnapshot;
    private final Piece[] piecesSnapshot;
    private final int score;

    public GameStateMemento(Color[][] board, Piece[] pieces, int score) {
        this.boardSnapshot = board;
        this.piecesSnapshot = pieces.clone();
        this.score = score;
    }

    public Color[][] getBoardSnapshot() { return boardSnapshot; }
    public Piece[] getPiecesSnapshot() { return piecesSnapshot.clone(); }
    public int getScore() { return score; }
}
