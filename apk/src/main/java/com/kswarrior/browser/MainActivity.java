package com.kswarrior.browser

import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize WebView
        val webView: WebView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()

        // Load a default URL
        webView.loadUrl("https://www.google.com")

        // Set up search functionality
        search_button.setOnClickListener {
            performSearch(webView)
        }

        search_bar.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                performSearch(webView)
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun performSearch(webView: WebView) {
        val query = search_bar.text.toString().trim()
        if (query.isNotEmpty()) {
            val url = if (query.contains("http://") || query.contains("https://")) {
                query
            } else {
                "https://www.google.com/search?q=$query"
            }
            webView.loadUrl(url)
        }
    }
}
