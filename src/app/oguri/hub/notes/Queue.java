package app.oguri.hub.notes;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Writers for the pending queue (see Prefs): the one place that
 * knows the op shapes, shared by the stream and the editor.
 */
public class Queue {

    private Queue() {
    }

    /** Queues a create for a local memo (its name is the local id). */
    public static void enqueueCreate(Prefs prefs, Memo memo) {
        try {
            JSONObject op = new JSONObject();
            op.put("op", "create");
            op.put("local", memo.name);
            op.put("content", memo.content);
            op.put("visibility", memo.visibility);
            prefs.enqueue(op);
        } catch (Exception ignored) {
        }
    }

    /**
     * Queues (or refreshes in place) the full-snapshot update op
     * for a memo: content, pinned and state travel together, so a
     * second edit before the sync just rewrites the same op.
     */
    public static void enqueueUpdate(Prefs prefs, Memo memo) {
        try {
            JSONArray ops = prefs.getPending();
            for (int i = 0; i < ops.length(); i++) {
                JSONObject op = ops.optJSONObject(i);
                if (op != null && "update".equals(op.optString("op"))
                        && memo.name.equals(op.optString("name"))) {
                    op.put("content", memo.content);
                    op.put("pinned", memo.pinned);
                    op.put("state", memo.state);
                    prefs.savePending(ops);
                    return;
                }
            }
            JSONObject op = new JSONObject();
            op.put("op", "update");
            op.put("name", memo.name);
            op.put("content", memo.content);
            op.put("pinned", memo.pinned);
            op.put("state", memo.state);
            prefs.enqueue(op);
        } catch (Exception ignored) {
        }
    }

    public static void enqueueDelete(Prefs prefs, String name) {
        try {
            JSONObject op = new JSONObject();
            op.put("op", "delete");
            op.put("name", name);
            prefs.enqueue(op);
        } catch (Exception ignored) {
        }
    }

    public static void enqueueAttach(Prefs prefs, String memoName, String filePath,
                                     String filename, String type) {
        try {
            JSONObject op = new JSONObject();
            op.put("op", "attach");
            op.put("memo", memoName);
            op.put("file", filePath);
            op.put("filename", filename);
            op.put("type", type);
            prefs.enqueue(op);
        } catch (Exception ignored) {
        }
    }

    /**
     * Rewrites the content of a still-queued create (a local memo
     * edited again before its first sync), so the server is born
     * with the latest text.
     */
    public static void rewriteCreateContent(Prefs prefs, String localName, String content) {
        try {
            JSONArray ops = prefs.getPending();
            for (int i = 0; i < ops.length(); i++) {
                JSONObject op = ops.optJSONObject(i);
                if (op != null && "create".equals(op.optString("op"))
                        && localName.equals(op.optString("local"))) {
                    op.put("content", content);
                    prefs.savePending(ops);
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Drops every queued op that references a memo (a local memo
     * deleted before it ever synced). Returns the file paths of
     * dropped attach ops so the caller can delete the files.
     */
    public static java.util.List<String> dropOpsFor(Prefs prefs, String name) {
        java.util.List<String> files = new java.util.ArrayList<String>();
        try {
            JSONArray ops = prefs.getPending();
            JSONArray kept = new JSONArray();
            for (int i = 0; i < ops.length(); i++) {
                JSONObject op = ops.optJSONObject(i);
                if (op == null) {
                    continue;
                }
                boolean hit = name.equals(op.optString("name"))
                        || name.equals(op.optString("local"))
                        || name.equals(op.optString("memo"));
                if (hit) {
                    String f = op.optString("file", "");
                    if (f.length() > 0) {
                        files.add(f);
                    }
                } else {
                    kept.put(op);
                }
            }
            prefs.savePending(kept);
        } catch (Exception ignored) {
        }
        return files;
    }
}
