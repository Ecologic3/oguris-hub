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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Oguri Hub home screen: one dark shell, five module cards.
 * Each card opens the module's own MainActivity, ported in full
 * under app.oguri.hub.<module>. A migration card at the bottom
 * imports data exported from the standalone Steps and Alarms apps.
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
        {"VPN Switch", "Tailscale off at home, on away", "app.oguri.hub.vpn", "179,157,217"},
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
        title.setText("Oguri Hub");
        title.setTextColor(GOLD);
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("All five Oguri apps, one home.\n");
        sub.setTextColor(MUTED);
        sub.setTextSize(15);
        root.addView(sub);

        for (final String[] m : MODULES) {
            root.addView(moduleCard(m));
        }

        TextView migTitle = new TextView(this);
        migTitle.setText("\nDATA MIGRATION");
        migTitle.setTextColor(GOLD);
        migTitle.setTextSize(13);
        migTitle.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(migTitle);

        TextView migText = new TextView(this);
        migText.setText("Coming from the standalone apps? Export your data there "
                + "(Steps and Alarms have an Export button in their migration "
                + "releases), then import the files here. Alarms are re-registered "
                + "with the system as soon as they are imported.\n");
        migText.setTextColor(MUTED);
        migText.setTextSize(14);
        root.addView(migText);

        root.addView(actionButton("IMPORT STEPS DATA", new View.OnClickListener() {
            @Override public void onClick(View v) { openImport("steps"); }
        }));
        root.addView(actionButton("IMPORT ALARMS DATA", new View.OnClickListener() {
            @Override public void onClick(View v) { openImport("alarms"); }
        }));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);
    }

    private View moduleCard(final String[] m) {
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

    private TextView actionButton(String label, View.OnClickListener click) {
        TextView b = new TextView(this);
        b.setText(label);
        b.setTextColor(BG);
        b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(13), dp(16), dp(13));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(GOLD);
        bg.setCornerRadius(dp(10));
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        b.setLayoutParams(lp);
        b.setOnClickListener(click);
        return b;
    }

    private void openImport(String mode) {
        Intent i = new Intent(this, ImportActivity.class);
        i.putExtra(ImportActivity.EXTRA_MODE, mode);
        startActivity(i);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
