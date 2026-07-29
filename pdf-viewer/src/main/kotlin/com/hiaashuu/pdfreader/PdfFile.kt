package com.hiaashuu.pdfreader

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.util.SparseBooleanArray
import com.hiaashuu.pdfreader.exception.PageRenderingException
import com.hiaashuu.pdfreader.util.FitPolicy
import com.hiaashuu.pdfreader.util.PageSizeCalculator
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.util.Size
import com.shockwave.pdfium.util.SizeF
import java.util.ArrayList

internal class PdfFile(
    private var pdfiumCore: PdfiumCore?,
    private var pdfDocument: PdfDocument?,
    private val pageFitPolicy: FitPolicy,
    viewSize: Size,
    private var originalUserPages: IntArray?,
    private val isVertical: Boolean,
    private val spacingPx: Int,
    private val autoSpacing: Boolean,
    private val fitEachPage: Boolean
) {

    var pagesCount: Int = 0
        private set

    private val originalPageSizes: MutableList<Size> = ArrayList()
    private val pageSizes: MutableList<SizeF> = ArrayList()
    private val openedPages = SparseBooleanArray()
    private var originalMaxWidthPageSize = Size(0, 0)
    private var originalMaxHeightPageSize = Size(0, 0)

    var maxHeightPageSize: SizeF = SizeF(0f, 0f)
        private set
    var maxWidthPageSize: SizeF = SizeF(0f, 0f)
        private set

    private val pageOffsets: MutableList<Float> = ArrayList()
    private val pageSpacing: MutableList<Float> = ArrayList()
    private var documentLength = 0f

    companion object {
        private val lock = Any()
    }

    init {
        setup(viewSize)
    }

    private fun setup(viewSize: Size) {
        pagesCount = if (originalUserPages != null) {
            originalUserPages!!.size
        } else {
            pdfiumCore!!.getPageCount(pdfDocument)
        }

        for (i in 0 until pagesCount) {
            val pageSize = pdfiumCore!!.getPageSize(pdfDocument, documentPage(i))
            if (pageSize.width > originalMaxWidthPageSize.width) {
                originalMaxWidthPageSize = pageSize
            }
            if (pageSize.height > originalMaxHeightPageSize.height) {
                originalMaxHeightPageSize = pageSize
            }
            originalPageSizes.add(pageSize)
        }

        recalculatePageSizes(viewSize)
    }

    fun recalculatePageSizes(viewSize: Size) {
        pageSizes.clear()
        val calculator = PageSizeCalculator(
            pageFitPolicy, originalMaxWidthPageSize,
            originalMaxHeightPageSize, viewSize, fitEachPage
        )
        maxWidthPageSize = calculator.optimalMaxWidthPageSize
        maxHeightPageSize = calculator.optimalMaxHeightPageSize

        for (size in originalPageSizes) {
            pageSizes.add(calculator.calculate(size))
        }
        if (autoSpacing) {
            prepareAutoSpacing(viewSize)
        }
        prepareDocLen()
        preparePagesOffset()
    }

    fun getPageSize(pageIndex: Int): SizeF {
        val docPage = documentPage(pageIndex)
        if (docPage < 0) {
            return SizeF(0f, 0f)
        }
        return pageSizes[pageIndex]
    }

    fun getScaledPageSize(pageIndex: Int, zoom: Float): SizeF {
        val size = getPageSize(pageIndex)
        return SizeF(size.width * zoom, size.height * zoom)
    }

    val maxPageSize: SizeF
        get() = if (isVertical) maxWidthPageSize else maxHeightPageSize

    val maxPageWidth: Float
        get() = maxPageSize.width

    val maxPageHeight: Float
        get() = maxPageSize.height

    private fun prepareAutoSpacing(viewSize: Size) {
        pageSpacing.clear()
        for (i in 0 until pagesCount) {
            val pageSize = pageSizes[i]
            var spacing = Math.max(
                0f,
                if (isVertical) viewSize.height - pageSize.height else viewSize.width - pageSize.width
            )
            if (i < pagesCount - 1) {
                spacing += spacingPx.toFloat()
            }
            pageSpacing.add(spacing)
        }
    }

    private fun prepareDocLen() {
        var length = 0f
        for (i in 0 until pagesCount) {
            val pageSize = pageSizes[i]
            length += if (isVertical) pageSize.height else pageSize.width
            if (autoSpacing) {
                length += pageSpacing[i]
            } else if (i < pagesCount - 1) {
                length += spacingPx.toFloat()
            }
        }
        documentLength = length
    }

    private fun preparePagesOffset() {
        pageOffsets.clear()
        var offset = 0f
        for (i in 0 until pagesCount) {
            val pageSize = pageSizes[i]
            val size = if (isVertical) pageSize.height else pageSize.width
            if (autoSpacing) {
                offset += pageSpacing[i] / 2f
                if (i == 0) {
                    offset -= spacingPx / 2f
                } else if (i == pagesCount - 1) {
                    offset += spacingPx / 2f
                }
                pageOffsets.add(offset)
                offset += size + pageSpacing[i] / 2f
            } else {
                pageOffsets.add(offset)
                offset += size + spacingPx
            }
        }
    }

    fun getDocLen(zoom: Float): Float {
        return documentLength * zoom
    }

    fun getPageLength(pageIndex: Int, zoom: Float): Float {
        val size = getPageSize(pageIndex)
        return (if (isVertical) size.height else size.width) * zoom
    }

    fun getPageSpacing(pageIndex: Int, zoom: Float): Float {
        val spacing = if (autoSpacing) pageSpacing[pageIndex] else spacingPx.toFloat()
        return spacing * zoom
    }

    fun getPageOffset(pageIndex: Int, zoom: Float): Float {
        val docPage = documentPage(pageIndex)
        if (docPage < 0) {
            return 0f
        }
        return pageOffsets[pageIndex] * zoom
    }

    fun getSecondaryPageOffset(pageIndex: Int, zoom: Float): Float {
        val pageSize = getPageSize(pageIndex)
        if (isVertical) {
            val maxWidth = maxPageWidth
            return zoom * (maxWidth - pageSize.width) / 2
        } else {
            val maxHeight = maxPageHeight
            return zoom * (maxHeight - pageSize.height) / 2
        }
    }

    fun getPageAtOffset(offset: Float, zoom: Float): Int {
        var currentPage = 0
        for (i in 0 until pagesCount) {
            val off = pageOffsets[i] * zoom - getPageSpacing(i, zoom) / 2f
            if (off >= offset) {
                break
            }
            currentPage++
        }
        return if (--currentPage >= 0) currentPage else 0
    }

    @Throws(PageRenderingException::class)
    fun openPage(pageIndex: Int): Boolean {
        val docPage = documentPage(pageIndex)
        if (docPage < 0) {
            return false
        }

        synchronized(lock) {
            if (openedPages.indexOfKey(docPage) < 0) {
                try {
                    pdfiumCore!!.openPage(pdfDocument, docPage)
                    openedPages.put(docPage, true)
                    return true
                } catch (e: Exception) {
                    openedPages.put(docPage, false)
                    throw PageRenderingException(pageIndex, e)
                }
            }
            return false
        }
    }

    fun pageHasError(pageIndex: Int): Boolean {
        val docPage = documentPage(pageIndex)
        return !openedPages.get(docPage, false)
    }

    fun renderPageBitmap(bitmap: Bitmap, pageIndex: Int, bounds: Rect, annotationRendering: Boolean) {
        val docPage = documentPage(pageIndex)
        pdfiumCore!!.renderPageBitmap(
            pdfDocument, bitmap, docPage,
            bounds.left, bounds.top, bounds.width(), bounds.height(), annotationRendering
        )
    }

    val metaData: PdfDocument.Meta?
        get() {
            if (pdfDocument == null) {
                return null
            }
            return pdfiumCore!!.getDocumentMeta(pdfDocument)
        }

    val bookmarks: List<PdfDocument.Bookmark>
        get() {
            if (pdfDocument == null) {
                return ArrayList()
            }
            return pdfiumCore!!.getTableOfContents(pdfDocument)
        }

    fun getPageLinks(pageIndex: Int): List<PdfDocument.Link> {
        val docPage = documentPage(pageIndex)
        return pdfiumCore!!.getPageLinks(pdfDocument, docPage)
    }

    fun mapRectToDevice(pageIndex: Int, startX: Int, startY: Int, sizeX: Int, sizeY: Int, rect: RectF?): RectF {
        val docPage = documentPage(pageIndex)
        return pdfiumCore!!.mapRectToDevice(pdfDocument, docPage, startX, startY, sizeX, sizeY, 0, rect)
    }

    fun dispose() {
        if (pdfiumCore != null && pdfDocument != null) {
            pdfiumCore!!.closeDocument(pdfDocument)
        }

        pdfDocument = null
        originalUserPages = null
    }

    fun determineValidPageNumberFrom(userPage: Int): Int {
        if (userPage <= 0) {
            return 0
        }
        if (originalUserPages != null) {
            if (userPage >= originalUserPages!!.size) {
                return originalUserPages!!.size - 1
            }
        } else {
            if (userPage >= pagesCount) {
                return pagesCount - 1
            }
        }
        return userPage
    }

    fun documentPage(userPage: Int): Int {
        var documentPage = userPage
        if (originalUserPages != null) {
            if (userPage < 0 || userPage >= originalUserPages!!.size) {
                return -1
            } else {
                documentPage = originalUserPages!![userPage]
            }
        }

        if (documentPage < 0 || userPage >= pagesCount) {
            return -1
        }

        return documentPage
    }
}