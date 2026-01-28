package com.astro.app.core.math;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the Matrix3x3 class.
 *
 * <p>Tests cover matrix multiplication, transpose, inverse, and determinant
 * calculations which are essential for coordinate transformations in
 * astronomical calculations.</p>
 */
public class Matrix3x3Test {

    private static final float EPSILON = 1e-5f;

    // ============================================
    // Construction Tests
    // ============================================

    @Test
    public void testConstructorWithComponents() {
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );

        assertEquals(1.0f, m.xx, EPSILON);
        assertEquals(2.0f, m.xy, EPSILON);
        assertEquals(3.0f, m.xz, EPSILON);
        assertEquals(4.0f, m.yx, EPSILON);
        assertEquals(5.0f, m.yy, EPSILON);
        assertEquals(6.0f, m.yz, EPSILON);
        assertEquals(7.0f, m.zx, EPSILON);
        assertEquals(8.0f, m.zy, EPSILON);
        assertEquals(9.0f, m.zz, EPSILON);
    }

    @Test
    public void testIdentityMatrix() {
        Matrix3x3 identity = Matrix3x3.identity;

        assertEquals(1.0f, identity.xx, EPSILON);
        assertEquals(0.0f, identity.xy, EPSILON);
        assertEquals(0.0f, identity.xz, EPSILON);
        assertEquals(0.0f, identity.yx, EPSILON);
        assertEquals(1.0f, identity.yy, EPSILON);
        assertEquals(0.0f, identity.yz, EPSILON);
        assertEquals(0.0f, identity.zx, EPSILON);
        assertEquals(0.0f, identity.zy, EPSILON);
        assertEquals(1.0f, identity.zz, EPSILON);
    }

    @Test
    public void testConstructorFromColumnVectors() {
        Vector3 col1 = new Vector3(1.0f, 2.0f, 3.0f);
        Vector3 col2 = new Vector3(4.0f, 5.0f, 6.0f);
        Vector3 col3 = new Vector3(7.0f, 8.0f, 9.0f);

        Matrix3x3 m = new Matrix3x3(col1, col2, col3, true);

        // Column vectors: first vector is first column
        assertEquals(1.0f, m.xx, EPSILON);
        assertEquals(4.0f, m.xy, EPSILON);
        assertEquals(7.0f, m.xz, EPSILON);
        assertEquals(2.0f, m.yx, EPSILON);
        assertEquals(5.0f, m.yy, EPSILON);
        assertEquals(8.0f, m.yz, EPSILON);
        assertEquals(3.0f, m.zx, EPSILON);
        assertEquals(6.0f, m.zy, EPSILON);
        assertEquals(9.0f, m.zz, EPSILON);
    }

    @Test
    public void testConstructorFromRowVectors() {
        Vector3 row1 = new Vector3(1.0f, 2.0f, 3.0f);
        Vector3 row2 = new Vector3(4.0f, 5.0f, 6.0f);
        Vector3 row3 = new Vector3(7.0f, 8.0f, 9.0f);

        Matrix3x3 m = new Matrix3x3(row1, row2, row3, false);

        // Row vectors: first vector is first row
        assertEquals(1.0f, m.xx, EPSILON);
        assertEquals(2.0f, m.xy, EPSILON);
        assertEquals(3.0f, m.xz, EPSILON);
        assertEquals(4.0f, m.yx, EPSILON);
        assertEquals(5.0f, m.yy, EPSILON);
        assertEquals(6.0f, m.yz, EPSILON);
        assertEquals(7.0f, m.zx, EPSILON);
        assertEquals(8.0f, m.zy, EPSILON);
        assertEquals(9.0f, m.zz, EPSILON);
    }

    // ============================================
    // Matrix Multiplication Tests
    // ============================================

    @Test
    public void testMultiplyWithIdentity() {
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );
        Matrix3x3 identity = Matrix3x3.identity;

        Matrix3x3 result = m.times(identity);

        assertEquals(m.xx, result.xx, EPSILON);
        assertEquals(m.xy, result.xy, EPSILON);
        assertEquals(m.xz, result.xz, EPSILON);
        assertEquals(m.yx, result.yx, EPSILON);
        assertEquals(m.yy, result.yy, EPSILON);
        assertEquals(m.yz, result.yz, EPSILON);
        assertEquals(m.zx, result.zx, EPSILON);
        assertEquals(m.zy, result.zy, EPSILON);
        assertEquals(m.zz, result.zz, EPSILON);
    }

    @Test
    public void testIdentityMultiplyWithMatrix() {
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );
        Matrix3x3 identity = Matrix3x3.identity;

        Matrix3x3 result = identity.times(m);

        assertEquals(m.xx, result.xx, EPSILON);
        assertEquals(m.xy, result.xy, EPSILON);
        assertEquals(m.xz, result.xz, EPSILON);
        assertEquals(m.yx, result.yx, EPSILON);
        assertEquals(m.yy, result.yy, EPSILON);
        assertEquals(m.yz, result.yz, EPSILON);
        assertEquals(m.zx, result.zx, EPSILON);
        assertEquals(m.zy, result.zy, EPSILON);
        assertEquals(m.zz, result.zz, EPSILON);
    }

    @Test
    public void testMatrixMultiplication() {
        Matrix3x3 a = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );
        Matrix3x3 b = new Matrix3x3(
            9.0f, 8.0f, 7.0f,
            6.0f, 5.0f, 4.0f,
            3.0f, 2.0f, 1.0f
        );

        Matrix3x3 result = a.times(b);

        // Row 1: [1*9+2*6+3*3, 1*8+2*5+3*2, 1*7+2*4+3*1] = [30, 24, 18]
        assertEquals(30.0f, result.xx, EPSILON);
        assertEquals(24.0f, result.xy, EPSILON);
        assertEquals(18.0f, result.xz, EPSILON);

        // Row 2: [4*9+5*6+6*3, 4*8+5*5+6*2, 4*7+5*4+6*1] = [84, 69, 54]
        assertEquals(84.0f, result.yx, EPSILON);
        assertEquals(69.0f, result.yy, EPSILON);
        assertEquals(54.0f, result.yz, EPSILON);

        // Row 3: [7*9+8*6+9*3, 7*8+8*5+9*2, 7*7+8*4+9*1] = [138, 114, 90]
        assertEquals(138.0f, result.zx, EPSILON);
        assertEquals(114.0f, result.zy, EPSILON);
        assertEquals(90.0f, result.zz, EPSILON);
    }

    @Test
    public void testMatrixMultiplicationNotCommutative() {
        Matrix3x3 a = new Matrix3x3(
            1.0f, 2.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f
        );
        Matrix3x3 b = new Matrix3x3(
            1.0f, 0.0f, 0.0f,
            3.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 1.0f
        );

        Matrix3x3 ab = a.times(b);
        Matrix3x3 ba = b.times(a);

        // A*B and B*A should be different
        assertNotEquals(ab.xx, ba.xx, EPSILON);
    }

    @Test
    public void testMatrixVectorMultiplication() {
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );
        Vector3 v = new Vector3(1.0f, 0.0f, 0.0f);

        Vector3 result = m.times(v);

        // Result is first column
        assertEquals(1.0f, result.x, EPSILON);
        assertEquals(4.0f, result.y, EPSILON);
        assertEquals(7.0f, result.z, EPSILON);
    }

    @Test
    public void testMatrixVectorMultiplicationGeneral() {
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );
        Vector3 v = new Vector3(1.0f, 2.0f, 3.0f);

        Vector3 result = m.times(v);

        // [1*1+2*2+3*3, 4*1+5*2+6*3, 7*1+8*2+9*3] = [14, 32, 50]
        assertEquals(14.0f, result.x, EPSILON);
        assertEquals(32.0f, result.y, EPSILON);
        assertEquals(50.0f, result.z, EPSILON);
    }

    @Test
    public void testIdentityMatrixVectorMultiplication() {
        Matrix3x3 identity = Matrix3x3.identity;
        Vector3 v = new Vector3(1.0f, 2.0f, 3.0f);

        Vector3 result = identity.times(v);

        assertEquals(v.x, result.x, EPSILON);
        assertEquals(v.y, result.y, EPSILON);
        assertEquals(v.z, result.z, EPSILON);
    }

    // ============================================
    // Transpose Tests
    // ============================================

    @Test
    public void testTranspose() {
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );

        m.transpose();

        assertEquals(1.0f, m.xx, EPSILON);
        assertEquals(4.0f, m.xy, EPSILON);
        assertEquals(7.0f, m.xz, EPSILON);
        assertEquals(2.0f, m.yx, EPSILON);
        assertEquals(5.0f, m.yy, EPSILON);
        assertEquals(8.0f, m.yz, EPSILON);
        assertEquals(3.0f, m.zx, EPSILON);
        assertEquals(6.0f, m.zy, EPSILON);
        assertEquals(9.0f, m.zz, EPSILON);
    }

    @Test
    public void testTransposeIdentity() {
        Matrix3x3 identity = Matrix3x3.identity.copyForJ();

        identity.transpose();

        // Identity transpose is identity
        assertEquals(1.0f, identity.xx, EPSILON);
        assertEquals(0.0f, identity.xy, EPSILON);
        assertEquals(0.0f, identity.xz, EPSILON);
        assertEquals(0.0f, identity.yx, EPSILON);
        assertEquals(1.0f, identity.yy, EPSILON);
        assertEquals(0.0f, identity.yz, EPSILON);
        assertEquals(0.0f, identity.zx, EPSILON);
        assertEquals(0.0f, identity.zy, EPSILON);
        assertEquals(1.0f, identity.zz, EPSILON);
    }

    @Test
    public void testDoubleTransposeRestoresOriginal() {
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );

        m.transpose();
        m.transpose();

        assertEquals(1.0f, m.xx, EPSILON);
        assertEquals(2.0f, m.xy, EPSILON);
        assertEquals(3.0f, m.xz, EPSILON);
        assertEquals(4.0f, m.yx, EPSILON);
        assertEquals(5.0f, m.yy, EPSILON);
        assertEquals(6.0f, m.yz, EPSILON);
        assertEquals(7.0f, m.zx, EPSILON);
        assertEquals(8.0f, m.zy, EPSILON);
        assertEquals(9.0f, m.zz, EPSILON);
    }

    @Test
    public void testSymmetricMatrixTranspose() {
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            2.0f, 5.0f, 6.0f,
            3.0f, 6.0f, 9.0f
        );

        Matrix3x3 original = m.copyForJ();
        m.transpose();

        // Symmetric matrix equals its transpose
        assertEquals(original.xx, m.xx, EPSILON);
        assertEquals(original.xy, m.xy, EPSILON);
        assertEquals(original.xz, m.xz, EPSILON);
        assertEquals(original.yx, m.yx, EPSILON);
        assertEquals(original.yy, m.yy, EPSILON);
        assertEquals(original.yz, m.yz, EPSILON);
        assertEquals(original.zx, m.zx, EPSILON);
        assertEquals(original.zy, m.zy, EPSILON);
        assertEquals(original.zz, m.zz, EPSILON);
    }

    // ============================================
    // Determinant Tests
    // ============================================

    @Test
    public void testDeterminantIdentity() {
        Matrix3x3 identity = Matrix3x3.identity;

        float det = identity.getDeterminant();

        assertEquals(1.0f, det, EPSILON);
    }

    @Test
    public void testDeterminantDiagonal() {
        Matrix3x3 m = new Matrix3x3(
            2.0f, 0.0f, 0.0f,
            0.0f, 3.0f, 0.0f,
            0.0f, 0.0f, 4.0f
        );

        float det = m.getDeterminant();

        // Determinant of diagonal matrix is product of diagonal elements
        assertEquals(24.0f, det, EPSILON);
    }

    @Test
    public void testDeterminantSingularMatrix() {
        // Matrix with linearly dependent rows
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );

        float det = m.getDeterminant();

        // This matrix is singular (det = 0)
        assertEquals(0.0f, det, EPSILON);
    }

    @Test
    public void testDeterminantGeneral() {
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            0.0f, 1.0f, 4.0f,
            5.0f, 6.0f, 0.0f
        );

        float det = m.getDeterminant();

        // det = 1*(1*0 - 4*6) - 2*(0*0 - 4*5) + 3*(0*6 - 1*5)
        // det = 1*(-24) - 2*(-20) + 3*(-5)
        // det = -24 + 40 - 15 = 1
        assertEquals(1.0f, det, EPSILON);
    }

    @Test
    public void testDeterminantOfRotationMatrix() {
        // 90 degree rotation around Z axis
        float angle = (float) (Math.PI / 2);
        Matrix3x3 rotation = new Matrix3x3(
            (float) Math.cos(angle), (float) -Math.sin(angle), 0.0f,
            (float) Math.sin(angle), (float) Math.cos(angle), 0.0f,
            0.0f, 0.0f, 1.0f
        );

        float det = rotation.getDeterminant();

        // Rotation matrices have determinant 1
        assertEquals(1.0f, det, EPSILON);
    }

    // ============================================
    // Inverse Tests
    // ============================================

    @Test
    public void testInverseIdentity() {
        Matrix3x3 identity = Matrix3x3.identity;

        Matrix3x3 inverse = identity.getInverse();

        assertNotNull(inverse);
        assertEquals(1.0f, inverse.xx, EPSILON);
        assertEquals(0.0f, inverse.xy, EPSILON);
        assertEquals(0.0f, inverse.xz, EPSILON);
        assertEquals(0.0f, inverse.yx, EPSILON);
        assertEquals(1.0f, inverse.yy, EPSILON);
        assertEquals(0.0f, inverse.yz, EPSILON);
        assertEquals(0.0f, inverse.zx, EPSILON);
        assertEquals(0.0f, inverse.zy, EPSILON);
        assertEquals(1.0f, inverse.zz, EPSILON);
    }

    @Test
    public void testInverseNullForSingularMatrix() {
        // Singular matrix (det = 0)
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );

        Matrix3x3 inverse = m.getInverse();

        assertNull(inverse);
    }

    @Test
    public void testInverseTimesOriginalIsIdentity() {
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            0.0f, 1.0f, 4.0f,
            5.0f, 6.0f, 0.0f
        );

        Matrix3x3 inverse = m.getInverse();
        assertNotNull(inverse);

        Matrix3x3 product = m.times(inverse);

        // M * M^-1 = I
        assertEquals(1.0f, product.xx, EPSILON);
        assertEquals(0.0f, product.xy, EPSILON);
        assertEquals(0.0f, product.xz, EPSILON);
        assertEquals(0.0f, product.yx, EPSILON);
        assertEquals(1.0f, product.yy, EPSILON);
        assertEquals(0.0f, product.yz, EPSILON);
        assertEquals(0.0f, product.zx, EPSILON);
        assertEquals(0.0f, product.zy, EPSILON);
        assertEquals(1.0f, product.zz, EPSILON);
    }

    @Test
    public void testOriginalTimesInverseIsIdentity() {
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            0.0f, 1.0f, 4.0f,
            5.0f, 6.0f, 0.0f
        );

        Matrix3x3 inverse = m.getInverse();
        assertNotNull(inverse);

        Matrix3x3 product = inverse.times(m);

        // M^-1 * M = I
        assertEquals(1.0f, product.xx, EPSILON);
        assertEquals(0.0f, product.xy, EPSILON);
        assertEquals(0.0f, product.xz, EPSILON);
        assertEquals(0.0f, product.yx, EPSILON);
        assertEquals(1.0f, product.yy, EPSILON);
        assertEquals(0.0f, product.yz, EPSILON);
        assertEquals(0.0f, product.zx, EPSILON);
        assertEquals(0.0f, product.zy, EPSILON);
        assertEquals(1.0f, product.zz, EPSILON);
    }

    @Test
    public void testInverseDiagonal() {
        Matrix3x3 m = new Matrix3x3(
            2.0f, 0.0f, 0.0f,
            0.0f, 4.0f, 0.0f,
            0.0f, 0.0f, 5.0f
        );

        Matrix3x3 inverse = m.getInverse();

        assertNotNull(inverse);
        assertEquals(0.5f, inverse.xx, EPSILON);
        assertEquals(0.0f, inverse.xy, EPSILON);
        assertEquals(0.0f, inverse.xz, EPSILON);
        assertEquals(0.0f, inverse.yx, EPSILON);
        assertEquals(0.25f, inverse.yy, EPSILON);
        assertEquals(0.0f, inverse.yz, EPSILON);
        assertEquals(0.0f, inverse.zx, EPSILON);
        assertEquals(0.0f, inverse.zy, EPSILON);
        assertEquals(0.2f, inverse.zz, EPSILON);
    }

    @Test
    public void testInverseOfInverseIsOriginal() {
        Matrix3x3 m = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            0.0f, 1.0f, 4.0f,
            5.0f, 6.0f, 0.0f
        );

        Matrix3x3 inverse = m.getInverse();
        assertNotNull(inverse);

        Matrix3x3 doubleInverse = inverse.getInverse();
        assertNotNull(doubleInverse);

        // (M^-1)^-1 = M
        assertEquals(m.xx, doubleInverse.xx, EPSILON);
        assertEquals(m.xy, doubleInverse.xy, EPSILON);
        assertEquals(m.xz, doubleInverse.xz, EPSILON);
        assertEquals(m.yx, doubleInverse.yx, EPSILON);
        assertEquals(m.yy, doubleInverse.yy, EPSILON);
        assertEquals(m.yz, doubleInverse.yz, EPSILON);
        assertEquals(m.zx, doubleInverse.zx, EPSILON);
        assertEquals(m.zy, doubleInverse.zy, EPSILON);
        assertEquals(m.zz, doubleInverse.zz, EPSILON);
    }

    // ============================================
    // Copy Tests
    // ============================================

    @Test
    public void testCopyForJ() {
        Matrix3x3 original = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );

        Matrix3x3 copy = original.copyForJ();

        // Should be equal
        assertEquals(original.xx, copy.xx, EPSILON);
        assertEquals(original.xy, copy.xy, EPSILON);
        assertEquals(original.xz, copy.xz, EPSILON);
        assertEquals(original.yx, copy.yx, EPSILON);
        assertEquals(original.yy, copy.yy, EPSILON);
        assertEquals(original.yz, copy.yz, EPSILON);
        assertEquals(original.zx, copy.zx, EPSILON);
        assertEquals(original.zy, copy.zy, EPSILON);
        assertEquals(original.zz, copy.zz, EPSILON);

        // But not the same object
        assertNotSame(original, copy);

        // Modifying copy should not affect original
        copy.xx = 100.0f;
        assertEquals(1.0f, original.xx, EPSILON);
    }

    // ============================================
    // Equality Tests
    // ============================================

    @Test
    public void testEquals() {
        Matrix3x3 m1 = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );
        Matrix3x3 m2 = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );

        assertEquals(m1, m2);
    }

    @Test
    public void testNotEquals() {
        Matrix3x3 m1 = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );
        Matrix3x3 m2 = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 10.0f  // Different
        );

        assertNotEquals(m1, m2);
    }

    @Test
    public void testHashCodeConsistency() {
        Matrix3x3 m1 = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );
        Matrix3x3 m2 = new Matrix3x3(
            1.0f, 2.0f, 3.0f,
            4.0f, 5.0f, 6.0f,
            7.0f, 8.0f, 9.0f
        );

        assertEquals(m1.hashCode(), m2.hashCode());
    }

    // ============================================
    // Rotation Matrix Properties Tests
    // ============================================

    @Test
    public void testRotationMatrixOrthogonality() {
        // Create a rotation matrix using the Geometry utility
        float angle = 45.0f;
        Vector3 axis = Vector3.unitZ();
        Matrix3x3 rotation = GeometryKt.calculateRotationMatrix(angle, axis);

        // Rotation matrix transpose should equal its inverse
        Matrix3x3 transpose = rotation.copyForJ();
        transpose.transpose();

        Matrix3x3 product = rotation.times(transpose);

        // R * R^T = I
        assertEquals(1.0f, product.xx, EPSILON);
        assertEquals(0.0f, product.xy, EPSILON);
        assertEquals(0.0f, product.xz, EPSILON);
        assertEquals(0.0f, product.yx, EPSILON);
        assertEquals(1.0f, product.yy, EPSILON);
        assertEquals(0.0f, product.yz, EPSILON);
        assertEquals(0.0f, product.zx, EPSILON);
        assertEquals(0.0f, product.zy, EPSILON);
        assertEquals(1.0f, product.zz, EPSILON);
    }

    @Test
    public void testRotationMatrixDeterminant() {
        float angle = 30.0f;
        Vector3 axis = new Vector3(1.0f, 1.0f, 1.0f);
        axis.normalize();
        Matrix3x3 rotation = GeometryKt.calculateRotationMatrix(angle, axis);

        float det = rotation.getDeterminant();

        // Proper rotation matrices have determinant 1
        assertEquals(1.0f, det, EPSILON);
    }

    @Test
    public void testRotationBy360DegreesIsIdentity() {
        Vector3 axis = Vector3.unitZ();
        Matrix3x3 rotation = GeometryKt.calculateRotationMatrix(360.0f, axis);

        // Rotation by 360 degrees should be identity
        assertEquals(1.0f, rotation.xx, EPSILON);
        assertEquals(0.0f, rotation.xy, EPSILON);
        assertEquals(0.0f, rotation.xz, EPSILON);
        assertEquals(0.0f, rotation.yx, EPSILON);
        assertEquals(1.0f, rotation.yy, EPSILON);
        assertEquals(0.0f, rotation.yz, EPSILON);
        assertEquals(0.0f, rotation.zx, EPSILON);
        assertEquals(0.0f, rotation.zy, EPSILON);
        assertEquals(1.0f, rotation.zz, EPSILON);
    }
}
