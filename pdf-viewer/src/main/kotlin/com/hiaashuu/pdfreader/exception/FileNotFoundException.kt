package com.hiaashuu.pdfreader.exception

@Deprecated("This exception is deprecated")
class FileNotFoundException : RuntimeException {
    constructor(detailMessage: String) : super(detailMessage)
    constructor(detailMessage: String, throwable: Throwable) : super(detailMessage, throwable)
}