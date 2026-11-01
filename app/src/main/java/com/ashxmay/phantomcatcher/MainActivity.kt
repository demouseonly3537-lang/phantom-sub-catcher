package com.ashxmay.phantomcatcher

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val mainHandler = Handler(Looper.getMainLooper())

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
        appendLog(">> Panel open. Key auto-fill + FORCE UNLOCK. Body intercept ON.")
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

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun log(msg: String) {
                runOnUiThread { appendLog("JS: $msg") }
            }
        }, "Catcher")

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
                        try {
                            val decoded = String(Base64.decode(payload.substringAfterLast("/"), Base64.DEFAULT), Charsets.UTF_8)
                            appendLog("LAST_PART_DECODE: $decoded")
                        } catch (_: Exception) {}
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
                injectCatcher(view)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    val m = it.message()
                    if (m.contains("sub", true) || m.contains("config", true) ||
                        m.contains("vless", true) || m.contains("vmess", true) ||
                        m.contains("FOUND", true) || m.contains("crypt", true) ||
                        m.contains("happ", true) || m.contains("BODY", true) ||
                        m.contains("KEY", true) || m.contains("redeem", true)
                    ) {
                        appendLog("CONSOLE: $m")
                    }
                }
                return true
            }
        }
    }

    private fun injectCatcher(view: WebView?) {
        val key = etKey.text.toString().trim()
        if (key.isEmpty()) return

        val js = """
            (function(){
                if(window.__catcherInjected) return;
                window.__catcherInjected = true;
                var KEY = '$key';

                // 1. Override fetch
                var origFetch = window.fetch;
                window.fetch = function(input, init) {
                    try {
                        var url = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
                        var method = (init && init.method) || 'GET';
                        var body = (init && init.body) ? String(init.body) : '';
                        Catcher.log('FETCH ' + method + ' ' + url + ' BODY=' + body.substring(0, 800));
                    } catch(e){}
                    return origFetch.apply(this, arguments);
                };

                // 2. Override XMLHttpRequest
                var origOpen = XMLHttpRequest.prototype.open;
                var origSend = XMLHttpRequest.prototype.send;
                XMLHttpRequest.prototype.open = function(method, url) {
                    this.__m = method; this.__u = url;
                    return origOpen.apply(this, arguments);
                };
                XMLHttpRequest.prototype.send = function(body) {
                    try {
                        Catcher.log('XHR ' + (this.__m||'?') + ' ' + (this.__u||'?') + ' BODY=' + (body ? String(body).substring(0,800) : ''));
                    } catch(e){}
                    return origSend.apply(this, arguments);
                };

                // 3. Force fill every possible input
                function forceFill() {
                    var inputs = document.querySelectorAll('input, textarea');
                    for(var i=0; i<inputs.length; i++){
                        var el = inputs[i];
                        var t = ((el.placeholder||'') + (el.name||'') + (el.id||'') + (el.className||'') + (el.type||'')).toLowerCase();
                        if(t.includes('key') || t.includes('token') || t.includes('code') || t.includes('phtm') || t.includes('access') || t.includes('text') || el.type==='text' || el.type==='password'){
                            el.focus();
                            el.value = KEY;
                            el.setAttribute('value', KEY);
                            el.dispatchEvent(new Event('input', {bubbles:true}));
                            el.dispatchEvent(new Event('change', {bubbles:true}));
                            el.dispatchEvent(new KeyboardEvent('keyup', {bubbles:true}));
                            Catcher.log('FILLED input: ' + (el.name||el.id||el.placeholder||'anon'));
                        }
                    }
                }

                // 4. Click UNLOCK / Connect buttons
                function tryClick() {
                    var btns = document.querySelectorAll('button, a, div[role=button], input[type=submit], span, div');
                    for(var j=0; j<btns.length; j++){
                        var b = btns[j];
                        var txt = (b.innerText || b.value || b.textContent || '').toLowerCase().trim();
                        if(txt === 'unlock' || txt.includes('unlock') || txt === 'connect' || txt.includes('connect vpn') || txt.includes('redeem')){
                            Catcher.log('CLICKING: ' + txt);
                            b.click();
                            return true;
                        }
                    }
                    return false;
                }

                forceFill();
                setTimeout(forceFill, 400);
                setTimeout(forceFill, 1000);
                setTimeout(function(){ forceFill(); tryClick(); }, 1200);
                setTimeout(function(){ forceFill(); tryClick(); }, 2000);
                setTimeout(function(){ forceFill(); tryClick(); }, 3000);

                Catcher.log('Catcher injected. Key=' + KEY);
            })();
        """.trimIndent()

        view?.evaluateJavascript(js, null)

        // Extra delayed click from native side
        mainHandler.postDelayed({
            view?.evaluateJavascript("""
                (function(){
                    var KEY = '$key';
                    var inputs = document.querySelectorAll('input');
                    for(var i=0;i<inputs.length;i++){
                        inputs[i].value = KEY;
                        inputs[i].dispatchEvent(new Event('input',{bubbles:true}));
                    }
                    var btns = document.querySelectorAll('button');
                    for(var j=0;j<btns.length;j++){
                        var t = (btns[j].innerText||'').toLowerCase();
                        if(t.includes('unlock')){ btns[j].click(); break; }
                    }
                })();
            """.trimIndent(), null)
        }, 1500)
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
        if (logBuilder.length > 14000) {
            logBuilder.delete(0, logBuilder.length - 10000)
        }
        tvLog.text = logBuilder.toString()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
