import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.Random;

public class DebrisParticle extends Particle {
    private final int size;
    private float rotation;
    private final float rotationSpeed;
    private final GamePanel.LevelTheme theme;
    private float animationTime;

    public DebrisParticle(float x, float y, float vx, float vy, int size,
                           Color color, GamePanel.LevelTheme theme,
                           Random random, float animationTime) {
        super(x, y, vx, vy, color);
        this.size = size;
        this.theme = theme;
        this.animationTime = animationTime;
        this.rotation = random.nextFloat() * 6.28f;
        this.rotationSpeed = -0.23f + random.nextFloat() * 0.46f;
    }

    public void setAnimationTime(float t) {
        this.animationTime = t;
    }

    @Override
    public void update() {
        x += vx;
        y += vy;
        vy += theme.gravity;
        vx *= theme.friction;
        if (theme == GamePanel.LevelTheme.CALM)
            x += (float) Math.sin(animationTime + y * 0.04) * 0.35f;
        rotation += rotationSpeed;
        life -= theme.fadeSpeed;
    }

    @Override
    public void draw(Graphics2D g2) {
        int alpha = Math.max(0, Math.min(255, (int) (255 * life)));
        int drawSize = Math.max(2, (int) (size * life));
        Graphics2D copy = (Graphics2D) g2.create();
        copy.translate(x, y);
        copy.rotate(rotation);

        copy.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));

        if (theme == GamePanel.LevelTheme.CALM) {
            copy.setColor(new Color(230, 235, 245, alpha));
            copy.fillOval(-drawSize, -drawSize, drawSize * 2, drawSize * 2);
        } else if (theme == GamePanel.LevelTheme.FIRESTORM || theme == GamePanel.LevelTheme.INFERNO) {
            Path2D shard = new Path2D.Double();
            shard.moveTo(0, -drawSize);
            shard.lineTo(drawSize, drawSize / 2.0);
            shard.lineTo(-drawSize / 2.0, drawSize);
            shard.closePath();
            copy.fill(shard);
        } else {
            copy.fillRoundRect(-drawSize / 2, -drawSize / 2, drawSize, drawSize, 5, 5);
        }
        copy.dispose();
    }
}
