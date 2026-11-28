package app.oguri.hub.notes;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * Notes module storage, one SharedPreferences file like every other
 * module: the sign-in (server, username, the short-lived access
 * token and the refresh cookie from sign-in), the last synced memo
 * stream per state as the offline cache, and the queue of changes
 * made while the server was unreachable. Nothing here leaves the
 * phone except the API calls themselves.
 */
public class Prefs {

    public static final String DEFAULT_SERVER = "https://mnemosyne:5230";

    private final SharedPreferences sp;

    public Prefs(Context ctx) {
        sp = ctx.getSharedPreferences("notes", Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------- sign-in

    public String getServer() {
        String server = sp.getString("server", DEFAULT_SERVER);
        // Since 1.17.6 the Memos front forces HTTPS (the dashboard
        // pattern); a server stored by an older build still says
        // http, so swap the scheme on read. saveSession stores
        // getServer(), which makes the swap permanent on next save.
        if (server.startsWith("http://")) {
            server = "https://" + server.substring("http://".length());
        }
        return server;
    }

    public String getUsername() {
        return sp.getString("username", "");
    }

    public String getAccessToken() {
        return sp.getString("access_token", "");
    }

    public long getAccessExpireMillis() {
        return sp.getLong("access_expire", 0);
    }

    public String getRefreshCookie() {
        return sp.getString("refresh_cookie", "");
    }

    public boolean hasSession() {
        return getAccessToken().length() > 0 || getRefreshCookie().length() > 0;
    }

    public void saveSession(String server, String username, String accessToken,
                            long expireMillis, String refreshCookie) {
        SharedPreferences.Editor e = sp.edit();
        e.putString("server", server);
        e.putString("username", username);
        if (accessToken != null) {
            e.putString("access_token", accessToken);
        }
        if (expireMillis > 0) {
            e.putLong("access_expire", expireMillis);
        }
        if (refreshCookie != null && refreshCookie.length() > 0) {
            e.putString("refresh_cookie", refreshCookie);
        }
        e.apply();
    }

    public void saveTokens(String accessToken, long expireMillis, String refreshCookie) {
        saveSession(getServer(), getUsername(), accessToken, expireMillis, refreshCookie);
    }

    public void clearSession() {
        sp.edit().remove("access_token").remove("access_expire")
                .remove("refresh_cookie").apply();
    }

    // ---------------------------------------------------------- stream cache

    private String cacheKey(String state) {
        return "ARCHIVED".equals(state) ? "cache_archived" : "cache_normal";
    }

    public List<Memo> getCache(String state) {
        try {
            return Memo.listFromJsonArray(new JSONArray(sp.getString(cacheKey(state), "[]")));
        } catch (Exception e) {
            return Memo.listFromJsonArray(null);
        }
    }

    public void saveCache(String state, List<Memo> memos) {
        sp.edit().putString(cacheKey(state), Memo.listToJsonArray(memos).toString()).apply();
    }

    /** Finds one memo by name across both state caches, or null. */
    public Memo findMemo(String name) {
        for (String state : new String[]{"NORMAL", "ARCHIVED"}) {
            for (Memo m : getCache(state)) {
                if (m.name.equals(name)) {
                    return m;
                }
            }
        }
        return null;
    }

    /** Inserts or replaces a memo in the cache of its own state. */
    public void upsertMemo(Memo memo) {
        List<Memo> list = getCache(memo.state);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).name.equals(memo.name)) {
                list.set(i, memo);
                saveCache(memo.state, list);
                return;
            }
        }
        list.add(0, memo);
        saveCache(memo.state, list);
    }

    public void removeMemo(String name) {
        for (String state : new String[]{"NORMAL", "ARCHIVED"}) {
            List<Memo> list = getCache(state);
            boolean changed = false;
            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i).name.equals(name)) {
                    list.remove(i);
                    changed = true;
                }
            }
            if (changed) {
                saveCache(state, list);
            }
        }
    }

    /** Renames a cached memo (a queued create landing) in place. */
    public void renameMemo(String oldName, String newName) {
        for (String state : new String[]{"NORMAL", "ARCHIVED"}) {
            List<Memo> list = getCache(state);
            boolean changed = false;
            for (Memo m : list) {
                if (m.name.equals(oldName)) {
                    m.name = newName;
                    changed = true;
                }
            }
            if (changed) {
                saveCache(state, list);
            }
        }
    }

    // ---------------------------------------------------------- pending queue
    //
    // Ops are JSON objects, flushed in order:
    //   {"op":"create","local":"local-<id>","content":...,"visibility":...}
    //   {"op":"update","name":...,"content":...,"pinned":bool,"state":...}
    //   {"op":"delete","name":...}
    //   {"op":"attach","memo":<name or local name>,"file":<path>,
    //    "filename":...,"type":...}

    public JSONArray getPending() {
        try {
            return new JSONArray(sp.getString("pending", "[]"));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    public void savePending(JSONArray ops) {
        sp.edit().putString("pending", ops.toString()).apply();
    }

    public void enqueue(JSONObject op) {
        JSONArray ops = getPending();
        ops.put(op);
        savePending(ops);
    }

    public boolean hasPendingFor(String name) {
        JSONArray ops = getPending();
        for (int i = 0; i < ops.length(); i++) {
            JSONObject op = ops.optJSONObject(i);
            if (op == null) {
                continue;
            }
            if (name.equals(op.optString("name", ""))
                    || name.equals(op.optString("local", ""))
                    || name.equals(op.optString("memo", ""))) {
                return true;
            }
        }
        return false;
    }
}
