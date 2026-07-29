package com.hiaashuu.pdfreader.listener

import android.view.MotionEvent
import com.hiaashuu.pdfreader.link.LinkHandler
import com.hiaashuu.pdfreader.model.LinkTapEvent

internal class Callbacks {
    var onLoadComplete: OnLoadCompleteListener? = null
    var onError: OnErrorListener? = null
    var onPageError: OnPageErrorListener? = null
    var onRender: OnRenderListener? = null
    var onPageChange: OnPageChangeListener? = null
    var onPageScroll: OnPageScrollListener? = null
    var onDraw: OnDrawListener? = null
    var onDrawAll: OnDrawListener? = null
    var onTap: OnTapListener? = null
    var onLongPress: OnLongPressListener? = null
    var linkHandler: LinkHandler? = null

    fun callOnLoadComplete(pagesCount: Int) {
        onLoadComplete?.loadComplete(pagesCount)
    }

    fun callOnPageError(page: Int, error: Throwable): Boolean {
        if (onPageError != null) {
            onPageError?.onPageError(page, error)
            return true
        }
        return false
    }

    fun callOnRender(pagesCount: Int) {
        onRender?.onInitiallyRendered(pagesCount)
    }

    fun callOnPageChange(page: Int, pagesCount: Int) {
        onPageChange?.onPageChanged(page, pagesCount)
    }

    fun callOnPageScroll(currentPage: Int, offset: Float) {
        onPageScroll?.onPageScrolled(currentPage, offset)
    }

    fun callOnTap(event: MotionEvent): Boolean {
        return onTap?.onTap(event) == true
    }

    fun callOnLongPress(event: MotionEvent) {
        onLongPress?.onLongPress(event)
    }

    fun callLinkHandler(event: LinkTapEvent) {
        linkHandler?.handleLinkEvent(event)
    }
}