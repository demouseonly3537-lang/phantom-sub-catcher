package com.ashxmay.phantomcatcher

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var tvLog: TextView
    private lateinit var etKey: EditText
    private lateinit var spinnerSite: Spinner
    private val logBuilder = StringBuilder()
    private val mainHandler = Handler(Looper.getMainLooper())

    data class Site(val name: String, val url: String)

    private val sites = listOf(
        Site("Phantom", "https://ph4nt0m.lol/"),
        Site("X Reg", "https://client.xreg.site/"),
        Site("Aim Hack", "https://client.aimhack.shop/"),
        Site("Troll Mods", "https://client.trollmodz.store/"),
        Site("Drip Wire", "https://dripwire.cc/"),
        Site("Zrex", "https://client.zrex.online/"),
        Site("Silent Cheat", "https://client.silentx.store/")
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        tvLog = findViewById(R.id.tvLog)
        etKey = findViewById(R.id.etKey)
        spinnerSite = findViewById(R.id.spinnerSite)
        val btnLoad = findViewById<Button>(R.id.btnLoad)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnCopy = findViewById<Button>(R.id.btnCopy)

        val names = sites.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        spinnerSite.adapter = adapter
        spinnerSite.setSelection(0)

        etKey.setText("PHTM-860A-9292-EA6A")

        setupWebView()

        btnLoad.setOnClickListener {
            val key = etKey.text.toString().trim()
            val site = sites[spinnerSite.selectedItemPosition]
            appendLog(">> Loading ${site.name} | key=$key")
            webView.loadUrl(site.url)
        }

        btnClear.setOnClickListener {
            logBuilder.clear()
            tvLog.text = "Waiting...\n"
        }

        btnCopy.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("caught", logBuilder.toString()))
            Toast.makeText(this, "Copied full dump", Toast.LENGTH_SHORT).show()
        }

        webView.loadUrl(sites[0].url)
        appendLog(">> Multi Sub Catcher ready. Site select karo → Key dalo → LOAD")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val ws = webView.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
        ws.databaseEnabled = true
        ws.cacheMode = WebSettings.LOAD_DEFAULT
        ws.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        ws.userAgentString =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

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
                    url.startsWith("sing-box://") || url.startsWith("v2raytun://") ||
                    url.startsWith("streisand://") || url.contains("crypt") ||
                    url.startsWith("shadowrocket://") || url.startsWith("surge://")
                ) {
                    appendLog("=== CUSTOM SCHEME CAUGHT ===")
                    appendLog(url)
                    appendLog("RAW: ${url.substringAfter("://")}")
                    appendLog("============================")
                    runOnUiThread {
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("happ", url))
                        Toast.makeText(this@MainActivity, "Deep-link copied!", Toast.LENGTH_LONG).show()
                    }
                    return true
                }
                return false
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
                    if (m.contains("BODY", true) || m.contains("redeem", true) ||
                        m.contains("happ", true) || m.contains("KEY", true) ||
                        m.contains("status", true) || m.contains("error", true) ||
                        m.contains("config", true) || m.contains("sub", true)
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

                var origFetch = window.fetch;
                window.fetch = function(input, init) {
                    var url = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
                    var method = (init && init.method) || 'GET';
                    var body = (init && init.body) ? String(init.body) : '';
                    Catcher.log('REQ ' + method + ' ' + url + ' BODY=' + body.substring(0, 600));

                    return origFetch.apply(this, arguments).then(function(resp) {
                        var clone = resp.clone();
                        clone.text().then(function(t) {
                            Catcher.log('RESP ' + resp.status + ' ' + url + ' → ' + t.substring(0, 900));
                        }).catch(function(){});
                        return resp;
                    });
                };

                var origOpen = XMLHttpRequest.prototype.open;
                var origSend = XMLHttpRequest.prototype.send;
                XMLHttpRequest.prototype.open = function(method, url) {
                    this.__m = method; this.__u = url;
                    return origOpen.apply(this, arguments);
                };
                XMLHttpRequest.prototype.send = function(body) {
                    var self = this;
                    Catcher.log('XHR-REQ ' + (this.__m||'?') + ' ' + (this.__u||'?') + ' BODY=' + (body ? String(body).substring(0,600) : ''));
                    this.addEventListener('load', function() {
                        Catcher.log('XHR-RESP ' + self.status + ' ' + (self.__u||'?') + ' → ' + (self.responseText||'').substring(0,900));
                    });
                    return origSend.apply(this, arguments);
                };

                function forceFill() {
                    var inputs = document.querySelectorAll('input, textarea');
                    for(var i=0; i<inputs.length; i++){
                        var el = inputs[i];
                        el.value = KEY;
                        el.setAttribute('value', KEY);
                        el.dispatchEvent(new Event('input', {bubbles:true}));
                        el.dispatchEvent(new Event('change', {bubbles:true}));
                    }
                }

                function tryClick() {
                    var btns = document.querySelectorAll('button, a, div[role=button], input[type=submit], span');
                    for(var j=0; j<btns.length; j++){
                        var b = btns[j];
                        var txt = (b.innerText || b.value || b.textContent || '').toLowerCase().trim();
                        if(txt.includes('unlock') || txt === 'connect' || txt.includes('redeem') ||
                           txt.includes('login') || txt.includes('activate') || txt.includes('start') ||
                           txt.includes('submit') || txt.includes('continue')){
                            Catcher.log('CLICK → ' + txt);
                            b.click();
                            return true;
                        }
                    }
                    return false;
                }

                forceFill();
                setTimeout(forceFill, 300);
                setTimeout(function(){ forceFill(); tryClick(); }, 800);
                setTimeout(function(){ forceFill(); tryClick(); }, 1600);
                setTimeout(function(){ forceFill(); tryClick(); }, 2800);

                Catcher.log('Injected OK | key=' + KEY);
            })();
        """.trimIndent()

        view?.evaluateJavascript(js, null)

        mainHandler.postDelayed({
            view?.evaluateJavascript("""
                (function(){
                    var KEY='$key';
                    document.querySelectorAll('input').forEach(function(el){
                        el.value=KEY;
                        el.dispatchEvent(new Event('input',{bubbles:true}));
                    });
                    document.querySelectorAll('button').forEach(function(b){
                        var t=(b.innerText||'').toLowerCase();
                        if(t.includes('unlock')||t.includes('connect')||t.includes('login')||t.includes('activate')) b.click();
                    });
                })();
            """.trimIndent(), null)
        }, 1400)
    }

    private fun appendLog(msg: String) {
        logBuilder.append(msg).append("\n")
        if (logBuilder.length > 18000) {
            logBuilder.delete(0, logBuilder.length - 14000)
        }
        tvLog.text = logBuilder.toString()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
