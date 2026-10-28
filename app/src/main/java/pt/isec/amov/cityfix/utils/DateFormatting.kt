package pt.isec.amov.cityfix.utils

import java.text.DateFormat
import java.util.Date

/** Formats [epochMillis] as a medium date in the device locale (java.text works on minSdk 24). */
fun formatDate(epochMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))
