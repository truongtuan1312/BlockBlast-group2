import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.Random;
/**
 * Abstract base-subclasses must honour the Particle contract:
 * update() moves the particle, isDead() returns true when life <= 0.
 * LSP-Liskov Substitution Principle.
 */
public class AtmosphereParticle extends Particle {
    private int size;
    private float rotation;
    private float rotationSpeed;
    private final LevelTheme theme;
    private final Random random;
    private float animationTime;
    private final int panelWidth;
    private final int panelHeight;

    public AtmosphereParticle(LevelTheme theme, boolean randomY,
                               Random random, float animationTime,
                               int panelWidth, int panelHeight) {
        super(0, 0, 0, 0, Color.WHITE);
        this.theme = theme;
        this.random = random;
        this.animationTime = animationTime;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        reset(randomY);
    }

    public void setAnimationTime(float t) {
        this.animationTime = t;
    }

    private void reset(boolean randomY) {
        x = random.nextInt(panelWidth);
        y = randomY ? random.nextInt(panelHeight) : -40;

        if (theme == LevelTheme.CALM) {
            vx = -0.45f + random.nextFloat() * 0.9f;
            vy = 0.55f + random.nextFloat() * 0.9f;
            size = 8 + random.nextInt(10);
            color = new Color(255, 170 + random.nextInt(60), 210 + random.nextInt(40), 150);
        } else if (theme == LevelTheme.GOLDEN_FLOW) {
            vx = -0.8f + random.nextFloat() * 1.5f;
            vy = 0.70f + random.nextFloat() * 1.2f;
            size = 10 + random.nextInt(12);
            color = new Color(210 + random.nextInt(45), 120 + random.nextInt(80), 30, 155);
        } else if (theme == LevelTheme.NEON_RUSH) {
            vx = -0.3f + random.nextFloat() * 0.6f;
            vy = 0.25f + random.nextFloat() * 0.65f;
            size = 2 + random.nextInt(5);
            color = new Color(120, 235, 255, 120);
        } else {
            vx = -0.35f + random.nextFloat() * 0.7f;
            vy = -1.8f - random.nextFloat() * 3.2f;
            size = 3 + random.nextInt(9);
            color = new Color(255, 90 + random.nextInt(150), 10, 135 + random.nextInt(90));
        }
        rotation = random.nextFloat() * 6.28f;
        rotationSpeed = -0.035f + random.nextFloat() * 0.07f;
    }

    @Override
    public void update() {
        x += vx + (float) Math.sin(animationTime + y * 0.018) * 0.30f;
        y += vy;
        rotation += rotationSpeed;

        if ((theme == LevelTheme.FIRESTORM || theme == LevelTheme.INFERNO) && y < -50) {
            y = panelHeight + random.nextInt(120);
            x = random.nextInt(panelWidth);
        }
        if (theme != LevelTheme.FIRESTORM && theme != LevelTheme.INFERNO && y > panelHeight + 50) reset(false);
        if (x < -60) x = panelWidth + 50;
        if (x > panelWidth + 60) x = -50;
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public void draw(Graphics2D g2) {
        Graphics2D copy = (Graphics2D) g2.create();
        copy.translate(x, y);
        copy.rotate(rotation);
        copy.setColor(color);

        if (theme == LevelTheme.CALM) {
            for (int i = 0; i < 5; i++) {
                double a = i * Math.PI * 2 / 5;
                copy.fillOval((int) (Math.cos(a) * size / 2.0) - size / 3,
                        (int) (Math.sin(a) * size / 2.0) - size / 3,
                        Math.max(4, size / 2), Math.max(4, size / 2));
            }
        } else if (theme == LevelTheme.GOLDEN_FLOW) {
            copy.fillOval(-size / 2, -size / 4, size, size / 2);
            copy.setColor(new Color(100, 65, 30, 125));
            copy.drawLine(0, 0, size / 2, size / 4);
        } else if (theme == LevelTheme.NEON_RUSH) {
            copy.fillOval(-size / 2, -size / 2, size, size);
        } else {
            Path2D spark = new Path2D.Double();
            spark.moveTo(0, -size);
            spark.lineTo(size / 2.0, size);
            spark.lineTo(-size / 2.0, size);
            spark.closePath();
            copy.fill(spark);
        }
        copy.dispose();
    }
}
