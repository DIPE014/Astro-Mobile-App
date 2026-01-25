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
    /**
     * Compute the object's apparent right ascension and declination as seen from Earth for the given date.
     *
     * @param date The observation date.
     * @return The geocentric `RaDec` (right ascension and declination) for the object at `date`.
     */
    override fun getRaDec(date: Date): RaDec {
        val earthCoords =
            heliocentricCoordinatesFromOrbitalElements(SolarSystemBody.Earth.getOrbitalElements(date))
        val myCoords = getMyHeliocentricCoordinates(date)
        myCoords -= earthCoords
        val equ = convertToEquatorialCoordinates(myCoords)
        return RaDec.fromGeocentricCoords(equ)
    }

    /**
         * Computes this object's heliocentric position for the given date.
         *
         * @param date The date at which to compute heliocentric coordinates.
         * @return The heliocentric Coordinates representing this object's position relative to the Sun on the given date.
         */
        protected open fun getMyHeliocentricCoordinates(date: Date) =
        heliocentricCoordinatesFromOrbitalElements(solarSystemBody.getOrbitalElements(date))

    /**
     * Provide the drawable resource id for the body's image.
     *
     * @param time Reference time for image selection; ignored by this implementation.
     * @return The drawable resource id for the body's image.
     */
    override fun getImageResourceId(time: Date): Int {
        return solarSystemBody.imageResourceId
    }
}