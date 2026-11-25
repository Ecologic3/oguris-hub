package app.oguri.hub.steps;

import app.oguri.hub.R;
import app.oguri.hub.UiKit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int REQ_IMPORT = 7;
    // The shared hub background since 1.3 (was a near-identical
    // private navy, #0F141F, before the unification).
    private static final int BG = UiKit.BG;
    private static final int CARD = Color.rgb(0x1B, 0x22, 0x33);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(0x94, 0xA3, 0xB8);
    private static final int ACCENT = Color.rgb(0x3D, 0xDC, 0x97);
    private static final int AMBER = Color.rgb(0xF0, 0xB4, 0x54);
    private static final int CHIPBG = Color.rgb(0x2A, 0x33, 0x47);

    private StepStore store;
    private TextView todayStepsView;
    private TextView todayStatsView;
    private TextView summaryView;
    private TextView inspectPill;
    private int selectedBar = -1;
    private String[] inspectTexts = new String[0];
    private TextView pausedBanner;
    private ChartView chart;
    private LinearLayout historyList;
    private LinearLayout chipRow;
    private int rangeDays = 7; // 7, 30, 90, 0 = all
    private final Handler handler = new Handler();
    private boolean resumed = false;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!resumed) return;
            refreshDynamic();
            handler.postDelayed(this, 2000);
        }
    };

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiKit.setSystemBars(this, BG);
        store = new StepStore(this);

        // No permission is requested on open since 1.5: grants live
        // on the hub's shared Permissions screen now. The counting
        // service starts as before; once the activity permission is
        // granted there, the next open registers the sensor.
        StepService.start(this);
        buildUi();
        refreshDynamic();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_IMPORT || resultCode != RESULT_OK
                || data == null || data.getData() == null) return;
        try {
            java.io.InputStream in = getContentResolver().openInputStream(data.getData());
            if (in == null) throw new java.io.IOException("no stream");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(in, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line).append('\n');
            reader.close();
            int changed = store.importCsvText(sb.toString());
            StepsWidget.updateAll(this);
            buildUi();
            refreshDynamic();
            Toast.makeText(this, changed > 0
                    ? "Imported " + changed + " days of history"
                    : "No days found in that file", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not read that file", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        buildUi();
        handler.post(ticker);
        refreshDynamic();
    }

    @Override protected void onPause() {
        resumed = false;
        handler.removeCallbacks(ticker);
        super.onPause();
    }

    // ---------- UI construction ----------

    private LinearLayout card() {
        return UiKit.card(this, CARD, 0);
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        UiKit.applyRootPadding(this, root);
        scroll.addView(root);

        root.addView(UiKit.header(this, "Steps", TEXT,
                R.mipmap.ic_mod_steps));
        String dateStr = new SimpleDateFormat("EEEE d MMMM", Locale.UK).format(new Date());
        TextView sub = text(dateStr, 14, MUTED, false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(0, dp(2), 0, dp(16));
        sub.setLayoutParams(subLp);
        root.addView(sub);

        pausedBanner = text("", 14, ACCENT, true);
        pausedBanner.setVisibility(View.GONE);
        root.addView(pausedBanner);

        // Today card
        LinearLayout todayCard = card();
        todayStepsView = text("0", 58, TEXT, true);
        todayStepsView.setGravity(Gravity.CENTER);
        todayCard.addView(todayStepsView);
        TextView stepsLabel = text("steps today", 14, MUTED, false);
        stepsLabel.setGravity(Gravity.CENTER);
        todayCard.addView(stepsLabel);
        todayStatsView = text("", 15, TEXT, false);
        todayStatsView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stLp.setMargins(0, dp(12), 0, 0);
        todayStatsView.setLayoutParams(stLp);
        todayCard.addView(todayStatsView);
        root.addView(todayCard);

        SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sm == null || sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) == null) {
            TextView warn = text("This phone reports no hardware step counter sensor, "
                    + "so counting cannot work.", 14, Color.rgb(0xF8, 0x71, 0x71), false);
            root.addView(warn);
        }

        // History card: chips + summary + chart + list
        LinearLayout histCard = card();
        histCard.addView(UiKit.sectionHeading(this, "History", TEXT));
        chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        chipLp.setMargins(0, dp(10), 0, dp(6));
        chipRow.setLayoutParams(chipLp);
        histCard.addView(chipRow);
        rebuildChips();

        summaryView = text("", 14, MUTED, false);
        histCard.addView(summaryView);

        inspectPill = text("", 13.5f, TEXT, false);
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setCornerRadius(dp(14));
        pillBg.setColor(CHIPBG);
        inspectPill.setBackground(pillBg);
        inspectPill.setPadding(dp(10), dp(5), dp(10), dp(5));
        LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pillLp.setMargins(0, dp(6), 0, 0);
        inspectPill.setLayoutParams(pillLp);
        inspectPill.setVisibility(View.GONE);
        inspectPill.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                selectedBar = -1;
                refreshDynamic();
            }
        });
        histCard.addView(inspectPill);

        chart = new ChartView(this);
        chart.setBarTapListener(new ChartView.BarTapListener() {
            @Override public void onBarTapped(int index) {
                selectedBar = index == selectedBar ? -1 : index;
                refreshDynamic();
            }
        });
        LinearLayout.LayoutParams chartLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(170));
        chartLp.setMargins(0, dp(8), 0, dp(10));
        chart.setLayoutParams(chartLp);
        histCard.addView(chart);

        historyList = new LinearLayout(this);
        historyList.setOrientation(LinearLayout.VERTICAL);
        histCard.addView(historyList);
        root.addView(histCard);

        // Setup card. The two permission rows that lived here
        // until 1.4 (physical activity, battery optimisation) are
        // on the hub's shared Permissions screen since 1.5; what
        // stays is counting control and the history exchange.
        LinearLayout setup = card();
        setup.addView(UiKit.sectionHeading(this, "Setup", TEXT));
        setup.addView(setupRow("Counting",
                store.paused() ? "Paused" : "Running",
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        store.setPaused(!store.paused());
                        StepService.start(MainActivity.this);
                        buildUi();
                        refreshDynamic();
                        Toast.makeText(MainActivity.this,
                                store.paused() ? "Counting paused" : "Counting resumed",
                                Toast.LENGTH_SHORT).show();
                    }
                }, store.paused() ? "Resume" : "Pause"));
        setup.addView(setupRow("Export history",
                "Share all recorded days as CSV",
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("text/plain");
                        i.putExtra(Intent.EXTRA_SUBJECT, "Steps history");
                        i.putExtra(Intent.EXTRA_TEXT, store.toCsv());
                        startActivity(Intent.createChooser(i, "Export history"));
                    }
                }, "Export"));
        setup.addView(setupRow("Import history",
                "Load a CSV exported from your old pedometer or this app. "
                        + "Days merge in, keeping the higher count per day.",
                new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("*/*");
                        i.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                                "text/csv", "text/comma-separated-values", "text/plain"});
                        startActivityForResult(i, REQ_IMPORT);
                    }
                }, "Import"));
        root.addView(setup);

        setContentView(scroll);
    }

    private LinearLayout setupRow(String title, String detail,
                                    final View.OnClickListener action, String buttonText) {
        return setupRow(title, detail, action, buttonText, false);
    }

    /**
     * One Setup row: title, state/detail text, and an action button.
     * With {@code granted} set (a permission already in place) the
     * button fades to the shared granted alpha instead of being
     * left out, the Alarms model.
     */
    private LinearLayout setupRow(String title, String detail,
                                    final View.OnClickListener action, String buttonText,
                                    boolean granted) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, 0);
        row.setLayoutParams(lp);
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        texts.addView(text(title, 15, TEXT, false));
        texts.addView(text(detail, 13, MUTED, false));
        row.addView(texts);
        if (action != null) {
            Button b = UiKit.smallButton(this, buttonText, CHIPBG,
                    UiKit.pressedOf(CHIPBG), TEXT);
            b.setAlpha(granted ? UiKit.GRANTED_ALPHA : 1.0f);
            b.setOnClickListener(action);
            row.addView(b);
        }
        return row;
    }

    private void rebuildChips() {
        chipRow.removeAllViews();
        int[] ranges = {7, 30, 90, 0};
        String[] names = {"7 days", "30 days", "90 days", "All time"};
        for (int i = 0; i < ranges.length; i++) {
            final int r = ranges[i];
            boolean selected = rangeDays == r;
            Button b = UiKit.pillButton(this, names[i],
                    selected ? ACCENT : CHIPBG,
                    UiKit.pressedOf(selected ? ACCENT : CHIPBG), 0,
                    selected ? Color.rgb(0x0B, 0x1F, 0x16) : TEXT);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.setMargins(dp(6), 0, 0, 0);
            b.setLayoutParams(lp);
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    rangeDays = r;
                    selectedBar = -1;
                    rebuildChips();
                    refreshDynamic();
                }
            });
            chipRow.addView(b);
        }
    }

    // ---------- Data refresh ----------

    private void refreshDynamic() {
        if (todayStepsView == null) return;
        int today = store.todaySteps();
        todayStepsView.setText(String.format(Locale.US, "%,d", today));
        todayStatsView.setText(String.format(Locale.US,
                "%.2f km      %d kcal      ~%d min walking",
                StepStore.distanceKm(today), StepStore.calories(today),
                Math.round(today / 100.0)));
        pausedBanner.setVisibility(store.paused() ? View.VISIBLE : View.GONE);
        if (store.paused()) pausedBanner.setText("Counting is paused");

        // Decide the window.
        List<String> keys = new ArrayList<String>();
        List<Integer> vals = new ArrayList<Integer>();
        Map<String, Integer> hist = store.history();
        if (rangeDays == 0) {
            List<String> days = new ArrayList<String>(hist.keySet());
            java.util.Collections.sort(days);
            // Fill gaps between first recorded day and today with zeros.
            if (!days.isEmpty()) {
                String first = days.get(0);
                String last = StepStore.todayKey();
                String cur = first;
                while (cur.compareTo(last) <= 0) {
                    keys.add(cur);
                    Integer v = hist.get(cur);
                    vals.add(v == null ? 0 : v);
                    cur = nextDay(cur);
                }
            }
        } else {
            String[] ks = store.lastNDayKeys(rangeDays);
            int[] vs = store.lastNDays(rangeDays);
            for (int i = 0; i < ks.length; i++) { keys.add(ks[i]); vals.add(vs[i]); }
        }

        int total = 0, best = 0; String bestDay = "";
        for (int i = 0; i < vals.size(); i++) {
            total += vals.get(i);
            if (vals.get(i) > best) { best = vals.get(i); bestDay = keys.get(i); }
        }
        // Daily average over recorded days only (26 Oct 2026):
        // days with no record do not count as zeros, and today is
        // excluded while it is still in progress.
        String todayKey = StepStore.todayKey();
        int recSum = 0, recDays = 0;
        for (int i = 0; i < keys.size(); i++) {
            if (!keys.get(i).equals(todayKey) && hist.containsKey(keys.get(i))) {
                recSum += vals.get(i);
                recDays++;
            }
        }
        int avg = recDays > 0 ? Math.round((float) recSum / recDays) : 0;
        String rangeName = rangeDays == 0 ? "All time" : "Last " + rangeDays + " days";
        summaryView.setText(String.format(Locale.US,
                "%s · Total %,d · Daily average %,d · Best %s (%,d)",
                rangeName, total, avg,
                bestDay.isEmpty() ? "-" : prettyDate(bestDay), best));

        boolean weekly = rangeDays == 90 || (rangeDays == 0 && keys.size() > 62);
        if (weekly) {
            // Aggregate into Monday-start weeks.
            List<String> wKeys = new ArrayList<String>();
            List<Integer> wVals = new ArrayList<Integer>();
            for (int i = 0; i < keys.size(); i++) {
                String mk = mondayKey(keys.get(i));
                int at = wKeys.indexOf(mk);
                if (at < 0) { wKeys.add(mk); wVals.add(vals.get(i)); }
                else wVals.set(at, wVals.get(at) + vals.get(i));
            }
            int n = wKeys.size();
            int[] chartVals = new int[n];
            String[] chartLabels = new String[n];
            String[] chartValueLabels = new String[n];
            inspectTexts = new String[n];
            int bestIdx = -1, wBest = -1;
            long wSum = 0; int wCount = 0;
            for (int i = 0; i < n; i++) {
                int v = wVals.get(i);
                chartVals[i] = v;
                if (v > wBest) { wBest = v; bestIdx = i; }
                if (v > 0) { wSum += v; wCount++; chartValueLabels[i] = fmtSteps(v); }
                inspectTexts[i] = String.format(Locale.US,
                        "Week of %s · %,d steps · %.2f km",
                        prettyDate(wKeys.get(i)), v, StepStore.distanceKm(v));
            }
            int lastLabeled = -10;
            for (int i = 0; i < n; i++) {
                if (i % 2 == 0 && i - lastLabeled >= 2) {
                    chartLabels[i] = monthDay(wKeys.get(i));
                    lastLabeled = i;
                }
            }
            float wAvg = wCount > 0 ? (float) wSum / wCount : -1f;
            String wAvgLabel = wCount > 0 ? "avg " + fmtSteps(Math.round(wAvg)) : null;
            if (selectedBar >= n) selectedBar = -1;
            chart.setData(chartVals, chartLabels, chartValueLabels,
                    n - 1, bestIdx, selectedBar, wAvg, wAvgLabel);
        } else {
            int n = keys.size();
            int[] chartVals = new int[n];
            String[] chartLabels = new String[n];
            String[] chartValueLabels = new String[n];
            inspectTexts = new String[n];
            int bestIdx = -1;
            for (int i = 0; i < n; i++) {
                int v = vals.get(i);
                chartVals[i] = v;
                if (v == best && best > 0 && bestIdx < 0) bestIdx = i;
                inspectTexts[i] = String.format(Locale.US, "%s · %,d steps · %.2f km",
                        prettyDate(keys.get(i)), v, StepStore.distanceKm(v));
            }
            if (n <= 7) {
                for (int i = 0; i < n; i++) chartLabels[i] = weekdayLetter(keys.get(i));
            } else if (n <= 14) {
                chartLabels[0] = monthDay(keys.get(0));
                chartLabels[n / 2] = monthDay(keys.get(n / 2));
                chartLabels[n - 1] = monthDay(keys.get(n - 1));
            } else {
                int step = (int) Math.ceil(n / 5.0);
                int lastLabeled = -10;
                for (int i = 0; i < n; i += step) {
                    chartLabels[i] = monthDay(keys.get(i));
                    lastLabeled = i;
                }
                if (n - 1 - lastLabeled >= 2) chartLabels[n - 1] = monthDay(keys.get(n - 1));
            }
            if (n <= 14) {
                for (int i = 0; i < n; i++) {
                    chartValueLabels[i] = String.format(Locale.US, "%,d", vals.get(i));
                }
            } else if (bestIdx >= 0) {
                chartValueLabels[bestIdx] = String.format(Locale.US, "%,d", best);
            }
            if (selectedBar >= n) selectedBar = -1;
            chart.setData(chartVals, chartLabels, chartValueLabels,
                    n - 1, bestIdx, selectedBar, avg,
                    avg > 0 ? "avg " + String.format(Locale.US, "%,d", avg) : null);
        }

        // Inspect pill for a tapped bar.
        if (selectedBar >= 0 && selectedBar < inspectTexts.length) {
            inspectPill.setText(inspectTexts[selectedBar]);
            inspectPill.setVisibility(View.VISIBLE);
        } else {
            inspectPill.setVisibility(View.GONE);
        }

        // History rows, newest first. The best day of the current
        // range is bold amber with a star. Days with no record are
        // skipped entirely (no zero rows for the time before history
        // starts); a recorded day shows even when its count is 0.
        historyList.removeAllViews();
        for (int i = keys.size() - 1; i >= 0; i--) {
            int v = vals.get(i);
            boolean isToday = keys.get(i).equals(StepStore.todayKey());
            if (v == 0 && !isToday && !hist.containsKey(keys.get(i))) continue;
            historyList.addView(historyRow(keys.get(i), v, isToday,
                    keys.get(i).equals(bestDay) && best > 0));
        }
        if (historyList.getChildCount() == 0) {
            historyList.addView(text("No steps recorded yet. "
                    + "Grant the physical activity permission and take a walk.",
                    14, MUTED, false));
        }
    }

    private LinearLayout historyRow(String day, int steps, boolean isToday, boolean isBest) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(7), 0, dp(7));
        row.setLayoutParams(lp);
        String label = isToday ? "Today" : prettyDate(day);
        if (isBest) label = "\u2605 " + label;
        TextView d = text(label, 15,
                isToday ? ACCENT : (isBest ? AMBER : TEXT), isToday || isBest);
        d.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(d);
        TextView s = text(String.format(Locale.US, "%,d", steps), 15,
                isBest ? AMBER : TEXT, true);
        row.addView(s);
        TextView km = text(String.format(Locale.US, "  %.2f km", StepStore.distanceKm(steps)),
                13, MUTED, false);
        row.addView(km);
        return row;
    }

    private String chartLabel(String dayKey, int index, int count) {
        if (count <= 7) return weekdayLetter(dayKey);
        if (count <= 31) return index % 5 == 0 || index == count - 1 ? dayOfMonth(dayKey) : "";
        return index % 30 == 0 || index == count - 1 ? dayOfMonth(dayKey) : "";
    }

    private static String monthDay(String dayKey) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dayKey);
            return new SimpleDateFormat("d MMM", Locale.UK).format(d);
        } catch (Exception e) {
            return dayKey;
        }
    }

    private static String mondayKey(String dayKey) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = f.parse(dayKey);
            Calendar c = Calendar.getInstance();
            c.setFirstDayOfWeek(Calendar.MONDAY);
            c.setTime(d);
            c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            return f.format(c.getTime());
        } catch (Exception e) {
            return dayKey;
        }
    }

    private static String fmtSteps(int v) {
        if (v >= 10000) return String.format(Locale.US, "%.1fk", v / 1000.0);
        return String.format(Locale.US, "%,d", v);
    }

    private static String nextDay(String dayKey) {
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = f.parse(dayKey);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            c.add(Calendar.DAY_OF_YEAR, 1);
            return f.format(c.getTime());
        } catch (Exception e) {
            return dayKey;
        }
    }

    private static String prettyDate(String dayKey) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dayKey);
            return new SimpleDateFormat("EEE d MMM", Locale.UK).format(d);
        } catch (Exception e) {
            return dayKey;
        }
    }

    private static String weekdayLetter(String dayKey) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dayKey);
            String s = new SimpleDateFormat("EEE", Locale.UK).format(d);
            return s.substring(0, 1);
        } catch (Exception e) {
            return "";
        }
    }

    private static String dayOfMonth(String dayKey) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dayKey);
            return new SimpleDateFormat("d", Locale.UK).format(d);
        } catch (Exception e) {
            return "";
        }
    }
}
