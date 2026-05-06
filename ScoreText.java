import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

public class ScoreText implements Drawable {
    private final String word;
    private final int gainedScore;
    private final int clearedLines;
    private final Color mainColor;
    private final Color glowColor;
    private final float baseSize;
    private final float startX;
    private final float startY;
    private final float direction;
    private float x;
    private float y;
    private float vx;
    private float vy;
    private float life;
    private float age;
    private float rotation;

    private final Random random;
    private final Font titleFont;
    private final Font gameFont;

    public ScoreText(String word, int x, int y, int gainedScore, int clearedLines, Color mainColor, Color glowColor, float baseSize, Random random, Font titleFont, Font gameFont) {
        this.word = word;
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.gainedScore = gainedScore;
        this.clearedLines = clearedLines;
        this.mainColor = mainColor;
        this.glowColor = glowColor;
        this.baseSize = baseSize;
        this.random = random;
        this.titleFont = titleFont;
        this.gameFont = gameFont;
        this.direction = random.nextBoolean() ? 1f : -1f;
        this.vx = direction * (0.20f + random.nextFloat() * 0.25f);
        this.vy = -2.35f;
        this.life = 1.0f;
        this.age = 0f;
        this.rotation = direction * (-0.05f + random.nextFloat() * 0.08f);
    }

    @Override
    public void update() {
        age += 0.055f;

        // Chữ bay lên, hơi lướt ngang và chậm dần như hiệu ứng arcade/fighting game.
        x += vx;
        y += vy;
        vx *= 0.970f;
        vy *= 0.955f;

        // PERFECT giữ lâu hơn một chút, GOOD biến mất nhanh hơn để không che board.
        float fade = "PERFECT".equals(word) ? 0.016f : "EXCELLENT".equals(word) ? 0.019f : 0.022f;
        life -= fade;
    }

    @Override
    public boolean isDead() {
        return life <= 0f;
    }

    @Override
    public void draw(Graphics2D g2) {
        if (life <= 0) return;

        Graphics2D copy = (Graphics2D) g2.create();
        copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        copy.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        copy.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        float intro = Math.min(1f, age / 0.32f);
        float outro = Math.max(0f, Math.min(1f, life / 0.42f));
        float punch = (float) Math.sin(intro * Math.PI);

        float scale;
        if (intro < 1f) {
            // Bật mạnh lúc vừa hiện ra.
            scale = 0.45f + intro * 0.88f + punch * 0.28f;
        } else {
            // Sau đó thu nhỏ rất nhẹ khi tan biến.
            scale = 1.0f + Math.max(0f, 0.16f * life);
        }

        int alpha = Math.max(0, Math.min(255, (int) (255 * Math.min(1f, life * 1.45f))));
        int glowAlpha = Math.max(0, Math.min(220, (int) (alpha * 0.72f)));

        Font wordFont = titleFont.deriveFont(Font.BOLD, baseSize * scale);
        copy.setFont(wordFont);
        FontMetrics fm = copy.getFontMetrics();
        int textW = fm.stringWidth(word);
        int textH = fm.getAscent();

        float centerX = x + (float) Math.sin(age * 9.0f) * 2.0f * life;
        float centerY = y;
        int textX = (int) (centerX - textW / 2.0f);
        int textY = (int) centerY;

        copy.translate(centerX, centerY - textH / 2.0f);
        copy.rotate(rotation * life);
        copy.translate(-centerX, -(centerY - textH / 2.0f));

        // Vệt sáng quét ngang phía sau chữ, nhìn giống chữ combo trong game đối kháng.
        int slashW = textW + 96;
        int slashH = Math.max(32, textH + 8);
        int slashX = (int) (centerX - slashW / 2.0f);
        int slashY = (int) (centerY - textH + 3);

        float sweep = Math.min(1f, age / 0.55f);
        int sweepOffset = (int) ((sweep - 0.5f) * 80);

        Path2D slash = new Path2D.Double();
        slash.moveTo(slashX + 34 + sweepOffset, slashY);
        slash.lineTo(slashX + slashW, slashY + 3);
        slash.lineTo(slashX + slashW - 34, slashY + slashH);
        slash.lineTo(slashX, slashY + slashH - 3);
        slash.closePath();

        GradientPaint slashPaint = new GradientPaint(
                slashX, slashY,
                new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), Math.min(105, glowAlpha / 2)),
                slashX + slashW, slashY + slashH,
                new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), Math.min(55, glowAlpha / 3))
        );
        copy.setPaint(slashPaint);
        copy.fill(slash);

        copy.setStroke(new BasicStroke(2.2f));
        copy.setColor(new Color(255, 255, 255, Math.min(145, glowAlpha / 2)));
        copy.draw(slash);
        copy.setStroke(new BasicStroke(1f));

        // Shockwave vòng ngoài khi vừa xuất hiện.
        if (age < 0.55f) {
            float ring = age / 0.55f;
            int rw = (int) ((textW + 30) * (0.78f + ring * 0.55f));
            int rh = (int) ((textH + 24) * (0.82f + ring * 0.40f));
            int ra = (int) ((1f - ring) * 150);
            copy.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), Math.max(0, ra)));
            copy.setStroke(new BasicStroke(3.2f));
            copy.drawOval((int) centerX - rw / 2, (int) centerY - textH / 2 - rh / 2 + 10, rw, rh);
            copy.setStroke(new BasicStroke(1f));
        }

        // Các tia năng lượng nhỏ bung ra rồi tan dần.
        int sparkCount = "PERFECT".equals(word) ? 18 : "EXCELLENT".equals(word) ? 12 : 8;
        for (int i = 0; i < sparkCount; i++) {
            double angle = (Math.PI * 2 * i / sparkCount) + age * 0.9;
            float dist = (26f + (i % 4) * 9f) * (0.35f + intro) * life;
            int sx = (int) (centerX + Math.cos(angle) * dist);
            int sy = (int) (centerY - textH / 2 + Math.sin(angle) * dist);
            int sparkA = Math.max(0, Math.min(180, (int) (alpha * 0.65f * outro)));
            copy.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), sparkA));
            copy.fillOval(sx - 3, sy - 3, 6, 6);
        }

        // Glow nhiều lớp sau chữ.
        copy.setFont(wordFont);
        for (int i = 10; i >= 2; i -= 2) {
            int a = Math.max(0, Math.min(75, glowAlpha / (i / 2 + 1)));
            copy.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), a));
            copy.drawString(word, textX - i / 2, textY);
            copy.drawString(word, textX + i / 2, textY);
            copy.drawString(word, textX, textY - i / 2);
            copy.drawString(word, textX, textY + i / 2);
        }

        // Bóng đen để chữ nổi khối.
        copy.setColor(new Color(0, 0, 0, Math.min(185, alpha)));
        copy.drawString(word, textX + 5, textY + 6);

        // Viền trắng dày giả lập bằng nhiều lần vẽ lệch.
        copy.setColor(new Color(255, 255, 255, alpha));
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (Math.abs(dx) + Math.abs(dy) <= 3) {
                    copy.drawString(word, textX + dx, textY + dy);
                }
            }
        }

        // Màu chính ở giữa.
        GradientPaint textPaint = new GradientPaint(
                textX, textY - textH,
                new Color(255, 255, 255, alpha),
                textX, textY,
                new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), alpha)
        );
        copy.setPaint(textPaint);
        copy.drawString(word, textX, textY);

        // Điểm cộng nhỏ bay theo bên dưới, tan nhanh hơn chữ chính.
        String bonus = "+" + gainedScore;
        if (clearedLines > 1) bonus += "  COMBO x" + clearedLines;

        copy.setFont(gameFont.deriveFont(Font.BOLD, 16f * Math.max(0.75f, scale * 0.82f)));
        FontMetrics bfm = copy.getFontMetrics();
        int bonusX = (int) (centerX - bfm.stringWidth(bonus) / 2.0f);
        int bonusY = (int) (centerY + 24 + (1f - life) * 18);

        copy.setColor(new Color(0, 0, 0, Math.min(150, alpha)));
        copy.drawString(bonus, bonusX + 2, bonusY + 2);
        copy.setColor(new Color(255, 255, 255, Math.min(240, alpha)));
        copy.drawString(bonus, bonusX, bonusY);

        copy.dispose();
    }
}

