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

import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.sqrt

data class RaDec(
    var ra: Float, // In degrees
    var dec: Float // In degrees
) {

    /**
     * Determines whether this Ra/Dec is always above the horizon at the specified observer location.
     *
     * @param loc Observer location (latitude in degrees).
     * @return `true` if the coordinate never sets for the given latitude, `false` otherwise.
     */
    private fun isCircumpolarFor(loc: LatLong): Boolean {
        // This should be relatively easy to do. In the northern hemisphere,
        // objects never set if dec > 90 - lat and never rise if dec < lat -
        // 90. In the southern hemisphere, objects never set if dec < -90 - lat
        // and never rise if dec > 90 + lat. There must be a better way to do
        // this...
        return if (loc.latitude > 0.0f) {
            dec > 90.0f - loc.latitude
        } else {
            dec < -90.0f - loc.latitude
        }
    }

    /**
     * Determine whether this Ra/Dec never rises above the horizon for the given observer location.
     *
     * @param loc Observer location; latitude in degrees.
     * @return `true` if the RA/Dec never rises above the horizon at the given location, `false` otherwise.
     */
    private fun isNeverVisible(loc: LatLong): Boolean {
        return if (loc.latitude > 0.0f) {
            dec < loc.latitude - 90.0f
        } else {
            dec > 90.0f + loc.latitude
        }
    }

    companion object {
        /**
         * Convert right ascension given in hours, minutes, and seconds to decimal degrees.
         *
         * @param h Hours of right ascension (typically 0–24).
         * @param m Minutes of right ascension (0–59).
         * @param s Seconds of right ascension (0–59.999...).
         * @return Right ascension in decimal degrees (0–360).
         */
        @JvmStatic
        fun raDegreesFromHMS(h: Float, m: Float, s: Float): Float {
            return 360 / 24 * (h + m / 60 + s / 60 / 60)
        }

        /**
         * Convert degrees, arcminutes and arcseconds to decimal degrees.
         *
         * @param d Degrees component (may be negative to indicate south/negative declination).
         * @param m Arcminutes component (added as positive fractional degrees).
         * @param s Arcseconds component (added as positive fractional degrees).
         * @return The angle in decimal degrees (d + m/60 + s/3600).
         */
        @JvmStatic
        fun decDegreesFromDMS(d: Float, m: Float, s: Float): Float {
            return d + m / 60 + s / 60 / 60
        }

        /**
         * Converts 3D geocentric rectangular equatorial coordinates to right ascension and declination.
         *
         * @param coords A 3-element vector representing geocentric rectangular equatorial coordinates (x, y, z).
         * @return A RaDec where `ra` is right ascension in degrees (0 ≤ ra < 360) and `dec` is declination in degrees (-90 ≤ dec ≤ 90).
         */
        @JvmStatic
        fun fromGeocentricCoords(coords: Vector3): RaDec {
            // find the RA and DEC from the rectangular equatorial coords
            val ra = mod2pi(atan2(coords.y, coords.x)) * RADIANS_TO_DEGREES
            val dec =
                (atan(coords.z / sqrt(coords.x * coords.x + coords.y * coords.y))
                        * RADIANS_TO_DEGREES)
            return RaDec(ra, dec)
        }

        /**
         * Create a RaDec from right ascension in hours/minutes/seconds and declination in degrees/arcminutes/arcseconds.
         *
         * @param raHours Right ascension hours component.
         * @param raMinutes Right ascension minutes component.
         * @param raSeconds Right ascension seconds component.
         * @param decDegrees Declination degrees component (sign indicates north/south).
         * @param decMinutes Declination arcminutes component.
         * @param decSeconds Declination arcseconds component.
         * @return A RaDec with `ra` converted to decimal degrees and `dec` converted to decimal degrees.
         */
        @JvmStatic
        fun fromHoursMinutesSeconds(
            raHours: Float, raMinutes: Float, raSeconds: Float,
            decDegrees: Float, decMinutes: Float, decSeconds: Float
        ) = RaDec(
            raDegreesFromHMS(raHours, raMinutes, raSeconds),
            decDegreesFromDMS(decDegrees, decMinutes, decSeconds)
        )
    }
}