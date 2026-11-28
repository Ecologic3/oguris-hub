package app.oguri.hub.notes;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * The Memos 26.x REST API, every path and field pinned here (the
 * old /api/v1 surface is gone in 26.x; REST lives under /api and
 * the shapes follow the upstream protos, copies in hidden/memos-api).
 * Plain HttpURLConnection, no libraries, like the rest of the hub.
 */
public class MemosApi {

    public static class Result {
        int status = 0;                 // 0 = transport failure
        String body = "";
        byte[] bytes = null;
        List<String> setCookies = new ArrayList<String>();
    }

    public static class Auth {
        String accessToken = "";
        long expireMillis = 0;
        String cookie = "";             // joined "name=value; ..." header value
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private static Result request(String server, String method, String path,
                                  String jsonBody, String token, String cookie,
                                  boolean binary) {
        Result r = new Result();
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(server + path).openConnection();
            c.setRequestMethod(method);
            c.setConnectTimeout(12000);
            c.setReadTimeout(20000);
            c.setRequestProperty("Accept", "application/json");
            if (token != null && token.length() > 0) {
                c.setRequestProperty("Authorization", "Bearer " + token);
            }
            if (cookie != null && cookie.length() > 0) {
                c.setRequestProperty("Cookie", cookie);
            }
            if (jsonBody != null) {
                byte[] data = jsonBody.getBytes("UTF-8");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestProperty("Content-Length", String.valueOf(data.length));
                OutputStream out = c.getOutputStream();
                out.write(data);
                out.close();
            }
            r.status = c.getResponseCode();
            List<String> cookies = c.getHeaderFields().get("Set-Cookie");
            if (cookies != null) {
                r.setCookies = cookies;
            }
            InputStream in = r.status >= 400 ? c.getErrorStream() : c.getInputStream();
            if (in != null) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] chunk = new byte[8192];
                int n;
                while ((n = in.read(chunk)) != -1) {
                    buf.write(chunk, 0, n);
                }
                in.close();
                if (binary) {
                    r.bytes = buf.toByteArray();
                } else {
                    r.body = new String(buf.toByteArray(), "UTF-8");
                }
            }
        } catch (Exception e) {
            r.status = 0;
            r.body = e.toString();
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
        return r;
    }

    /** Joins Set-Cookie headers into one Cookie header value. */
    private static String joinCookies(List<String> setCookies) {
        StringBuilder sb = new StringBuilder();
        for (String sc : setCookies) {
            int semi = sc.indexOf(';');
            String pair = semi >= 0 ? sc.substring(0, semi) : sc;
            if (pair.indexOf('=') <= 0) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(pair.trim());
        }
        return sb.toString();
    }

    /** Parses an RFC 3339 timestamp ("2026-11-28T00:30:00Z") to millis. */
    static long parseTime(String iso) {
        if (iso == null || iso.length() == 0) {
            return 0;
        }
        String s = iso;
        try {
            // Drop fractional seconds; SimpleDateFormat handles the rest.
            int dot = s.indexOf('.');
            if (dot > 0) {
                int end = dot + 1;
                while (end < s.length() && Character.isDigit(s.charAt(end))) {
                    end++;
                }
                s = s.substring(0, dot) + s.substring(end);
            }
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
            if (s.endsWith("Z")) {
                s = s.substring(0, s.length() - 1) + "+00:00";
            }
            Date d = f.parse(s);
            return d == null ? 0 : d.getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    static String nowIso() {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new Date());
    }

    // ---------------------------------------------------------- auth

    public static Auth signIn(String server, String username, String password)
            throws Exception {
        JSONObject cred = new JSONObject();
        cred.put("username", username);
        cred.put("password", password);
        JSONObject body = new JSONObject();
        body.put("passwordCredentials", cred);
        Result r = request(server, "POST", "/api/auth/signin", body.toString(), null, null, false);
        if (r.status != 200) {
            throw new Exception("Sign-in failed (" + r.status + ")");
        }
        return authFrom(r);
    }

    public static Auth refresh(String server, String cookie) throws Exception {
        Result r = request(server, "POST", "/api/auth/refresh", "{}", null, cookie, false);
        if (r.status != 200) {
            throw new Exception("Session refresh failed (" + r.status + ")");
        }
        return authFrom(r);
    }

    private static Auth authFrom(Result r) throws Exception {
        JSONObject o = new JSONObject(r.body);
        Auth a = new Auth();
        a.accessToken = o.optString("accessToken", "");
        a.expireMillis = parseTime(o.optString("accessTokenExpireTime", ""));
        a.cookie = joinCookies(r.setCookies);
        if (a.accessToken.length() == 0) {
            throw new Exception("Server returned no access token");
        }
        return a;
    }

    // ---------------------------------------------------------- memos

    /** One page of the memo list; filter is a CEL string ("" for none). */
    public static JSONObject listPage(String server, String token, String state,
                                      String filter, String pageToken) throws Exception {
        StringBuilder path = new StringBuilder("/api/memos?pageSize=50&orderBy="
                + enc("pinned desc, create_time desc"));
        String f = "parent == null";
        if (state != null && state.length() > 0) {
            f += " && state == " + state;
        }
        if (filter != null && filter.length() > 0) {
            f += " && " + filter;
        }
        path.append("&filter=").append(enc(f));
        if (pageToken != null && pageToken.length() > 0) {
            path.append("&pageToken=").append(enc(pageToken));
        }
        Result r = request(server, "GET", path.toString(), null, token, null, false);
        if (r.status != 200) {
            throw new Exception("List failed (" + r.status + ")");
        }
        return new JSONObject(r.body);
    }

    public static JSONObject createMemo(String server, String token, String content,
                                        String visibility) throws Exception {
        JSONObject memo = new JSONObject();
        memo.put("content", content);
        memo.put("visibility", visibility);
        JSONObject body = new JSONObject();
        body.put("memo", memo);
        Result r = request(server, "POST", "/api/memos", body.toString(), token, null, false);
        if (r.status != 200) {
            throw new Exception("Create failed (" + r.status + ")");
        }
        return new JSONObject(r.body);
    }

    public static JSONObject updateMemo(String server, String token, String name,
                                        String content, boolean pinned, String state)
            throws Exception {
        JSONObject memo = new JSONObject();
        memo.put("content", content);
        memo.put("pinned", pinned);
        memo.put("state", state);
        JSONObject body = new JSONObject();
        body.put("memo", memo);
        body.put("updateMask", "content,pinned,state");
        String uid = name.substring(name.indexOf('/') + 1);
        Result r = request(server, "PATCH", "/api/memos/" + enc(uid),
                body.toString(), token, null, false);
        if (r.status != 200) {
            throw new Exception("Update failed (" + r.status + ")");
        }
        return new JSONObject(r.body);
    }

    public static void deleteMemo(String server, String token, String name) throws Exception {
        String uid = name.substring(name.indexOf('/') + 1);
        Result r = request(server, "DELETE", "/api/memos/" + enc(uid), null, token, null, false);
        if (r.status != 200) {
            throw new Exception("Delete failed (" + r.status + ")");
        }
    }

    // ---------------------------------------------------------- attachments

    public static JSONObject createAttachment(String server, String token, String filename,
                                              String type, String base64Content,
                                              String memoName) throws Exception {
        JSONObject att = new JSONObject();
        att.put("filename", filename);
        att.put("type", type);
        att.put("content", base64Content);
        if (memoName != null && memoName.length() > 0) {
            att.put("memo", memoName);
        }
        JSONObject body = new JSONObject();
        body.put("attachment", att);
        Result r = request(server, "POST", "/api/attachments", body.toString(), token, null, false);
        if (r.status != 200) {
            throw new Exception("Upload failed (" + r.status + ")");
        }
        return new JSONObject(r.body);
    }

    /** Attachment bytes, served by the file route next to the API. */
    public static byte[] fetchAttachmentBytes(String server, String token,
                                              Memo.Attachment a) throws Exception {
        String path = "/file/attachments/" + enc(a.uid()) + "/" + enc(a.filename);
        Result r = request(server, "GET", path, null, token, null, true);
        if (r.status != 200 || r.bytes == null) {
            throw new Exception("Download failed (" + r.status + ")");
        }
        return r.bytes;
    }
}
