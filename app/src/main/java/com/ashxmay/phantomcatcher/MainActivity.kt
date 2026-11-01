package com.ashxmay.phantomcatcher

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.webkit.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.util.concurrent.Executors
import android.util.Base64

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

        etKey.setText("PHTM-860A-9292-EA6A")

        setupWebView()

        btnLoad.setOnClickListener {
            val key = etKey.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, "Key dalo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            appendLog(">> Loading panel with key: $key")
            webView.loadUrl("https://ph4nt0m.lol/")
        }

        btnClear.setOnClickListener {
            logBuilder.clear()
            tvLog.text = "Waiting for traffic...\n"
        }

        btnCopy.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("caught", logBuilder.toString()))
            Toast.makeText(this, "Copied full dump", Toast.LENGTH_SHORT).show()
        }

        webView.loadUrl("https://ph4nt0m.lol/")
        appendLog(">> Panel open. Key auto-fill. UNLOCK dabao. happ:// catch hoga.")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val ws = webView.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
        ws.databaseEnabled = true
        ws.cacheMode = WebSettings.LOAD_NO_CACHE
        ws.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        ws.userAgentString =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                appendLog("NAV: $url")

                if (url.startsWith("happ://") || url.startsWith("hiddify://") ||
                    url.startsWith("v2rayng://") || url.startsWith("clash://") ||
                    url.startsWith("sing-box://") || url.contains("crypt")
                ) {
                    appendLog("=== CUSTOM SCHEME CAUGHT ===")
                    appendLog(url)
                    try {
                        val payload = url.substringAfter("://")
                        appendLog("RAW PAYLOAD: $payload")
                        val parts = payload.split("/")
                        for (p in parts) {
                            if (p.length > 40) {
                                try {
                                    val decoded = String(Base64.decode(p, Base64.DEFAULT), Charsets.UTF_8)
                                    appendLog("DECODED: $decoded")
                                } catch (_: Exception) {}
                            }
                        }
                    } catch (e: Exception) {
                        appendLog("decode err: ${e.message}")
                    }
                    appendLog("============================")
                    return true
                }
                return false
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                val method = request.method ?: "GET"

                if (isInteresting(url)) {
                    executor.execute {
                        try {
                            val conn = URL(url).openConnection() as HttpURLConnection
                            conn.requestMethod = method
                            conn.connectTimeout = 10000
                            conn.readTimeout = 10000
                            conn.setRequestProperty("User-Agent", ws.userAgentString)
                            request.requestHeaders?.forEach { (k, v) ->
                                if (k.equals("Cookie", true) || k.equals("Authorization", true) ||
                                    k.equals("Content-Type", true) || k.equals("X-Token", true) ||
                                    k.equals("Token", true) || k.equals("Referer", true)
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
                                appendLog("--- $method $url  ($code)")
                                if (text.isNotBlank()) {
                                    if (text.length > 6000) {
                                        appendLog(text.take(6000) + "\n...[truncated]")
                                    } else {
                                        appendLog(text)
                                    }
                                }
                                appendLog("")
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                appendLog("!! $url -> ${e.message}")
                            }
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                appendLog("PAGE: $url")
                val key = etKey.text.toString().trim()
                if (key.isNotEmpty()) {
                    view?.evaluateJavascript(
                        """
                        (function(){
                            var key = '$key';
                            var inputs = document.querySelectorAll('input');
                            for(var i=0;i<inputs.length;i++){
                                var el = inputs[i];
                                var t = ((el.placeholder||'') + (el.name||'') + (el.id||'') + (el.className||'')).toLowerCase();
                                if(t.includes('key') || t.includes('token') || t.includes('code') || t.includes('phtm') || t.includes('access')){
                                    el.value = key;
                                    el.dispatchEvent(new Event('input',{bubbles:true}));
                                    el.dispatchEvent(new Event('change',{bubbles:true}));
                                }
                            }
                            var btns = document.querySelectorAll('button, a, div[role=button], input[type=submit]');
                            for(var j=0;j<btns.length;j++){
                                var b = btns[j];
                                var txt = (b.innerText||b.value||b.textContent||'').toLowerCase();
                                if(txt.includes('unlock') || txt.includes('connect') || txt.includes('start') || txt.includes('login') || txt.includes('redeem')){
                                    console.log('FOUND_BTN:' + txt);
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
                    val m = it.message()
                    if (m.contains("sub", true) || m.contains("config", true) ||
                        m.contains("vless", true) || m.contains("vmess", true) ||
                        m.contains("FOUND_BTN", true) || m.contains("crypt", true) ||
                        m.contains("happ", true)
                    ) {
                        appendLog("JS: $m")
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
                u.contains("redeem") || u.contains("happ") || u.contains("events") ||
                u.contains("subscribe") || u.contains("profile") || u.endsWith(".json") ||
                u.contains("crypt")
    }

    private fun appendLog(msg: String) {
        logBuilder.append(msg).append("\n")
        tvLog.text = logBuilder.toString()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
