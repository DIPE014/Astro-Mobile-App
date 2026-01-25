package com.astro.app.core.control.space

import com.astro.app.core.util.VisibleForTesting
import com.astro.app.core.control.SolarSystemBody
import com.astro.app.core.math.*
import java.util.*

import com.astro.app.core.math.RaDec.Companion.fromGeocentricCoords
import kotlin.math.cos
import kotlin.math.log10


/**
 * A celestial object that lives in our solar system.
 */
abstract class SolarSystemObject(protected val solarSystemBody : SolarSystemBody) : MovingObject() {
    /**
     * The update frequency for this solar system object.
     *
     * @return The update frequency in milliseconds.
     */
    fun getUpdateFrequencyMs(): Long {
        return solarSystemBody.updateFrequencyMs
    }

    /**
     * Get the string resource ID for this solar system object's name.
     *
     * @return The string resource ID corresponding to the object's name.
     */
    fun getNameResourceId(): Int {
        return solarSystemBody.nameResourceId
    }

    /**
 * Provides the drawable resource ID for this body's image at the given time.
 *
 * @param time The moment used to determine the body's appearance (for example, phase or orientation).
 * @return The drawable resource ID representing the body's image for the specified time.
 */
    abstract fun getImageResourceId(time: Date): Int

    /**
     * Calculates the illuminated fraction of the body's disk as a percentage (0.0–100.0).
     *
     * @param time The epoch at which to compute illumination.
     * @return The illuminated percentage, where 0.0 means no illumination and 100.0 means fully illuminated.
     */
    @VisibleForTesting
    open fun calculatePercentIlluminated(time: Date): Float {
        val phaseAngle: Float = this.calculatePhaseAngle(time)
        return 50.0f * (1.0f + cos(phaseAngle * DEGREES_TO_RADIANS))
    }

    /**
     * Compute the geocentric phase angle of this solar system body for the given time.
     *
     * For the Moon this uses an elongation-based approximation; for other bodies the
     * returned angle is the geocentric phase angle computed from heliocentric positions.
     *
     * @param time The time of observation.
     * @return The phase angle in degrees (0..180).
     */
    @VisibleForTesting
    open fun calculatePhaseAngle(time: Date): Float {
        // For the moon, we will approximate phase angle by calculating the
        // elongation of the moon relative to the sun. This is accurate to within
        // about 1%.
        if (solarSystemBody === SolarSystemBody.Moon) {
            val moonRaDec: RaDec = this.getRaDec(time)
            val moon: Vector3 = getGeocentricCoords(moonRaDec)
            val sunCoords: Vector3 =
                heliocentricCoordinatesFromOrbitalElements(SolarSystemBody.Earth.getOrbitalElements(time))
            val sunRaDec = fromGeocentricCoords(sunCoords)
            val (x, y, z) = getGeocentricCoords(sunRaDec)
            return 180.0f -
                    MathUtils.acos(x * moon.x + y * moon.y + z * moon.z) * RADIANS_TO_DEGREES
        }

        // First, determine position in the solar system.
        val planetCoords: Vector3 =
            heliocentricCoordinatesFromOrbitalElements(solarSystemBody.getOrbitalElements(time))

        // Second, determine position relative to Earth
        val earthCoords: Vector3 =
            heliocentricCoordinatesFromOrbitalElements(SolarSystemBody.Earth.getOrbitalElements(time))
        val earthDistance = planetCoords.distanceFrom(earthCoords)

        // Finally, calculate the phase of the body.
        return MathUtils.acos(
            (earthDistance * earthDistance +
                    planetCoords.length2 -
                    earthCoords.length2) /
                    (2.0f * earthDistance * planetCoords.length)
        ) * RADIANS_TO_DEGREES
    }

    /**
     * Provides an experimental scale factor for rendering the object's image based on its SolarSystemBody.
     *
     * Maps specific bodies to predetermined float sizes (fraction of some base dimension):
     * Sun, Moon -> 0.02; Mercury, Venus, Mars, Pluto -> 0.01; Jupiter -> 0.025; Uranus, Neptune -> 0.015; Saturn -> 0.035.
     *
     * @return A float scale factor to use for the planetary image.
     * @throws RuntimeException If the object's SolarSystemBody is not covered by the mapping.
     */
    fun getPlanetaryImageSize(): Float {
        return when (this.solarSystemBody) {
            SolarSystemBody.Sun, SolarSystemBody.Moon -> 0.02f
            SolarSystemBody.Mercury, SolarSystemBody.Venus, SolarSystemBody.Mars, SolarSystemBody.Pluto -> 0.01f
            SolarSystemBody.Jupiter -> 0.025f
            SolarSystemBody.Uranus, SolarSystemBody.Neptune -> 0.015f
            SolarSystemBody.Saturn -> 0.035f
            else -> throw RuntimeException("Unknown image size for Solar System Object: $this")
        }
    }

    /**
     * Compute the object's apparent visual magnitude at the specified time.
     *
     * @param time The date/time for which to compute the magnitude.
     * @return The apparent visual magnitude (smaller values indicate a brighter appearance).
     * @throws RuntimeException If no magnitude formula exists for this solar system body.
     */
    open fun getMagnitude(time: Date): Float {
        // First, determine position in the solar system.
        val planetCoords = heliocentricCoordinatesFromOrbitalElements(solarSystemBody.getOrbitalElements(time))

        // Second, determine position relative to Earth
        val earthCoords =
            heliocentricCoordinatesFromOrbitalElements(SolarSystemBody.Earth.getOrbitalElements(time))
        val earthDistance = planetCoords.distanceFrom(earthCoords)

        // Third, calculate the phase of the body.
        val phase = MathUtils.acos(
            (earthDistance * earthDistance +
                    planetCoords.length2 -
                    earthCoords.length2) /
                    (2.0f * earthDistance * planetCoords.length)
        ) * RADIANS_TO_DEGREES
        val p = phase / 100.0f // Normalized phase angle

        // Finally, calculate the magnitude of the body.
        // Apparent visual magnitude
        var mag = when (this.solarSystemBody) {
            SolarSystemBody.Mercury -> -0.42f + (3.80f - (2.73f - 2.00f * p) * p) * p
            SolarSystemBody.Venus -> -4.40f + (0.09f + (2.39f - 0.65f * p) * p) * p
            SolarSystemBody.Mars -> -1.52f + 1.6f * p
            SolarSystemBody.Jupiter -> -9.40f + 0.5f * p
            SolarSystemBody.Saturn -> -8.75f
            SolarSystemBody.Uranus -> -7.19f
            SolarSystemBody.Neptune -> -6.87f
            SolarSystemBody.Pluto -> -1.0f
            else -> throw RuntimeException("Unknown magnitude for solar system body $this")
        }
        return mag + 5.0f * log10(planetCoords.length * earthDistance)
    }
}