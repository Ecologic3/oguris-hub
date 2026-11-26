package app.oguri.hub.dashboard;

import app.oguri.hub.R;
import app.oguri.hub.UiKit;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * Dashboard module (Hub 1.8): a WebView window onto the mnemosyne
 * dashboard, so the live page (cards, VM and container controls,
 * history) is reachable from the hub home without a browser. The
 * dashboard itself stays on the box; this screen only frames it.
 *
 * The page is loaded from the home-LAN name first
 * (https://mnemosyne.home:3467, resolved by the router at home).
 * If that load fails at the connection level (name not resolvable,
 * host unreachable, TLS handshake failure: the failures that land
 * in onReceivedError), the tailnet address
 * (https://100.100.181.48:3467) is loaded once instead, which is
 * the route that works away from home while Tailscale is up. HTTP
 * errors are not a fallback trigger: a 5xx from the LAN address
 * means the box answered, so the error page stands. The fallback
 * is attempted at most once per screen, so a box that is down on
 * both routes shows the error instead of looping.
 *
 * TLS works because the app bundles the Mnemosyne Home CA and
 * trusts it alongside the system certificates (see
 * res/xml/network_security_config.xml). The dashboard's login
 * cookie persists in the WebView's cookie store, so the password
 * is typed once, not on every visit.
 *
 * The module icon is the hub launcher art for now; a dedicated
 * icon proposal comes later.
 */
public class MainActivity extends Activity {

    private static final String PRIMARY_URL = "https://mnemosyne.home:3467";
    private static final String FALLBACK_URL = "https://100.100.181.48:3467";

    private static final int COL_BG = UiKit.BG;
    private static final int COL_TEXT = Color.rgb(235, 242, 247);
    private static final int COL_BTN = Color.rgb(22, 50, 74);
    // The dashboard's own signature pink, as this module's accent.
    private static final int COL_ACCENT = 0xFFFF00AE;

    private WebView webView;
    private boolean fallbackUsed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        webView.loadUrl(PRIMARY_URL);
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
                R.mipmap.ic_launcher));

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
                if (webView != null) {
                    webView.reload();
                }
            }
        });
        root.addView(reload);

        webView = new WebView(this);
        LinearLayout.LayoutParams webLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        webLp.topMargin = UiKit.dp(this, 12);
        webView.setLayoutParams(webLp);

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
            public void onReceivedError(WebView view,
                                        WebResourceRequest request,
                                        WebResourceError error) {
                // Only connection-level failures land here. HTTP
                // errors go to onReceivedHttpError instead and
                // never trigger the fallback (see the class doc).
                if (request.isForMainFrame() && !fallbackUsed) {
                    fallbackUsed = true;
                    view.loadUrl(FALLBACK_URL);
                }
            }
        });
        root.addView(webView);

        setContentView(root);
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
