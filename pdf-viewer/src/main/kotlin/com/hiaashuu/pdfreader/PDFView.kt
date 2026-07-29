package com.hiaashuu.pdfreader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Log
import android.widget.RelativeLayout
import com.hiaashuu.pdfreader.exception.PageRenderingException
import com.hiaashuu.pdfreader.link.DefaultLinkHandler
import com.hiaashuu.pdfreader.link.LinkHandler
import com.hiaashuu.pdfreader.listener.Callbacks
import com.hiaashuu.pdfreader.listener.OnDrawListener
import com.hiaashuu.pdfreader.listener.OnErrorListener
import com.hiaashuu.pdfreader.listener.OnLoadCompleteListener
import com.hiaashuu.pdfreader.listener.OnLongPressListener
import com.hiaashuu.pdfreader.listener.OnPageChangeListener
import com.hiaashuu.pdfreader.listener.OnPageErrorListener
import com.hiaashuu.pdfreader.listener.OnPageScrollListener
import com.hiaashuu.pdfreader.listener.OnRenderListener
import com.hiaashuu.pdfreader.listener.OnTapListener
import com.hiaashuu.pdfreader.model.PagePart
import com.hiaashuu.pdfreader.scroll.ScrollHandle
import com.hiaashuu.pdfreader.source.AssetSource
import com.hiaashuu.pdfreader.source.ByteArraySource
import com.hiaashuu.pdfreader.source.DocumentSource
import com.hiaashuu.pdfreader.source.FileSource
import com.hiaashuu.pdfreader.source.InputStreamSource
import com.hiaashuu.pdfreader.source.UriSource
import com.hiaashuu.pdfreader.util.Constants
import com.hiaashuu.pdfreader.util.FitPolicy
import com.hiaashuu.pdfreader.util.MathUtils
import com.hiaashuu.pdfreader.util.SnapEdge
import com.hiaashuu.pdfreader.util.Util
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import com.shockwave.pdfium.util.Size
import com.shockwave.pdfium.util.SizeF
import java.io.File
import java.io.InputStream
import java.util.ArrayList

class PDFView(context: Context, set: AttributeSet?) : RelativeLayout(context, set) {

    companion object {
        private val TAG = PDFView::class.java.simpleName
        const val DEFAULT_MAX_SCALE = 3.0f
        const val DEFAULT_MID_SCALE = 1.75f
        const val DEFAULT_MIN_SCALE = 1.0f
    }

    var minZoom = DEFAULT_MIN_SCALE
    var midZoom = DEFAULT_MID_SCALE
    var maxZoom = DEFAULT_MAX_SCALE

    internal enum class ScrollDir {
        NONE, START, END
    }

    private var scrollDir = ScrollDir.NONE

    internal var cacheManager: CacheManager
    private var animationManager: AnimationManager
    internal var dragPinchManager: DragPinchManager

    internal var pdfFile: PdfFile? = null
    var currentPage: Int = 0
        private set

    var currentXOffset = 0f
        private set
    var currentYOffset = 0f
        private set

    var zoom = 1f
        private set

    var isRecycled = true
        private set

    private var state = State.DEFAULT
    private var decodingAsyncTask: DecodingAsyncTask? = null

    private var renderingHandlerThread: HandlerThread? = null
    internal var renderingHandler: RenderingHandler? = null
    private var pagesLoader: PagesLoader

    internal var callbacks = Callbacks()

    private var paint = Paint()
    private var debugPaint = Paint()

    var pageFitPolicy = FitPolicy.WIDTH
        private set

    var isFitEachPage = false
        private set

    private var defaultPage = 0

    var isSwipeVertical = true
        private set

    var isSwipeEnabled = true
        private set

    var isDoubletapEnabled = true
        internal set

    private var nightMode = false

    var isPageSnap = true

    private var pdfiumCore: PdfiumCore

    var scrollHandle: ScrollHandle? = null
        private set

    private var isScrollHandleInit = false

    var isBestQuality = false

    var isAnnotationRendering = false

    private var renderDuringScale = false

    var isAntialiasing = true
        private set
    private var antialiasFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    var spacingPx = 0
        private set

    var isAutoSpacingEnabled = false
        private set

    var isPageFlingEnabled = true

    private var onDrawPagesNums: MutableList<Int> = ArrayList(10)

    private var hasSize = false
    private var waitingDocumentConfigurator: Configurator? = null

    init {
        renderingHandlerThread = HandlerThread("PDF renderer")

        if (!isInEditMode) {
            cacheManager = CacheManager()
            animationManager = AnimationManager(this)
            dragPinchManager = DragPinchManager(this, animationManager)
            pagesLoader = PagesLoader(this)

            debugPaint.style = Paint.Style.STROKE

            pdfiumCore = PdfiumCore(context)
            setWillNotDraw(false)
        } else {
            cacheManager = CacheManager()
            animationManager = AnimationManager(this)
            dragPinchManager = DragPinchManager(this, animationManager)
            pagesLoader = PagesLoader(this)
            pdfiumCore = PdfiumCore(context)
        }
    }

    private fun load(docSource: DocumentSource, password: String?) {
        load(docSource, password, null)
    }

    private fun load(docSource: DocumentSource, password: String?, userPages: IntArray?) {
        if (!isRecycled) {
            throw IllegalStateException("Don't call load on a PDF View without recycling it first.")
        }
        isRecycled = false
        decodingAsyncTask = DecodingAsyncTask(docSource, password, userPages, this, pdfiumCore)
        decodingAsyncTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun jumpTo(page: Int, withAnimation: Boolean) {
        if (pdfFile == null) {
            return
        }

        val validPage = pdfFile!!.determineValidPageNumberFrom(page)
        val offset = if (validPage == 0) 0f else -pdfFile!!.getPageOffset(validPage, zoom)

        if (isSwipeVertical) {
            if (withAnimation) {
                animationManager.startYAnimation(currentYOffset, offset)
            } else {
                moveTo(currentXOffset, offset)
            }
        } else {
            if (withAnimation) {
                animationManager.startXAnimation(currentXOffset, offset)
            } else {
                moveTo(offset, currentYOffset)
            }
        }
        showPage(validPage)
    }

    fun jumpTo(page: Int) {
        jumpTo(page, false)
    }

    internal fun showPage(pageNb: Int) {
        if (isRecycled) {
            return
        }
        val validPageNb = pdfFile!!.determineValidPageNumberFrom(pageNb)
        currentPage = validPageNb
        loadPages()

        if (scrollHandle != null && !documentFitsView()) {
            scrollHandle?.setPageNum(currentPage + 1)
        }
        callbacks.callOnPageChange(currentPage, pdfFile!!.pagesCount)
    }

    val positionOffset: Float
        get() {
            val offset: Float = if (isSwipeVertical) {
                -currentYOffset / (pdfFile!!.getDocLen(zoom) - height)
            } else {
                -currentXOffset / (pdfFile!!.getDocLen(zoom) - width)
            }
            return MathUtils.limit(offset, 0f, 1f)
        }

    fun setPositionOffset(progress: Float, moveHandle: Boolean) {
        if (isSwipeVertical) {
            moveTo(currentXOffset, (-pdfFile!!.getDocLen(zoom) + height) * progress, moveHandle)
        } else {
            moveTo((-pdfFile!!.getDocLen(zoom) + width) * progress, currentYOffset, moveHandle)
        }
        loadPageByOffset()
    }

    fun setPositionOffset(progress: Float) {
        setPositionOffset(progress, true)
    }

    fun stopFling() {
        animationManager.stopFling()
    }

    val pageCount: Int
        get() {
            if (pdfFile == null) {
                return 0
            }
            return pdfFile!!.pagesCount
        }

    fun setNightMode(nightMode: Boolean) {
        this.nightMode = nightMode
        if (nightMode) {
            val colorMatrixInverted = ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            val filter = ColorMatrixColorFilter(colorMatrixInverted)
            paint.colorFilter = filter
        } else {
            paint.colorFilter = null
        }
    }

    internal fun onPageError(ex: PageRenderingException) {
        if (!callbacks.callOnPageError(ex.page, ex.cause!!)) {
            Log.e(TAG, "Cannot open page " + ex.page, ex.cause)
        }
    }

    fun recycle() {
        waitingDocumentConfigurator = null
        animationManager.stopAll()
        dragPinchManager.disable()

        if (renderingHandler != null) {
            renderingHandler?.stop()
            renderingHandler?.removeMessages(RenderingHandler.MSG_RENDER_TASK)
        }
        if (decodingAsyncTask != null) {
            decodingAsyncTask?.cancel(true)
        }

        cacheManager.recycle()

        if (scrollHandle != null && isScrollHandleInit) {
            scrollHandle?.destroyLayout()
        }

        if (pdfFile != null) {
            pdfFile?.dispose()
            pdfFile = null
        }

        renderingHandler = null
        scrollHandle = null
        isScrollHandleInit = false
        currentXOffset = 0f
        currentYOffset = 0f
        zoom = 1f
        isRecycled = true
        callbacks = Callbacks()
        state = State.DEFAULT
    }

    override fun computeScroll() {
        super.computeScroll()
        if (isInEditMode) {
            return
        }
        animationManager.computeFling()
    }

    override fun onDetachedFromWindow() {
        recycle()
        if (renderingHandlerThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                renderingHandlerThread?.quitSafely()
            } else {
                renderingHandlerThread?.quit()
            }
            renderingHandlerThread = null
        }
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        hasSize = true
        if (waitingDocumentConfigurator != null) {
            waitingDocumentConfigurator?.load()
        }
        if (isInEditMode || state != State.SHOWN) {
            return
        }

        val centerPointInStripXOffset = -currentXOffset + oldw * 0.5f
        val centerPointInStripYOffset = -currentYOffset + oldh * 0.5f

        val relativeCenterPointInStripXOffset: Float
        val relativeCenterPointInStripYOffset: Float

        if (isSwipeVertical) {
            relativeCenterPointInStripXOffset = centerPointInStripXOffset / pdfFile!!.maxPageWidth
            relativeCenterPointInStripYOffset = centerPointInStripYOffset / pdfFile!!.getDocLen(zoom)
        } else {
            relativeCenterPointInStripXOffset = centerPointInStripXOffset / pdfFile!!.getDocLen(zoom)
            relativeCenterPointInStripYOffset = centerPointInStripYOffset / pdfFile!!.maxPageHeight
        }

        animationManager.stopAll()
        pdfFile!!.recalculatePageSizes(Size(w, h))

        if (isSwipeVertical) {
            currentXOffset = -relativeCenterPointInStripXOffset * pdfFile!!.maxPageWidth + w * 0.5f
            currentYOffset = -relativeCenterPointInStripYOffset * pdfFile!!.getDocLen(zoom) + h * 0.5f
        } else {
            currentXOffset = -relativeCenterPointInStripXOffset * pdfFile!!.getDocLen(zoom) + w * 0.5f
            currentYOffset = -relativeCenterPointInStripYOffset * pdfFile!!.maxPageHeight + h * 0.5f
        }
        moveTo(currentXOffset, currentYOffset)
        loadPageByOffset()
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        if (pdfFile == null) {
            return true
        }
        if (isSwipeVertical) {
            if (direction < 0 && currentXOffset < 0) {
                return true
            } else if (direction > 0 && currentXOffset + toCurrentScale(pdfFile!!.maxPageWidth) > width) {
                return true
            }
        } else {
            if (direction < 0 && currentXOffset < 0) {
                return true
            } else if (direction > 0 && currentXOffset + pdfFile!!.getDocLen(zoom) > width) {
                return true
            }
        }
        return false
    }

    override fun canScrollVertically(direction: Int): Boolean {
        if (pdfFile == null) {
            return true
        }
        if (isSwipeVertical) {
            if (direction < 0 && currentYOffset < 0) {
                return true
            } else if (direction > 0 && currentYOffset + pdfFile!!.getDocLen(zoom) > height) {
                return true
            }
        } else {
            if (direction < 0 && currentYOffset < 0) {
                return true
            } else if (direction > 0 && currentYOffset + toCurrentScale(pdfFile!!.maxPageHeight) > height) {
                return true
            }
        }
        return false
    }

    override fun onDraw(canvas: Canvas) {
        if (isInEditMode) {
            return
        }

        if (isAntialiasing) {
            canvas.drawFilter = antialiasFilter
        }

        val bg = background
        if (bg == null) {
            canvas.drawColor(if (nightMode) Color.BLACK else Color.WHITE)
        } else {
            bg.draw(canvas)
        }

        if (isRecycled) {
            return
        }
        if (state != State.SHOWN) {
            return
        }

        val currentXOffset = this.currentXOffset
        val currentYOffset = this.currentYOffset
        canvas.translate(currentXOffset, currentYOffset)

        for (part in cacheManager.getThumbnails()) {
            drawPart(canvas, part)
        }

        for (part in cacheManager.getPageParts()) {
            drawPart(canvas, part)
            if (callbacks.onDrawAll != null && !onDrawPagesNums.contains(part.page)) {
                onDrawPagesNums.add(part.page)
            }
        }

        for (page in onDrawPagesNums) {
            drawWithListener(canvas, page, callbacks.onDrawAll)
        }
        onDrawPagesNums.clear()

        drawWithListener(canvas, currentPage, callbacks.onDraw)

        canvas.translate(-currentXOffset, -currentYOffset)
    }

    private fun drawWithListener(canvas: Canvas, page: Int, listener: OnDrawListener?) {
        if (listener != null) {
            val translateX: Float
            val translateY: Float
            if (isSwipeVertical) {
                translateX = 0f
                translateY = pdfFile!!.getPageOffset(page, zoom)
            } else {
                translateY = 0f
                translateX = pdfFile!!.getPageOffset(page, zoom)
            }

            canvas.translate(translateX, translateY)
            val size = pdfFile!!.getPageSize(page)
            listener.onLayerDrawn(
                canvas,
                toCurrentScale(size.width),
                toCurrentScale(size.height),
                page
            )
            canvas.translate(-translateX, -translateY)
        }
    }

    private fun drawPart(canvas: Canvas, part: PagePart) {
        val pageRelativeBounds = part.pageRelativeBounds
        val renderedBitmap = part.renderedBitmap ?: return

        if (renderedBitmap.isRecycled) {
            return
        }

        var localTranslationX = 0f
        var localTranslationY = 0f
        val size = pdfFile!!.getPageSize(part.page)

        if (isSwipeVertical) {
            localTranslationY = pdfFile!!.getPageOffset(part.page, zoom)
            val maxWidth = pdfFile!!.maxPageWidth
            localTranslationX = toCurrentScale(maxWidth - size.width) / 2
        } else {
            localTranslationX = pdfFile!!.getPageOffset(part.page, zoom)
            val maxHeight = pdfFile!!.maxPageHeight
            localTranslationY = toCurrentScale(maxHeight - size.height) / 2
        }
        canvas.translate(localTranslationX, localTranslationY)

        val srcRect = Rect(0, 0, renderedBitmap.width, renderedBitmap.height)

        val offsetX = toCurrentScale(pageRelativeBounds.left * size.width)
        val offsetY = toCurrentScale(pageRelativeBounds.top * size.height)
        val width = toCurrentScale(pageRelativeBounds.width() * size.width)
        val height = toCurrentScale(pageRelativeBounds.height() * size.height)

        val dstRect = RectF(
            offsetX, offsetY,
            offsetX + width,
            offsetY + height
        )

        val translationX = currentXOffset + localTranslationX
        val translationY = currentYOffset + localTranslationY
        if (translationX + dstRect.left >= getWidth() || translationX + dstRect.right <= 0 ||
            translationY + dstRect.top >= getHeight() || translationY + dstRect.bottom <= 0
        ) {
            canvas.translate(-localTranslationX, -localTranslationY)
            return
        }

        canvas.drawBitmap(renderedBitmap, srcRect, dstRect, paint)

        if (Constants.DEBUG_MODE) {
            debugPaint.color = if (part.page % 2 == 0) Color.RED else Color.BLUE
            canvas.drawRect(dstRect, debugPaint)
        }

        canvas.translate(-localTranslationX, -localTranslationY)
    }

    fun loadPages() {
        if (pdfFile == null || renderingHandler == null) {
            return
        }

        renderingHandler?.removeMessages(RenderingHandler.MSG_RENDER_TASK)
        cacheManager.makeANewSet()

        pagesLoader.loadPages()
        redraw()
    }

    internal fun loadComplete(pdfFile: PdfFile) {
        state = State.LOADED
        this.pdfFile = pdfFile

        if (renderingHandlerThread?.isAlive == false) {
            renderingHandlerThread?.start()
        }
        renderingHandler = RenderingHandler(renderingHandlerThread!!.looper, this)
        renderingHandler?.start()

        if (scrollHandle != null) {
            scrollHandle?.setupLayout(this)
            isScrollHandleInit = true
        }

        dragPinchManager.enable()
        callbacks.callOnLoadComplete(pdfFile.pagesCount)
        jumpTo(defaultPage, false)
    }

    internal fun loadError(t: Throwable) {
        state = State.ERROR
        val onErrorListener = callbacks.onError
        recycle()
        invalidate()
        if (onErrorListener != null) {
            onErrorListener.onError(t)
        } else {
            Log.e("PDFView", "load pdf error", t)
        }
    }

    internal fun redraw() {
        invalidate()
    }

    fun onBitmapRendered(part: PagePart) {
        if (state == State.LOADED) {
            state = State.SHOWN
            callbacks.callOnRender(pdfFile!!.pagesCount)
        }
        if (part.isThumbnail) {
            cacheManager.cacheThumbnail(part)
        } else {
            cacheManager.cachePart(part)
        }
        redraw()
    }

    fun moveTo(offsetX: Float, offsetY: Float) {
        moveTo(offsetX, offsetY, true)
    }

    fun moveTo(offsetX: Float, offsetY: Float, moveHandle: Boolean) {
        var newOffsetX = offsetX
        var newOffsetY = offsetY

        if (isSwipeVertical) {
            val scaledPageWidth = toCurrentScale(pdfFile!!.maxPageWidth)
            if (scaledPageWidth < width) {
                newOffsetX = width / 2f - scaledPageWidth / 2f
            } else {
                if (newOffsetX > 0) {
                    newOffsetX = 0f
                } else if (newOffsetX + scaledPageWidth < width) {
                    newOffsetX = width - scaledPageWidth
                }
            }

            val contentHeight = pdfFile!!.getDocLen(zoom)
            if (contentHeight < height) {
                newOffsetY = (height - contentHeight) / 2f
            } else {
                if (newOffsetY > 0) {
                    newOffsetY = 0f
                } else if (newOffsetY + contentHeight < height) {
                    newOffsetY = -contentHeight + height
                }
            }

            scrollDir = when {
                newOffsetY < currentYOffset -> ScrollDir.END
                newOffsetY > currentYOffset -> ScrollDir.START
                else -> ScrollDir.NONE
            }
        } else {
            val scaledPageHeight = toCurrentScale(pdfFile!!.maxPageHeight)
            if (scaledPageHeight < height) {
                newOffsetY = height / 2f - scaledPageHeight / 2f
            } else {
                if (newOffsetY > 0) {
                    newOffsetY = 0f
                } else if (newOffsetY + scaledPageHeight < height) {
                    newOffsetY = height - scaledPageHeight
                }
            }

            val contentWidth = pdfFile!!.getDocLen(zoom)
            if (contentWidth < width) {
                newOffsetX = (width - contentWidth) / 2f
            } else {
                if (newOffsetX > 0) {
                    newOffsetX = 0f
                } else if (newOffsetX + contentWidth < width) {
                    newOffsetX = -contentWidth + width
                }
            }

            scrollDir = when {
                newOffsetX < currentXOffset -> ScrollDir.END
                newOffsetX > currentXOffset -> ScrollDir.START
                else -> ScrollDir.NONE
            }
        }

        currentXOffset = newOffsetX
        currentYOffset = newOffsetY
        val posOffset = positionOffset

        if (moveHandle && scrollHandle != null && !documentFitsView()) {
            scrollHandle?.setScroll(posOffset)
        }

        callbacks.callOnPageScroll(currentPage, posOffset)
        redraw()
    }

    internal fun loadPageByOffset() {
        if (0 == pdfFile!!.pagesCount) {
            return
        }

        val offset: Float
        val screenCenter: Float
        if (isSwipeVertical) {
            offset = currentYOffset
            screenCenter = height.toFloat() / 2
        } else {
            offset = currentXOffset
            screenCenter = width.toFloat() / 2
        }

        val page = pdfFile!!.getPageAtOffset(-(offset - screenCenter), zoom)
        if (page in 0 until pdfFile!!.pagesCount && page != currentPage) {
            showPage(page)
        } else {
            loadPages()
        }
    }

    fun performPageSnap() {
        if (!isPageSnap || pdfFile == null || pdfFile!!.pagesCount == 0) {
            return
        }
        val centerPage = findFocusPage(currentXOffset, currentYOffset)
        val edge = findSnapEdge(centerPage)
        if (edge == SnapEdge.NONE) {
            return
        }

        val offset = snapOffsetForPage(centerPage, edge)
        if (isSwipeVertical) {
            animationManager.startYAnimation(currentYOffset, -offset)
        } else {
            animationManager.startXAnimation(currentXOffset, -offset)
        }
    }

    internal fun findSnapEdge(page: Int): SnapEdge {
        if (!isPageSnap || page < 0) {
            return SnapEdge.NONE
        }
        val currentOffset = if (isSwipeVertical) currentYOffset else currentXOffset
        val offset = -pdfFile!!.getPageOffset(page, zoom)
        val length = if (isSwipeVertical) height else width
        val pageLength = pdfFile!!.getPageLength(page, zoom)

        return when {
            length >= pageLength -> SnapEdge.CENTER
            currentOffset >= offset -> SnapEdge.START
            offset - pageLength > currentOffset - length -> SnapEdge.END
            else -> SnapEdge.NONE
        }
    }

    internal fun snapOffsetForPage(pageIndex: Int, edge: SnapEdge): Float {
        var offset = pdfFile!!.getPageOffset(pageIndex, zoom)
        val length = if (isSwipeVertical) height else width
        val pageLength = pdfFile!!.getPageLength(pageIndex, zoom)

        if (edge == SnapEdge.CENTER) {
            offset = offset - length / 2f + pageLength / 2f
        } else if (edge == SnapEdge.END) {
            offset = offset - length + pageLength
        }
        return offset
    }

    internal fun findFocusPage(xOffset: Float, yOffset: Float): Int {
        val currOffset = if (isSwipeVertical) yOffset else xOffset
        val length = if (isSwipeVertical) height else width
        if (currOffset > -1) {
            return 0
        } else if (currOffset < -pdfFile!!.getDocLen(zoom) + length + 1) {
            return pdfFile!!.pagesCount - 1
        }
        val center = currOffset - length / 2f
        return pdfFile!!.getPageAtOffset(-center, zoom)
    }

    fun pageFillsScreen(): Boolean {
        val start = -pdfFile!!.getPageOffset(currentPage, zoom)
        val end = start - pdfFile!!.getPageLength(currentPage, zoom)
        return if (isSwipeVertical) {
            start > currentYOffset && end < currentYOffset - height
        } else {
            start > currentXOffset && end < currentXOffset - width
        }
    }

    fun moveRelativeTo(dx: Float, dy: Float) {
        moveTo(currentXOffset + dx, currentYOffset + dy)
    }

    fun zoomTo(zoom: Float) {
        this.zoom = zoom
    }

    fun zoomCenteredTo(zoom: Float, pivot: PointF) {
        val dzoom = zoom / this.zoom
        zoomTo(zoom)
        var baseX = currentXOffset * dzoom
        var baseY = currentYOffset * dzoom
        baseX += (pivot.x - pivot.x * dzoom)
        baseY += (pivot.y - pivot.y * dzoom)
        moveTo(baseX, baseY)
    }

    fun zoomCenteredRelativeTo(dzoom: Float, pivot: PointF) {
        zoomCenteredTo(zoom * dzoom, pivot)
    }

    fun documentFitsView(): Boolean {
        val len = pdfFile!!.getDocLen(1f)
        return if (isSwipeVertical) {
            len < height
        } else {
            len < width
        }
    }

    fun fitToWidth(page: Int) {
        if (state != State.SHOWN) {
            Log.e(TAG, "Cannot fit, document not rendered yet")
            return
        }
        zoomTo(width / pdfFile!!.getPageSize(page).width)
        jumpTo(page)
    }

    fun getPageSize(pageIndex: Int): SizeF {
        if (pdfFile == null) {
            return SizeF(0f, 0f)
        }
        return pdfFile!!.getPageSize(pageIndex)
    }

    fun toRealScale(size: Float): Float {
        return size / zoom
    }

    fun toCurrentScale(size: Float): Float {
        return size * zoom
    }

    val isZooming: Boolean
        get() = zoom != minZoom

    private fun setDefaultPage(defaultPage: Int) {
        this.defaultPage = defaultPage
    }

    fun resetZoom() {
        zoomTo(minZoom)
    }

    fun resetZoomWithAnimation() {
        zoomWithAnimation(minZoom)
    }

    fun zoomWithAnimation(centerX: Float, centerY: Float, scale: Float) {
        animationManager.startZoomAnimation(centerX, centerY, zoom, scale)
    }

    fun zoomWithAnimation(scale: Float) {
        animationManager.startZoomAnimation(width / 2f, height / 2f, zoom, scale)
    }

    private fun setScrollHandle(scrollHandle: ScrollHandle?) {
        this.scrollHandle = scrollHandle
    }

    fun getPageAtPositionOffset(positionOffset: Float): Int {
        return pdfFile!!.getPageAtOffset(pdfFile!!.getDocLen(zoom) * positionOffset, zoom)
    }

    private fun setSwipeVertical(swipeVertical: Boolean) {
        this.isSwipeVertical = swipeVertical
    }

    fun enableAnnotationRendering(annotationRendering: Boolean) {
        this.isAnnotationRendering = annotationRendering
    }

    fun enableRenderDuringScale(renderDuringScale: Boolean) {
        this.renderDuringScale = renderDuringScale
    }

    fun enableAntialiasing(enableAntialiasing: Boolean) {
        this.isAntialiasing = enableAntialiasing
    }

    private fun setSpacing(spacingDp: Int) {
        this.spacingPx = Util.getDP(context, spacingDp)
    }

    private fun setAutoSpacing(autoSpacing: Boolean) {
        this.isAutoSpacingEnabled = autoSpacing
    }

    private fun setPageFitPolicy(pageFitPolicy: FitPolicy) {
        this.pageFitPolicy = pageFitPolicy
    }

    private fun setFitEachPage(fitEachPage: Boolean) {
        this.isFitEachPage = fitEachPage
    }

    fun doRenderDuringScale(): Boolean {
        return renderDuringScale
    }

    val documentMeta: PdfDocument.Meta?
        get() {
            if (pdfFile == null) {
                return null
            }
            return pdfFile!!.metaData
        }

    val tableOfContents: List<PdfDocument.Bookmark>
        get() {
            if (pdfFile == null) {
                return emptyList()
            }
            return pdfFile!!.bookmarks
        }

    fun getLinks(page: Int): List<PdfDocument.Link> {
        if (pdfFile == null) {
            return emptyList()
        }
        return pdfFile!!.getPageLinks(page)
    }

    fun fromAsset(assetName: String): Configurator {
        return Configurator(AssetSource(assetName))
    }

    fun fromFile(file: File): Configurator {
        return Configurator(FileSource(file))
    }

    fun fromUri(uri: Uri): Configurator {
        return Configurator(UriSource(uri))
    }

    fun fromBytes(bytes: ByteArray): Configurator {
        return Configurator(ByteArraySource(bytes))
    }

    fun fromStream(stream: InputStream): Configurator {
        return Configurator(InputStreamSource(stream))
    }

    fun fromSource(docSource: DocumentSource): Configurator {
        return Configurator(docSource)
    }

    private enum class State {
        DEFAULT, LOADED, SHOWN, ERROR
    }

    inner class Configurator constructor(private val documentSource: DocumentSource) {
        private var pageNumbers: IntArray? = null
        private var enableSwipe = true
        private var enableDoubletap = true
        private var onDrawListener: OnDrawListener? = null
        private var onDrawAllListener: OnDrawListener? = null
        private var onLoadCompleteListener: OnLoadCompleteListener? = null
        private var onErrorListener: OnErrorListener? = null
        private var onPageChangeListener: OnPageChangeListener? = null
        private var onPageScrollListener: OnPageScrollListener? = null
        private var onRenderListener: OnRenderListener? = null
        private var onTapListener: OnTapListener? = null
        private var onLongPressListener: OnLongPressListener? = null
        private var onPageErrorListener: OnPageErrorListener? = null
        private var linkHandler: LinkHandler = DefaultLinkHandler(this@PDFView)
        private var defaultPage = 0
        private var swipeHorizontal = false
        private var annotationRendering = false
        private var password: String? = null
        private var scrollHandle: ScrollHandle? = null
        private var antialiasing = true
        private var spacing = 0
        private var autoSpacing = false
        private var pageFitPolicy = FitPolicy.WIDTH
        private var fitEachPage = false
        private var pageFling = false
        private var pageSnap = false
        private var nightMode = false

        fun pages(vararg pageNumbers: Int): Configurator {
            this.pageNumbers = pageNumbers
            return this
        }

        fun enableSwipe(enableSwipe: Boolean): Configurator {
            this.enableSwipe = enableSwipe
            return this
        }

        fun enableDoubletap(enableDoubletap: Boolean): Configurator {
            this.enableDoubletap = enableDoubletap
            return this
        }

        fun enableAnnotationRendering(annotationRendering: Boolean): Configurator {
            this.annotationRendering = annotationRendering
            return this
        }

        fun onDraw(onDrawListener: OnDrawListener): Configurator {
            this.onDrawListener = onDrawListener
            return this
        }

        fun onDrawAll(onDrawAllListener: OnDrawListener): Configurator {
            this.onDrawAllListener = onDrawAllListener
            return this
        }

        fun onLoad(onLoadCompleteListener: OnLoadCompleteListener): Configurator {
            this.onLoadCompleteListener = onLoadCompleteListener
            return this
        }

        fun onPageScroll(onPageScrollListener: OnPageScrollListener): Configurator {
            this.onPageScrollListener = onPageScrollListener
            return this
        }

        fun onError(onErrorListener: OnErrorListener): Configurator {
            this.onErrorListener = onErrorListener
            return this
        }

        fun onPageError(onPageErrorListener: OnPageErrorListener): Configurator {
            this.onPageErrorListener = onPageErrorListener
            return this
        }

        fun onPageChange(onPageChangeListener: OnPageChangeListener): Configurator {
            this.onPageChangeListener = onPageChangeListener
            return this
        }

        fun onRender(onRenderListener: OnRenderListener): Configurator {
            this.onRenderListener = onRenderListener
            return this
        }

        fun onTap(onTapListener: OnTapListener): Configurator {
            this.onTapListener = onTapListener
            return this
        }

        fun onLongPress(onLongPressListener: OnLongPressListener): Configurator {
            this.onLongPressListener = onLongPressListener
            return this
        }

        fun linkHandler(linkHandler: LinkHandler): Configurator {
            this.linkHandler = linkHandler
            return this
        }

        fun defaultPage(defaultPage: Int): Configurator {
            this.defaultPage = defaultPage
            return this
        }

        fun swipeHorizontal(swipeHorizontal: Boolean): Configurator {
            this.swipeHorizontal = swipeHorizontal
            return this
        }

        fun password(password: String?): Configurator {
            this.password = password
            return this
        }

        fun scrollHandle(scrollHandle: ScrollHandle?): Configurator {
            this.scrollHandle = scrollHandle
            return this
        }

        fun enableAntialiasing(antialiasing: Boolean): Configurator {
            this.antialiasing = antialiasing
            return this
        }

        fun spacing(spacing: Int): Configurator {
            this.spacing = spacing
            return this
        }

        fun autoSpacing(autoSpacing: Boolean): Configurator {
            this.autoSpacing = autoSpacing
            return this
        }

        fun pageFitPolicy(pageFitPolicy: FitPolicy): Configurator {
            this.pageFitPolicy = pageFitPolicy
            return this
        }

        fun fitEachPage(fitEachPage: Boolean): Configurator {
            this.fitEachPage = fitEachPage
            return this
        }

        fun pageSnap(pageSnap: Boolean): Configurator {
            this.pageSnap = pageSnap
            return this
        }

        fun pageFling(pageFling: Boolean): Configurator {
            this.pageFling = pageFling
            return this
        }

        fun nightMode(nightMode: Boolean): Configurator {
            this.nightMode = nightMode
            return this
        }

        fun disableLongpress(): Configurator {
            this@PDFView.dragPinchManager.disableLongpress()
            return this
        }

        fun load() {
            if (!hasSize) {
                waitingDocumentConfigurator = this
                return
            }
            this@PDFView.recycle()
            this@PDFView.callbacks.onLoadComplete = onLoadCompleteListener
            this@PDFView.callbacks.onError = onErrorListener
            this@PDFView.callbacks.onDraw = onDrawListener
            this@PDFView.callbacks.onDrawAll = onDrawAllListener
            this@PDFView.callbacks.onPageChange = onPageChangeListener
            this@PDFView.callbacks.onPageScroll = onPageScrollListener
            this@PDFView.callbacks.onRender = onRenderListener
            this@PDFView.callbacks.onTap = onTapListener
            this@PDFView.callbacks.onLongPress = onLongPressListener
            this@PDFView.callbacks.onPageError = onPageErrorListener
            this@PDFView.callbacks.linkHandler = linkHandler
            this@PDFView.isSwipeEnabled = enableSwipe
            this@PDFView.setNightMode(nightMode)
            this@PDFView.isDoubletapEnabled = enableDoubletap
            this@PDFView.setDefaultPage(defaultPage)
            this@PDFView.setSwipeVertical(!swipeHorizontal)
            this@PDFView.enableAnnotationRendering(annotationRendering)
            this@PDFView.setScrollHandle(scrollHandle)
            this@PDFView.enableAntialiasing(antialiasing)
            this@PDFView.setSpacing(spacing)
            this@PDFView.setAutoSpacing(autoSpacing)
            this@PDFView.setPageFitPolicy(pageFitPolicy)
            this@PDFView.setFitEachPage(fitEachPage)
            this@PDFView.isPageSnap = pageSnap
            this@PDFView.isPageFlingEnabled = pageFling

            if (pageNumbers != null) {
                this@PDFView.load(documentSource, password, pageNumbers)
            } else {
                this@PDFView.load(documentSource, password)
            }
        }
    }
}