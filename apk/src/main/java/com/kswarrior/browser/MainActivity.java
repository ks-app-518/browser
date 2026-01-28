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
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {

    private WebView webView;
    private EditText etUrl;
    private ProgressBar progressBar;
    private Set<String> adHosts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        etUrl = findViewById(R.id.et_url);
        progressBar = findViewById(R.id.progress_bar);

        initializeAdBlocker();

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);         // Important: ks.42web.io requires JS
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
        etUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    loadUrlOrSearch();
                    return true;
                }
                return false;
            }
        });

        // Auto-load your site on start (only top bar visible before load)
        etUrl.setText("https://ks.42web.io");  // Show in address bar
        webView.loadUrl("https://ks.42web.io");
    }

    private void loadUrlOrSearch() {
        loadUrlOrSearch(etUrl.getText().toString().trim());
    }

    private void loadUrlOrSearch(String input) {
        if (TextUtils.isEmpty(input)) return;

        String url = input.trim();

        // If not looks like URL → make it Google search
        if (!isProbablyUrl(url)) {
            url = "https://www.google.com/search?q=" + url.replace(" ", "+");
        } else {
            // Add https:// if no protocol
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
        }

        etUrl.setText(url);
        etUrl.setSelection(url.length());

        webView.loadUrl(url);
    }

    private boolean isProbablyUrl(String text) {
        text = text.toLowerCase();
        return text.contains(".") ||
               text.startsWith("http") ||
               text.startsWith("www.") ||
               text.contains("://") ||
               text.endsWith(".com") || text.endsWith(".io") || text.endsWith(".org");
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
        // Add more domains as needed
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
            etUrl.setText(url); // Sync address bar
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
