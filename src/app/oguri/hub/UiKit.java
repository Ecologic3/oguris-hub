package app.oguri.hub;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The hub's shared UI kit (since 1.3): one background, one card,
 * one button convention, one section heading, one module header
 * and one root-padding rhythm, so the five modules read as one
 * app. Each module keeps its own palette (card fill, accent) and
 * passes its colours in; the geometry and the shared conventions
 * live here. Before 1.3 every module re-implemented all of this
 * privately, with five different answers to each question.
 */
public final class UiKit {

    /** The one shared background for the hub shell, Steps and VPN. */
    public static final int BG = 0xFF0D2030;

    public static final int CARD_RADIUS_DP = 16;
    public static final int CARD_PAD_H_DP = 18;
    public static final int CARD_PAD_V_DP = 16;
    public static final int CARD_SPACING_DP = 14;
    public static final int ROOT_PAD_H_DP = 20;
    public static final int ROOT_PAD_TOP_DP = 24;
    public static final int ROOT_PAD_BOTTOM_DP = 24;

    /**
     * Alpha of a granted permission's button (the Alarms model):
     * quiet, but never removed and still tappable.
     */
    public static final float GRANTED_ALPHA = 0.45f;

    private UiKit() {
    }

    public static int dp(Context ctx, float v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }

    /** Status and navigation bars in the screen's own background. */
    public static void setSystemBars(Activity activity, int color) {
        activity.getWindow().setStatusBarColor(color);
        activity.getWindow().setNavigationBarColor(color);
    }

    /** The shared root padding rhythm for a module's page root. */
    public static void applyRootPadding(Context ctx, LinearLayout root) {
        root.setPadding(dp(ctx, ROOT_PAD_H_DP), dp(ctx, ROOT_PAD_TOP_DP),
                dp(ctx, ROOT_PAD_H_DP), dp(ctx, ROOT_PAD_BOTTOM_DP));
    }

    /** Card background: the shared 16dp radius, module fill, optional 1dp stroke. */
    public static GradientDrawable cardBackground(Context ctx, int fill, int strokeColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(ctx, CARD_RADIUS_DP));
        if (strokeColor != 0) {
            bg.setStroke(dp(ctx, 1), strokeColor);
        }
        return bg;
    }

    /**
     * A vertical card with the shared padding and the shared bottom
     * spacing, ready to add to a page root.
     */
    public static LinearLayout card(Activity activity, int fill, int strokeColor) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBackground(activity, fill, strokeColor));
        card.setPadding(dp(activity, CARD_PAD_H_DP), dp(activity, CARD_PAD_V_DP),
                dp(activity, CARD_PAD_H_DP), dp(activity, CARD_PAD_V_DP));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(activity, CARD_SPACING_DP));
        card.setLayoutParams(lp);
        return card;
    }

    /** The one section heading: 18sp bold in the module's text colour. */
    public static TextView sectionHeading(Context ctx, String text, int color) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(18);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setTextColor(color);
        return t;
    }

    /** A button under the shared convention: sentence case, never all caps. */
    public static Button button(Context ctx, String text) {
        Button b = new Button(ctx);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }

    // ------------------------------------------------------------ buttons
    //
    // The one button style (since 1.5). Every button in the app is
    // built by one of the three factories below, so the only
    // differences left between buttons anywhere are the colours a
    // module passes in (its own accent/surface palette) and the
    // size role:
    //
    //   standard  15sp, padding 16x10dp, radius 12dp
    //   small     13sp, padding 12x6dp,  radius 12dp, no min size
    //   pill      14sp, padding 20x8dp,  fully rounded
    //
    // All three share the same state behaviour: a pressed colour
    // (the module's own where it has one, otherwise pressedOf()),
    // and the fill at 45% alpha while disabled.

    public static final int BTN_RADIUS_DP = 12;

    /** The shared pressed shade for a fill: blended 18% toward white. */
    public static int pressedOf(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        r += (255 - r) * 18 / 100;
        g += (255 - g) * 18 / 100;
        b += (255 - b) * 18 / 100;
        return Color.rgb(r, g, b);
    }

    /** The shared button background: normal / pressed / disabled states. */
    public static StateListDrawable buttonBackground(Context ctx, int fill,
                                                       int pressed, int stroke,
                                                       float radiusDp) {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_pressed},
                buttonState(ctx, pressed, stroke, radiusDp));
        s.addState(new int[]{-android.R.attr.state_enabled},
                buttonState(ctx, withAlpha(fill, 115), stroke, radiusDp));
        s.addState(new int[]{}, buttonState(ctx, fill, stroke, radiusDp));
        return s;
    }

    private static GradientDrawable buttonState(Context ctx, int color,
                                                int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(ctx, radiusDp));
        if (stroke != 0) {
            g.setStroke(dp(ctx, 1), stroke);
        }
        return g;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color),
                Color.blue(color));
    }

    /** Standard button: the size role most actions use. */
    public static Button button(Context ctx, String text, int fill,
                                int pressed, int textColor) {
        Button b = button(ctx, text);
        b.setTextSize(15);
        b.setTextColor(textColor);
        b.setPadding(dp(ctx, 16), dp(ctx, 10), dp(ctx, 16), dp(ctx, 10));
        b.setBackground(buttonBackground(ctx, fill, pressed, 0, BTN_RADIUS_DP));
        return b;
    }

    /** Small button: compact in-row actions (row items, steppers, chips). */
    public static Button smallButton(Context ctx, String text, int fill,
                                     int pressed, int textColor) {
        Button b = button(ctx, text);
        b.setTextSize(13);
        b.setTextColor(textColor);
        b.setPadding(dp(ctx, 12), dp(ctx, 6), dp(ctx, 12), dp(ctx, 6));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setBackground(buttonBackground(ctx, fill, pressed, 0, BTN_RADIUS_DP));
        return b;
    }

    /** Pill button: fully rounded; toggle chips and utility actions. */
    public static Button pillButton(Context ctx, String text, int fill,
                                    int pressed, int stroke, int textColor) {
        Button b = button(ctx, text);
        b.setTextSize(14);
        b.setTextColor(textColor);
        b.setPadding(dp(ctx, 20), dp(ctx, 8), dp(ctx, 20), dp(ctx, 8));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setBackground(buttonBackground(ctx, fill, pressed, stroke, 999));
        return b;
    }

    /**
     * The module header: a 30sp bold title on the left in the
     * module's near-white text colour, the module's own 48dp icon
     * at the top right, directly on the screen background.
     */
    public static LinearLayout header(Activity activity, String title,
                                      int titleColor, int iconRes) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = new TextView(activity);
        t.setText(title);
        t.setTextSize(30);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setTextColor(titleColor);
        row.addView(t, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        try {
            ImageView icon = new ImageView(activity);
            icon.setImageResource(iconRes);
            row.addView(icon, new LinearLayout.LayoutParams(
                    dp(activity, 48), dp(activity, 48)));
        } catch (Exception ignored) {
            // The icon is cosmetic only; the header works without it.
        }
        return row;
    }
}
