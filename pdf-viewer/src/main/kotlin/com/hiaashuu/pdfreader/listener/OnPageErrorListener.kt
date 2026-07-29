package com.hiaashuu.pdfreader.listener

interface OnPageErrorListener {
    fun onPageError(page: Int, t: Throwable)
}