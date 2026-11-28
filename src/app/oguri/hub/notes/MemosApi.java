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
        String error = "";              // transport failure detail
        byte[] bytes = null;
        List<String> setCookies = new ArrayList<String>();
    }

    /** The server answered 401: the access token is dead. */
    public static class AuthException extends Exception {
        AuthException(String message) {
            super(message);
        }
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
            for (java.util.Map.Entry<String, List<String>> e
                    : c.getHeaderFields().entrySet()) {
                // The REST gateway also delivers the sign-in refresh
                // cookie as Grpc-Metadata-Set-Cookie (see refresh()).
                if (e.getKey() != null && e.getValue() != null
                        && (e.getKey().equalsIgnoreCase("Set-Cookie")
                        || e.getKey().equalsIgnoreCase("Grpc-Metadata-Set-Cookie"))) {
                    r.setCookies.addAll(e.getValue());
                }
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
            r.error = e.getMessage() == null ? e.toString() : e.getMessage();
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
        // The REST route (/api/auth/refresh) is broken upstream in
        // 26.11: the gateway never forwards the Cookie header, so it
        // always answers 401 "refresh token not found" (verified
        // live against a local 26.11.2). The Connect route for the
        // same RPC reads the cookie properly and rotates it with a
        // real Set-Cookie, so refresh goes there. The sign-in
        // cookie itself arrives as Grpc-Metadata-Set-Cookie on the
        // REST sign-in response; request() collects both spellings.
        Result r = request(server, "POST", "/memos.api.AuthService/RefreshToken",
                "{}", null, cookie, false);
        if (r.status == 401) {
            throw new AuthException("Session expired");
        }
        if (r.status != 200) {
            throw new Exception(failure("Session refresh", r));
        }
        return authFrom(r);
    }

    /** One readable line for a failed call, transport failures included. */
    private static String failure(String what, Result r) {
        if (r.status == 0) {
            return what + " failed: no connection (" + r.error + ")";
        }
        return what + " failed (" + r.status + ")";
    }

    private static void requireOk(String what, Result r) throws Exception {
        if (r.status == 401) {
            throw new AuthException(what + " failed (401)");
        }
        if (r.status != 200) {
            throw new Exception(failure(what, r));
        }
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

    /**
     * One page of the memo list, deliberately bare (page size and
     * page token only): state, comment and ordering rules are
     * applied client-side over the cache, so the request itself
     * has no filter or ordering syntax left to get wrong.
     */
    public static JSONObject listPage(String server, String token, String pageToken,
                                      String state)
            throws Exception {
        String path = "/api/memos?pageSize=50";
        if (state != null && state.length() > 0) {
            path += "&state=" + enc(state);
        }
        if (pageToken != null && pageToken.length() > 0) {
            path += "&pageToken=" + enc(pageToken);
        }
        Result r = request(server, "GET", path, null, token, null, false);
        requireOk("List", r);
        return new JSONObject(r.body);
    }

    public static JSONObject createMemo(String server, String token, String content,
                                        String visibility) throws Exception {
        // The route's body binding is "memo": the body IS the Memo
        // message, not a wrapper around it. Wrapping it buries the
        // content under an unknown key and the server creates an
        // empty memo (the 1.17 field failure).
        JSONObject body = new JSONObject();
        body.put("content", content);
        body.put("visibility", visibility);
        Result r = request(server, "POST", "/api/memos", body.toString(), token, null, false);
        requireOk("Create", r);
        return new JSONObject(r.body);
    }

    public static JSONObject updateMemo(String server, String token, String name,
                                        String content, boolean pinned, String state)
            throws Exception {
        // Body binding is "memo" here too: the body is the Memo
        // itself, and update_mask travels as a query parameter.
        JSONObject body = new JSONObject();
        body.put("content", content);
        body.put("pinned", pinned);
        body.put("state", state);
        String uid = name.substring(name.indexOf('/') + 1);
        Result r = request(server, "PATCH", "/api/memos/" + enc(uid)
                        + "?updateMask=" + enc("content,pinned,state"),
                body.toString(), token, null, false);
        requireOk("Update", r);
        return new JSONObject(r.body);
    }

    public static void deleteMemo(String server, String token, String name) throws Exception {
        String uid = name.substring(name.indexOf('/') + 1);
        Result r = request(server, "DELETE", "/api/memos/" + enc(uid), null, token, null, false);
        requireOk("Delete", r);
    }

    // ---------------------------------------------------------- attachments

    public static JSONObject createAttachment(String server, String token, String filename,
                                              String type, String base64Content,
                                              String memoName) throws Exception {
        // Body binding is "attachment": the body is the Attachment
        // itself, same rule as the memo routes above.
        JSONObject body = new JSONObject();
        body.put("filename", filename);
        body.put("type", type);
        body.put("content", base64Content);
        if (memoName != null && memoName.length() > 0) {
            body.put("memo", memoName);
        }
        Result r = request(server, "POST", "/api/attachments", body.toString(), token, null, false);
        requireOk("Upload", r);
        return new JSONObject(r.body);
    }

    /** Attachment bytes, served by the file route next to the API. */
    public static byte[] fetchAttachmentBytes(String server, String token,
                                              Memo.Attachment a) throws Exception {
        String path = "/file/attachments/" + enc(a.uid()) + "/" + enc(a.filename);
        Result r = request(server, "GET", path, null, token, null, true);
        if (r.status == 401) {
            throw new AuthException("Download failed (401)");
        }
        if (r.status != 200 || r.bytes == null) {
            throw new Exception(failure("Download", r));
        }
        return r.bytes;
    }
}
