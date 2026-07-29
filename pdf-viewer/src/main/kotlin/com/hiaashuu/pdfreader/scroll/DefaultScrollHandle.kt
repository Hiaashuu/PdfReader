package com.hiaashuu.pdfreader.scroll

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hiaashuu.pdfreader.PDFView
import com.hiaashuu.pdfreader.util.Util

class DefaultScrollHandle @JvmOverloads constructor(
    context: Context,
    private val inverted: Boolean = false
) : RelativeLayout(context), ScrollHandle {

    companion object {
        private const val HANDLE_LONG = 65
        private const val HANDLE_SHORT = 40
        private const val DEFAULT_TEXT_SIZE = 16
    }

    private var relativeHandlerMiddle = 0f
    protected var textView: TextView = TextView(context)
    private var pdfView: PDFView? = null
    private var currentPos = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val hidePageScrollerRunnable = Runnable { hide() }

    init {
        visibility = INVISIBLE
        setTextColor(Color.BLACK)
        setTextSize(DEFAULT_TEXT_SIZE)
    }

    override fun setupLayout(pdfView: PDFView) {
        val align: Int
        val width: Int
        val height: Int
        // NOTE: If you need backgrounds for the scroll handle, you should create simple XML shapes 
        // in your drawable folder named default_scroll_handle_left/right/top/bottom.
        // I have removed the direct resource lookup here to prevent build failures if you haven't created them yet.
        val background: Drawable? = null 
        
        if (pdfView.isSwipeVertical) {
            width = HANDLE_LONG
            height = HANDLE_SHORT
            align = if (inverted) ALIGN_PARENT_LEFT else ALIGN_PARENT_RIGHT
        } else {
            width = HANDLE_SHORT
            height = HANDLE_LONG
            align = if (inverted) ALIGN_PARENT_TOP else ALIGN_PARENT_BOTTOM
        }

        if (background != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                setBackgroundDrawable(background)
            } else {
                setBackground(background)
            }
        } else {
            setBackgroundColor(Color.LTGRAY)
        }

        val lp = LayoutParams(Util.getDP(context, width), Util.getDP(context, height))
        lp.setMargins(0, 0, 0, 0)

        val tvlp = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        tvlp.addRule(CENTER_IN_PARENT, TRUE)

        addView(textView, tvlp)

        lp.addRule(align)
        pdfView.addView(this, lp)

        this.pdfView = pdfView
    }

    override fun destroyLayout() {
        pdfView?.removeView(this)
    }

    override fun setScroll(position: Float) {
        if (!shown()) {
            show()
        } else {
            handler.removeCallbacks(hidePageScrollerRunnable)
        }
        pdfView?.let {
            setPosition((if (it.isSwipeVertical) it.height.toFloat() else it.width.toFloat()) * position)
        }
    }

    private fun setPosition(pos: Float) {
        var mutablePos = pos
        if (mutablePos.isInfinite() || mutablePos.isNaN()) {
            return
        }
        val pdfViewSize = if (pdfView!!.isSwipeVertical) {
            pdfView!!.height.toFloat()
        } else {
            pdfView!!.width.toFloat()
        }
        mutablePos -= relativeHandlerMiddle

        if (mutablePos < 0) {
            mutablePos = 0f
        } else if (mutablePos > pdfViewSize - Util.getDP(context, HANDLE_SHORT)) {
            mutablePos = pdfViewSize - Util.getDP(context, HANDLE_SHORT)
        }

        if (pdfView!!.isSwipeVertical) {
            y = mutablePos
        } else {
            x = mutablePos
        }

        calculateMiddle()
        invalidate()
    }

    private fun calculateMiddle() {
        val pos: Float
        val viewSize: Float
        val pdfViewSize: Float
        if (pdfView!!.isSwipeVertical) {
            pos = y
            viewSize = height.toFloat()
            pdfViewSize = pdfView!!.height.toFloat()
        } else {
            pos = x
            viewSize = width.toFloat()
            pdfViewSize = pdfView!!.width.toFloat()
        }
        relativeHandlerMiddle = ((pos + relativeHandlerMiddle) / pdfViewSize) * viewSize
    }

    override fun hideDelayed() {
        handler.postDelayed(hidePageScrollerRunnable, 1000)
    }

    override fun setPageNum(pageNum: Int) {
        val text = pageNum.toString()
        if (textView.text != text) {
            textView.text = text
        }
    }

    override fun shown(): Boolean {
        return visibility == VISIBLE
    }

    override fun show() {
        visibility = VISIBLE
    }

    override fun hide() {
        visibility = INVISIBLE
    }

    fun setTextColor(color: Int) {
        textView.setTextColor(color)
    }

    fun setTextSize(size: Int) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size.toFloat())
    }

    private fun isPDFViewReady(): Boolean {
        return pdfView != null && pdfView!!.pageCount > 0 && !pdfView!!.documentFitsView()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isPDFViewReady()) {
            return super.onTouchEvent(event)
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                pdfView?.stopFling()
                handler.removeCallbacks(hidePageScrollerRunnable)
                currentPos = if (pdfView!!.isSwipeVertical) {
                    event.rawY - y
                } else {
                    event.rawX - x
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pdfView!!.isSwipeVertical) {
                    setPosition(event.rawY - currentPos + relativeHandlerMiddle)
                    pdfView?.setPositionOffset(relativeHandlerMiddle / height.toFloat(), false)
                } else {
                    setPosition(event.rawX - currentPos + relativeHandlerMiddle)
                    pdfView?.setPositionOffset(relativeHandlerMiddle / width.toFloat(), false)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                hideDelayed()
                pdfView?.performPageSnap()
                return true
            }
        }

        return super.onTouchEvent(event)
    }
}