package com.hiaashuu.pdfreader.scroll

import com.hiaashuu.pdfreader.PDFView

interface ScrollHandle {
    fun setScroll(position: Float)
    fun setupLayout(pdfView: PDFView)
    fun destroyLayout()
    fun setPageNum(pageNum: Int)
    fun shown(): Boolean
    fun show()
    fun hide()
    fun hideDelayed()
}