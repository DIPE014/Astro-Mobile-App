// Copyright 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.astro.app.core.math

import java.util.*
import kotlin.math.floor

public const val MINUTES_PER_HOUR = 60.0

public const val SECONDS_PER_HOUR = 3600.0

// Convert from hours to degrees
public const val HOURS_TO_DEGREES = 360.0f / 24.0f

/**
 * Utilities for working with Dates and times.
 *
 * @author Kevin Serafini
 * @author Brent Bryan
 */
/**
 * Calculates the number of Julian Centuries from the epoch 2000.0
 * (equivalent to Julian Day 2451545.0).
 */
fun julianCenturies(date: Date): Double {
    val jd = julianDay(date)
    val delta = jd - 2451545.0
    return delta / 36525.0
}

/**
 * Calculates the Julian Day for a given date using the following formula:
 * JD = 367 * Y - INT(7 * (Y + INT((M + 9)/12))/4) + INT(275 * M / 9)
 * + D + 1721013.5 + UT/24
 *
 * Note that this is only valid for the year range 1900 - 2099.
 */
fun julianDay(date: Date): Double {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
    cal.time = date
    val hour = (cal[Calendar.HOUR_OF_DAY]
            + cal[Calendar.MINUTE] / MINUTES_PER_HOUR + cal[Calendar.SECOND] / SECONDS_PER_HOUR)
    val year = cal[Calendar.YEAR]
    val month = cal[Calendar.MONTH] + 1
    val day = cal[Calendar.DAY_OF_MONTH]
    return (367.0 * year - floor(
        7.0 * (year
                + floor((month + 9.0) / 12.0)) / 4.0
    ) + floor(275.0 * month / 9.0) + day
            + 1721013.5 + hour / 24.0)
}

/**
 * Converts the given Julian Day to Gregorian Date (in UT time zone).
 * Uses the algorithm from Jean Meeus "Astronomical Algorithms" (2nd ed.)
 */
fun gregorianDate(julianDay: Double): Date {
    // Add 0.5 to convert from noon-based JD to midnight-based
    val jdPlusHalf = julianDay + 0.5
    val z = floor(jdPlusHalf).toInt()
    val f = jdPlusHalf - z

    val a: Int
    if (z < 2299161) {
        a = z
    } else {
        val alpha = ((z - 1867216.25) / 36524.25).toInt()
        a = z + 1 + alpha - alpha / 4
    }

    val b = a + 1524
    val c = ((b - 122.1) / 365.25).toInt()
    val d = (365.25 * c).toInt()
    val e = ((b - d) / 30.6001).toInt()

    val dayOfMonth = b - d - (30.6001 * e).toInt()
    val month = if (e < 14) e - 1 else e - 13
    val year = if (month > 2) c - 4716 else c - 4715

    // Convert fractional day to hours, minutes, seconds
    val dHours = f * 24.0
    val hours = dHours.toInt()
    val dMinutes = (dHours - hours) * 60.0
    val minutes = dMinutes.toInt()
    val seconds = ((dMinutes - minutes) * 60.0).toInt()

    val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
    cal[year, month - 1, dayOfMonth, hours, minutes] = seconds
    cal[Calendar.MILLISECOND] = 0
    return cal.time
}

/**
 * Calculates local mean sidereal time in degrees. Note that longitude is
 * negative for western longitude values.
 */
fun meanSiderealTime(date: Date, longitude: Float): Float {
    // First, calculate number of Julian days since J2000.0.
    val jd = julianDay(date)
    val delta = jd - 2451545.0f

    // Calculate the global and local sidereal times
    val gst = 280.461f + 360.98564737f * delta
    val lst = normalizeAngle(gst + longitude)
    return lst.toFloat()
}

/**
 * Normalizes the angle to the range 0 <= value < 360.
 */
private fun normalizeAngle(angleDegrees: Double): Double {
    return positiveMod(angleDegrees, 360.0)
}

/**
 * Normalizes the time to the range 0 <= value < 24.
 */
fun normalizeHours(time: Double): Double {
    return positiveMod(time, 24.0)
}

/**
 * Take a universal time between 0 and 24 and return a triple
 * [hours, minutes, seconds].
 *
 * @param universalTime Universal time - presumed to be between 0 and 24.
 * @return [hours, minutes, seconds]
 */
fun clockTimeFromHrs(universalTime: Double): IntArray {
    val hms = IntArray(3)
    hms[0] = floor(universalTime).toInt()
    val remainderMins = MINUTES_PER_HOUR * (universalTime - hms[0])
    hms[1] = floor(remainderMins).toInt()
    hms[2] = floor(remainderMins - hms[1]).toInt()
    return hms
}
