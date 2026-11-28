package app.oguri.hub.notes;

import app.oguri.hub.R;
import app.oguri.hub.UiKit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Notes: the hub's native front end for the Memos server on
 * mnemosyne (26.x API, see MemosApi). The module's own palette is a
 * crimson family on its own deep crimson background (the Alarms
 * pattern: every module tints its background to its theme), and
 * the geometry is the shared UiKit's, like every other module.
 *
 * The stream renders from the local cache first and syncs behind
 * it: every change is written to the cache and the pending queue
 * at once, then a sync flushes the queue in order and refetches
 * the stream, so the module works offline and catches up when the
 * server is back. Tag and search filters run over the cache.
 */
public class MainActivity extends Activity {

    private static final int COL_BG = 0xFF1D0D10;
    private static final int COL_SURFACE = 0xFF2B1D22;
    private static final int COL_RAISED = 0xFF3A252C;
    private static final int COL_BORDER = 0xFF52323B;
    private static final int COL_TEXT = 0xFFF9F1F1;
    private static final int COL_MUTED = 0xFFCBA9AE;
    private static final int COL_FAINT = 0xFF96757B;
    private static final int COL_ACCENT = 0xFFE04046;
    private static final int COL_ACCENT_FILL = 0xFFB01E24;
    private static final int COL_REMOVE = 0xFFFF9A9A;

    private Prefs prefs;
    private LinearLayout root;
    private LinearLayout listBox;
    private LinearLayout chipsBox;
    private TextView statusLine;
    private Button memosTab;
    private Button archivedTab;

    private String showState = "NORMAL";
    private String activeTag = "";
    private String searchText = "";
    private boolean streamBuilt = false;
    private boolean syncing = false;
    private long lastSyncMillis = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        if (prefs.hasSession()) {
            buildStream();
            renderFromCache();
            syncNow();
        } else {
            buildSignIn("");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (streamBuilt) {
            renderFromCache();
            if (System.currentTimeMillis() - lastSyncMillis > 30000
                    || prefs.getPending().length() > 0) {
                syncNow();
            }
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private LinearLayout.LayoutParams lp(int topMarginDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(topMarginDp);
        return p;
    }

    private LinearLayout makeCard(int topMarginDp) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.cardBackground(this, COL_SURFACE, COL_BORDER));
        card.setPadding(dp(UiKit.CARD_PAD_H_DP), dp(UiKit.CARD_PAD_V_DP),
                dp(UiKit.CARD_PAD_H_DP), dp(UiKit.CARD_PAD_V_DP));
        card.setLayoutParams(lp(topMarginDp));
        return card;
    }

    private TextView makeText(String text, float size, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private void beginPage() {
        getWindow().setBackgroundDrawable(new ColorDrawable(COL_BG));
        UiKit.setSystemBars(this, COL_BG);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COL_BG);
        UiKit.applyRootPadding(this, root);
        root.addView(UiKit.header(this, "Notes", COL_TEXT,
                R.mipmap.ic_mod_notes, COL_RAISED, COL_BORDER, COL_ACCENT));
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(COL_BG);
        scroll.addView(root);
        setContentView(scroll);
    }

    // ---------------------------------------------------------- sign-in

    private EditText makeField(String hint, boolean password, boolean mono) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(COL_FAINT);
        e.setTextColor(COL_TEXT);
        e.setTextSize(15);
        e.setSingleLine(true);
        if (mono) {
            e.setTypeface(Typeface.MONOSPACE);
            e.setTextSize(13.5f);
        }
        if (password) {
            e.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        e.setBackground(UiKit.cardBackground(this, COL_RAISED, COL_BORDER));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setLayoutParams(lp(6));
        return e;
    }

    private void buildSignIn(String error) {
        streamBuilt = false;
        beginPage();
        LinearLayout card = makeCard(16);

        TextView serverLabel = makeText("Server", 13, COL_MUTED);
        card.addView(serverLabel);
        final EditText server = makeField("", false, true);
        server.setText(prefs.getServer());
        card.addView(server);

        TextView userLabel = makeText("Username", 13, COL_MUTED);
        userLabel.setLayoutParams(lp(14));
        card.addView(userLabel);
        final EditText username = makeField("", false, false);
        username.setText(prefs.getUsername());
        card.addView(username);

        TextView passLabel = makeText("Password", 13, COL_MUTED);
        passLabel.setLayoutParams(lp(14));
        card.addView(passLabel);
        final EditText password = makeField("", true, false);
        card.addView(password);

        final TextView errorView = makeText(error, 14, COL_REMOVE);
        errorView.setLayoutParams(lp(12));
        errorView.setVisibility(error.length() > 0 ? View.VISIBLE : View.GONE);
        card.addView(errorView);

        final Button signIn = UiKit.button(this, "Sign in", COL_ACCENT_FILL,
                UiKit.pressedOf(COL_ACCENT_FILL), Color.WHITE);
        signIn.setLayoutParams(lp(16));
        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String srv = server.getText().toString().trim();
                while (srv.endsWith("/")) {
                    srv = srv.substring(0, srv.length() - 1);
                }
                final String fServer = srv;
                final String fUser = username.getText().toString().trim();
                final String fPass = password.getText().toString();
                if (fServer.length() == 0 || fUser.length() == 0 || fPass.length() == 0) {
                    errorView.setText("Fill in all three fields");
                    errorView.setVisibility(View.VISIBLE);
                    return;
                }
                signIn.setEnabled(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            MemosApi.Auth a = MemosApi.signIn(fServer, fUser, fPass);
                            prefs.saveSession(fServer, fUser, a.accessToken,
                                    a.expireMillis, a.cookie);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    buildStream();
                                    renderFromCache();
                                    syncNow();
                                }
                            });
                        } catch (final Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    signIn.setEnabled(true);
                                    errorView.setText("Could not sign in: " + e.getMessage());
                                    errorView.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    }
                }).start();
            }
        });
        card.addView(signIn);
        root.addView(card);
    }

    // ---------------------------------------------------------- stream

    private void buildStream() {
        streamBuilt = true;
        beginPage();

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setLayoutParams(lp(16));
        memosTab = UiKit.pillButton(this, "Memos", COL_RAISED,
                UiKit.pressedOf(COL_RAISED), COL_BORDER, COL_TEXT);
        memosTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showState = "NORMAL";
                activeTag = "";
                renderFromCache();
            }
        });
        tabs.addView(memosTab, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        archivedTab = UiKit.pillButton(this, "Archived", COL_RAISED,
                UiKit.pressedOf(COL_RAISED), COL_BORDER, COL_TEXT);
        LinearLayout.LayoutParams atLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        atLp.setMargins(dp(8), 0, 0, 0);
        archivedTab.setLayoutParams(atLp);
        archivedTab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showState = "ARCHIVED";
                activeTag = "";
                renderFromCache();
            }
        });
        tabs.addView(archivedTab);
        root.addView(tabs);

        Button newMemo = UiKit.button(this, "New memo", COL_ACCENT_FILL,
                UiKit.pressedOf(COL_ACCENT_FILL), Color.WHITE);
        newMemo.setLayoutParams(lp(12));
        newMemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditorActivity.class));
            }
        });
        root.addView(newMemo);

        LinearLayout searchCard = makeCard(12);
        searchCard.setPadding(dp(12), dp(2), dp(12), dp(2));
        final EditText search = new EditText(this);
        search.setHint("Search");
        search.setHintTextColor(COL_FAINT);
        search.setTextColor(COL_TEXT);
        search.setTextSize(15);
        search.setSingleLine(true);
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                searchText = v.getText().toString().trim().toLowerCase(Locale.US);
                renderFromCache();
                return true;
            }
        });
        searchCard.addView(search);
        root.addView(searchCard);

        chipsBox = new LinearLayout(this);
        chipsBox.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView chipsScroll = new HorizontalScrollView(this);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        chipsScroll.setLayoutParams(lp(12));
        chipsScroll.addView(chipsBox);
        root.addView(chipsScroll);

        statusLine = makeText("", 13, COL_FAINT);
        statusLine.setLayoutParams(lp(10));
        root.addView(statusLine);

        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);

        LinearLayout pillRow = new LinearLayout(this);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        pillRow.setGravity(Gravity.CENTER);
        pillRow.setLayoutParams(lp(18));
        Button signOut = UiKit.pillButton(this, "Sign out", COL_SURFACE,
                UiKit.pressedOf(COL_SURFACE), COL_BORDER, COL_MUTED);
        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.clearSession();
                buildSignIn("");
            }
        });
        pillRow.addView(signOut);
        root.addView(pillRow);
    }

    private void styleTabs() {
        boolean normal = "NORMAL".equals(showState);
        memosTab.setBackground(UiKit.buttonBackground(this,
                normal ? COL_ACCENT_FILL : COL_RAISED,
                UiKit.pressedOf(normal ? COL_ACCENT_FILL : COL_RAISED),
                COL_BORDER, 999));
        memosTab.setTextColor(normal ? Color.WHITE : COL_TEXT);
        archivedTab.setBackground(UiKit.buttonBackground(this,
                !normal ? COL_ACCENT_FILL : COL_RAISED,
                UiKit.pressedOf(!normal ? COL_ACCENT_FILL : COL_RAISED),
                COL_BORDER, 999));
        archivedTab.setTextColor(!normal ? Color.WHITE : COL_TEXT);
    }

    private void renderFromCache() {
        if (!streamBuilt) {
            return;
        }
        styleTabs();
        List<Memo> all = prefs.getCache(showState);

        // Tag chips from the whole cached stream of this state.
        LinkedHashSet<String> tagSet = new LinkedHashSet<String>();
        for (Memo m : all) {
            tagSet.addAll(m.tags);
        }
        if (activeTag.length() > 0 && !tagSet.contains(activeTag)) {
            activeTag = "";
        }
        chipsBox.removeAllViews();
        addChip("All", activeTag.length() == 0, "");
        for (final String tag : tagSet) {
            addChip("#" + tag, tag.equals(activeTag), tag);
        }

        List<Memo> shown = new ArrayList<Memo>();
        for (Memo m : all) {
            if (activeTag.length() > 0 && !m.tags.contains(activeTag)) {
                continue;
            }
            if (searchText.length() > 0
                    && !m.content.toLowerCase(Locale.US).contains(searchText)) {
                continue;
            }
            shown.add(m);
        }
        // Pinned first, cache order kept inside each group.
        List<Memo> ordered = new ArrayList<Memo>();
        for (Memo m : shown) {
            if (m.pinned) {
                ordered.add(m);
            }
        }
        for (Memo m : shown) {
            if (!m.pinned) {
                ordered.add(m);
            }
        }

        listBox.removeAllViews();
        if (ordered.isEmpty()) {
            TextView empty = makeText(all.isEmpty() ? "No memos yet" : "Nothing matches",
                    14, COL_MUTED);
            empty.setLayoutParams(lp(14));
            listBox.addView(empty);
        }
        for (Memo m : ordered) {
            listBox.addView(memoCard(m));
        }
    }

    private void addChip(String label, boolean active, final String tag) {
        Button chip = UiKit.pillButton(this, label,
                active ? COL_ACCENT_FILL : COL_RAISED,
                UiKit.pressedOf(active ? COL_ACCENT_FILL : COL_RAISED),
                COL_BORDER, active ? Color.WHITE : COL_TEXT);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(clp);
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeTag = tag;
                renderFromCache();
            }
        });
        chipsBox.addView(chip);
    }

    // ---------------------------------------------------------- memo cards

    private View memoCard(final Memo memo) {
        LinearLayout card = makeCard(12);

        for (Memo.Attachment a : memo.attachments) {
            if (a.isImage()) {
                final ImageView img = new ImageView(this);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                img.setBackground(UiKit.cardBackground(this, COL_RAISED, 0));
                img.setClipToOutline(true);
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(170));
                ilp.setMargins(0, 0, 0, dp(10));
                img.setLayoutParams(ilp);
                img.setTag(a.uid());
                card.addView(img);
                loadImage(img, a);
                break;
            }
        }

        String[] lines = memo.content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            if (isCheckbox(trimmed)) {
                card.addView(checkboxRow(memo, i, trimmed));
            } else {
                TextView t = makeText("", 15, COL_TEXT);
                t.setText(boldSpans(trimmed));
                t.setLineSpacing(0, 1.25f);
                card.addView(t);
            }
        }

        LinearLayout meta = new LinearLayout(this);
        meta.setOrientation(LinearLayout.HORIZONTAL);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.setLayoutParams(lp(10));
        if (memo.pinned) {
            TextView pin = makeText("Pinned", 12.5f, COL_ACCENT);
            pin.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            meta.addView(pin);
        }
        for (String tag : memo.tags) {
            TextView tg = makeText("#" + tag, 12.5f, COL_ACCENT);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.setMargins(memo.pinned || memo.tags.indexOf(tag) > 0 ? dp(10) : 0, 0, 0, 0);
            tg.setLayoutParams(tlp);
            meta.addView(tg);
        }
        TextView time = makeText(relativeTime(memo.createTime), 12.5f, COL_MUTED);
        LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        timeLp.setMargins(dp(10), 0, 0, 0);
        time.setLayoutParams(timeLp);
        meta.addView(time);
        if (memo.isLocal() || prefs.hasPendingFor(memo.name)) {
            TextView wait = makeText("Waiting to sync", 12.5f, COL_FAINT);
            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            wlp.setMargins(dp(10), 0, 0, 0);
            wait.setLayoutParams(wlp);
            meta.addView(wait);
        }
        card.addView(meta);

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, EditorActivity.class);
                i.putExtra(EditorActivity.EXTRA_NAME, memo.name);
                startActivity(i);
            }
        });
        return card;
    }

    private boolean isCheckbox(String trimmedLine) {
        return trimmedLine.startsWith("- [ ]") || trimmedLine.startsWith("- [x]")
                || trimmedLine.startsWith("- [X]");
    }

    private View checkboxRow(final Memo memo, final int lineIndex, String trimmedLine) {
        final boolean checked = trimmedLine.charAt(3) != ' ';
        String text = trimmedLine.length() > 5 ? trimmedLine.substring(5).trim() : "";
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, dp(3), 0, dp(3));
        row.setLayoutParams(rlp);

        final TextView box = new TextView(this);
        box.setText(checked ? "✓" : "");
        box.setTextSize(13);
        box.setTextColor(Color.WHITE);
        box.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        box.setGravity(Gravity.CENTER);
        box.setBackground(UiKit.cardBackground(this,
                checked ? COL_ACCENT_FILL : COL_RAISED,
                checked ? COL_ACCENT_FILL : COL_BORDER));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(21), dp(21));
        blp.setMargins(0, dp(1), dp(9), 0);
        box.setLayoutParams(blp);
        box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleCheckbox(memo, lineIndex);
            }
        });
        row.addView(box);

        TextView t = makeText("", 15, checked ? COL_MUTED : COL_TEXT);
        t.setText(boldSpans(text));
        if (checked) {
            t.setPaintFlags(t.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        row.addView(t, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    /** Renders **bold** pairs; everything else stays plain. */
    private CharSequence boldSpans(String text) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        int i = 0;
        while (i < text.length()) {
            int open = text.indexOf("**", i);
            if (open < 0) {
                sb.append(text.substring(i));
                break;
            }
            int close = text.indexOf("**", open + 2);
            if (close < 0) {
                sb.append(text.substring(i));
                break;
            }
            sb.append(text.substring(i, open));
            int start = sb.length();
            sb.append(text.substring(open + 2, close));
            sb.setSpan(new StyleSpan(Typeface.BOLD), start, sb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            i = close + 2;
        }
        return sb;
    }

    private void toggleCheckbox(Memo memo, int lineIndex) {
        String[] lines = memo.content.split("\n", -1);
        if (lineIndex < 0 || lineIndex >= lines.length) {
            return;
        }
        String line = lines[lineIndex];
        int marker = line.indexOf("[ ]");
        if (marker >= 0) {
            lines[lineIndex] = line.substring(0, marker) + "[x]" + line.substring(marker + 3);
        } else {
            marker = line.indexOf("[x]");
            if (marker < 0) {
                marker = line.indexOf("[X]");
            }
            if (marker < 0) {
                return;
            }
            lines[lineIndex] = line.substring(0, marker) + "[ ]" + line.substring(marker + 3);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(lines[i]);
        }
        memo.content = sb.toString();
        prefs.upsertMemo(memo);
        Queue.enqueueUpdate(prefs, memo);
        renderFromCache();
        syncNow();
    }

    private String relativeTime(String iso) {
        long t = MemosApi.parseTime(iso);
        if (t == 0) {
            return "";
        }
        long diff = System.currentTimeMillis() - t;
        if (diff < 60000) {
            return "just now";
        }
        if (diff < 3600000) {
            return (diff / 60000) + " min ago";
        }
        if (diff < 86400000) {
            return (diff / 3600000) + " h ago";
        }
        if (diff < 172800000) {
            return "Yesterday";
        }
        java.text.SimpleDateFormat f =
                new java.text.SimpleDateFormat("d MMM", Locale.US);
        return f.format(new java.util.Date(t));
    }

    // ---------------------------------------------------------- images

    private void loadImage(final ImageView img, final Memo.Attachment a) {
        final File cache = new File(new File(getCacheDir(), "notes_img"), a.uid());
        if (cache.isFile() && cache.length() > 0) {
            Bitmap bm = BitmapFactory.decodeFile(cache.getAbsolutePath());
            if (bm != null) {
                img.setImageBitmap(bm);
                return;
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] bytes;
                    try {
                        bytes = MemosApi.fetchAttachmentBytes(
                                prefs.getServer(), prefs.getAccessToken(), a);
                    } catch (MemosApi.AuthException ae) {
                        MemosApi.Auth auth = MemosApi.refresh(
                                prefs.getServer(), prefs.getRefreshCookie());
                        prefs.saveTokens(auth.accessToken, auth.expireMillis, auth.cookie);
                        bytes = MemosApi.fetchAttachmentBytes(
                                prefs.getServer(), auth.accessToken, a);
                    }
                    cache.getParentFile().mkdirs();
                    FileOutputStream out = new FileOutputStream(cache);
                    out.write(bytes);
                    out.close();
                    final Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (bm != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (a.uid().equals(img.getTag())) {
                                    img.setImageBitmap(bm);
                                }
                            }
                        });
                    }
                } catch (Exception ignored) {
                    // The card keeps its empty image area; the memo
                    // itself is unaffected.
                }
            }
        }).start();
    }

    // ---------------------------------------------------------- sync
    //
    // The stored access token is used as-is; the parsed expiry is
    // never trusted to kill it early. Only a real 401 earns a
    // refresh, and only a failed refresh ends the session. The
    // list request itself is bare (see MemosApi.listPage): state,
    // comments and ordering are all settled client-side.

    private void syncNow() {
        if (!streamBuilt || syncing || !prefs.hasSession()) {
            return;
        }
        syncing = true;
        statusLine.setText("Syncing…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String outcome = "";
                boolean deadSession = false;
                try {
                    String token = prefs.getAccessToken();
                    boolean flushed;
                    try {
                        flushed = flushPending(token);
                        fetchAll(token);
                    } catch (MemosApi.AuthException ae) {
                        MemosApi.Auth a = MemosApi.refresh(
                                prefs.getServer(), prefs.getRefreshCookie());
                        prefs.saveTokens(a.accessToken, a.expireMillis, a.cookie);
                        flushed = flushPending(a.accessToken);
                        fetchAll(a.accessToken);
                    }
                    lastSyncMillis = System.currentTimeMillis();
                    int left = prefs.getPending().length();
                    outcome = flushed && left == 0 ? "" : left + " waiting to sync";
                } catch (MemosApi.AuthException ae) {
                    deadSession = true;
                } catch (Exception e) {
                    outcome = prefs.getCache(showState).isEmpty()
                            ? e.getMessage()
                            : "Offline, showing saved notes";
                }
                final String fOutcome = outcome;
                final boolean fDead = deadSession;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        syncing = false;
                        if (fDead) {
                            prefs.clearSession();
                            buildSignIn("Session expired, sign in again");
                            return;
                        }
                        if (streamBuilt) {
                            statusLine.setText(fOutcome);
                            renderFromCache();
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * Refetches the whole stream and rebuilds both state caches.
     * Comments (memos with a parent) never enter the stream, the
     * order is pinned first then newest first, and local memos
     * whose create has not landed survive the refetch.
     */
    private void fetchAll(String token) throws Exception {
        List<Memo> fetched = new ArrayList<Memo>();
        String pageToken = "";
        for (int page = 0; page < 20; page++) {
            JSONObject o = MemosApi.listPage(prefs.getServer(), token, pageToken);
            JSONArray arr = o.optJSONArray("memos");
            if (arr != null) {
                fetched.addAll(Memo.listFromJsonArray(arr));
            }
            pageToken = o.optString("nextPageToken", "");
            if (pageToken.length() == 0) {
                break;
            }
        }
        java.util.Collections.sort(fetched, new java.util.Comparator<Memo>() {
            @Override
            public int compare(Memo a, Memo b) {
                if (a.pinned != b.pinned) {
                    return a.pinned ? -1 : 1;
                }
                long ta = MemosApi.parseTime(a.createTime);
                long tb = MemosApi.parseTime(b.createTime);
                return ta < tb ? 1 : (ta > tb ? -1 : 0);
            }
        });
        for (String state : new String[]{"NORMAL", "ARCHIVED"}) {
            List<Memo> part = new ArrayList<Memo>();
            for (Memo m : fetched) {
                if (m.parent.length() == 0 && state.equals(m.state)) {
                    part.add(m);
                }
            }
            for (Memo old : prefs.getCache(state)) {
                if (old.isLocal()) {
                    boolean present = false;
                    for (Memo m : part) {
                        if (m.name.equals(old.name)) {
                            present = true;
                            break;
                        }
                    }
                    if (!present) {
                        part.add(0, old);
                    }
                }
            }
            prefs.saveCache(state, part);
        }
    }

    /**
     * Flushes the pending queue in order against the server. Stops
     * at the first failure and keeps the rest queued. Returns true
     * when the queue drained.
     */
    private boolean flushPending(String token) {
        JSONArray ops = prefs.getPending();
        int i = 0;
        while (i < ops.length()) {
            JSONObject op = ops.optJSONObject(i);
            if (op == null) {
                i++;
                continue;
            }
            try {
                String kind = op.optString("op");
                if ("create".equals(kind)) {
                    JSONObject created = MemosApi.createMemo(prefs.getServer(), token,
                            op.optString("content"), op.optString("visibility", "PRIVATE"));
                    String realName = created.optString("name", "");
                    String local = op.optString("local");
                    if (realName.length() > 0 && local.length() > 0) {
                        prefs.renameMemo(local, realName);
                        rewriteReferences(ops, local, realName);
                    }
                } else if ("update".equals(kind)) {
                    MemosApi.updateMemo(prefs.getServer(), token, op.optString("name"),
                            op.optString("content"), op.optBoolean("pinned"),
                            op.optString("state", "NORMAL"));
                } else if ("delete".equals(kind)) {
                    MemosApi.deleteMemo(prefs.getServer(), token, op.optString("name"));
                } else if ("attach".equals(kind)) {
                    File f = new File(op.optString("file"));
                    byte[] bytes = readFile(f);
                    String b64 = android.util.Base64.encodeToString(bytes,
                            android.util.Base64.NO_WRAP);
                    MemosApi.createAttachment(prefs.getServer(), token,
                            op.optString("filename"), op.optString("type"),
                            b64, op.optString("memo"));
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
                ops.remove(i);
                prefs.savePending(ops);
            } catch (Exception e) {
                prefs.savePending(ops);
                return false;
            }
        }
        return true;
    }

    /** Points later ops at the real name once a queued create lands. */
    private void rewriteReferences(JSONArray ops, String local, String realName) {
        for (int i = 0; i < ops.length(); i++) {
            JSONObject op = ops.optJSONObject(i);
            if (op == null) {
                continue;
            }
            try {
                if (local.equals(op.optString("name"))) {
                    op.put("name", realName);
                }
                if (local.equals(op.optString("memo"))) {
                    op.put("memo", realName);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private byte[] readFile(File f) throws Exception {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) {
            buf.write(chunk, 0, n);
        }
        in.close();
        return buf.toByteArray();
    }
}
