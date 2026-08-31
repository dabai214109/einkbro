package info.plateaukao.einkbro.browser

import info.plateaukao.einkbro.view.EBWebView

/**
 * Injects the credentials observer into regular pages (login-form submit /
 * login-button click reporting) and owns the one-click fill routine.
 */
object SharkPasswordManager {

    /** Called from EBWebViewClient.onPageFinished for non-blank pages. */
    fun onPageFinished(ebWebView: EBWebView, url: String) {
        if (ebWebView.incognito) return
        if (!url.startsWith("http")) return
        ebWebView.evaluateJavascript(OBSERVER_SCRIPT, null)
    }

    /** Fills the first visible password input plus the last visible text input before it. */
    fun fillCredentials(ebWebView: EBWebView, username: String, password: String) {
        val user = org.json.JSONObject.quote(username)
        val pass = org.json.JSONObject.quote(password)
        ebWebView.evaluateJavascript(
            FILL_SCRIPT.replace("%USER%", user).replace("%PASS%", pass),
            null,
        )
    }

    // Reports through window.sharkDock (PasswordBridge). Debounced per page:
    // visibility toggles and double submits must not stack prompts.
    private const val OBSERVER_SCRIPT = "(function(){" +
        "if (window.__sharkPwInit) return; window.__sharkPwInit = true;" +
        "function bridge(){ return window.sharkDock; }" +
        "function visible(el){ return el && (el.offsetWidth > 0 || el.offsetHeight > 0 || el === document.activeElement); }" +
        "function findUser(pw){" +
        "var scope = pw.form || document;" +
        "var list = scope.querySelectorAll('input[type=text],input[type=email],input[type=tel],input[autocomplete*=\"username\"],input:not([type])');" +
        "for (var i = 0; i < list.length; i++){ if (list[i] !== pw && visible(list[i])) return list[i]; }" +
        "return null; }" +
        "var last = 0;" +
        "function report(pw){" +
        "var b = bridge(); if (!b || !pw.value) return;" +
        "var now = Date.now(); if (now - last < 2000) return; last = now;" +
        "var u = findUser(pw);" +
        "b.onCredentials(window.location.origin, u ? u.value : '', pw.value); }" +
        "document.addEventListener('submit', function(e){" +
        "var t = e.target; if (t && t.querySelector){" +
        "var pw = t.querySelector('input[type=password]'); if (pw) report(pw); } }, true);" +
        "document.addEventListener('click', function(e){" +
        "var t = e.target; if (!t || !t.closest) return;" +
        "var btn = t.closest('button,[type=submit],[role=button]');" +
        "if (!btn) return;" +
        "var form = btn.closest('form');" +
        "var pw = (form || document).querySelector('input[type=password]');" +
        "if (pw) report(pw); }, true);" +
        "if (bridge()) bridge().onPasswordPage(window.location.origin);" +
        "})();"

    private const val FILL_SCRIPT = "(function(){" +
        "var USER = %USER%, PASS = %PASS%;" +
        "function visible(el){ return el.offsetWidth > 0 || el.offsetHeight > 0 || el === document.activeElement; }" +
        "function setVal(el, v){" +
        "var proto = el.type === 'textarea' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;" +
        "var d = Object.getOwnPropertyDescriptor(proto, 'value');" +
        "if (d && d.set) d.set.call(el, v); else el.value = v;" +
        "el.dispatchEvent(new Event('input', {bubbles: true}));" +
        "el.dispatchEvent(new Event('change', {bubbles: true})); }" +
        "var pws = Array.prototype.slice.call(document.querySelectorAll('input[type=password]')).filter(visible);" +
        "if (!pws.length) pws = Array.prototype.slice.call(document.querySelectorAll('input[type=password]'));" +
        "var pw = pws[0]; if (!pw) return;" +
        "setVal(pw, PASS);" +
        "var scope = pw.form || document;" +
        "var inputs = Array.prototype.slice.call(scope.querySelectorAll('input')).filter(function(el){" +
        "return el !== pw && (el.type === 'text' || el.type === 'email' || el.type === 'tel' || !el.type) && visible(el); });" +
        "if (inputs.length) setVal(inputs[inputs.length - 1], USER);" +
        "})();"
}
