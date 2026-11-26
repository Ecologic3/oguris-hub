package app.oguri.hub.vpn;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Watchdog for VPN monitoring (Hub 1.8). A package replacement
 * kills the monitoring service, and before 1.8 nothing restarted
 * it until the app was opened by hand: the module log simply went
 * silent and Tailscale was never switched (Adam's field report of
 * 26 Nov 2026: healthy evaluations until 01:48, then nothing, not
 * even a mobile-data evaluation while travelling, until the app
 * was opened at 12:09). BootReceiver now restarts the service on
 * MY_PACKAGE_REPLACED, and this watchdog is the backstop for every
 * other silent death: an inexact repeating alarm checks about
 * every 15 minutes whether monitoring is enabled but the service
 * is not running, restarts it, and leaves a line in the module
 * log when it does, so a revival is visible where Adam reads it.
 * While monitoring is off the watchdog cancels itself. The alarm
 * is armed from SwitchService.onCreate, so every path that runs
 * the service (boot, update, UI, this watchdog) re-arms it.
 */
public class WatchdogReceiver extends BroadcastReceiver {

    private static final int REQUEST_CODE = 702;

    public static void schedule(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent(ctx));
    }

    public static void cancel(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        am.cancel(pendingIntent(ctx));
    }

    private static PendingIntent pendingIntent(Context ctx) {
        Intent i = new Intent(ctx, WatchdogReceiver.class);
        return PendingIntent.getBroadcast(ctx, REQUEST_CODE, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Prefs prefs = new Prefs(context);
        if (!prefs.isMonitoring()) {
            cancel(context);
            return;
        }
        if (!SwitchService.isRunning()) {
            prefs.addLog("Watchdog: monitoring service was not running, restarted it");
            try {
                SwitchService.start(context);
            } catch (Exception e) {
                prefs.addLog("Watchdog: restart blocked by the system, will retry");
            }
        }
    }
}
