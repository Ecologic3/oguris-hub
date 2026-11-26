package app.oguri.hub.dashboard;

import app.oguri.hub.R;
import app.oguri.hub.UiKit;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Dashboard module (Hub 1.8, loading reworked in 1.9): a WebView
 * window onto the mnemosyne dashboard, so the live page (cards,
 * VM and container controls, history) is reachable from the hub
 * home without a browser. The dashboard itself stays on the box;
 * this screen only frames it.
 *
 * The module loads exactly one address,
 * https://mnemosyne:3467. The bare name is the box's Tailscale
 * MagicDNS name, which resolves from anywhere: on the home LAN
 * and over the tailnet alike (Adam verified it live from Brno,
 * 26 Nov). Earlier designs tried the LAN name and the raw
 * addresses first, with probing and fallbacks; Adam overruled
 * that in favour of the single name that just works everywhere.
 *
 * What 1.9 keeps from the black-void fix: the 1.8 screen loaded
 * its first address with no loading or error state at all, so a
 * load that hung or failed left the WebView's black background
 * on screen indefinitely (Adam's eduroam screenshot). Now a
 * "Connecting to mnemosyne..." label covers the WebView area
 * while a load is in flight, and a failed load replaces it with
 * an error state and a Retry button. An SSL error is never
 * proceeded past: it cancels the load and shows the error state
 * with a certificate note instead of silently cancelling into
 * the same black page. The header Reload button loads the
 * address again, through the same connecting state.
 *
 * TLS works because the app bundles the Mnemosyne Home CA and
 * trusts it alongside the system certificates (see
 * res/xml/network_security_config.xml); the served certificate
 * covers this name. The dashboard's login cookie persists in
 * the WebView's cookie store, so the password is typed once,
 * not on every visit.
 *
 * The module icon is the hub launcher art for now; a dedicated
 * icon proposal comes later.
 */
public class MainActivity extends Activity {

    private static final String DASHBOARD_URL = "https://mnemosyne:3467";

    private static final int COL_BG = UiKit.BG;
    private static final int COL_TEXT = Color.rgb(235, 242, 247);
    private static final int COL_MUTED = Color.rgb(159, 179, 196);
    private static final int COL_BTN = Color.rgb(22, 50, 74);
    // The dashboard's own signature pink, as this module's accent.
    private static final int COL_ACCENT = 0xFFFF00AE;

    private WebView webView;
    private LinearLayout statusLayout;
    private TextView statusLabel;
    private Button retryButton;
    private boolean errorShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        loadDashboard();
    }

    private void buildUi() {
        getWindow().setBackgroundDrawable(new ColorDrawable(COL_BG));
        UiKit.setSystemBars(this, COL_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COL_BG);
        UiKit.applyRootPadding(this, root);

        // The shared module header: name on the left, the hub
        // launcher art at the top right until the module gets an
        // icon of its own.
        root.addView(UiKit.header(this, "Mnemosyne", COL_TEXT,
                R.mipmap.ic_launcher, COL_BTN, 0xFF264A66, COL_ACCENT));

        Button reload = UiKit.smallButton(this, "Reload", COL_BTN,
                UiKit.pressedOf(COL_BTN), COL_ACCENT);
        LinearLayout.LayoutParams reloadLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        reloadLp.topMargin = UiKit.dp(this, 12);
        reload.setLayoutParams(reloadLp);
        reload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDashboard();
            }
        });
        root.addView(reload);

        FrameLayout stage = new FrameLayout(this);
        LinearLayout.LayoutParams stageLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        stageLp.topMargin = UiKit.dp(this, 12);
        stage.setLayoutParams(stageLp);

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        webView.setBackgroundColor(COL_BG);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        // The dashboard page is desktop-shaped; the wide viewport
        // with overview mode opens it fitted to the phone's width.
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!errorShown) {
                    statusLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedError(WebView view,
                                        WebResourceRequest request,
                                        WebResourceError error) {
                // A failed main-frame load (name unresolved, host
                // unreachable, connection dropped) shows the error
                // state instead of the WebView's black page. HTTP
                // errors are separate and keep the default
                // behaviour: a 5xx means the box answered.
                if (request.isForMainFrame()) {
                    showError("Mnemosyne is not reachable"
                            + " on this network.");
                }
            }

            @Override
            public void onReceivedSslError(WebView view,
                                           SslErrorHandler handler,
                                           SslError error) {
                // Never proceed past a certificate failure. The
                // default cancel is made explicit, and the user
                // gets the error state with a note instead of a
                // silent black page.
                handler.cancel();
                showError("Mnemosyne is not reachable on this network."
                        + "\n\nCertificate error: the dashboard's"
                        + " certificate was rejected.");
            }
        });
        stage.addView(webView);

        // Status overlay: connecting label, or the error state
        // with its Retry button. Opaque module background, so the
        // WebView area is never a black void.
        statusLayout = new LinearLayout(this);
        statusLayout.setOrientation(LinearLayout.VERTICAL);
        statusLayout.setGravity(Gravity.CENTER);
        statusLayout.setBackgroundColor(COL_BG);
        statusLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        int padH = UiKit.dp(this, 32);
        statusLayout.setPadding(padH, 0, padH, 0);

        statusLabel = new TextView(this);
        statusLabel.setTextColor(COL_MUTED);
        statusLabel.setTextSize(16);
        statusLabel.setGravity(Gravity.CENTER);
        statusLayout.addView(statusLabel);

        retryButton = UiKit.smallButton(this, "Retry", COL_BTN,
                UiKit.pressedOf(COL_BTN), COL_ACCENT);
        LinearLayout.LayoutParams retryLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        retryLp.topMargin = UiKit.dp(this, 16);
        retryLp.gravity = Gravity.CENTER_HORIZONTAL;
        retryButton.setLayoutParams(retryLp);
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadDashboard();
            }
        });
        statusLayout.addView(retryButton);

        stage.addView(statusLayout);
        root.addView(stage);

        setContentView(root);
    }

    private void showConnecting() {
        errorShown = false;
        statusLabel.setText("Connecting to mnemosyne...");
        retryButton.setVisibility(View.GONE);
        statusLayout.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        errorShown = true;
        statusLabel.setText(message);
        retryButton.setVisibility(View.VISIBLE);
        statusLayout.setVisibility(View.VISIBLE);
    }

    /** Loads the one dashboard address, through the connecting state. */
    private void loadDashboard() {
        if (webView == null) {
            return;
        }
        showConnecting();
        webView.loadUrl(DASHBOARD_URL);
    }

    /** Back walks the WebView's own history before leaving the module. */
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) {
                parent.removeView(webView);
            }
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
