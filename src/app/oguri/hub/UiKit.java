package app.oguri.hub;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.View;
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
     * at the top right, directly on the screen background. Since
     * 1.10 a module can also carry a Hub pill between the title
     * and the icon (house glyph + "Hub", in the module's own
     * colours): one tap returns to the hub home, clearing the
     * activities above it. A zero pillAccent means no pill (the
     * hub home itself).
     */
    public static LinearLayout header(Activity activity, String title,
                                      int titleColor, int iconRes,
                                      int pillFill, int pillStroke,
                                      int pillAccent) {
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
        if (pillAccent != 0) {
            row.addView(hubPill(activity, pillFill, pillStroke, pillAccent));
        }
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

    /** The back-to-home pill shown in module headers since 1.10. */
    private static LinearLayout hubPill(final Activity activity, int fill,
                                        int stroke, int accent) {
        LinearLayout pill = new LinearLayout(activity);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(activity, 10), 0, dp(activity, 12), 0);
        pill.setBackground(buttonBackground(activity, fill,
                pressedOf(fill), stroke, 999));
        ImageView glyph = new ImageView(activity);
        glyph.setImageResource(R.drawable.ic_hub_home);
        glyph.setColorFilter(accent);
        LinearLayout.LayoutParams glyphLp = new LinearLayout.LayoutParams(
                dp(activity, 15), dp(activity, 15));
        glyphLp.setMargins(0, 0, dp(activity, 5), 0);
        pill.addView(glyph, glyphLp);
        TextView label = new TextView(activity);
        label.setText("Hub");
        label.setTextSize(14);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setTextColor(accent);
        pill.addView(label);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(activity, 34));
        lp.setMargins(0, 0, dp(activity, 10), 0);
        pill.setLayoutParams(lp);
        pill.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);
            }
        });
        return pill;
    }

    // ------------------------------------------------------------ dialogs
    //
    // The one dialog family (since 1.17.4): a themed card panel in
    // the calling module's own palette, modelled on the input
    // dialog Chips set as the pattern in 1.5. Adam's standing rule:
    // no stock Android popup UI anywhere in the hub, so every
    // confirmation, prompt, picker and time picker goes through
    // here (or through a module's own themed dialog in this shape).

    /** Callback for the themed time picker. */
    public interface TimeSetListener {
        void onTimeSet(int hourOfDay, int minute);
    }

    private static class DialogPanel {
        android.app.Dialog dialog;
        LinearLayout panel;
    }

    private static DialogPanel dialogPanel(Activity activity, int surface, int border) {
        DialogPanel dp = new DialogPanel();
        dp.dialog = new android.app.Dialog(activity);
        dp.dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dp.panel = new LinearLayout(activity);
        dp.panel.setOrientation(LinearLayout.VERTICAL);
        dp.panel.setBackground(cardBackground(activity, surface, border));
        dp.panel.setPadding(dp(activity, 18), dp(activity, 16),
                dp(activity, 18), dp(activity, 16));
        return dp;
    }

    private static void showDialog(Activity activity, DialogPanel dp, boolean focusInput) {
        android.widget.FrameLayout wrap = new android.widget.FrameLayout(activity);
        wrap.setPadding(dp(activity, 24), 0, dp(activity, 24), 0);
        wrap.addView(dp.panel);
        dp.dialog.setContentView(wrap);
        android.view.Window window = dp.dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            if (focusInput) {
                window.setSoftInputMode(android.view.WindowManager.LayoutParams
                        .SOFT_INPUT_STATE_VISIBLE);
            }
        }
        dp.dialog.show();
    }

    private static void addDialogButtons(final Activity activity, final DialogPanel dp,
                                         String positiveLabel, int cancelFill,
                                         int textColor, int positiveFill,
                                         int positiveTextColor,
                                         final Runnable onPositive) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, dp(activity, 16), 0, 0);
        row.setLayoutParams(rp);
        Button cancel = button(activity, "Cancel", cancelFill,
                pressedOf(cancelFill), textColor);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dp.dialog.dismiss();
            }
        });
        row.addView(cancel, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button ok = button(activity, positiveLabel, positiveFill,
                pressedOf(positiveFill), positiveTextColor);
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dp.dialog.dismiss();
                if (onPositive != null) {
                    onPositive.run();
                }
            }
        });
        LinearLayout.LayoutParams okp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        okp.setMargins(dp(activity, 8), 0, 0, 0);
        row.addView(ok, okp);
        dp.panel.addView(row);
    }

    /** A themed confirmation: title, optional message, Cancel + one action. */
    public static void confirmDialog(Activity activity, String title, String message,
                                     String positiveLabel, int surface, int border,
                                     int textColor, int mutedColor, int cancelFill,
                                     int positiveFill, int positiveTextColor,
                                     Runnable onPositive) {
        DialogPanel dp = dialogPanel(activity, surface, border);
        dp.panel.addView(sectionHeading(activity, title, textColor));
        if (message != null && message.length() > 0) {
            TextView m = new TextView(activity);
            m.setText(message);
            m.setTextSize(14.5f);
            m.setTextColor(mutedColor);
            LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mp.setMargins(0, dp(activity, 8), 0, 0);
            m.setLayoutParams(mp);
            dp.panel.addView(m);
        }
        addDialogButtons(activity, dp, positiveLabel, cancelFill, textColor,
                positiveFill, positiveTextColor, onPositive);
        showDialog(activity, dp, false);
    }

    /** A themed single-line text prompt; the raw text goes to done. */
    public static void inputDialog(final Activity activity, String title, String initial,
                                   String hint, String positiveLabel, int surface,
                                   int border, int fieldFill, int textColor,
                                   int mutedColor, int cancelFill, int positiveFill,
                                   int positiveTextColor,
                                   final java.util.function.Consumer<String> done) {
        final DialogPanel dp = dialogPanel(activity, surface, border);
        dp.panel.addView(sectionHeading(activity, title, textColor));
        final android.widget.EditText e = new android.widget.EditText(activity);
        e.setText(initial);
        if (initial != null) {
            e.setSelection(initial.length());
        }
        if (hint != null) {
            e.setHint(hint);
        }
        e.setHintTextColor(mutedColor);
        e.setTextColor(textColor);
        e.setTextSize(17);
        e.setSingleLine(true);
        GradientDrawable fieldBg = new GradientDrawable();
        fieldBg.setColor(fieldFill);
        fieldBg.setCornerRadius(dp(activity, 10));
        if (border != 0) {
            fieldBg.setStroke(dp(activity, 1), border);
        }
        e.setBackground(fieldBg);
        e.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ep.setMargins(0, dp(activity, 12), 0, 0);
        e.setLayoutParams(ep);
        dp.panel.addView(e);
        addDialogButtons(activity, dp, positiveLabel, cancelFill, textColor,
                positiveFill, positiveTextColor, new Runnable() {
                    @Override
                    public void run() {
                        done.accept(e.getText().toString());
                    }
                });
        showDialog(activity, dp, true);
        e.requestFocus();
    }

    /** A themed option picker: one full-width button per option, then Cancel. */
    public static void listDialog(final Activity activity, String title,
                                  final String[] options, int surface, int border,
                                  int textColor, int rowFill, int cancelFill,
                                  final java.util.function.Consumer<Integer> picked) {
        final DialogPanel dp = dialogPanel(activity, surface, border);
        dp.panel.addView(sectionHeading(activity, title, textColor));
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            Button rowButton = button(activity, options[i], rowFill,
                    pressedOf(rowFill), textColor);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.setMargins(0, dp(activity, i == 0 ? 12 : 8), 0, 0);
            rowButton.setLayoutParams(rp);
            rowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dp.dialog.dismiss();
                    picked.accept(idx);
                }
            });
            dp.panel.addView(rowButton);
        }
        Button cancel = button(activity, "Cancel", cancelFill,
                pressedOf(cancelFill), textColor);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, dp(activity, 12), 0, 0);
        cancel.setLayoutParams(cp);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dp.dialog.dismiss();
            }
        });
        dp.panel.addView(cancel);
        showDialog(activity, dp, false);
    }

    /** A themed 24-hour time picker: a big readout over hour/minute steppers. */
    public static void timeDialog(final Activity activity, String title,
                                  int initialHour, int initialMinute,
                                  int surface, int border, int textColor,
                                  int mutedColor, int stepperFill, int cancelFill,
                                  int positiveFill, int positiveTextColor,
                                  final TimeSetListener listener) {
        final DialogPanel dp = dialogPanel(activity, surface, border);
        dp.panel.addView(sectionHeading(activity, title, textColor));
        final int[] hm = new int[]{initialHour, initialMinute};
        final TextView clock = new TextView(activity);
        clock.setTextSize(40);
        clock.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        clock.setTextColor(textColor);
        clock.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, dp(activity, 10), 0, dp(activity, 2));
        clock.setLayoutParams(clp);
        dp.panel.addView(clock);
        dp.panel.addView(stepperRow(activity, "Hour", hm, 0, 24, clock,
                stepperFill, textColor, mutedColor));
        dp.panel.addView(stepperRow(activity, "Minute", hm, 1, 60, clock,
                stepperFill, textColor, mutedColor));
        updateClock(clock, hm);
        addDialogButtons(activity, dp, "Set", cancelFill, textColor,
                positiveFill, positiveTextColor, new Runnable() {
                    @Override
                    public void run() {
                        listener.onTimeSet(hm[0], hm[1]);
                    }
                });
        showDialog(activity, dp, false);
    }

    private static void updateClock(TextView clock, int[] hm) {
        clock.setText(String.format(java.util.Locale.US, "%02d:%02d", hm[0], hm[1]));
    }

    private static LinearLayout stepperRow(final Activity activity, String label,
                                           final int[] hm, final int idx, final int mod,
                                           final TextView clock, int stepperFill,
                                           int textColor, int mutedColor) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, dp(activity, 10), 0, 0);
        row.setLayoutParams(rp);
        TextView lab = new TextView(activity);
        lab.setText(label);
        lab.setTextSize(14.5f);
        lab.setTextColor(mutedColor);
        row.addView(lab, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView val = new TextView(activity);
        val.setTextSize(17);
        val.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        val.setTextColor(textColor);
        val.setGravity(Gravity.CENTER);
        val.setText(String.format(java.util.Locale.US, "%02d", hm[idx]));
        Button minus = smallButton(activity, "-", stepperFill,
                pressedOf(stepperFill), textColor);
        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hm[idx] = (hm[idx] + mod - 1) % mod;
                val.setText(String.format(java.util.Locale.US, "%02d", hm[idx]));
                updateClock(clock, hm);
            }
        });
        row.addView(minus, new LinearLayout.LayoutParams(
                dp(activity, 52), ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
                dp(activity, 48), ViewGroup.LayoutParams.WRAP_CONTENT);
        row.addView(val, vp);
        Button plus = smallButton(activity, "+", stepperFill,
                pressedOf(stepperFill), textColor);
        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hm[idx] = (hm[idx] + 1) % mod;
                val.setText(String.format(java.util.Locale.US, "%02d", hm[idx]));
                updateClock(clock, hm);
            }
        });
        row.addView(plus, new LinearLayout.LayoutParams(
                dp(activity, 52), ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }
}
