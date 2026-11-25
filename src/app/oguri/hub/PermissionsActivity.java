package app.oguri.hub;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * The hub's one Permissions screen (since 1.5): every permission
 * the five modules need, grouped by module, replacing the
 * per-module permission sections the modules used to carry. Each
 * row follows the settled Alarms model: the permission's name, a
 * status line that always shows where things stand, and a Grant
 * (runtime permission) or Open (system settings) button that
 * fades to UiKit.GRANTED_ALPHA once granted and is never removed.
 * Statuses are computed live and refreshed on resume and after
 * every permission result.
 */
public class PermissionsActivity extends Activity {

    private static final int CARD = Color.rgb(22, 50, 74);
    private static final int CARD_EDGE = Color.rgb(38, 74, 102);
    private static final int TEXT = Color.rgb(235, 242, 247);
    private static final int MUTED = Color.rgb(158, 182, 199);
    private static final int ON_ACCENT = Color.rgb(11, 27, 38);
    private static final int DIVIDER = 0x403A5A74;

    private static final int REQ_RUNTIME = 42;

    /** One permission row: its views plus how to read and grant it. */
    private abstract static class Row {
        String name;
        boolean settings; // true: "Open" (system settings), false: "Grant"
        TextView statusView;
        Button button;

        abstract boolean granted(Context ctx);

        abstract String status(Context ctx);

        abstract void act(Activity activity);
    }

    private final List<Row> rows = new ArrayList<Row>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        UiKit.setSystemBars(this, UiKit.BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(UiKit.BG);
        UiKit.applyRootPadding(this, root);

        LinearLayout header = UiKit.header(this, "Permissions", TEXT,
                R.mipmap.ic_launcher);
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.setMargins(0, 0, 0, dp(16));
        header.setLayoutParams(headerLp);
        root.addView(header);

        root.addView(moduleCard("Alarms", R.mipmap.ic_mod_alarms,
                Color.rgb(139, 92, 246), alarmsRows()));
        root.addView(moduleCard("Steps", R.mipmap.ic_mod_steps,
                Color.rgb(61, 220, 151), stepsRows()));
        root.addView(moduleCard("Chips", R.mipmap.ic_mod_chips,
                Color.rgb(217, 164, 65), null));
        root.addView(moduleCard("Printer", R.mipmap.ic_mod_printer,
                Color.rgb(245, 168, 61), printerRows()));
        root.addView(moduleCard("VPN Switch", R.mipmap.ic_mod_vpn,
                Color.rgb(69, 216, 255), vpnRows()));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(UiKit.BG);
        scroll.addView(root);
        setContentView(scroll);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refresh();
    }

    // ------------------------------------------------------------ cards/rows

    private View moduleCard(String name, int iconRes, int accent, List<Row> moduleRows) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.cardBackground(this, CARD, CARD_EDGE));
        card.setPadding(dp(UiKit.CARD_PAD_H_DP), dp(UiKit.CARD_PAD_V_DP),
                dp(UiKit.CARD_PAD_H_DP), dp(UiKit.CARD_PAD_V_DP));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(UiKit.CARD_SPACING_DP));
        card.setLayoutParams(lp);

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        View bar = new View(this);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(accent);
        barBg.setCornerRadius(dp(3));
        bar.setBackground(barBg);
        head.addView(bar, new LinearLayout.LayoutParams(dp(6), dp(30)));
        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        iconLp.setMargins(dp(12), 0, dp(12), 0);
        head.addView(icon, iconLp);
        head.addView(UiKit.sectionHeading(this, name, TEXT));
        card.addView(head);

        if (moduleRows == null) {
            TextView none = new TextView(this);
            none.setText("No permissions needed");
            none.setTextColor(MUTED);
            none.setTextSize(14);
            LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            nLp.setMargins(0, dp(10), 0, 0);
            none.setLayoutParams(nLp);
            card.addView(none);
            return card;
        }

        for (int i = 0; i < moduleRows.size(); i++) {
            if (i > 0) {
                View divider = new View(this);
                divider.setBackgroundColor(DIVIDER);
                LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
                dLp.setMargins(0, dp(10), 0, dp(10));
                divider.setLayoutParams(dLp);
                card.addView(divider);
            }
            card.addView(rowView(moduleRows.get(i), accent));
        }
        return card;
    }

    private View rowView(final Row row, int accent) {
        rows.add(row);
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(10), 0, 0);
        line.setLayoutParams(lp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(this);
        name.setText(row.name);
        name.setTextColor(TEXT);
        name.setTextSize(16);
        texts.addView(name);
        row.statusView = new TextView(this);
        row.statusView.setTextColor(MUTED);
        row.statusView.setTextSize(13);
        texts.addView(row.statusView);
        line.addView(texts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.button = UiKit.pillButton(this, row.settings ? "Open" : "Grant",
                accent, UiKit.pressedOf(accent), 0, ON_ACCENT);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                dp(84), ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.setMargins(dp(12), 0, 0, 0);
        row.button.setLayoutParams(bLp);
        row.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (row.granted(PermissionsActivity.this)) {
                    Toast.makeText(PermissionsActivity.this,
                            row.name + ": already granted",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                row.act(PermissionsActivity.this);
            }
        });
        line.addView(row.button);
        return line;
    }

    private void refresh() {
        for (Row row : rows) {
            if (row.statusView == null) {
                continue;
            }
            row.statusView.setText(row.status(this));
            row.button.setAlpha(row.granted(this) ? UiKit.GRANTED_ALPHA : 1.0f);
        }
    }

    // ------------------------------------------------------- shared helpers

    private boolean has(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean batteryExempt() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }

    private void request(String[] permissions) {
        requestPermissions(permissions, REQ_RUNTIME);
    }

    private void openBatterySettings() {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        try {
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        }
    }

    private Row batteryRow(String name) {
        Row row = new Row() {
            @Override boolean granted(Context ctx) {
                return batteryExempt();
            }

            @Override String status(Context ctx) {
                return batteryExempt() ? "Exempt" : "NOT exempt";
            }

            @Override void act(Activity activity) {
                openBatterySettings();
            }
        };
        row.name = name;
        row.settings = true;
        return row;
    }

    private Row notificationsRow() {
        Row row = new Row() {
            @Override boolean granted(Context ctx) {
                return Build.VERSION.SDK_INT < 33
                        || has(Manifest.permission.POST_NOTIFICATIONS);
            }

            @Override String status(Context ctx) {
                return granted(ctx) ? "Granted" : "NOT granted";
            }

            @Override void act(Activity activity) {
                if (Build.VERSION.SDK_INT >= 33) {
                    request(new String[]{Manifest.permission.POST_NOTIFICATIONS});
                }
            }
        };
        row.name = "Notifications";
        return row;
    }

    // ------------------------------------------------------------ the rows

    private List<Row> alarmsRows() {
        List<Row> list = new ArrayList<Row>();

        Row exact = new Row() {
            @Override boolean granted(Context ctx) {
                AlarmManager am = (AlarmManager) ctx.getSystemService(
                        Context.ALARM_SERVICE);
                return am != null && am.canScheduleExactAlarms();
            }

            @Override String status(Context ctx) {
                return granted(ctx) ? "Allowed" : "NOT allowed";
            }

            @Override void act(Activity activity) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + getPackageName()));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                }
            }
        };
        exact.name = "Exact alarm permission";
        exact.settings = true;
        list.add(exact);

        list.add(batteryRow("Battery optimisation exemption"));
        list.add(notificationsRow());

        Row bluetooth = new Row() {
            @Override boolean granted(Context ctx) {
                if (Build.VERSION.SDK_INT >= 31) {
                    return has(Manifest.permission.BLUETOOTH_CONNECT);
                }
                return has(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            @Override String status(Context ctx) {
                return granted(ctx) ? "Granted" : "NOT granted";
            }

            @Override void act(Activity activity) {
                if (Build.VERSION.SDK_INT >= 31) {
                    request(new String[]{Manifest.permission.BLUETOOTH_CONNECT});
                } else {
                    request(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
                }
            }
        };
        bluetooth.name = "Bluetooth permission";
        list.add(bluetooth);
        return list;
    }

    private List<Row> stepsRows() {
        List<Row> list = new ArrayList<Row>();

        Row activity = new Row() {
            @Override boolean granted(Context ctx) {
                return Build.VERSION.SDK_INT < 29
                        || has(Manifest.permission.ACTIVITY_RECOGNITION);
            }

            @Override String status(Context ctx) {
                return granted(ctx) ? "Granted" : "Needed to read the step sensor";
            }

            @Override void act(Activity activity) {
                if (Build.VERSION.SDK_INT >= 29) {
                    request(new String[]{Manifest.permission.ACTIVITY_RECOGNITION});
                }
            }
        };
        activity.name = "Physical activity permission";
        list.add(activity);

        Row battery = new Row() {
            @Override boolean granted(Context ctx) {
                return batteryExempt();
            }

            @Override String status(Context ctx) {
                return batteryExempt()
                        ? "Exempt, counting will not be throttled"
                        : "Exemption recommended so Android does not pause counting";
            }

            @Override void act(Activity activity) {
                openBatterySettings();
            }
        };
        battery.name = "Battery optimisation";
        battery.settings = true;
        list.add(battery);
        return list;
    }

    private List<Row> printerRows() {
        List<Row> list = new ArrayList<Row>();

        Row bluetooth = new Row() {
            @Override boolean granted(Context ctx) {
                if (Build.VERSION.SDK_INT < 31) {
                    return true;
                }
                return has(Manifest.permission.BLUETOOTH_SCAN)
                        && has(Manifest.permission.BLUETOOTH_CONNECT);
            }

            @Override String status(Context ctx) {
                return granted(ctx) ? "Granted" : "NOT granted";
            }

            @Override void act(Activity activity) {
                if (Build.VERSION.SDK_INT >= 31) {
                    request(new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT});
                }
            }
        };
        bluetooth.name = "Bluetooth permission";
        list.add(bluetooth);

        Row location = new Row() {
            @Override boolean granted(Context ctx) {
                return has(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            @Override String status(Context ctx) {
                return granted(ctx) ? "Granted" : "NOT granted";
            }

            @Override void act(Activity activity) {
                request(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
            }
        };
        location.name = "Location permission";
        list.add(location);
        return list;
    }

    private List<Row> vpnRows() {
        List<Row> list = new ArrayList<Row>();

        Row location = new Row() {
            @Override boolean granted(Context ctx) {
                return has(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            @Override String status(Context ctx) {
                return granted(ctx) ? "Granted" : "NOT granted";
            }

            @Override void act(Activity activity) {
                request(new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
            }
        };
        location.name = "Location permission";
        list.add(location);

        list.add(batteryRow("Battery optimisation exemption"));
        list.add(notificationsRow());
        return list;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
