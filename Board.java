import java.awt.Color;

public class Board {
    public static final int SIZE = 8;

    private final Color[][] cells;

    public Board() {
        cells = new Color[SIZE][SIZE];
    }

    public boolean isEmpty(int row, int col) {
        if (!isInside(row, col)) return false;
        return cells[row][col] == null;
    }

    public Color getCellColor(int row, int col) {
        if (!isInside(row, col)) return null;
        return cells[row][col];
    }

    public boolean canPlace(Piece piece, int startRow, int startCol) {
        if (piece == null) return false;

        boolean[][] shape = piece.getShape();

        for (int r = 0; r < piece.getHeight(); r++) {
            for (int c = 0; c < piece.getWidth(); c++) {
                if (shape[r][c]) {
                    int boardRow = startRow + r;
                    int boardCol = startCol + c;

                    if (!isInside(boardRow, boardCol)) return false;
                    if (cells[boardRow][boardCol] != null) return false;
                }
            }
        }

        return true;
    }

    public void placePiece(Piece piece, int startRow, int startCol) throws InvalidPlacementException {
        if (!canPlace(piece, startRow, startCol))
            throw new InvalidPlacementException(startRow, startCol);

        boolean[][] shape = piece.getShape();

        for (int r = 0; r < piece.getHeight(); r++) {
            for (int c = 0; c < piece.getWidth(); c++) {
                if (shape[r][c]) {
                    cells[startRow + r][startCol + c] = piece.getColor();
                }
            }
        }
    }

    public boolean[][] getFullLineCells() {
        boolean[][] marked = new boolean[SIZE][SIZE];
        markFullRows(marked);
        markFullColumns(marked);
        return marked;
    }

    private void markFullRows(boolean[][] marked) {
        for (int r = 0; r < SIZE; r++) {
            boolean full = true;

            for (int c = 0; c < SIZE; c++) {
                if (cells[r][c] == null) {
                    full = false;
                    break;
                }
            }

            if (full) {
                for (int c = 0; c < SIZE; c++) marked[r][c] = true;
            }
        }
    }

    private void markFullColumns(boolean[][] marked) {
        for (int c = 0; c < SIZE; c++) {
            boolean full = true;

            for (int r = 0; r < SIZE; r++) {
                if (cells[r][c] == null) {
                    full = false;
                    break;
                }
            }

            if (full) {
                for (int r = 0; r < SIZE; r++) marked[r][c] = true;
            }
        }
    }

    public int countFullLines() {
        return countFullRows() + countFullColumns();
    }

    private int countFullRows() {
        int count = 0;

        for (int r = 0; r < SIZE; r++) {
            boolean full = true;

            for (int c = 0; c < SIZE; c++) {
                if (cells[r][c] == null) {
                    full = false;
                    break;
                }
            }

            if (full) count++;
        }

        return count;
    }

    private int countFullColumns() {
        int count = 0;

        for (int c = 0; c < SIZE; c++) {
            boolean full = true;

            for (int r = 0; r < SIZE; r++) {
                if (cells[r][c] == null) {
                    full = false;
                    break;
                }
            }

            if (full) count++;
        }

        return count;
    }

    public boolean hasMarkedCell(boolean[][] marked) {
        if (marked == null) return false;

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (marked[r][c]) return true;
            }
        }

        return false;
    }

    public void clearMarkedCells(boolean[][] marked) {
        if (marked == null) return;

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (marked[r][c]) cells[r][c] = null;
            }
        }
    }

    public int clearFullLines() {
        boolean[][] marked = getFullLineCells();
        int clearedLines = countFullLines();
        clearMarkedCells(marked);
        return clearedLines;
    }

    public boolean canAnyPieceFit(Piece[] pieces) {
        if (pieces == null) return false;

        for (Piece piece : pieces) {
            if (piece == null) continue;
            if (canPieceFit(piece)) return true;
        }

        return false;
    }

    private boolean canPieceFit(Piece piece) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (canPlace(piece, r, c)) return true;
            }
        }

        return false;
    }

    public Color[][] copyCells() {
        Color[][] copy = new Color[SIZE][SIZE];

        for (int r = 0; r < SIZE; r++) {
            System.arraycopy(cells[r], 0, copy[r], 0, SIZE);
        }

        return copy;
    }

    public void restoreCells(Color[][] snapshot) {
        if (snapshot == null) return;

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                cells[r][c] = snapshot[r][c];
            }
        }
    }

    public void reset() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                cells[r][c] = null;
            }
        }
    }

    private boolean isInside(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }
}
