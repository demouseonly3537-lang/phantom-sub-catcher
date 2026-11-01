package com.ashxmay.phantomcatcher

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var tvLog: TextView
    private lateinit var etKey: EditText
    private val logBuilder = StringBuilder()
    private val executor = Executors.newSingleThreadExecutor()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        tvLog = findViewById(R.id.tvLog)
        etKey = findViewById(R.id.etKey)
        val btnLoad = findViewById<Button>(R.id.btnLoad)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnCopy = findViewById<Button>(R.id.btnCopy)

        // Pre-fill from screenshot key if empty
        etKey.setText("PHTM-860A-9292-EA6A")

        setupWebView()

        btnLoad.setOnClickListener {
            val key = etKey.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, "Key dalo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            appendLog(">> Loading panel with key: $key")
            // Common panel patterns – try both
            webView.loadUrl("https://ph4nt0m.lol/")
            // Also inject key via JS after load if needed
        }

        btnClear.setOnClickListener {
            logBuilder.clear()
            tvLog.text = "Waiting for traffic...\n"
        }

        btnCopy.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("caught", logBuilder.toString()))
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // Auto load
        webView.loadUrl("https://ph4nt0m.lol/")
        appendLog(">> Panel opened. Login / connect karo. Sab traffic yahan dikhega.")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val ws = webView.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
        ws.databaseEnabled = true
        ws.cacheMode = WebSettings.LOAD_NO_CACHE
        ws.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        ws.userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                val method = request.method

                // Capture interesting endpoints
                if (isInteresting(url)) {
                    executor.execute {
                        try {
                            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                            conn.requestMethod = method
                            conn.connectTimeout = 8000
                            conn.readTimeout = 8000
                            conn.setRequestProperty("User-Agent", ws.userAgentString)
                            // Copy headers if needed
                            request.requestHeaders?.forEach { (k, v) ->
                                if (k.equals("Cookie", true) || k.equals("Authorization", true) ||
                                    k.equals("X-Token", true) || k.equals("Token", true)
                                ) {
                                    conn.setRequestProperty(k, v)
                                }
                            }
                            val code = conn.responseCode
                            val bodyStream = if (code in 200..299) conn.inputStream else conn.errorStream
                            val body = bodyStream?.readBytes() ?: byteArrayOf()
                            val text = try {
                                String(body, Charset.forName("UTF-8"))
                            } catch (e: Exception) {
                                "[binary ${body.size} bytes]"
                            }
                            runOnUiThread {
                                appendLog("─── $method $url  ($code)")
                                if (text.length > 4000) {
                                    appendLog(text.take(4000) + "\n...[truncated]")
                                } else {
                                    appendLog(text)
                                }
                                appendLog("")
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                appendLog("!! intercept fail $url : ${e.message}")
                            }
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                appendLog("PAGE: $url")
                // Inject key if input exists
                val key = etKey.text.toString().trim()
                if (key.isNotEmpty()) {
                    view?.evaluateJavascript(
                        """
                        (function(){
                            var inputs = document.querySelectorAll('input');
                            for(var i=0;i<inputs.length;i++){
                                var t = (inputs[i].placeholder||'').toLowerCase() + (inputs[i].name||'').toLowerCase() + (inputs[i].id||'').toLowerCase();
                                if(t.includes('key') || t.includes('token') || t.includes('code') || t.includes('phtm')){
                                    inputs[i].value = '$key';
                                    inputs[i].dispatchEvent(new Event('input',{bubbles:true}));
                                }
                            }
                        })();
                        """.trimIndent(), null
                    )
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    if (it.message().contains("sub", true) || it.message().contains("config", true) ||
                        it.message().contains("vless", true) || it.message().contains("vmess", true)
                    ) {
                        appendLog("JS: ${it.message()}")
                    }
                }
                return true
            }
        }
    }

    private fun isInteresting(url: String): Boolean {
        val u = url.lowercase()
        return u.contains("sub") || u.contains("config") || u.contains("api") ||
                u.contains("token") || u.contains("key") || u.contains("client") ||
                u.contains("v2ray") || u.contains("sing") || u.contains("clash") ||
                u.contains("hiddify") || u.contains("phantom") || u.contains("null") ||
                u.endsWith(".json") || u.contains("subscribe") || u.contains("profile")
    }

    private fun appendLog(msg: String) {
        logBuilder.append(msg).append("\n")
        tvLog.text = logBuilder.toString()
        // auto scroll not critical
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
