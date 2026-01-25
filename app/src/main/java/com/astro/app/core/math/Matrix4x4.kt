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

import com.astro.app.core.math.MathUtils.sin
import com.astro.app.core.math.MathUtils.cos
import com.astro.app.core.math.MathUtils.tan

/**
 * Represents a 4x4 matrix, specifically for use in rendering code. Consequently includes
 * functions for creating matrices for doing things like rotations.
 */
data class Matrix4x4(private val contents: FloatArray) {
    val floatArray = FloatArray(16)

    init {
        assert(contents.size == 16)
        System.arraycopy(contents, 0, floatArray, 0, 16)
    }

    /**
     * Multiply this 4x4 matrix by another 4x4 matrix.
     *
     * @param mat2 The right-hand matrix to multiply by.
     * @return A new Matrix4x4 representing the product of this matrix and `mat2` (this * mat2).
     */
    operator fun times(mat2 : Matrix4x4): Matrix4x4 {
        val m = this.floatArray
        val n = mat2.floatArray
        return Matrix4x4(
            floatArrayOf(
                m[0] * n[0] + m[4] * n[1] + m[8] * n[2] + m[12] * n[3],
                m[1] * n[0] + m[5] * n[1] + m[9] * n[2] + m[13] * n[3],
                m[2] * n[0] + m[6] * n[1] + m[10] * n[2] + m[14] * n[3],
                m[3] * n[0] + m[7] * n[1] + m[11] * n[2] + m[15] * n[3],
                m[0] * n[4] + m[4] * n[5] + m[8] * n[6] + m[12] * n[7],
                m[1] * n[4] + m[5] * n[5] + m[9] * n[6] + m[13] * n[7],
                m[2] * n[4] + m[6] * n[5] + m[10] * n[6] + m[14] * n[7],
                m[3] * n[4] + m[7] * n[5] + m[11] * n[6] + m[15] * n[7],
                m[0] * n[8] + m[4] * n[9] + m[8] * n[10] + m[12] * n[11],
                m[1] * n[8] + m[5] * n[9] + m[9] * n[10] + m[13] * n[11],
                m[2] * n[8] + m[6] * n[9] + m[10] * n[10] + m[14] * n[11],
                m[3] * n[8] + m[7] * n[9] + m[11] * n[10] + m[15] * n[11],
                m[0] * n[12] + m[4] * n[13] + m[8] * n[14] + m[12] * n[15],
                m[1] * n[12] + m[5] * n[13] + m[9] * n[14] + m[13] * n[15],
                m[2] * n[12] + m[6] * n[13] + m[10] * n[14] + m[14] * n[15],
                m[3] * n[12] + m[7] * n[13] + m[11] * n[14] + m[15] * n[15]
            )
        )
    }

    /**
     * Transforms a 3D vector by this 4x4 matrix, applying rotation, scale, and translation.
     *
     * @param v The input Vector3 to transform; treated as a position (translation is applied).
     * @return The transformed Vector3 with x, y, and z computed from the matrix-vector product (including the matrix's translation column).
     */
    operator fun times(v : Vector3): Vector3 {
        val m = this.floatArray
        return Vector3(
            m[0] * v.x + m[4] * v.y + m[8] * v.z + m[12],
            m[1] * v.x + m[5] * v.y + m[9] * v.z + m[13],
            m[2] * v.x + m[6] * v.y + m[10] * v.z + m[14]
        )
    }

    companion object {
        /**
         * Creates an identity 4x4 matrix.
         *
         * @return A Matrix4x4 representing the identity transform (1.0 on the main diagonal, 0.0 elsewhere).
         */
        @JvmStatic
        fun createIdentity(): Matrix4x4 {
            return createScaling(1f, 1f, 1f)
        }

        /**
         * Creates a scaling matrix that scales coordinates along the x, y, and z axes.
         *
         * @param x Scale factor along the X axis.
         * @param y Scale factor along the Y axis.
         * @param z Scale factor along the Z axis.
         * @return A 4x4 matrix that applies the specified scaling with the homogeneous coordinate left as 1.
         */
        @JvmStatic
        fun createScaling(x: Float, y: Float, z: Float): Matrix4x4 {
            return Matrix4x4(
                floatArrayOf(
                    x, 0f, 0f, 0f, 0f, y, 0f, 0f, 0f, 0f, z, 0f, 0f, 0f, 0f, 1f
                )
            )
        }

        /**
         * Creates a 4x4 translation matrix that translates coordinates by the given amounts along each axis.
         *
         * @param x Translation distance along the X axis.
         * @param y Translation distance along the Y axis.
         * @param z Translation distance along the Z axis.
         * @return A Matrix4x4 representing the translation transform in homogeneous coordinates.
         */
        @JvmStatic
        fun createTranslation(x: Float, y: Float, z: Float): Matrix4x4 {
            return Matrix4x4(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f,
                    x, y, z, 1f
                )
            )
        }

        /**
         * Creates a 4x4 homogeneous rotation matrix for rotating by the specified angle about a given axis.
         *
         * @param angleRadians Rotation angle in radians.
         * @param unitAxis Axis of rotation; must be normalized (unit length).
         * @return A Matrix4x4 that applies the specified rotation in homogeneous coordinates (bottom-right element is 1).
         */
        @JvmStatic
        fun createRotation(angleRadians: Float, unitAxis: Vector3): Matrix4x4 {
            val m = FloatArray(16)
            val xSqr = unitAxis.x * unitAxis.x
            val ySqr = unitAxis.y * unitAxis.y
            val zSqr = unitAxis.z * unitAxis.z
            val sinAngle = sin(angleRadians)
            val cosAngle = cos(angleRadians)
            val oneMinusCosAngle = 1 - cosAngle
            val xSinAngle = unitAxis.x * sinAngle
            val ySinAngle = unitAxis.y * sinAngle
            val zSinAngle = unitAxis.z * sinAngle
            val zOneMinusCosAngle = unitAxis.z * oneMinusCosAngle
            val xyOneMinusCosAngle = unitAxis.x * unitAxis.y * oneMinusCosAngle
            val xzOneMinusCosAngle = unitAxis.x * zOneMinusCosAngle
            val yzOneMinusCosAngle = unitAxis.y * zOneMinusCosAngle
            m[0] = xSqr + (ySqr + zSqr) * cosAngle
            m[1] = xyOneMinusCosAngle + zSinAngle
            m[2] = xzOneMinusCosAngle - ySinAngle
            m[3] = 0f
            m[4] = xyOneMinusCosAngle - zSinAngle
            m[5] = ySqr + (xSqr + zSqr) * cosAngle
            m[6] = yzOneMinusCosAngle + xSinAngle
            m[7] = 0f
            m[8] = xzOneMinusCosAngle + ySinAngle
            m[9] = yzOneMinusCosAngle - xSinAngle
            m[10] = zSqr + (xSqr + ySqr) * cosAngle
            m[11] = 0f
            m[12] = 0f
            m[13] = 0f
            m[14] = 0f
            m[15] = 1f
            return Matrix4x4(m)
        }

        /**
         * Creates a perspective projection matrix for the given viewport dimensions and vertical field of view.
         *
         * @param width The viewport width in pixels (used to derive aspect ratio).
         * @param height The viewport height in pixels (used to derive aspect ratio).
         * @param fovyInRadians The vertical field of view in radians.
         * @return A 4x4 projection Matrix4x4 that maps camera (view) space to clip space using near = 0.01 and far = 10000.0.
         */
        @JvmStatic
        fun createPerspectiveProjection(
            width: Float,
            height: Float,
            fovyInRadians: Float
        ): Matrix4x4 {
            val near = 0.01f
            val far = 10000.0f
            val inverseAspectRatio = height / width
            val oneOverTanHalfRadiusOfView = 1.0f / tan(fovyInRadians)
            return Matrix4x4(
                floatArrayOf(
                    inverseAspectRatio * oneOverTanHalfRadiusOfView, 0f, 0f, 0f, 0f,
                    oneOverTanHalfRadiusOfView, 0f, 0f, 0f, 0f,
                    -(far + near) / (far - near), -1f, 0f, 0f,
                    -2 * far * near / (far - near), 0f
                )
            )
        }

        /**
         * Constructs a view matrix from the provided camera basis vectors.
         *
         * @param lookDir The camera's forward direction (look direction).
         * @param up The camera's up direction.
         * @param right The camera's right direction.
         * @return A 4x4 view matrix that maps world coordinates into camera space using the given basis vectors.
         */
        @JvmStatic
        fun createView(lookDir: Vector3, up: Vector3, right: Vector3): Matrix4x4 {
            return Matrix4x4(
                floatArrayOf(
                    right.x,
                    up.x,
                    -lookDir.x, 0f,
                    right.y,
                    up.y,
                    -lookDir.y, 0f,
                    right.z,
                    up.z,
                    -lookDir.z, 0f, 0f, 0f, 0f, 1f
                )
            )
        }

        /**
         * Multiply two 4x4 matrices and return their product.
         *
         * @param mat1 The left-hand matrix operand.
         * @param mat2 The right-hand matrix operand.
         * @return A new Matrix4x4 equal to mat1 multiplied by mat2.
         */
        @JvmStatic
        fun times(mat1: Matrix4x4, mat2: Matrix4x4): Matrix4x4 {
            return mat1 * mat2
        }

        /**
         * Transforms a Vector3 by a 4x4 matrix.
         *
         * @param mat The 4x4 matrix to apply.
         * @param v The vector to transform.
         * @return The transformed Vector3 with rotation, scale, and translation (translation taken from the matrix's last column).
         */
        @JvmStatic
        fun multiplyMV(mat: Matrix4x4, v: Vector3): Vector3 {
            return mat * v
        }

        /**
         * Applies a perspective-style transform to a 3D vector and performs the homogeneous w‑divide on x and y.
         *
         * @param mat The 4x4 transform matrix.
         * @param v The input 3D vector.
         * @return A new Vector3 whose x and y components are divided by the computed w (perspective divide); z is left as the resulting depth. 
         */
        @JvmStatic
        fun transformVector(mat: Matrix4x4, v: Vector3): Vector3 {
            val trans = multiplyMV(mat, v)
            val m = mat.floatArray
            val w = m[3] * v.x + m[7] * v.y + m[11] * v.z + m[15]
            val oneOverW = 1.0f / w
            trans.x *= oneOverW
            trans.y *= oneOverW
            // Don't transform z, we just leave it as a "pseudo-depth".
            return trans
        }
    }
}