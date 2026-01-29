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
    private String currentDisplayText = "";

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

        // ───── Top Bar ─────
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(16, 12, 16, 12);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(Color.parseColor("#F0F0F0"));

        TextView ks = new TextView(this);
        ks.setText("KS");
        ks.setTextSize(24);
        ks.setTypeface(null, android.graphics.Typeface.BOLD);
        ks.setTextColor(Color.BLACK);
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
        etUrl.setTextColor(Color.BLACK);
        etUrl.setPadding(16, 12, 16, 12);
        etUrl.setBackgroundColor(Color.WHITE);

        topBar.addView(ks);
        topBar.addView(etUrl);

        // ───── Progress Bar ─────
        progressBar = new ProgressBar(this, null,
                android.R.attr.progressBarStyleHorizontal);
        progressBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 4
        ));
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);

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

        // ───── Clients ─────
        webView.setWebViewClient(new AdBlockClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(progress);
                if (progress == 100) progressBar.setVisibility(View.GONE);
            }
        });

        // ───── Address Bar Action ─────
        etUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                loadFromBar();
                return true;
            }
            return false;
        });

        // ───── Load HOME ─────
        webView.loadUrl("file:///android_asset/index.html");
    }

    // ─────────────────────────────
    // URL / SEARCH LOGIC
    // ─────────────────────────────
    private void loadFromBar() {
        String input = etUrl.getText().toString().trim();
        if (TextUtils.isEmpty(input)) return;

        String url;
        if (isProbablyUrl(input)) {
            url = input.startsWith("http") ? input : "https://" + input;
        } else {
            url = "https://www.google.com/search?q=" +
                    input.replace(" ", "+");
        }

        currentDisplayText = url;
        etUrl.setText(currentDisplayText);
        etUrl.setSelection(currentDisplayText.length());

        webView.loadUrl(url);
    }

    private boolean isProbablyUrl(String t) {
        t = t.toLowerCase();
        return t.startsWith("http") ||
               t.startsWith("www.") ||
               (t.contains(".") &&
               (t.endsWith(".com") || t.endsWith(".org") ||
                t.endsWith(".net") || t.endsWith(".io") ||
                t.endsWith(".in")));
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
        adHosts.add("facebook.com");
        adHosts.add("twitter.com");
    }

    private class AdBlockClient extends WebViewClient {

        @Override
        public WebResourceResponse shouldInterceptRequest(
                WebView view, WebResourceRequest request) {

            String host = request.getUrl().getHost();
            String url = request.getUrl().toString().toLowerCase();

            if (host != null && adHosts.contains(host)) {
                return empty();
            }
            if (url.contains("/ads/") || url.contains("doubleclick")) {
                return empty();
            }
            return super.shouldInterceptRequest(view, request);
        }

        private WebResourceResponse empty() {
            return new WebResourceResponse(
                    "text/plain", "utf-8",
                    new ByteArrayInputStream(new byte[0])
            );
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (!url.startsWith("file:///")) {
                etUrl.setText(url);
                etUrl.setSelection(url.length());
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(
                WebView view, WebResourceRequest request) {
            view.loadUrl(request.getUrl().toString());
            return true;
        }
    }

    // ─────────────────────────────
    // BACK
    // ─────────────────────────────
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
