import java.awt.Color;
import java.awt.Graphics2D;

public abstract class Particle implements Drawable {
    protected float x;
    protected float y;
    protected float vx;
    protected float vy;
    protected float life;
    protected Color color;

    public Particle(float x, float y, float vx, float vy, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.life = 1.0f;
    }

    @Override
    public boolean isDead() {
        return life <= 0;
    }

    public float getLife() {
        return life;
    }

    public float getX() { return x; }
    public float getY() { return y; }

    @Override
    public abstract void update();

    @Override
    public abstract void draw(Graphics2D g2);
}
