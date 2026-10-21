package com.calcke.qc

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayOutputStream

/**
 * Shows the bundled web app (assets/index.html) full-screen.
 * It serves the files from https://appassets.androidplatform.net so that localStorage,
 * IndexedDB (your saved photos) and everything else behave like a normal secure website.
 * No internet permission is needed: the app is fully offline.
 */
class MainActivity : Activity() {

    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null

    // File being received from the web page, piece by piece.
    private var curName = ""
    private var curMime = ""
    private var buf = ByteArrayOutputStream()
    private val pending = ArrayList<Uri>()
    private var savedToDownloads = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = true
            setSupportZoom(false)
            builtInZoomControls = false
            textZoom = 100
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? = loader.shouldInterceptRequest(request.url)
        }

        webView.webChromeClient = object : WebChromeClient() {
            // Lets <input type="file"> open the photo picker (single or multiple).
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    startActivityForResult(params.createIntent(), REQ_FILE)
                    true
                } catch (e: Exception) {
                    fileCallback = null
                    false
                }
            }

            override fun onJsConfirm(
                view: WebView, url: String, message: String, result: JsResult
            ): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ -> result.confirm() }
                    .setNegativeButton("Cancel") { _, _ -> result.cancel() }
                    .setOnCancelListener { result.cancel() }
                    .show()
                return true
            }

            override fun onJsAlert(
                view: WebView, url: String, message: String, result: JsResult
            ): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ -> result.confirm() }
                    .setOnCancelListener { result.confirm() }
                    .show()
                return true
            }
        }

        webView.addJavascriptInterface(Bridge(), "QCApp")
        webView.loadUrl(START_URL)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_FILE) {
            fileCallback?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            )
            fileCallback = null
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    /** What the web page can call (as window.QCApp). */
    inner class Bridge {
        @JavascriptInterface
        fun begin(name: String, mime: String) {
            curName = name
            curMime = mime
            buf = ByteArrayOutputStream()
        }

        @JavascriptInterface
        fun chunk(base64Piece: String) {
            buf.write(Base64.decode(base64Piece, Base64.DEFAULT))
        }

        @JavascriptInterface
        fun end() {
            val uri = insertFile(curName, curMime, buf.toByteArray())
            if (uri != null) pending.add(uri)
            buf = ByteArrayOutputStream()
        }

        /** action = "save" (just keep the files) or "share" (open the share sheet). */
        @JavascriptInterface
        fun finish(action: String) {
            val uris = ArrayList(pending)
            pending.clear()
            runOnUiThread {
                if (uris.isEmpty()) {
                    toast("Could not save the file")
                } else if (action == "share") {
                    share(uris)
                } else {
                    val where = if (savedToDownloads) "Downloads/QuoteCards" else "Pictures/QuoteCards"
                    toast("Saved ${uris.size} file(s) to $where")
                }
            }
        }
    }

    /** Saves into the gallery (images) or Downloads (zip), so it shows up in Photos. */
    private fun insertFile(name: String, mime: String, bytes: ByteArray): Uri? {
        return try {
            val isImage = mime.startsWith("image/")
            savedToDownloads = !isImage
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    (if (isImage) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_DOWNLOADS) + "/QuoteCards"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val collection = if (isImage)
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = contentResolver.insert(collection, values) ?: return null
            contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
            uri
        } catch (e: Exception) {
            null
        }
    }

    private fun share(uris: ArrayList<Uri>) {
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = contentResolver.getType(uris[0]) ?: "image/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share"))
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    companion object {
        private const val REQ_FILE = 1001
        private const val START_URL = "https://appassets.androidplatform.net/assets/index.html"
    }
}
