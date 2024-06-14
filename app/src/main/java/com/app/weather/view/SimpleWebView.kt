package com.app.weather.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

class SimpleWebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {

    init {
        settings.apply {
            loadsImagesAutomatically = true
            javaScriptEnabled = false
            domStorageEnabled = false
            builtInZoomControls = false
            useWideViewPort = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_NO_CACHE
            setSupportZoom(false)
        }

        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        setBackgroundColor(Color.TRANSPARENT)

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean = false
        }
    }
}