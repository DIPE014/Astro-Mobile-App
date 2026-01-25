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
 * Compute the number of Julian centuries elapsed since J2000.0 for the given date.
 *
 * @param date The moment to evaluate (interpreted in UTC).
 * @return Julian centuries since J2000.0 (Julian Day 2451545.0).
 */
fun julianCenturies(date: Date): Double {
    val jd = julianDay(date)
    val delta = jd - 2451545.0
    return delta / 36525.0
}

/**
 * Compute the Julian Day for the given Date interpreted in UTC.
 *
 * The input Date's year, month, day and time components are read in GMT/UTC.
 * Valid for dates in the year range 1900–2099.
 *
 * @param date The date and time to convert (interpreted in UTC).
 * @return The Julian Day corresponding to the provided UTC date and time.
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
 * Convert an astronomical Julian Day to a Gregorian calendar Date in UTC.
 *
 * The input `julianDay` is the astronomical Julian Day (days start at noon); the function returns the corresponding UTC calendar date and time.
 *
 * @param julianDay The Julian Day number to convert (astronomical convention).
 * @return A `Date` representing the Gregorian date and time in UTC that corresponds to `julianDay`.
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
 * Computes the local mean sidereal time for the given UTC date and longitude.
 *
 * @param date The date/time interpreted in UTC.
 * @param longitude Geographic longitude in degrees; negative values denote west.
 * @return Local mean sidereal time in degrees, normalized to [0, 360).
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
 * Normalizes an angle to the range [0, 360).
 *
 * @param angleDegrees Angle in degrees; may be any real value.
 * @return The equivalent angle in degrees within [0, 360).
 */
private fun normalizeAngle(angleDegrees: Double): Double {
    return positiveMod(angleDegrees, 360.0)
}

/**
 * Normalize a time value in hours to the range [0, 24).
 *
 * @return The equivalent time constrained to be greater than or equal to 0 and less than 24.
 */
fun normalizeHours(time: Double): Double {
    return positiveMod(time, 24.0)
}

/**
 * Convert a universal time in hours to an array of hours, minutes, and seconds.
 *
 * @param universalTime Universal time in hours (typically in the range 0–24).
 * @return An IntArray of length 3: [hours, minutes, seconds], where
 *         - hours = floor(universalTime) (0–23),
 *         - minutes = floor((universalTime - hours) * 60) (0–59),
 *         - seconds = floor(((universalTime - hours) * 60) - minutes) (0–59).
 */
fun clockTimeFromHrs(universalTime: Double): IntArray {
    val hms = IntArray(3)
    hms[0] = floor(universalTime).toInt()
    val remainderMins = MINUTES_PER_HOUR * (universalTime - hms[0])
    hms[1] = floor(remainderMins).toInt()
    hms[2] = floor(remainderMins - hms[1]).toInt()
    return hms
}