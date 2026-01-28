package com.kswarrior.browser;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ProgressBar;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private WebView webView;
    private EditText etUrl;
    private ProgressBar progressBar;
    private Set<String> adHosts;

    private String currentDisplayText = "";  // What we show in the bar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        etUrl = findViewById(R.id.et_url);
        progressBar = findViewById(R.id.progress_bar);

        initializeAdBlocker();

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);         // Required for ks.42web.io
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

        // Handle Enter / Go
        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadUrlOrSearch();
                return true;
            }
            return false;
        });

        // Start with your site + empty address bar
        currentDisplayText = "";
        etUrl.setText(currentDisplayText);
        webView.loadUrl("https://ks.42web.io");
    }

    private void loadUrlOrSearch() {
        String input = etUrl.getText().toString().trim();
        if (TextUtils.isEmpty(input)) return;

        // Remember what user typed → show this in bar
        currentDisplayText = input;
        etUrl.setText(currentDisplayText);
        etUrl.setSelection(currentDisplayText.length());

        String urlToLoad;

        if (!isProbablyUrl(input)) {
            // It's a search → load Google but show only user text in bar
            urlToLoad = "https://www.google.com/search?q=" + input.replace(" ", "+");
        } else {
            // It's a URL → add https if needed
            urlToLoad = input;
            if (!urlToLoad.startsWith("http://") && !urlToLoad.startsWith("https://")) {
                urlToLoad = "https://" + urlToLoad;
            }
            currentDisplayText = urlToLoad;  // For real URLs we show full cleaned URL
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
        // Add more if needed
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
            // Only update bar if it's a real navigation (not search)
            // During search we keep the clean query text
            if (!url.contains("google.com/search")) {
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
