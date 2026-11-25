package app.oguri.hub;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;

import app.oguri.hub.alarms.HubAlarmImport;
import app.oguri.hub.steps.StepService;
import app.oguri.hub.steps.StepsWidget;

/**
 * Data import for the hub: reads one JSON export file produced by a
 * standalone Oguri app's migration release and restores it into the
 * matching hub module. Mode comes from EXTRA_MODE ("steps"/"alarms").
 */
public class ImportActivity extends Activity {

    public static final String EXTRA_MODE = "mode";
    private static final int REQ_OPEN = 41;

    private static final int BG = Color.rgb(13, 32, 48);
    private static final int TEXT = Color.rgb(235, 242, 247);
    private static final int MUTED = Color.rgb(158, 182, 199);
    private static final int GOLD = Color.rgb(255, 209, 102);

    private String mode;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = "steps";
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));

        TextView title = new TextView(this);
        title.setText("Import " + ("alarms".equals(mode) ? "Alarms" : "Steps") + " data");
        title.setTextColor(GOLD);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        status = new TextView(this);
        status.setText("Pick the export file you saved from the standalone app.\n");
        status.setTextColor(MUTED);
        status.setTextSize(15);
        root.addView(status);

        TextView done = new TextView(this);
        done.setText("DONE");
        done.setTextColor(BG);
        done.setTextSize(15);
        done.setTypeface(Typeface.DEFAULT_BOLD);
        done.setGravity(Gravity.CENTER);
        done.setPadding(dp(16), dp(13), dp(16), dp(13));
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(GOLD);
        bg.setCornerRadius(dp(10));
        done.setBackground(bg);
        done.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        root.addView(done);
        setContentView(root);

        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"application/json", "text/plain"});
        startActivityForResult(i, REQ_OPEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_OPEN) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            status.setText("Import cancelled.\n");
            return;
        }
        Uri uri = data.getData();
        // Keep read access across restarts where the provider allows it.
        try {
            getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {
            // Not all providers grant persistable permissions; harmless.
        }
        try {
            String text = readAll(uri);
            if ("alarms".equals(mode)) {
                HubAlarmImport.Result r = HubAlarmImport.importFromJson(this, text);
                status.setText("Imported " + r.alarms + " alarms, " + r.armed
                        + " of them switched on and re-registered with the "
                        + "system alarm service.\n");
                Toast.makeText(this, "Imported " + r.alarms + " alarms",
                        Toast.LENGTH_LONG).show();
            } else {
                int days = importSteps(text);
                status.setText("Imported " + days + " days of step history.\n");
                Toast.makeText(this, "Imported " + days + " days of history",
                        Toast.LENGTH_LONG).show();
            }
            status.setTextColor(TEXT);
        } catch (Exception e) {
            status.setText("Import failed: " + e.getMessage() + "\n");
            status.setTextColor(Color.rgb(248, 113, 113));
            Toast.makeText(this, "Import failed: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private String readAll(Uri uri) throws Exception {
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) throw new java.io.IOException("Could not open that file");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line).append('\n');
        reader.close();
        return sb.toString();
    }

    /**
     * Steps export schema 1: merges history into the hub steps store
     * (higher count per day wins, matching the app's own import rule),
     * restores the paused setting, and deliberately does NOT restore
     * counting.lastRaw, so the hub re-baselines on its first reading.
     * Returns the number of days written.
     */
    private int importSteps(String jsonText) throws Exception {
        JSONObject root = new JSONObject(jsonText);
        if (!"app.oguri.steps".equals(root.optString("app", ""))) {
            throw new IllegalArgumentException("Not an Oguri's Steps export file");
        }
        if (root.optInt("schema", -1) != 1) {
            throw new IllegalArgumentException("Unsupported export schema");
        }
        JSONArray hist = root.getJSONArray("history");

        SharedPreferences sp = getSharedPreferences("steps_prefs", MODE_PRIVATE);
        Map<String, Integer> merged = new TreeMap<String, Integer>();
        String existing = sp.getString("steps_history", "");
        if (existing != null && existing.length() > 0) {
            String[] parts = existing.split(";");
            for (int i = 0; i < parts.length; i++) {
                int eq = parts[i].indexOf('=');
                if (eq > 0) {
                    try {
                        merged.put(parts[i].substring(0, eq),
                                Integer.valueOf(parts[i].substring(eq + 1)));
                    } catch (NumberFormatException ignored) {
                        // Skip a corrupt entry, same as StepStore parsing.
                    }
                }
            }
        }
        int written = 0;
        for (int i = 0; i < hist.length(); i++) {
            JSONObject day = hist.getJSONObject(i);
            String date = day.getString("date");
            int steps = day.getInt("steps");
            Integer cur = merged.get(date);
            if (cur == null || steps > cur.intValue()) {
                merged.put(date, Integer.valueOf(steps));
            }
            written++;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : merged.entrySet()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.getKey()).append('=').append(e.getValue().intValue());
        }
        SharedPreferences.Editor ed = sp.edit();
        ed.putString("steps_history", sb.toString());
        JSONObject settings = root.optJSONObject("settings");
        if (settings != null && settings.has("paused")) {
            ed.putBoolean("steps_paused", settings.getBoolean("paused"));
        }
        ed.apply();

        StepsWidget.updateAll(this);
        StepService.start(this);
        return written;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
