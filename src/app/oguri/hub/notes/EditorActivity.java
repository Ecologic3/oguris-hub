package app.oguri.hub.notes;

import app.oguri.hub.R;
import app.oguri.hub.UiKit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The memo editor: one text field (Markdown, as Memos itself
 * edits), image attachments, and for an existing memo the pin,
 * archive and delete actions. Saving writes the cache and the
 * pending queue and returns to the stream, which syncs; nothing
 * here talks to the server directly.
 */
public class EditorActivity extends Activity {

    public static final String EXTRA_NAME = "name";
    private static final int REQ_PICK_IMAGE = 7;

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
    private static final int COL_REMOVE_BG = 0xFF3A252C;

    private Prefs prefs;
    private Memo memo;
    private boolean isNew;
    private EditText field;
    private LinearLayout attachBox;
    private Button pinButton;
    private Button archiveButton;

    private static class PendingAttach {
        String filePath;
        String filename;
        String type;
    }

    private final List<PendingAttach> pendingAttach = new ArrayList<PendingAttach>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        String name = getIntent().getStringExtra(EXTRA_NAME);
        Memo found = name == null ? null : prefs.findMemo(name);
        if (found != null) {
            memo = found;
            isNew = false;
        } else {
            memo = new Memo();
            isNew = true;
        }
        buildUi();
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

    private TextView makeText(String text, float size, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private void buildUi() {
        getWindow().setBackgroundDrawable(new ColorDrawable(COL_BG));
        UiKit.setSystemBars(this, COL_BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COL_BG);
        UiKit.applyRootPadding(this, root);
        root.addView(UiKit.header(this, isNew ? "New memo" : "Edit memo", COL_TEXT,
                R.mipmap.ic_mod_notes, COL_RAISED, COL_BORDER, COL_ACCENT));

        LinearLayout editCard = new LinearLayout(this);
        editCard.setOrientation(LinearLayout.VERTICAL);
        editCard.setBackground(UiKit.cardBackground(this, COL_SURFACE, COL_BORDER));
        editCard.setPadding(dp(14), dp(10), dp(14), dp(10));
        editCard.setLayoutParams(lp(16));
        field = new EditText(this);
        field.setText(memo.content);
        field.setSelection(memo.content.length());
        field.setHint("Write a memo...");
        field.setHintTextColor(COL_FAINT);
        field.setTextColor(COL_TEXT);
        field.setTextSize(15.5f);
        field.setGravity(Gravity.TOP);
        field.setMinLines(9);
        field.setBackgroundColor(Color.TRANSPARENT);
        field.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editCard.addView(field);
        root.addView(editCard);

        attachBox = new LinearLayout(this);
        attachBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(attachBox);
        renderAttachments();

        Button attach = UiKit.smallButton(this, "Attach image", COL_RAISED,
                UiKit.pressedOf(COL_RAISED), COL_TEXT);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        alp.topMargin = dp(10);
        attach.setLayoutParams(alp);
        attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                i.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(i, REQ_PICK_IMAGE);
            }
        });
        root.addView(attach);

        if (!isNew) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(lp(16));
            pinButton = UiKit.smallButton(this, "", COL_RAISED,
                    UiKit.pressedOf(COL_RAISED), COL_TEXT);
            pinButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    memo.pinned = !memo.pinned;
                    styleToggles();
                }
            });
            row.addView(pinButton, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            archiveButton = UiKit.smallButton(this, "", COL_RAISED,
                    UiKit.pressedOf(COL_RAISED), COL_TEXT);
            LinearLayout.LayoutParams abLp = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            abLp.setMargins(dp(8), 0, dp(8), 0);
            archiveButton.setLayoutParams(abLp);
            archiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    memo.state = "ARCHIVED".equals(memo.state) ? "NORMAL" : "ARCHIVED";
                    styleToggles();
                }
            });
            row.addView(archiveButton);
            Button delete = UiKit.smallButton(this, "Delete", COL_REMOVE_BG,
                    UiKit.pressedOf(COL_REMOVE_BG), COL_REMOVE);
            delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDelete();
                }
            });
            row.addView(delete, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            root.addView(row);
            styleToggles();
        }

        Button save = UiKit.button(this, "Save", COL_ACCENT_FILL,
                UiKit.pressedOf(COL_ACCENT_FILL), Color.WHITE);
        save.setLayoutParams(lp(16));
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        root.addView(save);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(COL_BG);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void styleToggles() {
        if (pinButton == null) {
            return;
        }
        pinButton.setText(memo.pinned ? "Pinned" : "Pin");
        pinButton.setBackground(UiKit.buttonBackground(this,
                memo.pinned ? COL_ACCENT_FILL : COL_RAISED,
                UiKit.pressedOf(memo.pinned ? COL_ACCENT_FILL : COL_RAISED),
                0, UiKit.BTN_RADIUS_DP));
        pinButton.setTextColor(memo.pinned ? Color.WHITE : COL_TEXT);
        archiveButton.setText("ARCHIVED".equals(memo.state) ? "Restore" : "Archive");
    }

    private void renderAttachments() {
        attachBox.removeAllViews();
        for (Memo.Attachment a : memo.attachments) {
            TextView t = makeText(a.filename, 13.5f, COL_MUTED);
            t.setLayoutParams(lp(8));
            attachBox.addView(t);
        }
        for (PendingAttach p : pendingAttach) {
            TextView t = makeText(p.filename + " (new)", 13.5f, COL_ACCENT);
            t.setLayoutParams(lp(8));
            attachBox.addView(t);
        }
    }

    // ---------------------------------------------------------- actions

    private void save() {
        String content = field.getText().toString();
        if (content.trim().length() == 0 && memo.attachments.isEmpty()
                && pendingAttach.isEmpty()) {
            Toast.makeText(this, "Write something first", Toast.LENGTH_SHORT).show();
            return;
        }
        memo.content = content;
        if (isNew) {
            memo.name = "local-" + UUID.randomUUID().toString();
            memo.createTime = MemosApi.nowIso();
            memo.updateTime = memo.createTime;
            memo.state = "NORMAL";
            memo.visibility = "PRIVATE";
            prefs.upsertMemo(memo);
            Queue.enqueueCreate(prefs, memo);
        } else {
            prefs.removeMemo(memo.name);
            prefs.upsertMemo(memo);
            if (memo.isLocal()) {
                Queue.rewriteCreateContent(prefs, memo.name, memo.content);
            }
            Queue.enqueueUpdate(prefs, memo);
        }
        for (PendingAttach p : pendingAttach) {
            Queue.enqueueAttach(prefs, memo.name, p.filePath, p.filename, p.type);
        }
        finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete memo")
                .setMessage("Delete this memo?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteMemo();
                    }
                })
                .show();
    }

    private void deleteMemo() {
        if (memo.isLocal()) {
            List<String> files = Queue.dropOpsFor(prefs, memo.name);
            for (String f : files) {
                //noinspection ResultOfMethodCallIgnored
                new File(f).delete();
            }
        } else {
            Queue.enqueueDelete(prefs, memo.name);
        }
        for (PendingAttach p : pendingAttach) {
            //noinspection ResultOfMethodCallIgnored
            new File(p.filePath).delete();
        }
        prefs.removeMemo(memo.name);
        finish();
    }

    // ---------------------------------------------------------- image picking

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_IMAGE || resultCode != RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        try {
            String type = getContentResolver().getType(uri);
            if (type == null) {
                type = "image/jpeg";
            }
            String filename = displayName(uri);
            File dir = new File(getCacheDir(), "notes_pending");
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File out = new File(dir, UUID.randomUUID().toString() + ".img");
            InputStream in = getContentResolver().openInputStream(uri);
            FileOutputStream fos = new FileOutputStream(out);
            byte[] chunk = new byte[8192];
            int n;
            while (in != null && (n = in.read(chunk)) != -1) {
                fos.write(chunk, 0, n);
            }
            if (in != null) {
                in.close();
            }
            fos.close();
            PendingAttach p = new PendingAttach();
            p.filePath = out.getAbsolutePath();
            p.filename = filename;
            p.type = type;
            pendingAttach.add(p);
            renderAttachments();
        } catch (Exception e) {
            Toast.makeText(this, "Could not read that image", Toast.LENGTH_SHORT).show();
        }
    }

    private String displayName(Uri uri) {
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String name = c.getString(idx);
                    if (name != null && name.length() > 0) {
                        return name;
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return "image.jpg";
    }
}
