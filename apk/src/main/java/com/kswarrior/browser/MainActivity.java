package com.kswarrior.browser;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String HOME_URL = "file:///android_asset/index.html";

    private LinearLayout topBar;
    private WebView webView;
    private EditText etUrl;
    private ProgressBar progressBar;

    private Set<String> adHosts;
    private String currentDisplayText = "";
    private boolean isOnHome = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ───── Root Layout ─────
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // ───── Top Bar (initially hidden on home) ─────
        topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(16, 12, 16, 12);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(0xFFF0F0F0); // Keep light as in your screenshot/XML; change to #050508 for dark if preferred
        topBar.setVisibility(View.GONE); // Hidden on start (home)

        TextView ks = new TextView(this);
        ks.setText("KS");
        ks.setTextSize(24);
        ks.setTypeface(null, android.graphics.Typeface.BOLD);
        ks.setTextColor(0xFF000000);
        ks.setPadding(0, 0, 16, 0);

        etUrl = new EditText(this);
        etUrl.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));
        etUrl.setHint("Search or enter URL");
        etUrl.setSingleLine(true);
        etUrl.setImeOptions(EditorInfo.IME_ACTION_GO);
        etUrl.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_URI);
        etUrl.setTextColor(0xFF000000);
        etUrl.setHintTextColor(0xFF888888);
        etUrl.setPadding(16, 12, 16, 12);
        etUrl.setBackgroundColor(0xFFFFFFFF);

        topBar.addView(ks);
        topBar.addView(etUrl);

        // ───── Progress Bar (thin, hidden on home) ─────
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 4
        ));
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);
        // Set progress color to your theme red (#ff003c)
        if (progressBar.getProgressDrawable() != null) {
            progressBar.getProgressDrawable().setColorFilter(0xFFFF003C, android.graphics.PorterDuff.Mode.SRC_IN);
        }

        // ───── WebView ─────
        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ));

        // ───── Assemble Layout ─────
        root.addView(topBar);
        root.addView(progressBar);
        root.addView(webView);
        setContentView(root);

        // ───── AdBlock Init ─────
        initAdBlock();

        // ───── WebView Settings ─────
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(true);
        ws.setSupportZoom(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);

        // ───── Add JS Interface for home page to show top bar on tap/focus ─────
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidBridge");

        // ───── Clients ─────
        webView.setWebViewClient(new AdBlockClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(progress);
                if (progress == 100) progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (!isOnHome && !TextUtils.isEmpty(title)) {
                    etUrl.setHint(title);
                }
            }
        });

        // ───── Address Bar Action ─────
        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                            event.getAction() == KeyEvent.ACTION_DOWN)) {
                loadFromBar();
                return true;
            }
            return false;
        });

        etUrl.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                etUrl.setSelection(etUrl.getText().length());
                if (isOnHome) {
                    etUrl.setText("");
                }
            }
        });

        // ───── Load HOME on start (full-screen home, top bar hidden) ─────
        goHome();
    }

    // ─────────────────────────────
    // Navigation Logic
    // ─────────────────────────────
    private void loadFromBar() {
        String input = etUrl.getText().toString().trim();
        if (TextUtils.isEmpty(input)) {
            goHome();
            return;
        }

        String url;
        if (isProbablyUrl(input)) {
            url = input.startsWith("http") ? input : "https://" + input;
        } else {
            url = "https://www.google.com/search?q=" + input.replace(" ", "+");
        }

        currentDisplayText = url;
        etUrl.setText(url);
        etUrl.setSelection(url.length());
        etUrl.clearFocus();

        topBar.setVisibility(View.VISIBLE); // Ensure shown when loading
        webView.loadUrl(url);
        isOnHome = false;
    }

    private void goHome() {
        webView.loadUrl(HOME_URL);
        etUrl.setText("");
        etUrl.setHint("Search or enter URL");
        currentDisplayText = "";
        isOnHome = true;
        etUrl.clearFocus();

        topBar.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private boolean isProbablyUrl(String t) {
        if (TextUtils.isEmpty(t)) return false;
        t = t.toLowerCase().trim();
        return t.startsWith("http") ||
                t.startsWith("www.") ||
                (t.contains(".") &&
                        (t.endsWith(".com") || t.endsWith(".org") || t.endsWith(".net") ||
                         t.endsWith(".io") || t.endsWith(".in") || t.endsWith(".co") ||
                         t.endsWith(".app") || t.endsWith(".dev") || t.endsWith(".ai") ||
                         t.endsWith(".gg") || t.endsWith(".tv")));
    }

    // ─────────────────────────────
    // ADBLOCK
    // ─────────────────────────────
    private void initAdBlock() {
        adHosts = new HashSet<>();
        adHosts.add("googleads.g.doubleclick.net");
        adHosts.add("pagead2.googlesyndication.com");
        adHosts.add("ads.google.com");
        adHosts.add("admob.com");
        adHosts.add("amazon-adsystem.com");
        adHosts.add("adservice.google.com");
        adHosts.add("pubmatic.com");
        adHosts.add("casalemedia.com");
        adHosts.add("openx.net");
        adHosts.add("adnxs.com");
    }

    private class AdBlockClient extends WebViewClient {

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String host = request.getUrl().getHost();
            if (host != null && adHosts.contains(host.toLowerCase())) {
                return emptyResponse();
            }

            String urlStr = request.getUrl().toString().toLowerCase();
            if (urlStr.contains("/ads/") || urlStr.contains("doubleclick") ||
                    urlStr.contains("googleadservices") || urlStr.contains("adsbygoogle") ||
                    urlStr.contains("adserver") || urlStr.contains("banner")) {
                return emptyResponse();
            }

            return super.shouldInterceptRequest(view, request);
        }

        private WebResourceResponse emptyResponse() {
            return new WebResourceResponse("text/plain", "utf-8",
                    new ByteArrayInputStream(new byte[0]));
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (url.startsWith("file:///android_asset/")) {
                etUrl.setText("");
                etUrl.setHint("Search or enter URL");
                isOnHome = true;
                topBar.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            } else {
                etUrl.setText(url);
                etUrl.setSelection(url.length());
                isOnHome = false;
                topBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!url.startsWith("file:///")) {
                currentDisplayText = url;
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            view.loadUrl(url);
            return true;
        }
    }

    // ─────────────────────────────
    // Back Button
    // ─────────────────────────────
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // ─────────────────────────────
    // JS Interface to show top bar from home page
    // ─────────────────────────────
    private static class WebAppInterface {
        private final MainActivity mActivity;

        WebAppInterface(MainActivity activity) {
            mActivity = activity;
        }

        @JavascriptInterface
        public void showAddressBar() {
            mActivity.runOnUiThread(() -> {
                mActivity.topBar.setVisibility(View.VISIBLE);
                mActivity.etUrl.requestFocus();
                InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(mActivity.etUrl, InputMethodManager.SHOW_IMPLICIT);
                }
            });
        }
    }
}
