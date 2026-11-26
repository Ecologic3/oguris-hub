package app.oguri.hub.vpn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Restarts the monitoring service after a reboot, if monitoring
 * is on, and after the app itself is updated (Hub 1.8): a package
 * replacement kills the running service, and before 1.8 nothing
 * brought it back until the app was opened by hand. Both paths
 * also re-arm the watchdog (alarms do not survive a reboot).
 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        Prefs prefs = new Prefs(context);
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            WatchdogReceiver.schedule(context);
            if (prefs.isMonitoring()) {
                SwitchService.start(context);
            }
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            WatchdogReceiver.schedule(context);
            if (prefs.isMonitoring()) {
                prefs.addLog("App updated, monitoring restarted");
                SwitchService.start(context);
            }
        }
    }
}
