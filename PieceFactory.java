import java.awt.Color;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class PieceFactory {
    private static final Random random = new Random();

    private static final Color[] COLORS = {
            new Color(255, 87, 87),
            new Color(255, 183, 77),
            new Color(255, 235, 59),
            new Color(77, 208, 225),
            new Color(79, 195, 247),
            new Color(149, 117, 205),
            new Color(129, 199, 132),
            new Color(240, 98, 146)
    };

    private static final boolean[][][] SHAPES = {
            // 1 block
            {
                    {true}
            },

            // 2 horizontal
            {
                    {true, true}
            },

            // 2 vertical
            {
                    {true},
                    {true}
            },

            // 3 horizontal
            {
                    {true, true, true}
            },

            // 3 vertical
            {
                    {true},
                    {true},
                    {true}
            },

            // square 2x2
            {
                    {true, true},
                    {true, true}
            },

            // L shape
            {
                    {true, false},
                    {true, false},
                    {true, true}
            },

            // reverse L
            {
                    {false, true},
                    {false, true},
                    {true, true}
            },

            // small L
            {
                    {true, false},
                    {true, true}
            },

            // T shape
            {
                    {true, true, true},
                    {false, true, false}
            },

            // Z shape
            {
                    {true, true, false},
                    {false, true, true}
            },

            // big 3x3 square
            {
                    {true, true, true},
                    {true, true, true},
                    {true, true, true}
            }
    };

    public static Piece createRandomPiece() {
        boolean[][] original = SHAPES[random.nextInt(SHAPES.length)];
        boolean[][] copy = copyShape(original);
        Color color = COLORS[random.nextInt(COLORS.length)];

        return new Piece(copy, color);
    }

    public static Piece[] createThreePieces() {
    Piece[] pieces = new Piece[3];
    Set<Integer> usedShapes = new HashSet<>();   
    for (int i = 0; i < pieces.length; i++) {
        int shapeIndex;
        do {
            shapeIndex = random.nextInt(SHAPES.length);
        } while (usedShapes.contains(shapeIndex));
        usedShapes.add(shapeIndex);
        boolean[][] copy = copyShape(SHAPES[shapeIndex]);
        Color color = COLORS[random.nextInt(COLORS.length)];
        pieces[i] = new Piece(copy, color);
    }
    return pieces;
    }

    private static boolean[][] copyShape(boolean[][] original) {
        boolean[][] copy = new boolean[original.length][original[0].length];

        for (int r = 0; r < original.length; r++) {
            System.arraycopy(original[r], 0, copy[r], 0, original[r].length);
        }

        return copy;
    }
}