package app.oguri.hub;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Oguri's Hub home screen: one dark shell, five module cards.
 * Each card opens the module's own MainActivity, ported in full
 * under app.oguri.hub.<module>.
 */
public class MainActivity extends Activity {

    private static final int BG = Color.rgb(13, 32, 48);
    private static final int CARD = Color.rgb(22, 50, 74);
    private static final int CARD_EDGE = Color.rgb(38, 74, 102);
    private static final int TEXT = Color.rgb(235, 242, 247);
    private static final int MUTED = Color.rgb(158, 182, 199);
    private static final int GOLD = Color.rgb(255, 209, 102);

    private static final String[][] MODULES = {
        {"Alarms", "Band alarms, backups and sync", "app.oguri.hub.alarms", "255,138,128"},
        {"Steps", "Step counting, history and widget", "app.oguri.hub.steps", "105,240,174"},
        {"Chips", "Poker chip table and game engine", "app.oguri.hub.chips", "255,213,79"},
        {"Printer", "Thermal printer over BLE and SPP", "app.oguri.hub.printer", "130,177,255"},
        {"Oguri's VPN Switch", "Tailscale off at home, on away", "app.oguri.hub.vpn", "179,157,217"},
    };

    // Each card carries its module's own standalone launcher icon.
    private static final int[] MODULE_ICONS = {
        R.mipmap.ic_mod_alarms,
        R.mipmap.ic_mod_steps,
        R.mipmap.ic_mod_chips,
        R.mipmap.ic_mod_printer,
        R.mipmap.ic_mod_vpn,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));

        TextView title = new TextView(this);
        title.setText("Oguri's Hub");
        title.setTextColor(GOLD);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("All five Oguri apps, one home.\n");
        sub.setTextColor(MUTED);
        sub.setTextSize(15);
        root.addView(sub);

        for (int i = 0; i < MODULES.length; i++) {
            root.addView(moduleCard(MODULES[i], MODULE_ICONS[i]));
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);
    }

    private View moduleCard(final String[] m, int iconRes) {
        String[] rgb = m[3].split(",");
        int accent = Color.rgb(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]),
                Integer.parseInt(rgb[2]));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dp(1), CARD_EDGE);
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(lp);

        View bar = new View(this);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(accent);
        barBg.setCornerRadius(dp(3));
        bar.setBackground(barBg);
        card.addView(bar, new LinearLayout.LayoutParams(dp(6), dp(46)));

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(46), dp(46));
        iconLp.setMargins(dp(14), 0, 0, 0);
        card.addView(icon, iconLp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(dp(14), 0, 0, 0);
        TextView name = new TextView(this);
        name.setText(m[0]);
        name.setTextColor(TEXT);
        name.setTextSize(19);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        texts.addView(name);
        TextView desc = new TextView(this);
        desc.setText(m[1]);
        desc.setTextColor(MUTED);
        desc.setTextSize(14);
        texts.addView(desc);
        card.addView(texts, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = new TextView(this);
        arrow.setText("›");
        arrow.setTextColor(accent);
        arrow.setTextSize(28);
        card.addView(arrow);

        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent();
                i.setComponent(new ComponentName(MainActivity.this,
                        m[2] + ".MainActivity"));
                startActivity(i);
            }
        });
        return card;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
