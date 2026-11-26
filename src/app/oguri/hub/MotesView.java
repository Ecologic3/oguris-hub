package app.oguri.hub;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/**
 * The hub home's living background (since 1.11): tiny specks in
 * the six module accent colours drifting slowly upward behind
 * the cards, fading in at the bottom and out at the top, each
 * with a gentle sideways sway. Adam picked this design from
 * animated renders on 26 Nov 2026 ("Rising motes"); the shipped
 * motion runs far slower than the preview did. Positions are a
 * pure function of the clock, so the field never drifts out of
 * sync, and the draw loop runs only while the home screen is
 * resumed (MainActivity starts and stops it).
 */
public final class MotesView extends View {

    private static final int[] COLORS = {
            Color.rgb(255, 0, 174),    // Mnemosyne pink
            Color.rgb(139, 92, 246),   // Alarms violet
            Color.rgb(61, 220, 151),   // Steps green
            Color.rgb(245, 168, 61),   // Printer orange
            Color.rgb(69, 216, 255),   // VPN cyan
            Color.rgb(217, 164, 65),   // Chips gold
    };
    private static final int COUNT = 24;
    private static final int PEAK_ALPHA = 140;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] xs = new float[COUNT];
    private final float[] sizes = new float[COUNT];
    private final float[] periods = new float[COUNT];
    private final float[] phases = new float[COUNT];
    private final float[] sways = new float[COUNT];
    private boolean running;

    public MotesView(Context ctx) {
        super(ctx);
        Random rnd = new Random(20261126L);
        for (int i = 0; i < COUNT; i++) {
            xs[i] = 0.02f + rnd.nextFloat() * 0.96f;
            sizes[i] = 2f + rnd.nextFloat() * 2f;          // 2-4 dp across
            periods[i] = 7000f + rnd.nextFloat() * 6000f; // 7-13 s per rise
            phases[i] = rnd.nextFloat();
            sways[i] = 4f + rnd.nextFloat() * 6f;         // 4-10 dp sway
        }
    }

    /** Called from the home activity's onResume. */
    public void start() {
        if (!running) {
            running = true;
            invalidate();
        }
    }

    /** Called from the home activity's onPause. */
    public void stop() {
        running = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        long now = SystemClock.uptimeMillis();
        for (int i = 0; i < COUNT; i++) {
            float p = (now / periods[i] + phases[i]) % 1f;
            float y = h - p * (h + 20f * density) + 10f * density;
            float x = xs[i] * w + (float) Math.sin(
                    p * Math.PI * 2 + phases[i] * Math.PI * 2) * sways[i] * density;
            float fade = Math.min(p / 0.12f, 1f) * Math.min((1f - p) / 0.12f, 1f);
            paint.setColor(COLORS[i % COLORS.length]);
            paint.setAlpha(Math.round(PEAK_ALPHA * Math.max(0f, fade)));
            canvas.drawCircle(x, y, sizes[i] * density / 2f, paint);
        }
        if (running) {
            postInvalidateDelayed(16);
        }
    }
}
