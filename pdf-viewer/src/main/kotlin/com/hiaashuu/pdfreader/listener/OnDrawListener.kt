package com.hiaashuu.pdfreader.listener

import android.graphics.Canvas

interface OnDrawListener {
    fun onLayerDrawn(canvas: Canvas, pageWidth: Float, pageHeight: Float, displayedPage: Int)
}