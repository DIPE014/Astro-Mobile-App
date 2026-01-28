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

    override fun getMyHeliocentricCoordinates(date: Date) =
        Vector3(0.0f, 0.0f, 0.0f)

    override fun getMagnitude(time: Date) = -27.0f
}
