package com.kswarrior.ai;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true); // Enable JS if your site needs it
        webSettings.setDomStorageEnabled(true); // Enable storage for better compatibility

        webView.setWebViewClient(new WebViewClient()); // Keep navigation inside the app
        webView.loadUrl("https://aixks.pages.dev"); // Your website URL
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack(); // Navigate back inside WebView
        } else {
            super.onBackPressed();
        }
    }
}
