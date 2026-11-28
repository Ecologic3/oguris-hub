package app.oguri.hub.notes;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * One memo as the Memos server carries it (26.x API: the resource
 * name is "memos/{uid}", content is Markdown, tags and attachments
 * ride along). Locally created memos that have not synced yet carry
 * a "local-" name until the create lands and the server name is
 * known.
 */
public class Memo {

    public String name = "";
    public String content = "";
    public String parent = "";      // "memos/{uid}" for comments, "" for top-level
    public String visibility = "PRIVATE";
    public String state = "NORMAL";
    public boolean pinned = false;
    public String createTime = "";
    public String updateTime = "";
    public List<String> tags = new ArrayList<String>();
    public List<Attachment> attachments = new ArrayList<Attachment>();

    public static class Attachment {
        public String name = "";
        public String filename = "";
        public String type = "";

        JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("name", name);
                o.put("filename", filename);
                o.put("type", type);
            } catch (Exception ignored) {
            }
            return o;
        }

        static Attachment fromJson(JSONObject o) {
            Attachment a = new Attachment();
            a.name = o.optString("name", "");
            a.filename = o.optString("filename", "");
            a.type = o.optString("type", "");
            return a;
        }

        /** The "{uid}" segment of "attachments/{uid}". */
        String uid() {
            int i = name.indexOf('/');
            return i >= 0 ? name.substring(i + 1) : name;
        }

        boolean isImage() {
            return type.startsWith("image/");
        }
    }

    public boolean isLocal() {
        return name.startsWith("local-");
    }

    JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("name", name);
            o.put("content", content);
            o.put("parent", parent);
            o.put("visibility", visibility);
            o.put("state", state);
            o.put("pinned", pinned);
            o.put("createTime", createTime);
            o.put("updateTime", updateTime);
            JSONArray t = new JSONArray();
            for (String tag : tags) {
                t.put(tag);
            }
            o.put("tags", t);
            JSONArray at = new JSONArray();
            for (Attachment a : attachments) {
                at.put(a.toJson());
            }
            o.put("attachments", at);
        } catch (Exception ignored) {
        }
        return o;
    }

    static Memo fromJson(JSONObject o) {
        Memo m = new Memo();
        m.name = o.optString("name", "");
        m.content = o.optString("content", "");
        m.parent = o.optString("parent", "");
        m.visibility = o.optString("visibility", "PRIVATE");
        m.state = o.optString("state", "NORMAL");
        m.pinned = o.optBoolean("pinned", false);
        m.createTime = o.optString("createTime", "");
        m.updateTime = o.optString("updateTime", "");
        JSONArray t = o.optJSONArray("tags");
        if (t != null) {
            for (int i = 0; i < t.length(); i++) {
                String tag = t.optString(i, "");
                if (tag.length() > 0) {
                    m.tags.add(tag);
                }
            }
        }
        JSONArray at = o.optJSONArray("attachments");
        if (at != null) {
            for (int i = 0; i < at.length(); i++) {
                JSONObject ao = at.optJSONObject(i);
                if (ao != null) {
                    m.attachments.add(Attachment.fromJson(ao));
                }
            }
        }
        return m;
    }

    static List<Memo> listFromJsonArray(JSONArray arr) {
        List<Memo> out = new ArrayList<Memo>();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) {
                    out.add(fromJson(o));
                }
            }
        }
        return out;
    }

    static JSONArray listToJsonArray(List<Memo> memos) {
        JSONArray arr = new JSONArray();
        for (Memo m : memos) {
            arr.put(m.toJson());
        }
        return arr;
    }
}
