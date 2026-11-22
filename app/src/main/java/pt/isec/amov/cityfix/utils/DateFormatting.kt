package pt.isec.amov.cityfix.utils

import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun formatDate(epochMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))

fun formatDateTime(epochMillis: Long, locale: Locale): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(epochMillis))
