public class InvalidPlacementException extends Exception {
    private final int row;
    private final int col;

    public InvalidPlacementException(int row, int col) {
        super("Cannot place piece at row=" + row + ", col=" + col);
        this.row = row;
        this.col = col;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
}
