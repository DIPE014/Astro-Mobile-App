// Copyright 2008 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.astro.app.core.math

/**
 * Basic methods for doing mathematical operations with floats.
 *
 * @author Brent Bryan
 */
const val PI = Math.PI.toFloat()
const val TWO_PI = 2f * PI
const val DEGREES_TO_RADIANS = PI / 180.0f
const val RADIANS_TO_DEGREES = 180.0f / PI

/**
 * Compute the floored modulo of `a` by `n`, producing a value in the range [0, n).
 *
 * @param a The value to reduce modulo `n`.
 * @param n The modulus; the result is constrained to the half-open interval [0, n).
 * @return The remainder of `a` modulo `n` as a Float, adjusted to lie in [0, n).
 */
fun flooredMod(a: Float, n: Float) = (if (a < 0) a % n + n else a) % n

/**
 * Wraps an angle to the range [0, 2π).
 *
 * @param x Angle in radians.
 * @return An equivalent angle in radians in the range [0, 2π).
 */
fun mod2pi(x: Float) = positiveMod(x, TWO_PI)

/**
 * Computes x modulo y and returns the non-negative remainder in [0, y) when y > 0.
 *
 * @param x The dividend.
 * @param y The divisor; when y > 0 the result is guaranteed to be in [0, y).
 * @return The remainder r satisfying r ≡ x (mod y) adjusted to be >= 0.
 */
fun positiveMod(x: Double, y: Double): Double {
    var remainder = x % y
    if (remainder < 0) remainder += y
    return remainder
}

/**
 * Calculates x modulus y, but ensures that the result lies in [0, y)
 */
/**
 * Compute the modulo of `x` by `y` and return a non-negative remainder.
 *
 * @param x The dividend.
 * @param y The divisor; when `y > 0` the result is guaranteed to be in `[0, y)`.
 * @return The remainder of `x` modulo `y`, adjusted to be non-negative.
private fun positiveMod(x: Float, y: Float): Float {
    var remainder = x % y
    if (remainder < 0) remainder += y
    return remainder
}

/**
 * Calculates the Euclidean length of the 3D vector (x, y, z).
 *
 * @param x X component of the vector.
 * @param y Y component of the vector.
 * @param z Z component of the vector.
 * @return The Euclidean norm (length) of the vector.
 */
fun norm(x: Float, y: Float, z: Float) = kotlin.math.sqrt(x * x + y * y + z * z)


// TODO(jontayler): eliminate this class if we can eliminate floats.
object MathUtils {
    /**
     * Computes the absolute value of the given floating-point number.
     *
     * @param x The value whose absolute value is computed.
     * @return The absolute value of `x`.
     */
    @JvmStatic
    fun abs(x: Float) = kotlin.math.abs(x)

    /**
     * Compute the square root of x.
     *
     * @param x The value whose square root is computed.
     * @return The non-negative square root of x.
     */
    @JvmStatic
    fun sqrt(x: Float) = kotlin.math.sqrt(x)

    /**
     * Compute the sine of the given angle in radians.
     *
     * @param x Angle in radians.
     * @return The sine of the angle.
     */
    @JvmStatic
    fun sin(x: Float) = kotlin.math.sin(x)

    /**
     * Computes the cosine of the specified angle in radians.
     *
     * @param x Angle in radians.
     * @return The cosine of the angle.
     */
    @JvmStatic
    fun cos(x: Float) = kotlin.math.cos(x)

    /**
     * Computes the tangent of an angle.
     *
     * @param x Angle in radians.
     * @return The tangent of `x`.
     */
    @JvmStatic
    fun tan(x: Float) = kotlin.math.tan(x)

    /**
     * Computes the arcsine (inverse sine) of the given value.
     *
     * @param x Input value in the range [-1, 1].
     * @return The angle in radians whose sine is `x`, in the range [-PI/2, PI/2].
     */
    @JvmStatic
    fun asin(x: Float) = kotlin.math.asin(x)

    /**
     * Computes the arccosine of the given value.
     *
     * @param x Input value; expected in the range [-1, 1].
     * @return The angle in radians in the range [0, π] whose cosine is `x`.
     */
    @JvmStatic
    fun acos(x: Float) = kotlin.math.acos(x)

    /**
     * Computes the angle, in radians, between the positive X-axis and the point (`x`, `y`).
     *
     * @param y The Y coordinate of the point.
     * @param x The X coordinate of the point.
     * @return The angle in radians, between -π and π.
     */
    @JvmStatic
    fun atan2(y: Float, x: Float) = kotlin.math.atan2(y, x)

}