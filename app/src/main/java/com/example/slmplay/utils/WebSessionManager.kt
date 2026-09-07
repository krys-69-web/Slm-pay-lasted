package com.example.slmplay.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * WebSessionManager ensures the user's web session, active page, cookies,
 * DOM storage, and browsing state are strictly preserved when switching tabs
 * or leaving the web browser section.
 */
object WebSessionManager {
    @SuppressLint("StaticFieldLeak")
    private var persistentWebView: WebView? = null
    val savedStateBundle = Bundle()

    @SuppressLint("StaticFieldLeak")
    private var privateWebView: WebView? = null
    val privateSavedStateBundle = Bundle()

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(context: Context): WebView {
        if (persistentWebView == null) {
            persistentWebView = WebView(context.applicationContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.parseColor("#0B0E14"))

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    mediaPlaybackRequiresUserGesture = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    allowFileAccess = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    cacheMode = WebSettings.LOAD_DEFAULT
                    javaScriptCanOpenWindowsAutomatically = true
                }

                // Enable fluid nested scrolling and hardware acceleration
                isNestedScrollingEnabled = true
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = true
                isScrollbarFadingEnabled = true
                scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY
                overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS

                // Strictly preserve all cookies and third-party session tokens
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)
                cookieManager.flush()
            }
        }

        val wv = persistentWebView!!
        // Safely detach from previous parent ViewGroup so AndroidView can attach it
        (wv.parent as? ViewGroup)?.removeView(wv)
        return wv
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreatePrivateWebView(context: Context): WebView {
        if (privateWebView == null) {
            privateWebView = WebView(context.applicationContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(android.graphics.Color.parseColor("#0B0E14"))

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = false // Isolated
                    mediaPlaybackRequiresUserGesture = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    allowFileAccess = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    cacheMode = WebSettings.LOAD_NO_CACHE // Private mode
                    javaScriptCanOpenWindowsAutomatically = false
                }

                // Enable fluid nested scrolling and hardware acceleration
                isNestedScrollingEnabled = true
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = true
                isScrollbarFadingEnabled = true
                scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY
                overScrollMode = WebView.OVER_SCROLL_IF_CONTENT_SCROLLS
            }
        }

        val pwv = privateWebView!!
        (pwv.parent as? ViewGroup)?.removeView(pwv)
        return pwv
    }

    /**
     * Completely wipes private session, cookies, cache and DOM data (Auto Incognito Mode)
     */
    fun clearPrivateSession(context: Context) {
        try {
            privateWebView?.apply {
                clearCache(true)
                clearHistory()
                clearFormData()
                clearSslPreferences()
                loadUrl("about:blank")
                (parent as? ViewGroup)?.removeView(this)
                destroy()
            }
            privateWebView = null
            privateSavedStateBundle.clear()
        } catch (_: Exception) {}
    }

    fun flushSession() {
        try {
            CookieManager.getInstance().flush()
            persistentWebView?.saveState(savedStateBundle)
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    fun getCurrentWebView(): WebView? = persistentWebView
    fun getCurrentPrivateWebView(): WebView? = privateWebView
}
