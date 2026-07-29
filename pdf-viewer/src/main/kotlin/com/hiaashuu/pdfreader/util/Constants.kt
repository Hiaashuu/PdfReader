package com.hiaashuu.pdfreader.util

object Constants {

    @JvmField
    var DEBUG_MODE = false

    @JvmField
    var THUMBNAIL_RATIO = 0.3f

    @JvmField
    var PART_SIZE = 256f

    @JvmField
    var PRELOAD_OFFSET = 20

    object Cache {
        @JvmField
        var CACHE_SIZE = 120

        @JvmField
        var THUMBNAILS_CACHE_SIZE = 8
    }

    object Pinch {
        @JvmField
        var MAXIMUM_ZOOM = 10f

        @JvmField
        var MINIMUM_ZOOM = 1f
    }
}