package com.astro.app.core.control.space

import com.astro.app.core.control.SolarSystemBody
import com.astro.app.core.math.*
import com.astro.app.core.math.MathUtils.asin
import com.astro.app.core.math.MathUtils.atan2
import com.astro.app.core.math.MathUtils.cos
import com.astro.app.core.math.MathUtils.sin
import java.util.*

/**
 * A class to represent the Moon.
 */
class Moon : EarthOrbitingObject(SolarSystemBody.Moon) {
    override fun getRaDec(date: Date): RaDec {
        /**
         * Calculate the geocentric right ascension and declination of the moon using
         * an approximation as described on page D22 of the 2008 Astronomical Almanac
         * All of the variables in this method use the same names as those described
         * in the text: lambda = Ecliptic longitude (degrees) beta = Ecliptic latitude
         * (degrees) pi = horizontal parallax (degrees) r = distance (Earth radii)
         *
         * NOTE: The text does not give a specific time period where the approximation
         * is valid, but it should be valid through at least 2009.
         */
        // First, calculate the number of Julian centuries from J2000.0.
        val t = ((julianDay(date) - 2451545.0f) / 36525.0f).toFloat()
        // Second, calculate the approximate geocentric orbital elements.
        val lambda = (218.32f + 481267.881f * t + (6.29f
                * sin((135.0f + 477198.87f * t) * DEGREES_TO_RADIANS)) - 1.27f
                * sin((259.3f - 413335.36f * t) * DEGREES_TO_RADIANS)) + (0.66f
                * sin((235.7f + 890534.22f * t) * DEGREES_TO_RADIANS)) + (0.21f
                * sin((269.9f + 954397.74f * t) * DEGREES_TO_RADIANS)) - (0.19f
                * sin((357.5f + 35999.05f * t) * DEGREES_TO_RADIANS)) - (0.11f
                * sin((186.5f + 966404.03f * t) * DEGREES_TO_RADIANS))
        val beta = (5.13f * sin((93.3f + 483202.02f * t) * DEGREES_TO_RADIANS) + 0.28f
                * sin((228.2f + 960400.89f * t) * DEGREES_TO_RADIANS)) - (0.28f
                * sin((318.3f + 6003.15f * t) * DEGREES_TO_RADIANS)) - (0.17f
                * sin((217.6f - 407332.21f * t) * DEGREES_TO_RADIANS))

        // Third, convert to RA and Dec.
        val l = (cos(beta * DEGREES_TO_RADIANS)
                * cos(lambda * DEGREES_TO_RADIANS))
        val m = (0.9175f * cos(beta * DEGREES_TO_RADIANS)
                * sin(lambda * DEGREES_TO_RADIANS)) - 0.3978f * sin(beta * DEGREES_TO_RADIANS)
        val n = (0.3978f * cos(beta * DEGREES_TO_RADIANS)
                * sin(lambda * DEGREES_TO_RADIANS)) + 0.9175f * sin(beta * DEGREES_TO_RADIANS)
        val ra: Float = mod2pi(atan2(m, l)) * RADIANS_TO_DEGREES
        val dec: Float = asin(n) * RADIANS_TO_DEGREES
        return RaDec(ra, dec)
    }

    /** Returns the resource id for the planet's image.  */
    override fun getImageResourceId(time: Date) = getLunarPhaseImageId(time)

    /**
     * Determine the Moon's phase and return the resource ID of the correct
     * image.
     *
     * Phase angle interpretation (from calculatePhaseAngle):
     * - 0° = Full moon (Moon opposite Sun, fully illuminated)
     * - 180° = New moon (Moon between Earth and Sun, not illuminated)
     *
     * Note: Returns placeholder values. Replace with actual R.drawable.moon* values
     * when resources are added.
     */
    fun getLunarPhaseImageId(time: Date): Int {
        // First, calculate phase angle:
        val phase: Float = calculatePhaseAngle(time)

        // Next, figure out what resource id to return.
        // Phase angle: 0° = full moon, 180° = new moon
        if (phase < 22.5f) {
            // Full moon (phase near 0°).
            return 4 // R.drawable.moon4
        } else if (phase > 157.5f) {
            // New moon (phase near 180°).
            return 0 // R.drawable.moon0
        }

        // Either crescent, quarter, or gibbous. Need to see whether we are
        // waxing or waning. Calculate the phase angle one day in the future.
        // Waning: phase angle increasing (toward new moon)
        // Waxing: phase angle decreasing (toward full moon)
        val tomorrow = Date(time.time + 24 * 3600 * 1000)
        val phase2: Float = calculatePhaseAngle(tomorrow)
        val isWaning = phase2 > phase

        if (phase < 67.5f) {
            // Gibbous (near full moon)
            return if (isWaning) 5 else 3 // waning gibbous : waxing gibbous
        } else if (phase < 112.5f) {
            // Quarter
            return if (isWaning) 6 else 2 // waning quarter : waxing quarter
        }

        // Crescent (near new moon)
        return if (isWaning) 7 else 1 // waning crescent : waxing crescent
    }

    override val bodySize = -0.83f

    override fun getMagnitude(time: Date) = -10.0f
}
