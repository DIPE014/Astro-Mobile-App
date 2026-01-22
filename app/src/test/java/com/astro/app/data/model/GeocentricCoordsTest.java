package com.astro.app.data.model;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the GeocentricCoords class.
 *
 * <p>Tests cover coordinate creation, angular distance calculations,
 * and Vector3 conversion which are essential for determining celestial
 * object positions in the sky map.</p>
 */
public class GeocentricCoordsTest {

    private static final float EPSILON = 1e-4f;

    // ============================================
    // Factory Method Tests
    // ============================================

    @Test
    public void testFromDegrees() {
        GeocentricCoords coords = GeocentricCoords.fromDegrees(180.0f, 45.0f);

        assertEquals(180.0f, coords.getRa(), EPSILON);
        assertEquals(45.0f, coords.getDec(), EPSILON);
    }

    @Test
    public void testFromDegrees_ZeroValues() {
        GeocentricCoords coords = GeocentricCoords.fromDegrees(0.0f, 0.0f);

        assertEquals(0.0f, coords.getRa(), EPSILON);
        assertEquals(0.0f, coords.getDec(), EPSILON);
    }

    @Test
    public void testFromDegrees_NegativeDec() {
        GeocentricCoords coords = GeocentricCoords.fromDegrees(90.0f, -45.0f);

        assertEquals(90.0f, coords.getRa(), EPSILON);
        assertEquals(-45.0f, coords.getDec(), EPSILON);
    }

    @Test
    public void testFromHoursAndDegrees() {
        // 12 hours = 180 degrees
        GeocentricCoords coords = GeocentricCoords.fromHoursAndDegrees(12.0f, 45.0f);

        assertEquals(180.0f, coords.getRa(), EPSILON);
        assertEquals(45.0f, coords.getDec(), EPSILON);
    }

    @Test
    public void testFromHoursAndDegrees_SixHours() {
        // 6 hours = 90 degrees
        GeocentricCoords coords = GeocentricCoords.fromHoursAndDegrees(6.0f, 0.0f);

        assertEquals(90.0f, coords.getRa(), EPSILON);
    }

    @Test
    public void testFromRadians() {
        // PI radians = 180 degrees, PI/4 radians = 45 degrees
        GeocentricCoords coords = GeocentricCoords.fromRadians(
            (float) Math.PI,
            (float) (Math.PI / 4)
        );

        assertEquals(180.0f, coords.getRa(), EPSILON);
        assertEquals(45.0f, coords.getDec(), EPSILON);
    }

    @Test
    public void testFromVector3_XAxis() {
        // Point on X axis (RA = 0, Dec = 0)
        GeocentricCoords coords = GeocentricCoords.fromVector3(1.0f, 0.0f, 0.0f);

        assertEquals(0.0f, coords.getRa(), EPSILON);
        assertEquals(0.0f, coords.getDec(), EPSILON);
    }

    @Test
    public void testFromVector3_YAxis() {
        // Point on Y axis (RA = 90, Dec = 0)
        GeocentricCoords coords = GeocentricCoords.fromVector3(0.0f, 1.0f, 0.0f);

        assertEquals(90.0f, coords.getRa(), EPSILON);
        assertEquals(0.0f, coords.getDec(), EPSILON);
    }

    @Test
    public void testFromVector3_ZAxis() {
        // Point on Z axis (Dec = 90, north celestial pole)
        GeocentricCoords coords = GeocentricCoords.fromVector3(0.0f, 0.0f, 1.0f);

        assertEquals(90.0f, coords.getDec(), EPSILON);
    }

    @Test
    public void testFromVector3_NegativeX() {
        // Point on negative X axis (RA = 180, Dec = 0)
        GeocentricCoords coords = GeocentricCoords.fromVector3(-1.0f, 0.0f, 0.0f);

        assertEquals(180.0f, coords.getRa(), EPSILON);
        assertEquals(0.0f, coords.getDec(), EPSILON);
    }

    @Test
    public void testFromVector3_NegativeY() {
        // Point on negative Y axis (RA = 270, Dec = 0)
        GeocentricCoords coords = GeocentricCoords.fromVector3(0.0f, -1.0f, 0.0f);

        assertEquals(270.0f, coords.getRa(), EPSILON);
        assertEquals(0.0f, coords.getDec(), EPSILON);
    }

    @Test
    public void testFromVector3_SouthPole() {
        // Point on negative Z axis (Dec = -90)
        GeocentricCoords coords = GeocentricCoords.fromVector3(0.0f, 0.0f, -1.0f);

        assertEquals(-90.0f, coords.getDec(), EPSILON);
    }

    // ============================================
    // RA Normalization Tests
    // ============================================

    @Test
    public void testRaNormalization_OverRange() {
        // RA > 360 should be normalized
        GeocentricCoords coords = GeocentricCoords.fromDegrees(400.0f, 0.0f);

        assertEquals(40.0f, coords.getRa(), EPSILON);
    }

    @Test
    public void testRaNormalization_NegativeValue() {
        // RA < 0 should be normalized
        GeocentricCoords coords = GeocentricCoords.fromDegrees(-30.0f, 0.0f);

        assertEquals(330.0f, coords.getRa(), EPSILON);
    }

    @Test
    public void testRaNormalization_Exactly360() {
        // RA = 360 should become 0
        GeocentricCoords coords = GeocentricCoords.fromDegrees(360.0f, 0.0f);

        assertEquals(0.0f, coords.getRa(), EPSILON);
    }

    @Test
    public void testRaNormalization_LargeValue() {
        // RA = 720 should become 0
        GeocentricCoords coords = GeocentricCoords.fromDegrees(720.0f, 0.0f);

        assertEquals(0.0f, coords.getRa(), EPSILON);
    }

    // ============================================
    // Dec Clamping Tests
    // ============================================

    @Test
    public void testDecClamping_OverRange() {
        // Dec > 90 should be clamped
        GeocentricCoords coords = GeocentricCoords.fromDegrees(0.0f, 100.0f);

        assertEquals(90.0f, coords.getDec(), EPSILON);
    }

    @Test
    public void testDecClamping_UnderRange() {
        // Dec < -90 should be clamped
        GeocentricCoords coords = GeocentricCoords.fromDegrees(0.0f, -100.0f);

        assertEquals(-90.0f, coords.getDec(), EPSILON);
    }

    @Test
    public void testDecClamping_AtBoundary() {
        GeocentricCoords north = GeocentricCoords.fromDegrees(0.0f, 90.0f);
        GeocentricCoords south = GeocentricCoords.fromDegrees(0.0f, -90.0f);

        assertEquals(90.0f, north.getDec(), EPSILON);
        assertEquals(-90.0f, south.getDec(), EPSILON);
    }

    // ============================================
    // Getter Tests
    // ============================================

    @Test
    public void testGetRaHours() {
        // 180 degrees = 12 hours
        GeocentricCoords coords = GeocentricCoords.fromDegrees(180.0f, 0.0f);

        assertEquals(12.0f, coords.getRaHours(), EPSILON);
    }

    @Test
    public void testGetRaRadians() {
        // 180 degrees = PI radians
        GeocentricCoords coords = GeocentricCoords.fromDegrees(180.0f, 0.0f);

        assertEquals((float) Math.PI, coords.getRaRadians(), EPSILON);
    }

    @Test
    public void testGetDecRadians() {
        // 90 degrees = PI/2 radians
        GeocentricCoords coords = GeocentricCoords.fromDegrees(0.0f, 90.0f);

        assertEquals((float) (Math.PI / 2), coords.getDecRadians(), EPSILON);
    }

    // ============================================
    // Vector3 Conversion Tests
    // ============================================

    @Test
    public void testToVector3_Origin() {
        // RA = 0, Dec = 0 -> (1, 0, 0)
        GeocentricCoords coords = GeocentricCoords.fromDegrees(0.0f, 0.0f);

        float[] vector = coords.toVector3();

        assertEquals(1.0f, vector[0], EPSILON); // x
        assertEquals(0.0f, vector[1], EPSILON); // y
        assertEquals(0.0f, vector[2], EPSILON); // z
    }

    @Test
    public void testToVector3_Ra90() {
        // RA = 90, Dec = 0 -> (0, 1, 0)
        GeocentricCoords coords = GeocentricCoords.fromDegrees(90.0f, 0.0f);

        float[] vector = coords.toVector3();

        assertEquals(0.0f, vector[0], EPSILON);
        assertEquals(1.0f, vector[1], EPSILON);
        assertEquals(0.0f, vector[2], EPSILON);
    }

    @Test
    public void testToVector3_NorthPole() {
        // RA = any, Dec = 90 -> (0, 0, 1)
        GeocentricCoords coords = GeocentricCoords.fromDegrees(0.0f, 90.0f);

        float[] vector = coords.toVector3();

        assertEquals(0.0f, vector[0], EPSILON);
        assertEquals(0.0f, vector[1], EPSILON);
        assertEquals(1.0f, vector[2], EPSILON);
    }

    @Test
    public void testToVector3_SouthPole() {
        // RA = any, Dec = -90 -> (0, 0, -1)
        GeocentricCoords coords = GeocentricCoords.fromDegrees(0.0f, -90.0f);

        float[] vector = coords.toVector3();

        assertEquals(0.0f, vector[0], EPSILON);
        assertEquals(0.0f, vector[1], EPSILON);
        assertEquals(-1.0f, vector[2], EPSILON);
    }

    @Test
    public void testToVector3_IsUnitVector() {
        GeocentricCoords coords = GeocentricCoords.fromDegrees(123.456f, 45.678f);

        float[] vector = coords.toVector3();

        // Calculate length
        float length = (float) Math.sqrt(
            vector[0] * vector[0] +
            vector[1] * vector[1] +
            vector[2] * vector[2]
        );

        assertEquals(1.0f, length, EPSILON);
    }

    @Test
    public void testToVector3_RoundTrip() {
        // Convert coordinates to vector and back
        GeocentricCoords original = GeocentricCoords.fromDegrees(45.0f, 30.0f);

        float[] vector = original.toVector3();
        GeocentricCoords recovered = GeocentricCoords.fromVector3(
            vector[0], vector[1], vector[2]
        );

        assertEquals(original.getRa(), recovered.getRa(), EPSILON);
        assertEquals(original.getDec(), recovered.getDec(), EPSILON);
    }

    // ============================================
    // Angular Distance Tests
    // ============================================

    @Test
    public void testAngularDistance_SamePoint() {
        GeocentricCoords coords = GeocentricCoords.fromDegrees(90.0f, 45.0f);

        float distance = coords.angularDistanceTo(coords);

        assertEquals(0.0f, distance, EPSILON);
    }

    @Test
    public void testAngularDistance_OppositePoints() {
        // Points 180 degrees apart
        GeocentricCoords north = GeocentricCoords.fromDegrees(0.0f, 90.0f);
        GeocentricCoords south = GeocentricCoords.fromDegrees(0.0f, -90.0f);

        float distance = north.angularDistanceTo(south);

        assertEquals(180.0f, distance, EPSILON);
    }

    @Test
    public void testAngularDistance_90DegreesApart() {
        // Points 90 degrees apart on equator
        GeocentricCoords p1 = GeocentricCoords.fromDegrees(0.0f, 0.0f);
        GeocentricCoords p2 = GeocentricCoords.fromDegrees(90.0f, 0.0f);

        float distance = p1.angularDistanceTo(p2);

        assertEquals(90.0f, distance, EPSILON);
    }

    @Test
    public void testAngularDistance_EquatorToNorthPole() {
        GeocentricCoords equator = GeocentricCoords.fromDegrees(0.0f, 0.0f);
        GeocentricCoords northPole = GeocentricCoords.fromDegrees(0.0f, 90.0f);

        float distance = equator.angularDistanceTo(northPole);

        assertEquals(90.0f, distance, EPSILON);
    }

    @Test
    public void testAngularDistance_IsCommutative() {
        GeocentricCoords p1 = GeocentricCoords.fromDegrees(30.0f, 40.0f);
        GeocentricCoords p2 = GeocentricCoords.fromDegrees(120.0f, -20.0f);

        float distance1 = p1.angularDistanceTo(p2);
        float distance2 = p2.angularDistanceTo(p1);

        assertEquals(distance1, distance2, EPSILON);
    }

    @Test
    public void testAngularDistance_SmallDistance() {
        // Two points very close together
        GeocentricCoords p1 = GeocentricCoords.fromDegrees(100.0f, 50.0f);
        GeocentricCoords p2 = GeocentricCoords.fromDegrees(100.1f, 50.1f);

        float distance = p1.angularDistanceTo(p2);

        // Should be a small but non-zero distance
        assertTrue(distance > 0.0f);
        assertTrue(distance < 1.0f);
    }

    @Test
    public void testAngularDistance_AcrossRaBoundary() {
        // Points on either side of RA=0/360 boundary
        GeocentricCoords p1 = GeocentricCoords.fromDegrees(350.0f, 0.0f);
        GeocentricCoords p2 = GeocentricCoords.fromDegrees(10.0f, 0.0f);

        float distance = p1.angularDistanceTo(p2);

        // Should be 20 degrees, not 340
        assertEquals(20.0f, distance, EPSILON);
    }

    // ============================================
    // Known Star Pair Distances Tests
    // ============================================

    @Test
    public void testAngularDistance_SiriusToPolaris() {
        // Sirius: RA = 101.287, Dec = -16.716
        // Polaris: RA = 37.954, Dec = 89.264
        GeocentricCoords sirius = GeocentricCoords.fromDegrees(101.287f, -16.716f);
        GeocentricCoords polaris = GeocentricCoords.fromDegrees(37.954f, 89.264f);

        float distance = sirius.angularDistanceTo(polaris);

        // Expected distance is about 106 degrees
        assertEquals(106.0f, distance, 1.0f);
    }

    @Test
    public void testAngularDistance_VegaToAltair() {
        // Vega: RA = 279.234, Dec = 38.784
        // Altair: RA = 297.695, Dec = 8.868
        GeocentricCoords vega = GeocentricCoords.fromDegrees(279.234f, 38.784f);
        GeocentricCoords altair = GeocentricCoords.fromDegrees(297.695f, 8.868f);

        float distance = vega.angularDistanceTo(altair);

        // Expected distance is about 34 degrees (Summer Triangle)
        assertEquals(34.0f, distance, 1.0f);
    }

    // ============================================
    // Equality Tests
    // ============================================

    @Test
    public void testEquals() {
        GeocentricCoords c1 = GeocentricCoords.fromDegrees(180.0f, 45.0f);
        GeocentricCoords c2 = GeocentricCoords.fromDegrees(180.0f, 45.0f);

        assertEquals(c1, c2);
    }

    @Test
    public void testNotEquals_DifferentRa() {
        GeocentricCoords c1 = GeocentricCoords.fromDegrees(180.0f, 45.0f);
        GeocentricCoords c2 = GeocentricCoords.fromDegrees(181.0f, 45.0f);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testNotEquals_DifferentDec() {
        GeocentricCoords c1 = GeocentricCoords.fromDegrees(180.0f, 45.0f);
        GeocentricCoords c2 = GeocentricCoords.fromDegrees(180.0f, 46.0f);

        assertNotEquals(c1, c2);
    }

    @Test
    public void testHashCodeConsistency() {
        GeocentricCoords c1 = GeocentricCoords.fromDegrees(180.0f, 45.0f);
        GeocentricCoords c2 = GeocentricCoords.fromDegrees(180.0f, 45.0f);

        assertEquals(c1.hashCode(), c2.hashCode());
    }

    // ============================================
    // ToString Tests
    // ============================================

    @Test
    public void testToString() {
        GeocentricCoords coords = GeocentricCoords.fromDegrees(180.0f, 45.0f);

        String str = coords.toString();

        assertTrue(str.contains("180"));
        assertTrue(str.contains("45"));
        assertTrue(str.contains("ra"));
        assertTrue(str.contains("dec"));
    }
}
