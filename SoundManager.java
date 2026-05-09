import javax.sound.sampled.*;

/**
 * SoundManager — Singleton Pattern.
 * Single Responsibility: chỉ quản lý âm thanh.
 * Tự tạo âm thanh bằng code (không cần file .wav bên ngoài).
 */
public class SoundManager {

    // ── Singleton ────────────────────────────────────────────────────
    private static SoundManager instance;
    public static SoundManager getInstance() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    // ── Enum các loại âm thanh (Open/Closed: thêm sound mới chỉ cần thêm enum) ──
    public enum SoundType {
        PLACE,       // đặt piece xuống
        CLEAR_LINE,  // xóa hàng
        COMBO,       // combo nhiều hàng
        GAME_OVER,   // thua
        UNDO,        // undo
        NEW_GAME,    // bắt đầu game mới
        LEVEL_UP     // lên theme mới
    }

    private boolean muted = false;
    private float volume = 0.75f; // 0.0 → 1.0

    private SoundManager() {}

    // ── Public API ───────────────────────────────────────────────────
    public void play(SoundType type) {
        if (muted) return;
        // Chạy trên thread riêng để không block game loop
        new Thread(() -> {
            try {
                byte[] data = generateSound(type);
                if (data == null) return;
                AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
                if (!AudioSystem.isLineSupported(info)) return;
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(fmt);
                applyVolume(line);
                line.start();
                line.write(data, 0, data.length);
                line.drain();
                line.close();
            } catch (Exception ignored) {}
        }, "Sound-" + type).start();
    }

    public void toggleMute() { muted = !muted; }
    public boolean isMuted()  { return muted; }
    public void setVolume(float v) { volume = Math.max(0f, Math.min(1f, v)); }
    public float getVolume() { return volume; }

    // ── Sound generation ─────────────────────────────────────────────
    private byte[] generateSound(SoundType type) {
        return switch (type) {
            case PLACE      -> genTone(440, 80,  Shape.SINE,    0.55f);
            case CLEAR_LINE -> genChord(new int[]{523, 659, 784}, 220, 0.7f);
            case COMBO      -> genArpeggio(new int[]{523, 659, 784, 1047}, 90, 0.8f);
            case GAME_OVER  -> genDescend(new int[]{440, 370, 311, 262}, 180, 0.7f);
            case UNDO       -> genTone(330, 120, Shape.SINE,    0.5f);
            case NEW_GAME   -> genArpeggio(new int[]{262, 330, 392, 523}, 80, 0.6f);
            case LEVEL_UP   -> genArpeggio(new int[]{523, 659, 784, 1047, 1319}, 70, 0.75f);
        };
    }

    private enum Shape { SINE, SQUARE, TRIANGLE }

    /** Tạo một tone đơn */
    private byte[] genTone(int freq, int ms, Shape shape, float amp) {
        int samples = 44100 * ms / 1000;
        byte[] buf = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double t = (double) i / 44100;
            double wave = switch (shape) {
                case SINE     -> Math.sin(2 * Math.PI * freq * t);
                case SQUARE   -> Math.signum(Math.sin(2 * Math.PI * freq * t));
                case TRIANGLE -> 2.0 / Math.PI * Math.asin(Math.sin(2 * Math.PI * freq * t));
            };
            // Envelope: fade out cuối để không bị click
            double env = i < samples * 0.1 ? i / (samples * 0.1)
                       : i > samples * 0.7 ? (samples - i) / (samples * 0.3)
                       : 1.0;
            short val = (short)(wave * env * amp * Short.MAX_VALUE);
            buf[i * 2]     = (byte)(val & 0xFF);
            buf[i * 2 + 1] = (byte)((val >> 8) & 0xFF);
        }
        return buf;
    }

    /** Nhiều tone cùng lúc (chord) */
    private byte[] genChord(int[] freqs, int ms, float amp) {
        int samples = 44100 * ms / 1000;
        byte[] buf = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double t = (double) i / 44100;
            double wave = 0;
            for (int f : freqs) wave += Math.sin(2 * Math.PI * f * t);
            wave /= freqs.length;
            double env = i > samples * 0.6 ? (samples - i) / (samples * 0.4) : 1.0;
            short val = (short)(wave * env * amp * Short.MAX_VALUE);
            buf[i * 2]     = (byte)(val & 0xFF);
            buf[i * 2 + 1] = (byte)((val >> 8) & 0xFF);
        }
        return buf;
    }

    /** Các tone phát lần lượt (arpeggio) */
    private byte[] genArpeggio(int[] freqs, int msEach, float amp) {
        int samplesEach = 44100 * msEach / 1000;
        byte[] buf = new byte[freqs.length * samplesEach * 2];
        for (int n = 0; n < freqs.length; n++) {
            int freq = freqs[n];
            for (int i = 0; i < samplesEach; i++) {
                double t = (double) i / 44100;
                double wave = Math.sin(2 * Math.PI * freq * t);
                double env = i > samplesEach * 0.6 ? (samplesEach - i) / (samplesEach * 0.4) : 1.0;
                short val = (short)(wave * env * amp * Short.MAX_VALUE);
                int idx = (n * samplesEach + i) * 2;
                buf[idx]     = (byte)(val & 0xFF);
                buf[idx + 1] = (byte)((val >> 8) & 0xFF);
            }
        }
        return buf;
    }

    /** Âm thanh đi xuống (game over) */
    private byte[] genDescend(int[] freqs, int msEach, float amp) {
        return genArpeggio(freqs, msEach, amp);
    }

    /** Apply volume gain to line */
    private void applyVolume(SourceDataLine line) {
        try {
            FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float)(20.0 * Math.log10(Math.max(0.0001, volume)));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
        } catch (Exception ignored) {}
    }
}
