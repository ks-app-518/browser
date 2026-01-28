package com.kswarrior.browser;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private WebView webView;
    private Set<String> adHosts; // Simple set of common ad domains (expand as needed)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ad blocker hosts list
        initializeAdBlocker();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Enable JS if your site needs it
        webSettings.setDomStorageEnabled(true); // Enable storage for better compatibility

        // Custom WebViewClient with ad blocking
        webView.setWebViewClient(new AdBlockingWebViewClient());
        webView.loadUrl("https://www.google.com"); // Your website URL
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack(); // Navigate back inside WebView
        } else {
            super.onBackPressed();
        }
    }

    private void initializeAdBlocker() {
        adHosts = new HashSet<>();
        // Common ad domains - this is a minimal list for demo. In production, load from a file or API (e.g., StevenBlack hosts).
        // Add more: doubleclick.net, googlesyndication.com, adservice.google.com, etc.
        adHosts.add("googleads.g.doubleclick.net");
        adHosts.add("pagead2.googlesyndication.com");
        adHosts.add("ads.google.com");
        adHosts.add("admob.com");
        adHosts.add("amazon-adsystem.com");
        adHosts.add("facebook.com/tr");
        adHosts.add("twitter.com/i/ads");
        // Expand with patterns if needed, e.g., check URL.contains("ads/") or use regex for subdomains.
    }

    private class AdBlockingWebViewClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString().toLowerCase();
            String host = request.getUrl().getHost().toLowerCase();

            // Block if host matches known ad domains
            if (adHosts.contains(host)) {
                // Return empty response to block the request
                return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
            }

            // Optional: Block by URL patterns (e.g., anything with /ads/ or .js ad scripts)
            if (url.contains("/ads/") || url.contains("ad.") || url.endsWith(".ad.js")) {
                return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
            }

            // Allow the request
            return super.shouldInterceptRequest(view, request);
        }
    }
}
