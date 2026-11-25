package app.oguri.hub.alarms;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Hub-side importer for the standalone Oguri's Alarms export file
 * (format "oguris-alarms-export", formatVersion 1, produced by the
 * 1.8 migration release). Restores the alarm store verbatim into this
 * module's own prefs, re-registers every enabled alarm with
 * AlarmManager through BackupScheduler, restores the direct-band bond
 * state, and attempts one band set-all sync so the band matches too.
 */
public final class HubAlarmImport {

    private HubAlarmImport() {}

    /** Result counts for reporting to the user. */
    public static final class Result {
        public int alarms;
        public int armed;
    }

    public static Result importFromJson(Context ctx, String jsonText) throws Exception {
        JSONObject root = new JSONObject(jsonText);
        if (!"oguris-alarms-export".equals(root.optString("format", ""))) {
            throw new IllegalArgumentException("Not an Oguri's Alarms export file");
        }
        if (root.optInt("formatVersion", -1) != 1) {
            throw new IllegalArgumentException("Unsupported export format version");
        }
        if (!"app.oguri.alarms".equals(root.optString("app", ""))) {
            throw new IllegalArgumentException("Export file is from a different app");
        }
        JSONArray alarms = root.getJSONArray("alarms");
        JSONArray ids = root.optJSONArray("alarmIds");

        SharedPreferences sp = ctx.getSharedPreferences("alarms_store",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor e = sp.edit();

        StringBuilder joined = new StringBuilder();
        Result result = new Result();
        for (int i = 0; i < alarms.length(); i++) {
            JSONObject a = alarms.getJSONObject(i);
            String id = a.getString("id");
            if (joined.length() > 0) joined.append(',');
            joined.append(id);
            if (a.isNull("customName")) {
                e.remove("name_" + id);
            } else {
                e.putString("name_" + id, a.getString("customName"));
            }
            e.putBoolean("enabled_" + id, a.getBoolean("enabled"));
            e.putInt("hour_" + id, a.getInt("hour"));
            e.putInt("minute_" + id, a.getInt("minute"));
            e.putInt("days_" + id, a.getInt("daysMask"));
            result.alarms++;
        }
        // Prefer the explicit ordered id list when present.
        if (ids != null && ids.length() == alarms.length()) {
            joined.setLength(0);
            for (int i = 0; i < ids.length(); i++) {
                if (i > 0) joined.append(',');
                joined.append(ids.getString(i));
            }
        }
        e.putString("alarm_ids", joined.toString());
        e.putInt("next_user_seq", root.optInt("nextUserSeq", 1));

        JSONObject settings = root.optJSONObject("settings");
        if (settings != null) {
            if (settings.isNull("backupSoundUri")) {
                e.remove("backup_sound_uri");
            } else {
                e.putString("backup_sound_uri", settings.getString("backupSoundUri"));
            }
        }
        JSONObject state = root.optJSONObject("state");
        if (state != null) {
            if (!state.isNull("lastBackupOutcome")) {
                e.putString("last_backup_outcome", state.getString("lastBackupOutcome"));
            }
            e.putInt("band_battery_level", state.optInt("bandBatteryLevel", -1));
            e.putLong("band_battery_read_at",
                    state.optLong("bandBatteryReadAtMillis", 0L));
        }
        e.apply();

        JSONObject band = root.optJSONObject("directBand");
        if (band != null) {
            SharedPreferences.Editor be = ctx.getSharedPreferences(
                    "alarms_direct_band_state", Context.MODE_PRIVATE).edit();
            if (band.isNull("secretKeyHex")) be.remove("secret_key");
            else be.putString("secret_key", band.getString("secretKeyHex"));
            if (band.isNull("fakeMac")) be.remove("fake_mac");
            else be.putString("fake_mac", band.getString("fakeMac"));
            be.putLong("iv_counter", band.optLong("ivCounter", 0L));
            be.apply();
        }

        // Re-register every enabled alarm with AlarmManager.
        for (int i = 0; i < alarms.length(); i++) {
            JSONObject a = alarms.getJSONObject(i);
            if (a.getBoolean("enabled")) {
                BackupScheduler.arm(ctx, a.getString("id"));
                result.armed++;
            }
        }
        // One best-effort band set-all sync, same call the module's own
        // UI makes after edits; failures surface through the transport's
        // own error reporting and never fail the import.
        try {
            BandTransports.active().setAllAlarms(ctx,
                    BandAlarmMapper.enabledAlarms(ctx));
        } catch (Exception ignored) {
            // Band unreachable right now; the next module sync fixes it.
        }
        return result;
    }
}
