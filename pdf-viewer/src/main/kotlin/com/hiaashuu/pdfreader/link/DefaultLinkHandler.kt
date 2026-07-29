package com.hiaashuu.pdfreader.link

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.hiaashuu.pdfreader.PDFView
import com.hiaashuu.pdfreader.model.LinkTapEvent

class DefaultLinkHandler(private val pdfView: PDFView) : LinkHandler {

    companion object {
        private val TAG = DefaultLinkHandler::class.java.simpleName
    }

    override fun handleLinkEvent(event: LinkTapEvent) {
        val uri = event.link.uri
        val page = event.link.destPageIdx
        if (!uri.isNullOrEmpty()) {
            handleUri(uri)
        } else if (page != null) {
            handlePage(page)
        }
    }

    private fun handleUri(uri: String) {
        val parsedUri = Uri.parse(uri)
        val intent = Intent(Intent.ACTION_VIEW, parsedUri)
        val context: Context = pdfView.context
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Log.w(TAG, "No activity found for URI: $uri")
        }
    }

    private fun handlePage(page: Int) {
        pdfView.jumpTo(page)
    }
}