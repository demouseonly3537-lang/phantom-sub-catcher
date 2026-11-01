# Phantom Sub Catcher

Android tool to intercept and display subscription / config payloads that flow from panels like **ph4nt0m.lol** into Hiddify / Phantom clients.

## What it does

- Opens the panel inside a WebView
- Intercepts network requests that look like subscription, config, API, token, vless/vmess, clash, sing-box etc.
- Shows raw response body in the bottom log
- Pre-fills the key you already have (PHTM-xxxx)
- One-tap copy of everything captured

## Why you need it

On the real Hiddify client the subscription is encrypted / hidden when "Hide servers settings" + "Encrypted subscription" is on.  
This app sits in the middle and dumps whatever the panel actually sends before the client locks it.

## Build APK

### GitHub Actions (recommended)
1. Push to `main`
2. Go to Actions → Build APK → download the artifact

### Local
```bash
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Install APK
2. Key already filled (change if needed)
3. Tap LOAD or just use the page
4. Login / press Connect on the panel
5. Watch bottom log – subscription links, nodes, headers, everything that travels
6. COPY ALL → paste into any editor or into Hiddify manually if you want

## Notes

- Cleartext + user certs allowed so you can also MITM with other tools if you want
- No root required
- Pure Kotlin, minSdk 24

ashxmay forged.
