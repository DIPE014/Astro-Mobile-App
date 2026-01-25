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

import com.astro.app.core.control.OrbitalElements
import com.astro.app.core.math.MathUtils.asin
import com.astro.app.core.math.MathUtils.atan2
import com.astro.app.core.math.MathUtils.cos
import com.astro.app.core.math.MathUtils.sin

/**
 * Utilities for manipulating different coordinate systems.
 *
 * |RaDec| represents right ascension and declination.  It's a pair of angles roughly analogous
 * to latitude and longitude. Centered on Earth, they are fixed in space.
 *
 * Geocentric coordinates. These are coordinates centered on Earth and fixed in space. They
 * can be freely converted to the angles |RaDec|. The z axis corresponds to a Dec of 90 degrees
 * and the x axis to a right ascension of zero and a dec of zero.
 */

/**
 * Updates the given vector with the supplied [RaDec].
 */
/**
 * Set this vector to the unit geocentric Cartesian coordinates corresponding to the given right ascension and declination.
 *
 * @param raDec Right ascension and declination (in degrees) defining the position on the celestial sphere.
 */
fun Vector3.updateFromRaDec(raDec: RaDec) {
    this.updateFromRaDec(raDec.ra, raDec.dec)
}

/**
 * Set this vector to the unit-sphere geocentric Cartesian coordinates for the given right
 * ascension and declination.
 *
 * @receiver Vector to update; after the call it represents a unit geocentric vector (x, y, z).
 * @param ra Right ascension in degrees (0°–360°).
 * @param dec Declination in degrees (−90°–90°).
 */
private fun Vector3.updateFromRaDec(ra: Float, dec: Float) {
    val raRadians = ra * DEGREES_TO_RADIANS
    val decRadians = dec * DEGREES_TO_RADIANS
    this.x = cos(raRadians) * cos(decRadians)
    this.y = sin(raRadians) * cos(decRadians)
    this.z = sin(decRadians)
}

/**
 * Compute the right ascension from a unit geocentric vector.
 *
 * The vector is interpreted in geocentric coordinates and is assumed to lie on the unit sphere.
 *
 * @param v The unit geocentric vector.
 * @return Right ascension in degrees.
 */
fun getRaOfUnitGeocentricVector(v: Vector3): Float {
    // Assumes unit sphere.
    return RADIANS_TO_DEGREES * atan2(v.y, v.x)
}

/**
 * Compute the declination (degrees) from a unit geocentric vector.
 *
 * Assumes `v` lies on the unit sphere in geocentric coordinates. Result is in degrees in the range −90 to 90.
 *
 * @param v The unit geocentric vector.
 * @return The declination in degrees.
 */
fun getDecOfUnitGeocentricVector(v: Vector3): Float {
    // Assumes unit sphere.
    return RADIANS_TO_DEGREES * asin(v.z)
}

/**
 * Create a unit geocentric Cartesian vector from right ascension and declination.
 *
 * @param raDec Right ascension and declination in degrees.
 * @return A `Vector3` on the unit sphere representing the geocentric coordinates corresponding to the given RA/Dec.
 */
fun getGeocentricCoords(raDec: RaDec): Vector3 {
    return getGeocentricCoords(raDec.ra, raDec.dec)
}

/**
 * Create a unit-sphere geocentric Vector3 from right ascension and declination.
 *
 * @param ra Right ascension in degrees.
 * @param dec Declination in degrees.
 * @return A Vector3 whose components are the geocentric (x, y, z) coordinates on the unit sphere.
 */
fun getGeocentricCoords(ra: Float, dec: Float): Vector3 {
    val coords = Vector3(0.0f, 0.0f, 0.0f)
    coords.updateFromRaDec(ra, dec)
    return coords
}

// Value of the obliquity of the ecliptic for J2000
private const val OBLIQUITY = 23.439281f * DEGREES_TO_RADIANS

/**
 * Compute heliocentric Cartesian coordinates from the given orbital elements.
 *
 * @param elem Orbital elements describing the body's orbit (must provide anomaly, eccentricity,
 * perihelion, ascending node, inclination, and orbital distance).
 * @return A Vector3 containing the body's heliocentric Cartesian coordinates in astronomical units,
 * with the Sun at the origin and the z-axis normal to Earth's orbital plane.
fun heliocentricCoordinatesFromOrbitalElements(elem: OrbitalElements): Vector3 {
    val anomaly = elem.anomaly
    val ecc = elem.eccentricity
    val radius = elem.distance * (1 - ecc * ecc) / (1 + ecc * cos(anomaly))

    // heliocentric rectangular coordinates of planet
    val per = elem.perihelion
    val asc = elem.ascendingNode
    val inc = elem.inclination
    val xh = radius *
            (cos(asc) * cos(anomaly + per - asc) -
                    sin(asc) * sin(anomaly + per - asc) *
                    cos(inc))
    val yh = radius *
            (sin(asc) * cos(anomaly + per - asc) +
                    cos(asc) * sin(anomaly + per - asc) *
                    cos(inc))
    val zh = radius * (sin(anomaly + per - asc) * sin(inc))
    return Vector3(xh, yh, zh)
}

/**
 * Rotate a 3D position from Earth's orbital plane into Earth's equatorial plane using the J2000 obliquity.
 *
 * @param earthOrbitalPlane A vector expressed in Earth's orbital (ecliptic) plane coordinates.
 * @return A new `Vector3` representing the same position in Earth's equatorial coordinates.
 */
fun convertToEquatorialCoordinates(earthOrbitalPlane : Vector3): Vector3 {
    return Vector3(
        earthOrbitalPlane.x,
        earthOrbitalPlane.y * cos(OBLIQUITY) - earthOrbitalPlane.z * sin(OBLIQUITY),
        earthOrbitalPlane.y * sin(OBLIQUITY) + earthOrbitalPlane.z * cos(OBLIQUITY)
    )
}
