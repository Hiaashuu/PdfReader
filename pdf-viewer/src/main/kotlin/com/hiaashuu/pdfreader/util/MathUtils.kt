package com.hiaashuu.pdfreader.util

object MathUtils {

    private const val BIG_ENOUGH_INT = 16 * 1024
    private const val BIG_ENOUGH_FLOOR = BIG_ENOUGH_INT.toDouble()
    private const val BIG_ENOUGH_CEIL = 16384.999999999996

    @JvmStatic
    fun limit(number: Int, between: Int, and: Int): Int {
        if (number <= between) {
            return between
        }
        if (number >= and) {
            return and
        }
        return number
    }

    @JvmStatic
    fun limit(number: Float, between: Float, and: Float): Float {
        if (number <= between) {
            return between
        }
        if (number >= and) {
            return and
        }
        return number
    }

    @JvmStatic
    fun max(number: Float, max: Float): Float {
        if (number > max) {
            return max
        }
        return number
    }

    @JvmStatic
    fun min(number: Float, min: Float): Float {
        if (number < min) {
            return min
        }
        return number
    }

    @JvmStatic
    fun max(number: Int, max: Int): Int {
        if (number > max) {
            return max
        }
        return number
    }

    @JvmStatic
    fun min(number: Int, min: Int): Int {
        if (number < min) {
            return min
        }
        return number
    }

    @JvmStatic
    fun floor(value: Float): Int {
        return (value + BIG_ENOUGH_FLOOR).toInt() - BIG_ENOUGH_INT
    }

    @JvmStatic
    fun ceil(value: Float): Int {
        return (value + BIG_ENOUGH_CEIL).toInt() - BIG_ENOUGH_INT
    }
}