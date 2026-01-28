package com.kswarrior.browser;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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

    private WebView webView;
    private EditText etUrl;
    private ProgressBar progressBar;
    private Set<String> adHosts;

    private String currentDisplayText = "";  // What we show in address bar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create root LinearLayout (vertical)
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Top bar: KS label + EditText
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(16, 12, 16, 12);
        topBar.setBackgroundColor(Color.parseColor("#F0F0F0"));
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        // KS TextView (brand)
        TextView ksLabel = new TextView(this);
        ksLabel.setText("KS");
        ksLabel.setTextSize(24);
        ksLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        ksLabel.setTextColor(Color.BLACK);
        ksLabel.setPadding(0, 0, 16, 0);
        topBar.addView(ksLabel);

        // Address / Search EditText
        etUrl = new EditText(this);
        LinearLayout.LayoutParams etParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        etUrl.setLayoutParams(etParams);
        etUrl.setHint("Search or enter URL");
        etUrl.setImeOptions(EditorInfo.IME_ACTION_GO);
        etUrl.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        etUrl.setSingleLine(true);
        etUrl.setTextColor(Color.BLACK);
        etUrl.setTextSize(16);
        etUrl.setPadding(16, 12, 16, 12);
        etUrl.setBackgroundColor(Color.WHITE);
        // Simple rounded background (you can create drawable or use shape programmatically)
        etUrl.setBackgroundColor(Color.WHITE); // for now plain white

        topBar.addView(etUrl);

        rootLayout.addView(topBar);

        // 2. ProgressBar (thin horizontal)
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                4));  // thin height
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);
        rootLayout.addView(progressBar);

        // 3. WebView (fills remaining space)
        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1f));  // weight=1 to fill rest

        rootLayout.addView(webView);

        // Set the root layout as content
        setContentView(rootLayout);

        // ────────────────────────────────────────
        // Rest is same as before: settings, listeners, adblock, etc.
        // ────────────────────────────────────────

        initializeAdBlocker();

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        webView.setWebViewClient(new AdBlockingWebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                }
            }
        });

        // Handle Enter / Go in keyboard
        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrlOrSearch();
                return true;
            }
            return false;
        });

        // Start with local HTML + empty address bar
        currentDisplayText = "";
        etUrl.setText(currentDisplayText);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void loadUrlOrSearch() {
        String input = etUrl.getText().toString().trim();
        if (TextUtils.isEmpty(input)) return;

        currentDisplayText = input;
        etUrl.setText(currentDisplayText);
        etUrl.setSelection(currentDisplayText.length());

        String urlToLoad;

        if (!isProbablyUrl(input)) {
            urlToLoad = "https://www.google.com/search?q=" + input.replace(" ", "+");
        } else {
            urlToLoad = input;
            if (!urlToLoad.startsWith("http://") && !urlToLoad.startsWith("https://")) {
                urlToLoad = "https://" + urlToLoad;
            }
            currentDisplayText = urlToLoad;
            etUrl.setText(currentDisplayText);
        }

        webView.loadUrl(urlToLoad);
    }

    private boolean isProbablyUrl(String text) {
        text = text.toLowerCase().trim();
        return text.startsWith("http") ||
               text.startsWith("www.") ||
               text.contains("://") ||
               (text.contains(".") && (text.endsWith(".com") || text.endsWith(".io") ||
                text.endsWith(".org") || text.endsWith(".net") || text.endsWith(".in")));
    }

    private void initializeAdBlocker() {
        adHosts = new HashSet<>();
        adHosts.add("googleads.g.doubleclick.net");
        adHosts.add("pagead2.googlesyndication.com");
        adHosts.add("ads.google.com");
        adHosts.add("admob.com");
        adHosts.add("amazon-adsystem.com");
        adHosts.add("facebook.com");
        adHosts.add("twitter.com");
    }

    private class AdBlockingWebViewClient extends WebViewClient {

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString().toLowerCase();
            String host = request.getUrl().getHost();

            if (host != null && adHosts.contains(host.toLowerCase())) {
                return createEmptyResponse();
            }

            if (url.contains("/ads/") || url.contains("ad.") || url.contains("doubleclick") || url.endsWith(".ad.js")) {
                return createEmptyResponse();
            }

            return super.shouldInterceptRequest(view, request);
        }

        @SuppressWarnings("deprecation")
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            url = url.toLowerCase();
            if (adHosts.contains(getHost(url)) || url.contains("/ads/") || url.contains("ad.")) {
                return createEmptyResponse();
            }
            return super.shouldInterceptRequest(view, url);
        }

        private String getHost(String url) {
            try {
                return new java.net.URI(url).getHost().toLowerCase();
            } catch (Exception e) {
                return "";
            }
        }

        private WebResourceResponse createEmptyResponse() {
            return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            if (!url.contains("google.com/search") && !url.startsWith("file:///android_asset/")) {
                currentDisplayText = url;
                etUrl.setText(currentDisplayText);
                etUrl.setSelection(currentDisplayText.length());
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return true;
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
