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
 * the seven module accent colours drifting slowly upward behind
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
            Color.rgb(176, 30, 36),    // Notes crimson
            Color.rgb(61, 220, 151),   // Steps green
            Color.rgb(245, 168, 61),   // Printer orange
            Color.rgb(69, 216, 255),   // VPN cyan
            Color.rgb(217, 164, 65),   // Chips gold
    };
    private static final int COUNT = 72;
    private static final int PEAK_ALPHA = 140;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] xs = new float[COUNT];
    private final float[] sizes = new float[COUNT];
    private final float[] periods = new float[COUNT];
    private final float[] phases = new float[COUNT];
    private final float[] sways = new float[COUNT];
    private boolean running;
    private long startMillis = -1;

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
            startMillis = SystemClock.uptimeMillis();
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
        if (startMillis < 0) {
            startMillis = SystemClock.uptimeMillis();
        }
        // Phase math in double precision on elapsed time: dividing
        // the raw uptime clock (hundreds of millions of ms) in
        // float32 quantised the phase into visible steps, which is
        // what made 1.11's motion jagged.
        long elapsed = SystemClock.uptimeMillis() - startMillis;
        for (int i = 0; i < COUNT; i++) {
            double p = (elapsed / (double) periods[i] + phases[i]) % 1.0;
            float y = (float) (h - p * (h + 20f * density) + 10f * density);
            float x = (float) (xs[i] * w + Math.sin(
                    p * Math.PI * 2 + phases[i] * Math.PI * 2) * sways[i] * density);
            float fade = (float) (Math.min(p / 0.12, 1.0)
                    * Math.min((1.0 - p) / 0.12, 1.0));
            paint.setColor(COLORS[i % COLORS.length]);
            paint.setAlpha(Math.round(PEAK_ALPHA * Math.max(0f, fade)));
            canvas.drawCircle(x, y, sizes[i] * density / 2f, paint);
        }
        if (running) {
            // Vsync-aligned redraw; a plain delayed invalidate
            // drifts against the display refresh and judders.
            postInvalidateOnAnimation();
        }
    }
}
