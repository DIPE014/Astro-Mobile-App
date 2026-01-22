package com.astro.app.core.control.space

import com.astro.app.core.control.SolarSystemBody
import com.astro.app.core.math.RaDec
import com.astro.app.core.math.convertToEquatorialCoordinates
import com.astro.app.core.math.heliocentricCoordinatesFromOrbitalElements
import java.util.*

/**
 * An object that orbits the sun.
 */
open class SunOrbitingObject(solarSystemBody : SolarSystemBody) : SolarSystemObject(solarSystemBody) {
    override fun getRaDec(date: Date): RaDec {
        val earthCoords =
            heliocentricCoordinatesFromOrbitalElements(SolarSystemBody.Earth.getOrbitalElements(date))
        val myCoords = getMyHeliocentricCoordinates(date)
        myCoords -= earthCoords
        val equ = convertToEquatorialCoordinates(myCoords)
        return RaDec.fromGeocentricCoords(equ)
    }

    protected open fun getMyHeliocentricCoordinates(date: Date) =
        heliocentricCoordinatesFromOrbitalElements(solarSystemBody.getOrbitalElements(date))

    /** Returns the resource id for the planet's image.  */
    override fun getImageResourceId(time: Date): Int {
        return solarSystemBody.imageResourceId
    }
}
