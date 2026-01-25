package com.astro.app.core.control.space

import java.util.*

import com.astro.app.core.util.TimeConstants
import com.astro.app.core.math.*
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin


/**
 * Base class for any celestial objects.
 */
abstract class CelestialObject {
    /**
 * Compute the object's right ascension and declination for the specified date.
 *
 * @param date The date and time to evaluate the object's coordinates.
 * @return A `RaDec` containing the object's right ascension and declination at the specified date.
 */
abstract fun getRaDec(date : Date) : RaDec

    /**
     * Enum that identifies whether we are interested in rise or set time.
     */
    enum class RiseSetIndicator {
        RISE, SET
    }

    // Maximum number of times to calculate rise/set times. If we cannot
    // converge after this many iterations, we will fail.
    private val MAX_ITERATIONS = 25

    /**
     * Compute the next rise or set time for this object as seen by an observer.
     *
     * Returns null if the event does not occur within the next day.
     *
     * @param now Reference time from which to search for the next event.
     * @param loc Observer latitude and longitude.
     * @param indicator Selects whether to find the next rise (`RiseSetIndicator.RISE`) or set (`RiseSetIndicator.SET`) time.
     * @return A Calendar set to the next rise or set time in the observer's local time zone, or `null` if no event occurs within the next day.
     */
    open fun calcNextRiseSetTime(
        now: Calendar, loc: LatLong,
        indicator: RiseSetIndicator
    ): Calendar? {
        // Make a copy of the calendar to return.
        val riseSetTime = Calendar.getInstance()
        val riseSetUt = calcRiseSetTime(now.time, loc, indicator)
        // Early out if no nearby rise set time.
        if (riseSetUt < 0) {
            return null
        }

        // Find the start of this day in the local time zone. The (a / b) * b
        // formulation looks weird, it's using the properties of int arithmetic
        // so that (a / b) is really floor(a / b).
        val dayStart = now.timeInMillis / TimeConstants.MILLISECONDS_PER_DAY * TimeConstants.MILLISECONDS_PER_DAY - riseSetTime[Calendar.ZONE_OFFSET]
        val riseSetUtMillis = (calcRiseSetTime(now.time, loc, indicator)
                * TimeConstants.MILLISECONDS_PER_HOUR).toLong()
        var newTime = dayStart + riseSetUtMillis + riseSetTime[Calendar.ZONE_OFFSET]
        // If the newTime is before the current time, go forward 1 day.
        if (newTime < now.timeInMillis) {
            newTime += TimeConstants.MILLISECONDS_PER_DAY
        }
        riseSetTime.timeInMillis = newTime
        return riseSetTime
    }

    // Used in Rise/Set calculations
    open protected val bodySize = 0.0f

    // Internally calculate the rise and set time of an object.
    /**
     * Computes the Universal Time hour (0–24) of the next rise or set for this object on the given date.
     *
     * @param d Date for which the rise/set is sought (date component used in UT).
     * @param loc Observer latitude/longitude.
     * @param indicator Whether to compute the rise time or the set time.
     * @return The hour-of-day in UT (decimal hours between 0 and 24) when the event occurs, or `-1.0` if the calculation did not converge.
     */
    private fun calcRiseSetTime(
        d: Date, loc: LatLong,
        indicator: RiseSetIndicator
    ): Double {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UT"))
        cal.time = d
        val sign = if (indicator == RiseSetIndicator.RISE) 1.0f else -1.0f
        var delta = 5.0f
        var ut = 12.0f
        var counter = 0
        while (Math.abs(delta) > 0.008 && counter < MAX_ITERATIONS) {
            cal[Calendar.HOUR_OF_DAY] = floor(ut).toInt()
            val minutes: Float = (ut - floor(ut)) * 60.0f
            cal[Calendar.MINUTE] = minutes.toInt()
            cal[Calendar.SECOND] = ((minutes - floor(minutes)) * 60f).toInt()

            // Calculate the hour angle and declination of the planet.
            val tmp = cal.time
            val (ra, dec) = getRaDec(tmp)

            // GHA = GST - RA. (In degrees.)
            val gst: Float = meanSiderealTime(tmp, 0f)
            val gha = gst - ra

            // The value of -0.83 works for the diameter of the Sun and Moon. We
            // assume that other objects are simply points.
            val bodySize = bodySize
            val hourAngle = calculateHourAngle(bodySize, loc.latitude, dec)
            delta = (gha + loc.longitude + sign * hourAngle) / 15.0f
            while (delta < -24.0f) {
                delta = delta + 24.0f
            }
            while (delta > 24.0f) {
                delta = delta - 24.0f
            }
            ut = ut - delta

            // Normalize UT
            while (ut < 0.0f) {
                ut = ut + 24.0f
            }
            while (ut > 24.0f) {
                ut = ut - 24.0f
            }
            ++counter
        }

        // Return failure if we didn't converge.
        if (counter == MAX_ITERATIONS) {
            return (-1.0f).toDouble()
        }

        return ut.toDouble()
    }


    // Calculates the hour angle of a given declination for the given location.
    // This is a helper application for the rise and set calculations.
    /**
     * Computes the hour angle (in degrees) for a celestial body at a specified altitude, observer latitude, and declination.
     *
     * @param altitude Apparent altitude of the body in degrees.
     * @param latitude Observer latitude in degrees.
     * @param declination Body declination in degrees.
     * @return The hour angle in degrees.
     */
    open fun calculateHourAngle(
        altitude: Float, latitude: Float,
        declination: Float
    ): Float {
        val altRads: Float = altitude * DEGREES_TO_RADIANS
        val latRads: Float = latitude * DEGREES_TO_RADIANS
        val decRads: Float = declination * DEGREES_TO_RADIANS
        val cosHa: Float =
            (sin(altRads) - sin(latRads) * sin(decRads)) /
                    (cos(latRads) * cos(decRads))
        return RADIANS_TO_DEGREES * acos(cosHa)
    }
}