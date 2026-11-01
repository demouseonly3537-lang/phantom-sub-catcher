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
    private lateinit var tvSiteUrl: TextView
    private val logBuilder = StringBuilder()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var injectCount = 0

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
        tvSiteUrl = findViewById(R.id.tvSiteUrl)
        val btnLoad = findViewById<Button>(R.id.btnLoad)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnCopy = findViewById<Button>(R.id.btnCopy)

        val names = sites.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSite.adapter = adapter

        spinnerSite.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val site = sites[position]
                tvSiteUrl.text = site.url
                appendLog(">> Site changed → ${site.name}")
                loadSelectedSite()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        etKey.setText("")
        etKey.hint = "KEY yahan paste karo"

        setupWebView()

        btnLoad.setOnClickListener {
            loadSelectedSite()
        }

        btnClear.setOnClickListener {
            logBuilder.clear()
            tvLog.text = "Waiting...\n"
            injectCount = 0
        }

        btnCopy.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("caught", logBuilder.toString()))
            Toast.makeText(this, "Log copied!", Toast.LENGTH_SHORT).show()
        }

        tvSiteUrl.text = sites[0].url
        appendLog(">> Ready. Site select → Key paste → auto load / GO")
    }

    private fun loadSelectedSite() {
        val pos = spinnerSite.selectedItemPosition
        if (pos < 0) return
        val site = sites[pos]
        val key = etKey.text.toString().trim()
        injectCount = 0
        appendLog(">> LOAD ${site.name} | key=${if (key.isEmpty()) "(empty)" else key}")
        webView.stopLoading()
        webView.loadUrl("about:blank")
        mainHandler.postDelayed({
            webView.loadUrl(site.url)
        }, 150)
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
                    appendLog("=== DEEP LINK CAUGHT ===")
                    appendLog(url)
                    appendLog("========================")
                    runOnUiThread {
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("deeplink", url))
                        Toast.makeText(this@MainActivity, "Deep-link COPIED!", Toast.LENGTH_LONG).show()
                    }
                    return true
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url == null || url == "about:blank") return
                appendLog("PAGE: $url")
                injectCatcher(view)
                mainHandler.postDelayed({ injectCatcher(view) }, 600)
                mainHandler.postDelayed({ injectCatcher(view) }, 1500)
                mainHandler.postDelayed({ injectCatcher(view) }, 2800)
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
        if (key.isEmpty()) {
            appendLog("!! Key empty – pehle key paste karo")
            return
        }
        injectCount++
        val n = injectCount

        val js = """
            (function(){
                var KEY = '$key';
                var tag = '[$n]';

                if(!window.__fetchPatched){
                    window.__fetchPatched = true;
                    var origFetch = window.fetch;
                    window.fetch = function(input, init) {
                        var url = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
                        var method = (init && init.method) || 'GET';
                        var body = (init && init.body) ? String(init.body) : '';
                        Catcher.log(tag + ' REQ ' + method + ' ' + url + ' BODY=' + body.substring(0, 700));
                        return origFetch.apply(this, arguments).then(function(resp) {
                            var clone = resp.clone();
                            clone.text().then(function(t) {
                                Catcher.log(tag + ' RESP ' + resp.status + ' ' + url + ' → ' + t.substring(0, 1000));
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
                        Catcher.log(tag + ' XHR ' + (this.__m||'?') + ' ' + (this.__u||'?') + ' BODY=' + (body ? String(body).substring(0,700) : ''));
                        this.addEventListener('load', function() {
                            Catcher.log(tag + ' XHR-RESP ' + self.status + ' → ' + (self.responseText||'').substring(0,1000));
                        });
                        return origSend.apply(this, arguments);
                    };
                    Catcher.log(tag + ' fetch/XHR patched');
                }

                function forceKey() {
                    var inputs = document.querySelectorAll('input, textarea');
                    var filled = 0;
                    for(var i=0; i<inputs.length; i++){
                        var el = inputs[i];
                        try {
                            var nativeInputValueSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
                            if(nativeInputValueSetter){
                                nativeInputValueSetter.call(el, KEY);
                            } else {
                                el.value = KEY;
                            }
                            el.setAttribute('value', KEY);
                            el.dispatchEvent(new Event('input', {bubbles:true}));
                            el.dispatchEvent(new Event('change', {bubbles:true}));
                            el.dispatchEvent(new KeyboardEvent('keyup', {bubbles:true}));
                            filled++;
                        } catch(e){}
                    }
                    if(filled > 0) Catcher.log(tag + ' FILLED ' + filled + ' inputs with ' + KEY);
                }

                function tryClick() {
                    var btns = document.querySelectorAll('button, a, div[role=button], input[type=submit]');
                    for(var j=0; j<btns.length; j++){
                        var b = btns[j];
                        var txt = (b.innerText || b.value || b.textContent || '').toLowerCase().trim();
                        if(txt.includes('unlock') || txt === 'connect' || txt.includes('redeem') ||
                           txt.includes('login') || txt.includes('activate') || txt.includes('start') ||
                           txt === 'go' || txt.includes('submit')){
                            Catcher.log(tag + ' CLICK → ' + txt);
                            b.click();
                            return true;
                        }
                    }
                    return false;
                }

                forceKey();
                setTimeout(forceKey, 200);
                setTimeout(function(){ forceKey(); tryClick(); }, 500);
                setTimeout(function(){ forceKey(); tryClick(); }, 1200);

                Catcher.log(tag + ' inject OK key=' + KEY);
            })();
        """.trimIndent()

        view?.evaluateJavascript(js, null)
    }

    private fun appendLog(msg: String) {
        logBuilder.append(msg).append("\n")
        if (logBuilder.length > 20000) {
            logBuilder.delete(0, logBuilder.length - 15000)
        }
        tvLog.text = logBuilder.toString()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
