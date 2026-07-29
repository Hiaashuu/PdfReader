package com.hiaashuu.pdfreader.exception

class PageRenderingException(val page: Int, cause: Throwable) : Exception(cause)