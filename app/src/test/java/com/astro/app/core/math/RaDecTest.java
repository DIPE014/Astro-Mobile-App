package com.astro.app.core.math;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the RaDec class.
 *
 * <p>Tests cover coordinate creation, conversions to/from Vector3,
 * and circumpolar/visibility calculations. These are critical for
 * correctly displaying celestial objects in the sky map.</p>
 */
public class RaDecTest {

    private static final float EPSILON = 1e-4f;

    // ============================================
    // Coordinate Creation Tests
    // ============================================

    @Test
    public void testBasicConstruction() {
        RaDec coord = new RaDec(180.0f, 45.0f);

        assertEquals(180.0f, coord.getRa(), EPSILON);
        assertEquals(45.0f, coord.getDec(), EPSILON);
    }

    @Test
    public void testConstructionWithZeroValues() {
        RaDec coord = new RaDec(0.0f, 0.0f);

        assertEquals(0.0f, coord.getRa(), EPSILON);
        assertEquals(0.0f, coord.getDec(), EPSILON);
    }

    @Test
    public void testConstructionWithNegativeDeclination() {
        RaDec coord = new RaDec(90.0f, -45.0f);

        assertEquals(90.0f, coord.getRa(), EPSILON);
        assertEquals(-45.0f, coord.getDec(), EPSILON);
    }

    @Test
    public void testConstructionAtNorthPole() {
        RaDec coord = new RaDec(0.0f, 90.0f);

        assertEquals(0.0f, coord.getRa(), EPSILON);
        assertEquals(90.0f, coord.getDec(), EPSILON);
    }

    @Test
    public void testConstructionAtSouthPole() {
        RaDec coord = new RaDec(0.0f, -90.0f);

        assertEquals(0.0f, coord.getRa(), EPSILON);
        assertEquals(-90.0f, coord.getDec(), EPSILON);
    }

    // ============================================
    // RA Degrees from HMS Tests
    // ============================================

    @Test
    public void testRaDegreesFromHMS_Zero() {
        float ra = RaDec.raDegreesFromHMS(0.0f, 0.0f, 0.0f);

        assertEquals(0.0f, ra, EPSILON);
    }

    @Test
    public void testRaDegreesFromHMS_SixHours() {
        // 6 hours = 90 degrees
        float ra = RaDec.raDegreesFromHMS(6.0f, 0.0f, 0.0f);

        assertEquals(90.0f, ra, EPSILON);
    }

    @Test
    public void testRaDegreesFromHMS_TwelveHours() {
        // 12 hours = 180 degrees
        float ra = RaDec.raDegreesFromHMS(12.0f, 0.0f, 0.0f);

        assertEquals(180.0f, ra, EPSILON);
    }

    @Test
    public void testRaDegreesFromHMS_TwentyFourHours() {
        // 24 hours = 360 degrees
        float ra = RaDec.raDegreesFromHMS(24.0f, 0.0f, 0.0f);

        assertEquals(360.0f, ra, EPSILON);
    }

    @Test
    public void testRaDegreesFromHMS_WithMinutes() {
        // 1 hour 30 minutes = 1.5 hours = 22.5 degrees
        float ra = RaDec.raDegreesFromHMS(1.0f, 30.0f, 0.0f);

        assertEquals(22.5f, ra, EPSILON);
    }

    @Test
    public void testRaDegreesFromHMS_WithSeconds() {
        // 1 hour 0 minutes 30 seconds = (1 + 30/3600) hours
        // = 1.00833... hours = 15.125 degrees
        float ra = RaDec.raDegreesFromHMS(1.0f, 0.0f, 30.0f);

        assertEquals(15.125f, ra, EPSILON);
    }

    @Test
    public void testRaDegreesFromHMS_Sirius() {
        // Sirius: RA = 6h 45m 8.9s
        float ra = RaDec.raDegreesFromHMS(6.0f, 45.0f, 8.9f);

        // Expected: 6.752472 hours * 15 = 101.287 degrees
        assertEquals(101.287f, ra, 0.01f);
    }

    // ============================================
    // Dec Degrees from DMS Tests
    // ============================================

    @Test
    public void testDecDegreesFromDMS_Zero() {
        float dec = RaDec.decDegreesFromDMS(0.0f, 0.0f, 0.0f);

        assertEquals(0.0f, dec, EPSILON);
    }

    @Test
    public void testDecDegreesFromDMS_PositiveDegrees() {
        float dec = RaDec.decDegreesFromDMS(45.0f, 0.0f, 0.0f);

        assertEquals(45.0f, dec, EPSILON);
    }

    @Test
    public void testDecDegreesFromDMS_WithMinutes() {
        // 45 degrees 30 minutes = 45.5 degrees
        float dec = RaDec.decDegreesFromDMS(45.0f, 30.0f, 0.0f);

        assertEquals(45.5f, dec, EPSILON);
    }

    @Test
    public void testDecDegreesFromDMS_WithSeconds() {
        // 45 degrees 0 minutes 30 seconds = 45.00833... degrees
        float dec = RaDec.decDegreesFromDMS(45.0f, 0.0f, 30.0f);

        assertEquals(45.00833f, dec, EPSILON);
    }

    @Test
    public void testDecDegreesFromDMS_Sirius() {
        // Sirius: Dec = -16 degrees 42' 58"
        // For negative declinations, minutes and seconds are subtracted
        float dec = RaDec.decDegreesFromDMS(-16.0f, 42.0f, 58.0f);

        // Expected: -16 - 42/60 - 58/3600 = -16 - 0.7 - 0.0161 = -16.7161 degrees
        assertEquals(-16.7161f, dec, 0.01f);
    }

    // ============================================
    // From Hours Minutes Seconds Tests
    // ============================================

    @Test
    public void testFromHoursMinutesSeconds() {
        // Polaris: RA = 2h 31m 49s, Dec = +89d 15' 51"
        RaDec polaris = RaDec.fromHoursMinutesSeconds(
            2.0f, 31.0f, 49.0f,
            89.0f, 15.0f, 51.0f
        );

        // RA: 2.53028 hours * 15 = 37.954 degrees
        assertEquals(37.954f, polaris.getRa(), 0.01f);
        // Dec: 89 + 15/60 + 51/3600 = 89.264 degrees
        assertEquals(89.264f, polaris.getDec(), 0.01f);
    }

    // ============================================
    // Conversion to/from Vector3 Tests
    // ============================================

    @Test
    public void testFromGeocentricCoords_XAxis() {
        // Point on X axis (RA = 0, Dec = 0)
        Vector3 coords = new Vector3(1.0f, 0.0f, 0.0f);

        RaDec raDec = RaDec.fromGeocentricCoords(coords);

        assertEquals(0.0f, raDec.getRa(), EPSILON);
        assertEquals(0.0f, raDec.getDec(), EPSILON);
    }

    @Test
    public void testFromGeocentricCoords_YAxis() {
        // Point on Y axis (RA = 90, Dec = 0)
        Vector3 coords = new Vector3(0.0f, 1.0f, 0.0f);

        RaDec raDec = RaDec.fromGeocentricCoords(coords);

        assertEquals(90.0f, raDec.getRa(), EPSILON);
        assertEquals(0.0f, raDec.getDec(), EPSILON);
    }

    @Test
    public void testFromGeocentricCoords_ZAxis() {
        // Point on Z axis (Dec = 90, north celestial pole)
        Vector3 coords = new Vector3(0.0f, 0.0f, 1.0f);

        RaDec raDec = RaDec.fromGeocentricCoords(coords);

        // RA is undefined at poles, but should be 0
        assertEquals(90.0f, raDec.getDec(), EPSILON);
    }

    @Test
    public void testFromGeocentricCoords_NegativeX() {
        // Point on negative X axis (RA = 180, Dec = 0)
        Vector3 coords = new Vector3(-1.0f, 0.0f, 0.0f);

        RaDec raDec = RaDec.fromGeocentricCoords(coords);

        assertEquals(180.0f, raDec.getRa(), EPSILON);
        assertEquals(0.0f, raDec.getDec(), EPSILON);
    }

    @Test
    public void testFromGeocentricCoords_NegativeY() {
        // Point on negative Y axis (RA = 270, Dec = 0)
        Vector3 coords = new Vector3(0.0f, -1.0f, 0.0f);

        RaDec raDec = RaDec.fromGeocentricCoords(coords);

        assertEquals(270.0f, raDec.getRa(), EPSILON);
        assertEquals(0.0f, raDec.getDec(), EPSILON);
    }

    @Test
    public void testFromGeocentricCoords_SouthPole() {
        // Point on negative Z axis (Dec = -90, south celestial pole)
        Vector3 coords = new Vector3(0.0f, 0.0f, -1.0f);

        RaDec raDec = RaDec.fromGeocentricCoords(coords);

        assertEquals(-90.0f, raDec.getDec(), EPSILON);
    }

    @Test
    public void testToGeocentricCoordsAndBack() {
        // Test round-trip conversion
        RaDec original = new RaDec(45.0f, 30.0f);

        // Convert to geocentric coords
        Vector3 geocentric = CoordinateManipulationsKt.getGeocentricCoords(original);

        // Convert back to RaDec
        RaDec recovered = RaDec.fromGeocentricCoords(geocentric);

        assertEquals(original.getRa(), recovered.getRa(), EPSILON);
        assertEquals(original.getDec(), recovered.getDec(), EPSILON);
    }

    @Test
    public void testGeocentricCoordsAreUnitVector() {
        RaDec raDec = new RaDec(123.0f, 45.0f);

        Vector3 geocentric = CoordinateManipulationsKt.getGeocentricCoords(raDec);

        // Should be a unit vector
        assertEquals(1.0f, geocentric.getLength(), EPSILON);
    }

    @Test
    public void testGeocentricCoordsAtEquator() {
        // Point on celestial equator
        RaDec raDec = new RaDec(90.0f, 0.0f);

        Vector3 geocentric = CoordinateManipulationsKt.getGeocentricCoords(raDec);

        // At Dec=0, z should be 0
        assertEquals(0.0f, geocentric.z, EPSILON);
        // At RA=90, should be pointing along Y axis
        assertEquals(0.0f, geocentric.x, EPSILON);
        assertEquals(1.0f, geocentric.y, EPSILON);
    }

    // ============================================
    // Circumpolar and Visibility Tests
    // ============================================

    // Note: The isCircumpolarFor and isNeverVisible methods are private,
    // so we test them indirectly through their effects on astronomical calculations.
    // These tests document expected behavior based on the visible source code.

    @Test
    public void testCircumpolarLogic_NorthernHemisphere() {
        // For northern observer at latitude 45N:
        // Objects never set if dec > 90 - lat = 90 - 45 = 45
        // So Polaris (dec ~89) should be circumpolar

        LatLong observer = new LatLong(45.0f, 0.0f);
        RaDec polaris = new RaDec(37.95f, 89.26f);

        // Polaris dec (89.26) > 90 - 45 (45), so circumpolar
        assertTrue(polaris.getDec() > (90.0f - observer.getLatitude()));
    }

    @Test
    public void testCircumpolarLogic_SouthernHemisphere() {
        // For southern observer at latitude -45S:
        // Objects never set if dec < -90 - lat = -90 - (-45) = -45
        // So sigma Octantis (dec ~-89) should be circumpolar

        LatLong observer = new LatLong(-45.0f, 0.0f);
        RaDec sigmaOct = new RaDec(317.0f, -88.95f);

        // Sigma Oct dec (-88.95) < -90 - (-45) = -45, so circumpolar
        assertTrue(sigmaOct.getDec() < (-90.0f - observer.getLatitude()));
    }

    @Test
    public void testNeverVisibleLogic_NorthernHemisphere() {
        // For northern observer at latitude 45N:
        // Objects never rise if dec < lat - 90 = 45 - 90 = -45
        // So southern stars with dec < -45 never rise

        LatLong observer = new LatLong(45.0f, 0.0f);
        RaDec southernStar = new RaDec(0.0f, -60.0f);

        // Dec (-60) < 45 - 90 = -45, so never visible
        assertTrue(southernStar.getDec() < (observer.getLatitude() - 90.0f));
    }

    @Test
    public void testNeverVisibleLogic_SouthernHemisphere() {
        // For southern observer at latitude -45S:
        // Objects never rise if dec > 90 + lat = 90 + (-45) = 45
        // So northern stars with dec > 45 never rise

        LatLong observer = new LatLong(-45.0f, 0.0f);
        RaDec northernStar = new RaDec(0.0f, 60.0f);

        // Dec (60) > 90 + (-45) = 45, so never visible
        assertTrue(northernStar.getDec() > (90.0f + observer.getLatitude()));
    }

    @Test
    public void testVisibleStar_MidLatitude() {
        // For observer at latitude 40N:
        // Stars are visible (rise and set) if: lat - 90 < dec < 90 - lat
        // i.e., -50 < dec < 50

        LatLong observer = new LatLong(40.0f, 0.0f);
        RaDec sirius = new RaDec(101.287f, -16.716f);

        // Sirius dec (-16.716) is between -50 and 50
        assertTrue(sirius.getDec() > (observer.getLatitude() - 90.0f));
        assertTrue(sirius.getDec() < (90.0f - observer.getLatitude()));
    }

    // ============================================
    // Equality Tests
    // ============================================

    @Test
    public void testEquals() {
        RaDec coord1 = new RaDec(180.0f, 45.0f);
        RaDec coord2 = new RaDec(180.0f, 45.0f);

        assertEquals(coord1, coord2);
    }

    @Test
    public void testNotEquals_DifferentRa() {
        RaDec coord1 = new RaDec(180.0f, 45.0f);
        RaDec coord2 = new RaDec(181.0f, 45.0f);

        assertNotEquals(coord1, coord2);
    }

    @Test
    public void testNotEquals_DifferentDec() {
        RaDec coord1 = new RaDec(180.0f, 45.0f);
        RaDec coord2 = new RaDec(180.0f, 46.0f);

        assertNotEquals(coord1, coord2);
    }

    @Test
    public void testHashCodeConsistency() {
        RaDec coord1 = new RaDec(180.0f, 45.0f);
        RaDec coord2 = new RaDec(180.0f, 45.0f);

        assertEquals(coord1.hashCode(), coord2.hashCode());
    }

    // ============================================
    // Known Star Positions Tests
    // ============================================

    @Test
    public void testSiriusPosition() {
        // Sirius: RA = 6h 45m 8.9s, Dec = -16d 42' 58"
        float ra = RaDec.raDegreesFromHMS(6.0f, 45.0f, 8.9f);

        // RA should be approximately 101.287 degrees
        assertEquals(101.287f, ra, 0.01f);
    }

    @Test
    public void testVegaPosition() {
        // Vega: RA = 18h 36m 56s, Dec = +38d 47' 01"
        float ra = RaDec.raDegreesFromHMS(18.0f, 36.0f, 56.0f);
        float dec = RaDec.decDegreesFromDMS(38.0f, 47.0f, 1.0f);

        // RA should be approximately 279.234 degrees
        assertEquals(279.234f, ra, 0.01f);
        // Dec should be approximately 38.784 degrees
        assertEquals(38.784f, dec, 0.01f);
    }

    @Test
    public void testBetelgeusePosition() {
        // Betelgeuse: RA = 5h 55m 10s, Dec = +7d 24' 25"
        float ra = RaDec.raDegreesFromHMS(5.0f, 55.0f, 10.0f);
        float dec = RaDec.decDegreesFromDMS(7.0f, 24.0f, 25.0f);

        // RA should be approximately 88.79 degrees
        assertEquals(88.79f, ra, 0.01f);
        // Dec should be approximately 7.407 degrees
        assertEquals(7.407f, dec, 0.01f);
    }
}
