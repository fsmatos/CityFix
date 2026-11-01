package pt.isec.amov.cityfix.utils

import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Formats [epochMillis] as a medium date in the device locale (java.text works on minSdk 24). */
fun formatDate(epochMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))

/** Formats [epochMillis] as a medium date and short time in [locale], device time zone (java.text works on minSdk 24). */
fun formatDateTime(epochMillis: Long, locale: Locale): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(epochMillis))
