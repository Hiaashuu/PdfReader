package com.hiaashuu.pdfreader

import android.graphics.RectF
import com.hiaashuu.pdfreader.model.PagePart
import com.hiaashuu.pdfreader.util.Constants.Cache.CACHE_SIZE
import com.hiaashuu.pdfreader.util.Constants.Cache.THUMBNAILS_CACHE_SIZE
import java.util.ArrayList
import java.util.PriorityQueue

internal class CacheManager {

    private val orderComparator = PagePartComparator()
    private val activeCache: PriorityQueue<PagePart> = PriorityQueue(CACHE_SIZE, orderComparator)
    private val passiveCache: PriorityQueue<PagePart> = PriorityQueue(CACHE_SIZE, orderComparator)
    private val thumbnails: MutableList<PagePart> = ArrayList()
    private val passiveActiveLock = Any()

    fun cachePart(part: PagePart) {
        synchronized(passiveActiveLock) {
            makeAFreeSpace()
            activeCache.offer(part)
        }
    }

    fun makeANewSet() {
        synchronized(passiveActiveLock) {
            passiveCache.addAll(activeCache)
            activeCache.clear()
        }
    }

    private fun makeAFreeSpace() {
        synchronized(passiveActiveLock) {
            while (activeCache.size + passiveCache.size >= CACHE_SIZE && passiveCache.isNotEmpty()) {
                val part = passiveCache.poll()
                part?.renderedBitmap?.recycle()
            }

            while (activeCache.size + passiveCache.size >= CACHE_SIZE && activeCache.isNotEmpty()) {
                activeCache.poll()?.renderedBitmap?.recycle()
            }
        }
    }

    fun cacheThumbnail(part: PagePart) {
        synchronized(thumbnails) {
            while (thumbnails.size >= THUMBNAILS_CACHE_SIZE) {
                thumbnails.removeAt(0).renderedBitmap?.recycle()
            }
            addWithoutDuplicates(thumbnails, part)
        }
    }

    fun upPartIfContained(page: Int, pageRelativeBounds: RectF, toOrder: Int): Boolean {
        val fakePart = PagePart(page, null, pageRelativeBounds, false, 0)

        synchronized(passiveActiveLock) {
            val found = find(passiveCache, fakePart)
            if (found != null) {
                passiveCache.remove(found)
                found.cacheOrder = toOrder
                activeCache.offer(found)
                return true
            }

            return find(activeCache, fakePart) != null
        }
    }

    fun containsThumbnail(page: Int, pageRelativeBounds: RectF): Boolean {
        val fakePart = PagePart(page, null, pageRelativeBounds, true, 0)
        synchronized(thumbnails) {
            for (part in thumbnails) {
                if (part == fakePart) {
                    return true
                }
            }
            return false
        }
    }

    private fun addWithoutDuplicates(collection: MutableCollection<PagePart>, newPart: PagePart) {
        for (part in collection) {
            if (part == newPart) {
                newPart.renderedBitmap?.recycle()
                return
            }
        }
        collection.add(newPart)
    }

    private fun find(vector: PriorityQueue<PagePart>, fakePart: PagePart): PagePart? {
        for (part in vector) {
            if (part == fakePart) {
                return part
            }
        }
        return null
    }

    fun getPageParts(): List<PagePart> {
        synchronized(passiveActiveLock) {
            val parts: MutableList<PagePart> = ArrayList(passiveCache)
            parts.addAll(activeCache)
            return parts
        }
    }

    fun getThumbnails(): List<PagePart> {
        synchronized(thumbnails) {
            return thumbnails
        }
    }

    fun recycle() {
        synchronized(passiveActiveLock) {
            for (part in passiveCache) {
                part.renderedBitmap?.recycle()
            }
            passiveCache.clear()
            for (part in activeCache) {
                part.renderedBitmap?.recycle()
            }
            activeCache.clear()
        }
        synchronized(thumbnails) {
            for (part in thumbnails) {
                part.renderedBitmap?.recycle()
            }
            thumbnails.clear()
        }
    }

    internal inner class PagePartComparator : Comparator<PagePart> {
        override fun compare(part1: PagePart, part2: PagePart): Int {
            if (part1.cacheOrder == part2.cacheOrder) {
                return 0
            }
            return if (part1.cacheOrder > part2.cacheOrder) 1 else -1
        }
    }
}