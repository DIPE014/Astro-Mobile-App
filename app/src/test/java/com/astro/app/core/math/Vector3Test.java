package com.astro.app.core.math;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the Vector3 class.
 *
 * <p>Tests cover vector arithmetic operations, dot product, cross product,
 * normalization, and distance calculations which are fundamental to
 * coordinate transformations in the astronomy app.</p>
 */
public class Vector3Test {

    private static final float EPSILON = 1e-5f;

    // ============================================
    // Construction Tests
    // ============================================

    @Test
    public void testConstructorWithComponents() {
        Vector3 v = new Vector3(1.0f, 2.0f, 3.0f);

        assertEquals(1.0f, v.x, EPSILON);
        assertEquals(2.0f, v.y, EPSILON);
        assertEquals(3.0f, v.z, EPSILON);
    }

    @Test
    public void testConstructorWithArray() {
        float[] xyz = {1.0f, 2.0f, 3.0f};
        Vector3 v = new Vector3(xyz);

        assertEquals(1.0f, v.x, EPSILON);
        assertEquals(2.0f, v.y, EPSILON);
        assertEquals(3.0f, v.z, EPSILON);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testConstructorWithInvalidArray() {
        float[] xyz = {1.0f, 2.0f}; // Wrong size
        new Vector3(xyz);
    }

    @Test
    public void testZeroVector() {
        Vector3 zero = Vector3.zero();

        assertEquals(0.0f, zero.x, EPSILON);
        assertEquals(0.0f, zero.y, EPSILON);
        assertEquals(0.0f, zero.z, EPSILON);
    }

    @Test
    public void testUnitVectors() {
        Vector3 unitX = Vector3.unitX();
        Vector3 unitY = Vector3.unitY();
        Vector3 unitZ = Vector3.unitZ();

        assertEquals(1.0f, unitX.x, EPSILON);
        assertEquals(0.0f, unitX.y, EPSILON);
        assertEquals(0.0f, unitX.z, EPSILON);

        assertEquals(0.0f, unitY.x, EPSILON);
        assertEquals(1.0f, unitY.y, EPSILON);
        assertEquals(0.0f, unitY.z, EPSILON);

        assertEquals(0.0f, unitZ.x, EPSILON);
        assertEquals(0.0f, unitZ.y, EPSILON);
        assertEquals(1.0f, unitZ.z, EPSILON);
    }

    // ============================================
    // Addition Tests
    // ============================================

    @Test
    public void testAddition() {
        Vector3 v1 = new Vector3(1.0f, 2.0f, 3.0f);
        Vector3 v2 = new Vector3(4.0f, 5.0f, 6.0f);

        Vector3 result = v1.plus(v2);

        assertEquals(5.0f, result.x, EPSILON);
        assertEquals(7.0f, result.y, EPSILON);
        assertEquals(9.0f, result.z, EPSILON);
    }

    @Test
    public void testAdditionWithNegativeValues() {
        Vector3 v1 = new Vector3(-1.0f, 2.0f, -3.0f);
        Vector3 v2 = new Vector3(1.0f, -2.0f, 3.0f);

        Vector3 result = v1.plus(v2);

        assertEquals(0.0f, result.x, EPSILON);
        assertEquals(0.0f, result.y, EPSILON);
        assertEquals(0.0f, result.z, EPSILON);
    }

    @Test
    public void testAdditionWithZero() {
        Vector3 v = new Vector3(1.0f, 2.0f, 3.0f);
        Vector3 zero = Vector3.zero();

        Vector3 result = v.plus(zero);

        assertEquals(v.x, result.x, EPSILON);
        assertEquals(v.y, result.y, EPSILON);
        assertEquals(v.z, result.z, EPSILON);
    }

    // ============================================
    // Subtraction Tests
    // ============================================

    @Test
    public void testSubtraction() {
        Vector3 v1 = new Vector3(5.0f, 7.0f, 9.0f);
        Vector3 v2 = new Vector3(1.0f, 2.0f, 3.0f);

        Vector3 result = v1.minus(v2);

        assertEquals(4.0f, result.x, EPSILON);
        assertEquals(5.0f, result.y, EPSILON);
        assertEquals(6.0f, result.z, EPSILON);
    }

    @Test
    public void testSubtractionFromSelf() {
        Vector3 v = new Vector3(1.0f, 2.0f, 3.0f);

        Vector3 result = v.minus(v);

        assertEquals(0.0f, result.x, EPSILON);
        assertEquals(0.0f, result.y, EPSILON);
        assertEquals(0.0f, result.z, EPSILON);
    }

    @Test
    public void testMinusAssign() {
        Vector3 v1 = new Vector3(5.0f, 7.0f, 9.0f);
        Vector3 v2 = new Vector3(1.0f, 2.0f, 3.0f);

        v1.minusAssign(v2);

        assertEquals(4.0f, v1.x, EPSILON);
        assertEquals(5.0f, v1.y, EPSILON);
        assertEquals(6.0f, v1.z, EPSILON);
    }

    // ============================================
    // Scalar Multiplication Tests
    // ============================================

    @Test
    public void testScalarMultiplication() {
        Vector3 v = new Vector3(1.0f, 2.0f, 3.0f);

        Vector3 result = v.times(2.0f);

        assertEquals(2.0f, result.x, EPSILON);
        assertEquals(4.0f, result.y, EPSILON);
        assertEquals(6.0f, result.z, EPSILON);
    }

    @Test
    public void testScalarMultiplicationByZero() {
        Vector3 v = new Vector3(1.0f, 2.0f, 3.0f);

        Vector3 result = v.times(0.0f);

        assertEquals(0.0f, result.x, EPSILON);
        assertEquals(0.0f, result.y, EPSILON);
        assertEquals(0.0f, result.z, EPSILON);
    }

    @Test
    public void testScalarMultiplicationByNegative() {
        Vector3 v = new Vector3(1.0f, 2.0f, 3.0f);

        Vector3 result = v.times(-1.0f);

        assertEquals(-1.0f, result.x, EPSILON);
        assertEquals(-2.0f, result.y, EPSILON);
        assertEquals(-3.0f, result.z, EPSILON);
    }

    @Test
    public void testTimesAssign() {
        Vector3 v = new Vector3(1.0f, 2.0f, 3.0f);

        v.timesAssign(3.0f);

        assertEquals(3.0f, v.x, EPSILON);
        assertEquals(6.0f, v.y, EPSILON);
        assertEquals(9.0f, v.z, EPSILON);
    }

    // ============================================
    // Division Tests
    // ============================================

    @Test
    public void testDivision() {
        Vector3 v = new Vector3(2.0f, 4.0f, 6.0f);

        Vector3 result = v.div(2.0f);

        assertEquals(1.0f, result.x, EPSILON);
        assertEquals(2.0f, result.y, EPSILON);
        assertEquals(3.0f, result.z, EPSILON);
    }

    @Test
    public void testDivisionByOne() {
        Vector3 v = new Vector3(1.0f, 2.0f, 3.0f);

        Vector3 result = v.div(1.0f);

        assertEquals(1.0f, result.x, EPSILON);
        assertEquals(2.0f, result.y, EPSILON);
        assertEquals(3.0f, result.z, EPSILON);
    }

    // ============================================
    // Dot Product Tests
    // ============================================

    @Test
    public void testDotProductBasic() {
        Vector3 v1 = new Vector3(1.0f, 2.0f, 3.0f);
        Vector3 v2 = new Vector3(4.0f, 5.0f, 6.0f);

        float dot = v1.dot(v2);

        // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        assertEquals(32.0f, dot, EPSILON);
    }

    @Test
    public void testDotProductWithOrthogonalVectors() {
        Vector3 unitX = Vector3.unitX();
        Vector3 unitY = Vector3.unitY();

        float dot = unitX.dot(unitY);

        assertEquals(0.0f, dot, EPSILON);
    }

    @Test
    public void testDotProductWithParallelVectors() {
        Vector3 v1 = new Vector3(1.0f, 0.0f, 0.0f);
        Vector3 v2 = new Vector3(2.0f, 0.0f, 0.0f);

        float dot = v1.dot(v2);

        assertEquals(2.0f, dot, EPSILON);
    }

    @Test
    public void testDotProductWithAntiparallelVectors() {
        Vector3 v1 = new Vector3(1.0f, 0.0f, 0.0f);
        Vector3 v2 = new Vector3(-1.0f, 0.0f, 0.0f);

        float dot = v1.dot(v2);

        assertEquals(-1.0f, dot, EPSILON);
    }

    @Test
    public void testDotProductWithSelf() {
        Vector3 v = new Vector3(3.0f, 4.0f, 0.0f);

        float dot = v.dot(v);

        // Should equal length squared
        assertEquals(25.0f, dot, EPSILON);
    }

    // ============================================
    // Cross Product Tests
    // ============================================

    @Test
    public void testCrossProductUnitVectors() {
        Vector3 unitX = Vector3.unitX();
        Vector3 unitY = Vector3.unitY();

        // X x Y = Z
        Vector3 result = unitX.times(unitY);

        assertEquals(0.0f, result.x, EPSILON);
        assertEquals(0.0f, result.y, EPSILON);
        assertEquals(1.0f, result.z, EPSILON);
    }

    @Test
    public void testCrossProductYxZ() {
        Vector3 unitY = Vector3.unitY();
        Vector3 unitZ = Vector3.unitZ();

        // Y x Z = X
        Vector3 result = unitY.times(unitZ);

        assertEquals(1.0f, result.x, EPSILON);
        assertEquals(0.0f, result.y, EPSILON);
        assertEquals(0.0f, result.z, EPSILON);
    }

    @Test
    public void testCrossProductZxX() {
        Vector3 unitZ = Vector3.unitZ();
        Vector3 unitX = Vector3.unitX();

        // Z x X = Y
        Vector3 result = unitZ.times(unitX);

        assertEquals(0.0f, result.x, EPSILON);
        assertEquals(1.0f, result.y, EPSILON);
        assertEquals(0.0f, result.z, EPSILON);
    }

    @Test
    public void testCrossProductAnticommutative() {
        Vector3 v1 = new Vector3(1.0f, 2.0f, 3.0f);
        Vector3 v2 = new Vector3(4.0f, 5.0f, 6.0f);

        Vector3 cross1 = v1.times(v2);
        Vector3 cross2 = v2.times(v1);

        // v1 x v2 = -(v2 x v1)
        assertEquals(-cross2.x, cross1.x, EPSILON);
        assertEquals(-cross2.y, cross1.y, EPSILON);
        assertEquals(-cross2.z, cross1.z, EPSILON);
    }

    @Test
    public void testCrossProductWithParallelVectors() {
        Vector3 v1 = new Vector3(1.0f, 0.0f, 0.0f);
        Vector3 v2 = new Vector3(2.0f, 0.0f, 0.0f);

        Vector3 result = v1.times(v2);

        // Parallel vectors have zero cross product
        assertEquals(0.0f, result.x, EPSILON);
        assertEquals(0.0f, result.y, EPSILON);
        assertEquals(0.0f, result.z, EPSILON);
    }

    @Test
    public void testCrossProductOrthogonalToInputs() {
        Vector3 v1 = new Vector3(1.0f, 2.0f, 3.0f);
        Vector3 v2 = new Vector3(4.0f, 5.0f, 6.0f);

        Vector3 cross = v1.times(v2);

        // Cross product should be orthogonal to both inputs
        assertEquals(0.0f, cross.dot(v1), EPSILON);
        assertEquals(0.0f, cross.dot(v2), EPSILON);
    }

    // ============================================
    // Length and Normalization Tests
    // ============================================

    @Test
    public void testLength() {
        Vector3 v = new Vector3(3.0f, 4.0f, 0.0f);

        float length = v.getLength();

        assertEquals(5.0f, length, EPSILON);
    }

    @Test
    public void testLength3D() {
        Vector3 v = new Vector3(1.0f, 2.0f, 2.0f);

        float length = v.getLength();

        // sqrt(1 + 4 + 4) = sqrt(9) = 3
        assertEquals(3.0f, length, EPSILON);
    }

    @Test
    public void testLengthSquared() {
        Vector3 v = new Vector3(3.0f, 4.0f, 0.0f);

        float length2 = v.getLength2();

        assertEquals(25.0f, length2, EPSILON);
    }

    @Test
    public void testLengthOfZeroVector() {
        Vector3 zero = Vector3.zero();

        assertEquals(0.0f, zero.getLength(), EPSILON);
    }

    @Test
    public void testLengthOfUnitVector() {
        Vector3 unitX = Vector3.unitX();

        assertEquals(1.0f, unitX.getLength(), EPSILON);
    }

    @Test
    public void testNormalize() {
        Vector3 v = new Vector3(3.0f, 4.0f, 0.0f);

        v.normalize();

        assertEquals(1.0f, v.getLength(), EPSILON);
        assertEquals(0.6f, v.x, EPSILON);
        assertEquals(0.8f, v.y, EPSILON);
        assertEquals(0.0f, v.z, EPSILON);
    }

    @Test
    public void testNormalizedCopy() {
        Vector3 v = new Vector3(3.0f, 4.0f, 0.0f);

        Vector3 normalized = v.normalizedCopy();

        // Original should be unchanged
        assertEquals(3.0f, v.x, EPSILON);
        assertEquals(4.0f, v.y, EPSILON);

        // Normalized copy should have unit length
        assertEquals(1.0f, normalized.getLength(), EPSILON);
        assertEquals(0.6f, normalized.x, EPSILON);
        assertEquals(0.8f, normalized.y, EPSILON);
    }

    @Test
    public void testNormalizedCopyOfNearZeroVector() {
        Vector3 v = new Vector3(0.0000001f, 0.0f, 0.0f);

        Vector3 normalized = v.normalizedCopy();

        // Should return zero vector for very small vectors
        assertEquals(0.0f, normalized.x, EPSILON);
        assertEquals(0.0f, normalized.y, EPSILON);
        assertEquals(0.0f, normalized.z, EPSILON);
    }

    // ============================================
    // Distance Tests
    // ============================================

    @Test
    public void testDistanceFrom() {
        Vector3 v1 = new Vector3(0.0f, 0.0f, 0.0f);
        Vector3 v2 = new Vector3(3.0f, 4.0f, 0.0f);

        float distance = v1.distanceFrom(v2);

        assertEquals(5.0f, distance, EPSILON);
    }

    @Test
    public void testDistanceFromSelf() {
        Vector3 v = new Vector3(1.0f, 2.0f, 3.0f);

        float distance = v.distanceFrom(v);

        assertEquals(0.0f, distance, EPSILON);
    }

    @Test
    public void testDistanceIsCommutative() {
        Vector3 v1 = new Vector3(1.0f, 2.0f, 3.0f);
        Vector3 v2 = new Vector3(4.0f, 5.0f, 6.0f);

        float distance1 = v1.distanceFrom(v2);
        float distance2 = v2.distanceFrom(v1);

        assertEquals(distance1, distance2, EPSILON);
    }

    @Test
    public void testDistance3D() {
        Vector3 v1 = new Vector3(0.0f, 0.0f, 0.0f);
        Vector3 v2 = new Vector3(1.0f, 2.0f, 2.0f);

        float distance = v1.distanceFrom(v2);

        // sqrt(1 + 4 + 4) = 3
        assertEquals(3.0f, distance, EPSILON);
    }

    // ============================================
    // Other Operations Tests
    // ============================================

    @Test
    public void testUnaryMinus() {
        Vector3 v = new Vector3(1.0f, -2.0f, 3.0f);

        Vector3 negated = v.unaryMinus();

        assertEquals(-1.0f, negated.x, EPSILON);
        assertEquals(2.0f, negated.y, EPSILON);
        assertEquals(-3.0f, negated.z, EPSILON);
    }

    @Test
    public void testAssignComponents() {
        Vector3 v = new Vector3(0.0f, 0.0f, 0.0f);

        v.assign(1.0f, 2.0f, 3.0f);

        assertEquals(1.0f, v.x, EPSILON);
        assertEquals(2.0f, v.y, EPSILON);
        assertEquals(3.0f, v.z, EPSILON);
    }

    @Test
    public void testAssignFromOther() {
        Vector3 v1 = new Vector3(0.0f, 0.0f, 0.0f);
        Vector3 v2 = new Vector3(1.0f, 2.0f, 3.0f);

        v1.assign(v2);

        assertEquals(1.0f, v1.x, EPSILON);
        assertEquals(2.0f, v1.y, EPSILON);
        assertEquals(3.0f, v1.z, EPSILON);
    }

    @Test
    public void testCopyForJ() {
        Vector3 original = new Vector3(1.0f, 2.0f, 3.0f);

        Vector3 copy = original.copyForJ();

        // Should be equal
        assertEquals(original.x, copy.x, EPSILON);
        assertEquals(original.y, copy.y, EPSILON);
        assertEquals(original.z, copy.z, EPSILON);

        // But not the same object
        assertNotSame(original, copy);

        // Modifying copy should not affect original
        copy.x = 10.0f;
        assertEquals(1.0f, original.x, EPSILON);
    }

    @Test
    public void testProjectOnto() {
        Vector3 v = new Vector3(3.0f, 4.0f, 0.0f);
        Vector3 unitX = Vector3.unitX();

        Vector3 projection = v.projectOnto(unitX);

        assertEquals(3.0f, projection.x, EPSILON);
        assertEquals(0.0f, projection.y, EPSILON);
        assertEquals(0.0f, projection.z, EPSILON);
    }

    @Test
    public void testCosineSimilarity() {
        Vector3 v1 = new Vector3(1.0f, 0.0f, 0.0f);
        Vector3 v2 = new Vector3(1.0f, 0.0f, 0.0f);

        float similarity = v1.cosineSimilarity(v2);

        assertEquals(1.0f, similarity, EPSILON);
    }

    @Test
    public void testCosineSimilarityOrthogonal() {
        Vector3 v1 = new Vector3(1.0f, 0.0f, 0.0f);
        Vector3 v2 = new Vector3(0.0f, 1.0f, 0.0f);

        float similarity = v1.cosineSimilarity(v2);

        assertEquals(0.0f, similarity, EPSILON);
    }

    @Test
    public void testCosineSimilarityOpposite() {
        Vector3 v1 = new Vector3(1.0f, 0.0f, 0.0f);
        Vector3 v2 = new Vector3(-1.0f, 0.0f, 0.0f);

        float similarity = v1.cosineSimilarity(v2);

        assertEquals(-1.0f, similarity, EPSILON);
    }

    // ============================================
    // Equality Tests
    // ============================================

    @Test
    public void testEquals() {
        Vector3 v1 = new Vector3(1.0f, 2.0f, 3.0f);
        Vector3 v2 = new Vector3(1.0f, 2.0f, 3.0f);

        assertEquals(v1, v2);
    }

    @Test
    public void testNotEquals() {
        Vector3 v1 = new Vector3(1.0f, 2.0f, 3.0f);
        Vector3 v2 = new Vector3(1.0f, 2.0f, 4.0f);

        assertNotEquals(v1, v2);
    }

    @Test
    public void testHashCodeConsistency() {
        Vector3 v1 = new Vector3(1.0f, 2.0f, 3.0f);
        Vector3 v2 = new Vector3(1.0f, 2.0f, 3.0f);

        assertEquals(v1.hashCode(), v2.hashCode());
    }
}
