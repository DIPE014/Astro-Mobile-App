package com.astro.app.core.control.space

import com.astro.app.core.control.SolarSystemBody
import com.astro.app.core.math.Vector3
import java.util.*

/**
 * The Sun is special as it's at the center of the solar system.
 *
 * It's a sort of trivial sun-orbiting object.
 */
class Sun : SunOrbitingObject(SolarSystemBody.Sun) {
    override val bodySize = -0.83f

    /**
         * Provide the Sun's heliocentric coordinates.
         *
         * @param date The time for which coordinates are requested; ignored because the Sun is fixed at the heliocentric origin.
         * @return A Vector3 at the origin (0.0f, 0.0f, 0.0f).
         */
        override fun getMyHeliocentricCoordinates(date: Date) =
        Vector3(0.0f, 0.0f, 0.0f)

    /**
 * Provides the Sun's apparent visual magnitude.
 *
 * @param time Ignored; present for API compatibility.
 * @return The Sun's apparent visual magnitude (-27.0f).
 */
override fun getMagnitude(time: Date) = -27.0f
}