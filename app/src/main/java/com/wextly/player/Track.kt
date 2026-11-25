package com.wextly.player

/** Один трек из библиотеки. uri - адрес файла, который выдал системный выбор файлов. */
data class Track(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val artPath: String?,
)
