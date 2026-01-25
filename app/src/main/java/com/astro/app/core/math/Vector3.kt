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

import kotlin.math.sqrt


data class Vector3(@JvmField var x : Float, @JvmField var y : Float, @JvmField var z : Float) {

    /**
     * The square of the vector's length
     */
    val length2
        get() = x * x + y * y + z * z

    val length
        get() = sqrt(length2)

    /**
     * Constructs a Vector3 from a float[2] object.
     * Checks for length. This is probably inefficient, so if you're using this
     * you should already be questioning your use of float[] instead of Vector3.
     * @param xyz
     */
    constructor(xyz: FloatArray) : this(xyz[0], xyz[1], xyz[2]) {
        require(xyz.size == 3) { "Trying to create 3 vector from array of length: " + xyz.size }
    }

    /**
     * Set the vector's components to the given values.
     *
     * @param x New X component.
     * @param y New Y component.
     * @param z New Z component.
     */
    fun assign(x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
    }

    /**
     * Assigns the values of the other vector to this one.
     */
    fun assign(other: Vector3) {
        x = other.x
        y = other.y
        z = other.z
    }

    /**
     * Sets this vector to a unit vector pointing in the same direction.
     *
     * If the vector has length equal to zero, the components will become `NaN` or ±`Infinity`.
     */
    fun normalize() : Unit {
        val norm = length
        x /= norm
        y /= norm
        z /= norm
    }

    /**
     * Multiply each component of this vector by the given factor, modifying the vector.
     *
     * @param scale The multiplicative factor applied to x, y, and z.
     */
    operator fun timesAssign(scale: Float) {
        x *= scale
        y *= scale
        z *= scale
    }

    /**
     * Subtracts the components of the specified vector from this vector in place.
     *
     * @param other The vector whose components will be subtracted from this vector.
     */
    operator fun minusAssign(other: Vector3) {
        x -= other.x
        y -= other.y
        z -= other.z
    }

    /**
     * Computes the dot product of this vector with another vector.
     *
     * @param p2 The other vector to compute the dot product with.
     * @return The scalar dot product (x * p2.x + y * p2.y + z * p2.z).
     */
    infix fun dot(p2: Vector3): Float {
        return x * p2.x + y * p2.y + z * p2.z
    }

    /**
     * Compute the cross product of this vector with another vector.
     *
     * @param p2 The right-hand operand vector to cross with.
     * @return The cross product vector (this × p2), perpendicular to both operands following the right-hand rule.
     */
    operator fun times(p2: Vector3): Vector3 {
        return Vector3(
            y * p2.z - z * p2.y,
            -x * p2.z + z * p2.x,
            x * p2.y - y * p2.x
        )
    }

    /**
     * Compute the Euclidean distance from this vector to another vector.
     *
     * @param other The vector to measure distance to.
     * @return The Euclidean distance between this vector and `other`.
     */
    fun distanceFrom(other: Vector3): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Creates and returns a new Vector3 with the same components as this vector.
     *
     * Exposed for Java compatibility.
     *
     * @return A new Vector3 matching this vector's x, y, and z components.
     */
    fun copyForJ() : Vector3 {
        return copy()
    }

    /**
     * Adds this vector and another vector component-wise.
     *
     * @param v2 The vector to add.
     * @return A new Vector3 representing the component-wise sum of this vector and `v2`.
     */
    operator fun plus(v2: Vector3): Vector3 {
        return Vector3(x + v2.x, y + v2.y, z + v2.z)
    }

    /**
     * Subtracts the given vector from this vector and returns the difference.
     *
     * @param v2 The vector to subtract from this vector.
     * @return A new Vector3 representing this vector minus `v2`.
     */
    operator fun minus(v2: Vector3): Vector3 {
        return plus(-v2)
    }

    /**
     * Scale this vector by the given factor and return the result.
     *
     * @param factor Scale multiplier applied to each component.
     * @return A new Vector3 whose components are each multiplied by `factor`.
     */
    operator fun times(factor: Float): Vector3 {
        val scaled = copy()
        scaled *= factor
        return scaled
    }

    /**
     * Divide this vector by a scalar and return the resulting vector.
     *
     * @return A new Vector3 whose components are this vector's components divided by `factor`.
     */
    operator fun div(factor: Float): Vector3 {
        return this * (1 / factor)
    }

    /**
     * Creates a new vector with each component negated.
     *
     * @return A `Vector3` whose `x`, `y`, and `z` are the negated values of this vector.
     */
    operator fun unaryMinus(): Vector3 {
        return this * -1f
    }

    /**
     * Returns a unit-length copy of this vector or a zero vector when the vector is effectively zero.
     *
     * @return A normalized copy of this vector with length 1, or `Vector3.zero()` if this vector's length is less than 0.000001f.
     */
    fun normalizedCopy(): Vector3 {
        return if (length < 0.000001f) {
            zero()
        } else this / length
    }

    /**
     * Projects this vector onto the provided unit vector.
     *
     * @param unitVector A unit-length vector to project onto.
     * @return A vector parallel to `unitVector` whose magnitude equals the dot product of this vector with `unitVector`.
     */
    fun projectOnto(unitVector: Vector3): Vector3 {
        return unitVector * (this dot unitVector)
    }

    /**
     * Computes the cosine similarity between this vector and another vector.
     *
     * @param v The other vector to compare against.
     * @return The cosine of the angle between the two vectors; yields a value in [-1, 1] when both vectors have non-zero length, and `NaN` if either vector has zero length.
     */
    fun cosineSimilarity(
        v: Vector3
    ) = ((this dot v)
            / sqrt(
        (this dot this)
                * (v dot v)
    ))

    companion object Factory {
        /**
         * Create a vector with all components set to zero.
         *
         * @return A Vector3 whose x, y, and z components are all 0f.
         */
        @JvmStatic
        fun zero() = Vector3(0f, 0f, 0f)
        /**
         * Creates a unit vector pointing along the positive X axis.
         *
         * @return A Vector3 with components (1f, 0f, 0f).
         */
        @JvmStatic
        fun unitX() = Vector3(1f, 0f, 0f)
        /**
         * Create a unit vector pointing along the positive Y axis.
         *
         * @return A Vector3 with x = 0f, y = 1f, z = 0f.
         */
        @JvmStatic
        fun unitY() = Vector3(0f, 1f, 0f)
        /**
         * Create a unit vector pointing along the positive Z axis.
         *
         * @return A Vector3 with components (0f, 0f, 1f).
         */
        @JvmStatic
        fun unitZ() = Vector3(0f, 0f, 1f)
    }
}
