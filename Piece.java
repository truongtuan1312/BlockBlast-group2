import java.awt.Color;

public class Piece {
    private final boolean[][] shape;
    private final Color color;

    public Piece(boolean[][] shape, Color color) {
        this.shape = shape;
        this.color = color;
    }

    public boolean[][] getShape() {
        return shape;
    }

    public Color getColor() {
        return color;
    }

    public int getHeight() {
        return shape.length;
    }

    public int getWidth() {
        return shape[0].length;
    }

    public int getBlockCount() {
        int count = 0;

        for (boolean[] row : shape) {
            for (boolean cell : row) {
                if (cell) count++;
            }
        }

        return count;
    }
}