package com.hiaashuu.pdfreader

import android.os.AsyncTask
import com.hiaashuu.pdfreader.source.DocumentSource
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.util.Size
import java.lang.ref.WeakReference

internal class DecodingAsyncTask(
    private val docSource: DocumentSource,
    private val password: String?,
    private val userPages: IntArray?,
    pdfView: PDFView,
    private val pdfiumCore: PdfiumCore
) : AsyncTask<Void?, Void?, Throwable?>() {

    private var cancelled = false
    private val pdfViewReference: WeakReference<PDFView> = WeakReference(pdfView)
    private var pdfFile: PdfFile? = null

    override fun doInBackground(vararg params: Void?): Throwable? {
        try {
            val pdfView = pdfViewReference.get()
            if (pdfView != null) {
                val pdfDocument = docSource.createDocument(pdfView.context, pdfiumCore, password)
                pdfFile = PdfFile(
                    pdfiumCore, pdfDocument, pdfView.pageFitPolicy, getViewSize(pdfView),
                    userPages, pdfView.isSwipeVertical, pdfView.spacingPx, pdfView.isAutoSpacingEnabled,
                    pdfView.isFitEachPage
                )
                return null
            } else {
                return NullPointerException("pdfView == null")
            }
        } catch (t: Throwable) {
            return t
        }
    }

    private fun getViewSize(pdfView: PDFView): Size {
        return Size(pdfView.width, pdfView.height)
    }

    override fun onPostExecute(t: Throwable?) {
        val pdfView = pdfViewReference.get()
        if (pdfView != null) {
            if (t != null) {
                pdfView.loadError(t)
                return
            }
            if (!cancelled) {
                pdfView.loadComplete(pdfFile!!)
            }
        }
    }

    override fun onCancelled() {
        cancelled = true
    }
}