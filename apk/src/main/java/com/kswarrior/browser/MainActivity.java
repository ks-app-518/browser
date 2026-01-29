package com.kswarrior.browser;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String HOME_URL = "file:///android_asset/index.html";
    private static final String TOP_BAR_URL = "file:///android_asset/top_bar.html";

    private WebView webView;
    private WebView topBarWebView;
    private ProgressBar progressBar;

    private Set<String> adHosts;
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

        // ───── Top Bar WebView ─────
        topBarWebView = new WebView(this);
        topBarWebView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        WebSettings topWs = topBarWebView.getSettings();
        topWs.setJavaScriptEnabled(true);
        topWs.setDomStorageEnabled(true);

        topBarWebView.addJavascriptInterface(new TopBarBridge(), "AndroidTopBar");
        topBarWebView.loadUrl(TOP_BAR_URL);

        // ───── Progress Bar ─────
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 4
        ));
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);

        // ───── Main WebView ─────
        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ));

        // ───── Assemble Layout ─────
        root.addView(topBarWebView);
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

        // ───── Clients ─────
        webView.setWebViewClient(new AdBlockClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(progress);
                if (progress == 100) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        // ───── Load HOME on start ─────
        goHome();
    }

    // ─────────────────────────────
    // Top Bar Bridge
    // ─────────────────────────────
    private class TopBarBridge {
        @JavascriptInterface
        public void submit(String text) {
            runOnUiThread(() -> loadFromBar(text));
        }
    }

    // ─────────────────────────────
    // Navigation Logic
    // ─────────────────────────────
    private void loadFromBar(String input) {
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

        webView.loadUrl(url);
        isOnHome = false;
    }

    private void goHome() {
        webView.loadUrl(HOME_URL);
        updateTopBar("");
        isOnHome = true;
    }

    private boolean isProbablyUrl(String t) {
        if (TextUtils.isEmpty(t)) return false;
        t = t.toLowerCase().trim();
        return t.startsWith("http") ||
                t.startsWith("www.") ||
                (t.contains(".") &&
                        (t.endsWith(".com") || t.endsWith(".org") ||
                                t.endsWith(".net") || t.endsWith(".io") ||
                                t.endsWith(".in") || t.endsWith(".co") ||
                                t.endsWith(".app") || t.endsWith(".dev")));
    }

    // ─────────────────────────────
    // Update Top Bar
    // ─────────────────────────────
    private void updateTopBar(String text) {
        String safe = text.replace("'", "\\'");
        topBarWebView.evaluateJavascript(
                "setAddress('" + safe + "')", null
        );
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
    }

    private class AdBlockClient extends WebViewClient {

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String host = request.getUrl().getHost();
            if (host != null && adHosts.contains(host.toLowerCase())) {
                return emptyResponse();
            }

            String url = request.getUrl().toString().toLowerCase();
            if (url.contains("/ads/") || url.contains("doubleclick") ||
                    url.contains("googleadservices") || url.contains("adsbygoogle")) {
                return emptyResponse();
            }

            return super.shouldInterceptRequest(view, request);
        }

        private WebResourceResponse emptyResponse() {
            return new WebResourceResponse(
                    "text/plain", "utf-8",
                    new ByteArrayInputStream(new byte[0])
            );
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!url.startsWith("file:///")) {
                updateTopBar(url); // 🔥 AUTO UPDATE TOP BAR
                isOnHome = false;
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
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
}
