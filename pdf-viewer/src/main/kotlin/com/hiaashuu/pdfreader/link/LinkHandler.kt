package com.hiaashuu.pdfreader.link

import com.hiaashuu.pdfreader.model.LinkTapEvent

interface LinkHandler {
    fun handleLinkEvent(event: LinkTapEvent)
}