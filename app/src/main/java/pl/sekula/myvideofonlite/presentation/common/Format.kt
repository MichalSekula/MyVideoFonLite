package pl.sekula.myvideofonlite.presentation.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun formatDateTime(millis: Long): String =
    dateTimeFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

fun formatTime(millis: Long): String =
    timeFormatter.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
