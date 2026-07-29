package com.hiaashuu.pdfreader

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.hiaashuu.pdfreader.exception.PageRenderingException
import com.hiaashuu.pdfreader.model.PagePart
import kotlin.math.roundToInt

internal class RenderingHandler(looper: Looper, private val pdfView: PDFView) : Handler(looper) {

    companion object {
        const val MSG_RENDER_TASK = 1
        private val TAG = RenderingHandler::class.java.name
    }

    private val renderBounds = RectF()
    private val roundedRenderBounds = Rect()
    private val renderMatrix = Matrix()
    private var running = false

    fun addRenderingTask(
        page: Int, width: Float, height: Float, bounds: RectF, thumbnail: Boolean,
        cacheOrder: Int, bestQuality: Boolean, annotationRendering: Boolean
    ) {
        val task = RenderingTask(width, height, bounds, page, thumbnail, cacheOrder, bestQuality, annotationRendering)
        val msg = obtainMessage(MSG_RENDER_TASK, task)
        sendMessage(msg)
    }

    override fun handleMessage(message: Message) {
        val task = message.obj as RenderingTask
        try {
            val part = proceed(task)
            if (part != null) {
                if (running) {
                    pdfView.post { pdfView.onBitmapRendered(part) }
                } else {
                    part.renderedBitmap.recycle()
                }
            }
        } catch (ex: PageRenderingException) {
            pdfView.post { pdfView.onPageError(ex) }
        }
    }

    @Throws(PageRenderingException::class)
    private fun proceed(renderingTask: RenderingTask): PagePart? {
        val pdfFile = pdfView.pdfFile!!
        pdfFile.openPage(renderingTask.page)

        val w = renderingTask.width.roundToInt()
        val h = renderingTask.height.roundToInt()

        if (w == 0 || h == 0 || pdfFile.pageHasError(renderingTask.page)) {
            return null
        }

        val render: Bitmap
        try {
            render = Bitmap.createBitmap(w, h, if (renderingTask.bestQuality) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Cannot create bitmap", e)
            return null
        }
        calculateBounds(w, h, renderingTask.bounds)

        pdfFile.renderPageBitmap(render, renderingTask.page, roundedRenderBounds, renderingTask.annotationRendering)

        return PagePart(
            renderingTask.page, render,
            renderingTask.bounds, renderingTask.thumbnail,
            renderingTask.cacheOrder
        )
    }

    private fun calculateBounds(width: Int, height: Int, pageSliceBounds: RectF) {
        renderMatrix.reset()
        renderMatrix.postTranslate(-pageSliceBounds.left * width, -pageSliceBounds.top * height)
        renderMatrix.postScale(1 / pageSliceBounds.width(), 1 / pageSliceBounds.height())

        renderBounds.set(0f, 0f, width.toFloat(), height.toFloat())
        renderMatrix.mapRect(renderBounds)
        renderBounds.round(roundedRenderBounds)
    }

    fun stop() {
        running = false
    }

    fun start() {
        running = true
    }

    private inner class RenderingTask(
        var width: Float,
        var height: Float,
        var bounds: RectF,
        var page: Int,
        var thumbnail: Boolean,
        var cacheOrder: Int,
        var bestQuality: Boolean,
        var annotationRendering: Boolean
    )
}