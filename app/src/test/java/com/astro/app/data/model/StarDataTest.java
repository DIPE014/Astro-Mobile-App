package com.astro.app.data.model;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit tests for the StarData class.
 *
 * <p>Tests cover the builder pattern, spectral color mapping,
 * and naked eye visibility calculations which are essential for
 * correctly displaying stars in the sky map.</p>
 */
public class StarDataTest {

    private static final float EPSILON = 1e-5f;

    // ============================================
    // Builder Pattern Tests
    // ============================================

    @Test
    public void testBuilder_MinimalStar() {
        StarData star = StarData.builder()
            .setId("test-star")
            .setName("Test Star")
            .build();

        assertEquals("test-star", star.getId());
        assertEquals("Test Star", star.getName());
        assertEquals(0.0f, star.getMagnitude(), EPSILON);
    }

    @Test
    public void testBuilder_AllFields() {
        StarData star = StarData.builder()
            .setId("sirius")
            .setName("Sirius")
            .setRa(101.287f)
            .setDec(-16.716f)
            .setMagnitude(-1.46f)
            .setSpectralType("A1V")
            .setDistance(8.6f)
            .setConstellationId("CMa")
            .setSize(8)
            .setColor(0xFFAABFFF)
            .build();

        assertEquals("sirius", star.getId());
        assertEquals("Sirius", star.getName());
        assertEquals(101.287f, star.getRa(), EPSILON);
        assertEquals(-16.716f, star.getDec(), EPSILON);
        assertEquals(-1.46f, star.getMagnitude(), EPSILON);
        assertEquals("A1V", star.getSpectralType());
        assertEquals(8.6f, star.getDistance(), EPSILON);
        assertEquals("CMa", star.getConstellationId());
        assertEquals(8, star.getSize());
        assertEquals(0xFFAABFFF, star.getColor());
    }

    @Test
    public void testBuilder_MethodChaining() {
        // Verify that builder methods return the builder for chaining
        StarData.Builder builder = StarData.builder()
            .setId("star")
            .setName("Star")
            .setRa(0.0f)
            .setDec(0.0f)
            .setMagnitude(1.0f)
            .setSpectralType("G2V")
            .setDistance(100.0f)
            .setConstellationId("Ori")
            .setSize(3)
            .setColor(0xFFFFFFFF);

        assertNotNull(builder);
        assertNotNull(builder.build());
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilder_MissingId() {
        StarData.builder()
            .setName("No Id Star")
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilder_MissingName() {
        StarData.builder()
            .setId("no-name")
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilder_EmptyId() {
        StarData.builder()
            .setId("")
            .setName("Star")
            .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilder_EmptyName() {
        StarData.builder()
            .setId("star-id")
            .setName("")
            .build();
    }

    @Test
    public void testBuilder_WithAlternateNames() {
        StarData star = StarData.builder()
            .setId("sirius")
            .setName("Sirius")
            .addAlternateName("Alpha Canis Majoris")
            .addAlternateName("HD 48915")
            .build();

        assertEquals(2, star.getAlternateNames().size());
        assertTrue(star.getAlternateNames().contains("Alpha Canis Majoris"));
        assertTrue(star.getAlternateNames().contains("HD 48915"));
    }

    @Test
    public void testBuilder_SetAlternateNamesList() {
        StarData star = StarData.builder()
            .setId("vega")
            .setName("Vega")
            .setAlternateNames(Arrays.asList("Alpha Lyrae", "HD 172167"))
            .build();

        assertEquals(2, star.getAlternateNames().size());
        assertTrue(star.hasAlternateNames());
    }

    @Test
    public void testBuilder_WithCoordinates() {
        GeocentricCoords coords = GeocentricCoords.fromDegrees(101.287f, -16.716f);
        StarData star = StarData.builder()
            .setId("star")
            .setName("Star")
            .setCoordinates(coords)
            .build();

        assertEquals(101.287f, star.getRa(), EPSILON);
        assertEquals(-16.716f, star.getDec(), EPSILON);
    }

    @Test
    public void testBuilder_CalculateSizeFromMagnitude_Bright() {
        StarData star = StarData.builder()
            .setId("bright")
            .setName("Bright Star")
            .setMagnitude(-1.0f)
            .calculateSizeFromMagnitude()
            .build();

        // Brighter stars should have larger sizes
        assertTrue(star.getSize() >= 7);
    }

    @Test
    public void testBuilder_CalculateSizeFromMagnitude_Dim() {
        StarData star = StarData.builder()
            .setId("dim")
            .setName("Dim Star")
            .setMagnitude(6.0f)
            .calculateSizeFromMagnitude()
            .build();

        // Dimmer stars should have smaller sizes
        assertTrue(star.getSize() <= 2);
    }

    @Test
    public void testBuilder_CalculateSizeFromMagnitude_Medium() {
        StarData star = StarData.builder()
            .setId("medium")
            .setName("Medium Star")
            .setMagnitude(2.5f)
            .calculateSizeFromMagnitude()
            .build();

        // Medium brightness should have medium size
        assertTrue(star.getSize() >= 3 && star.getSize() <= 5);
    }

    // ============================================
    // Magnitude Tests
    // ============================================

    @Test
    public void testMagnitude_Sirius() {
        StarData sirius = StarData.builder()
            .setId("sirius")
            .setName("Sirius")
            .setMagnitude(-1.46f)
            .build();

        assertEquals(-1.46f, sirius.getMagnitude(), EPSILON);
    }

    @Test
    public void testMagnitude_Vega() {
        // Vega is the reference star (magnitude 0)
        StarData vega = StarData.builder()
            .setId("vega")
            .setName("Vega")
            .setMagnitude(0.0f)
            .build();

        assertEquals(0.0f, vega.getMagnitude(), EPSILON);
    }

    @Test
    public void testMagnitude_DimStar() {
        StarData dimStar = StarData.builder()
            .setId("dim")
            .setName("Dim Star")
            .setMagnitude(6.5f)
            .build();

        assertEquals(6.5f, dimStar.getMagnitude(), EPSILON);
    }

    // ============================================
    // Spectral Color Mapping Tests
    // ============================================

    @Test
    public void testSpectralColor_TypeO() {
        StarData star = StarData.builder()
            .setId("o-star")
            .setName("O-Type Star")
            .setSpectralType("O5")
            .build();

        int color = star.getSpectralColor();

        // O-type stars are blue
        assertEquals(0xFF9BB0FF, color);
    }

    @Test
    public void testSpectralColor_TypeB() {
        StarData star = StarData.builder()
            .setId("b-star")
            .setName("B-Type Star")
            .setSpectralType("B2")
            .build();

        int color = star.getSpectralColor();

        // B-type stars are blue-white
        assertEquals(0xFFAABFFF, color);
    }

    @Test
    public void testSpectralColor_TypeA() {
        StarData star = StarData.builder()
            .setId("a-star")
            .setName("A-Type Star")
            .setSpectralType("A1V")
            .build();

        int color = star.getSpectralColor();

        // A-type stars are white
        assertEquals(0xFFCAD7FF, color);
    }

    @Test
    public void testSpectralColor_TypeF() {
        StarData star = StarData.builder()
            .setId("f-star")
            .setName("F-Type Star")
            .setSpectralType("F5")
            .build();

        int color = star.getSpectralColor();

        // F-type stars are yellow-white
        assertEquals(0xFFF8F7FF, color);
    }

    @Test
    public void testSpectralColor_TypeG() {
        StarData star = StarData.builder()
            .setId("g-star")
            .setName("G-Type Star")
            .setSpectralType("G2V")
            .build();

        int color = star.getSpectralColor();

        // G-type stars are yellow (like our Sun)
        assertEquals(0xFFFFF4EA, color);
    }

    @Test
    public void testSpectralColor_TypeK() {
        StarData star = StarData.builder()
            .setId("k-star")
            .setName("K-Type Star")
            .setSpectralType("K5")
            .build();

        int color = star.getSpectralColor();

        // K-type stars are orange
        assertEquals(0xFFFFD2A1, color);
    }

    @Test
    public void testSpectralColor_TypeM() {
        StarData star = StarData.builder()
            .setId("m-star")
            .setName("M-Type Star")
            .setSpectralType("M0")
            .build();

        int color = star.getSpectralColor();

        // M-type stars are orange-red
        assertEquals(0xFFFFCC6F, color);
    }

    @Test
    public void testSpectralColor_NoType() {
        StarData star = StarData.builder()
            .setId("unknown")
            .setName("Unknown Star")
            .setColor(0xFFFFFFFF)
            .build();

        int color = star.getSpectralColor();

        // No spectral type should return default color
        assertEquals(0xFFFFFFFF, color);
    }

    @Test
    public void testSpectralColor_EmptyType() {
        StarData star = StarData.builder()
            .setId("empty")
            .setName("Empty Type Star")
            .setSpectralType("")
            .setColor(0xFFFFFFFF)
            .build();

        int color = star.getSpectralColor();

        // Empty spectral type should return default color
        assertEquals(0xFFFFFFFF, color);
    }

    @Test
    public void testSpectralColor_UnknownType() {
        StarData star = StarData.builder()
            .setId("x-star")
            .setName("X-Type Star")
            .setSpectralType("X9")
            .setColor(0xFFFFFFFF)
            .build();

        int color = star.getSpectralColor();

        // Unknown spectral type should return default color
        assertEquals(0xFFFFFFFF, color);
    }

    // ============================================
    // Naked Eye Visibility Tests
    // ============================================

    @Test
    public void testNakedEyeVisibility_VeryBright() {
        StarData sirius = StarData.builder()
            .setId("sirius")
            .setName("Sirius")
            .setMagnitude(-1.46f)
            .build();

        assertTrue(sirius.isNakedEyeVisible());
    }

    @Test
    public void testNakedEyeVisibility_Bright() {
        StarData vega = StarData.builder()
            .setId("vega")
            .setName("Vega")
            .setMagnitude(0.0f)
            .build();

        assertTrue(vega.isNakedEyeVisible());
    }

    @Test
    public void testNakedEyeVisibility_AtLimit() {
        StarData limitStar = StarData.builder()
            .setId("limit")
            .setName("Limit Star")
            .setMagnitude(6.0f)
            .build();

        assertTrue(limitStar.isNakedEyeVisible());
    }

    @Test
    public void testNakedEyeVisibility_JustBeyondLimit() {
        StarData dimStar = StarData.builder()
            .setId("dim")
            .setName("Dim Star")
            .setMagnitude(6.1f)
            .build();

        assertFalse(dimStar.isNakedEyeVisible());
    }

    @Test
    public void testNakedEyeVisibility_VeryDim() {
        StarData veryDim = StarData.builder()
            .setId("very-dim")
            .setName("Very Dim Star")
            .setMagnitude(10.0f)
            .build();

        assertFalse(veryDim.isNakedEyeVisible());
    }

    // ============================================
    // Luminosity Ratio Tests
    // ============================================

    @Test
    public void testLuminosityRatio_SameMagnitude() {
        StarData star = StarData.builder()
            .setId("star")
            .setName("Star")
            .setMagnitude(1.0f)
            .build();

        float ratio = star.getLuminosityRatio(1.0f);

        assertEquals(1.0f, ratio, EPSILON);
    }

    @Test
    public void testLuminosityRatio_OneMagnitudeBrighter() {
        StarData star = StarData.builder()
            .setId("star")
            .setName("Star")
            .setMagnitude(0.0f)
            .build();

        float ratio = star.getLuminosityRatio(1.0f);

        // One magnitude difference = 2.512x brightness
        assertEquals(2.512f, ratio, 0.01f);
    }

    @Test
    public void testLuminosityRatio_OneMagnitudeDimmer() {
        StarData star = StarData.builder()
            .setId("star")
            .setName("Star")
            .setMagnitude(2.0f)
            .build();

        float ratio = star.getLuminosityRatio(1.0f);

        // One magnitude dimmer = 1/2.512 brightness
        assertEquals(0.398f, ratio, 0.01f);
    }

    @Test
    public void testLuminosityRatio_FiveMagnitudeDifference() {
        StarData star = StarData.builder()
            .setId("star")
            .setName("Star")
            .setMagnitude(0.0f)
            .build();

        float ratio = star.getLuminosityRatio(5.0f);

        // Five magnitude difference = exactly 100x brightness
        assertEquals(100.0f, ratio, 0.1f);
    }

    // ============================================
    // Distance Tests
    // ============================================

    @Test
    public void testDistance_Known() {
        StarData sirius = StarData.builder()
            .setId("sirius")
            .setName("Sirius")
            .setDistance(8.6f)
            .build();

        assertEquals(8.6f, sirius.getDistance(), EPSILON);
        assertTrue(sirius.hasKnownDistance());
    }

    @Test
    public void testDistance_Unknown() {
        StarData unknown = StarData.builder()
            .setId("unknown")
            .setName("Unknown Distance Star")
            .build();

        assertEquals(StarData.DISTANCE_UNKNOWN, unknown.getDistance(), EPSILON);
        assertFalse(unknown.hasKnownDistance());
    }

    @Test
    public void testDistance_ExplicitlyUnknown() {
        StarData unknown = StarData.builder()
            .setId("unknown")
            .setName("Unknown Distance Star")
            .setDistance(StarData.DISTANCE_UNKNOWN)
            .build();

        assertFalse(unknown.hasKnownDistance());
    }

    @Test
    public void testDistance_Zero() {
        StarData star = StarData.builder()
            .setId("zero")
            .setName("Zero Distance Star")
            .setDistance(0.0f)
            .build();

        // Zero distance is not considered "known"
        assertFalse(star.hasKnownDistance());
    }

    // ============================================
    // Constellation Tests
    // ============================================

    @Test
    public void testConstellation_Assigned() {
        StarData betelgeuse = StarData.builder()
            .setId("betelgeuse")
            .setName("Betelgeuse")
            .setConstellationId("Ori")
            .build();

        assertEquals("Ori", betelgeuse.getConstellationId());
        assertTrue(betelgeuse.hasConstellation());
    }

    @Test
    public void testConstellation_NotAssigned() {
        StarData star = StarData.builder()
            .setId("star")
            .setName("Star")
            .build();

        assertNull(star.getConstellationId());
        assertFalse(star.hasConstellation());
    }

    @Test
    public void testConstellation_EmptyString() {
        StarData star = StarData.builder()
            .setId("star")
            .setName("Star")
            .setConstellationId("")
            .build();

        assertFalse(star.hasConstellation());
    }

    // ============================================
    // Coordinate Tests
    // ============================================

    @Test
    public void testGetCoordinates() {
        StarData star = StarData.builder()
            .setId("star")
            .setName("Star")
            .setRa(101.287f)
            .setDec(-16.716f)
            .build();

        GeocentricCoords coords = star.getCoordinates();

        assertEquals(101.287f, coords.getRa(), EPSILON);
        assertEquals(-16.716f, coords.getDec(), EPSILON);
    }

    @Test
    public void testAngularDistanceTo() {
        StarData sirius = StarData.builder()
            .setId("sirius")
            .setName("Sirius")
            .setRa(101.287f)
            .setDec(-16.716f)
            .build();

        StarData canopus = StarData.builder()
            .setId("canopus")
            .setName("Canopus")
            .setRa(95.988f)
            .setDec(-52.696f)
            .build();

        float distance = sirius.angularDistanceTo(canopus);

        // Expected distance is about 36 degrees
        assertEquals(36.0f, distance, 1.0f);
    }

    // ============================================
    // Equality Tests
    // ============================================

    @Test
    public void testEquals_SameId() {
        StarData star1 = StarData.builder()
            .setId("star")
            .setName("Star 1")
            .setMagnitude(1.0f)
            .build();

        StarData star2 = StarData.builder()
            .setId("star")
            .setName("Star 2")
            .setMagnitude(2.0f)
            .build();

        // Equality is based on ID
        assertEquals(star1, star2);
    }

    @Test
    public void testNotEquals_DifferentId() {
        StarData star1 = StarData.builder()
            .setId("star1")
            .setName("Star")
            .build();

        StarData star2 = StarData.builder()
            .setId("star2")
            .setName("Star")
            .build();

        assertNotEquals(star1, star2);
    }

    @Test
    public void testHashCodeConsistency() {
        StarData star1 = StarData.builder()
            .setId("star")
            .setName("Star")
            .build();

        StarData star2 = StarData.builder()
            .setId("star")
            .setName("Star")
            .build();

        assertEquals(star1.hashCode(), star2.hashCode());
    }

    // ============================================
    // ToString Tests
    // ============================================

    @Test
    public void testToString() {
        StarData sirius = StarData.builder()
            .setId("sirius")
            .setName("Sirius")
            .setRa(101.287f)
            .setDec(-16.716f)
            .setMagnitude(-1.46f)
            .setSpectralType("A1V")
            .build();

        String str = sirius.toString();

        assertTrue(str.contains("sirius"));
        assertTrue(str.contains("Sirius"));
        assertTrue(str.contains("101.287"));
        assertTrue(str.contains("-16.716"));
        assertTrue(str.contains("-1.46"));
        assertTrue(str.contains("A1V"));
    }

    // ============================================
    // Known Star Tests
    // ============================================

    @Test
    public void testKnownStar_Sirius() {
        StarData sirius = StarData.builder()
            .setId("sirius")
            .setName("Sirius")
            .addAlternateName("Alpha Canis Majoris")
            .setRa(101.287f)
            .setDec(-16.716f)
            .setMagnitude(-1.46f)
            .setSpectralType("A1V")
            .setDistance(8.6f)
            .setConstellationId("CMa")
            .calculateSizeFromMagnitude()
            .build();

        assertEquals("sirius", sirius.getId());
        assertEquals("Sirius", sirius.getName());
        assertTrue(sirius.getAlternateNames().contains("Alpha Canis Majoris"));
        assertEquals(101.287f, sirius.getRa(), EPSILON);
        assertEquals(-16.716f, sirius.getDec(), EPSILON);
        assertEquals(-1.46f, sirius.getMagnitude(), EPSILON);
        assertEquals("A1V", sirius.getSpectralType());
        assertEquals(8.6f, sirius.getDistance(), EPSILON);
        assertEquals("CMa", sirius.getConstellationId());
        assertTrue(sirius.isNakedEyeVisible());
        assertTrue(sirius.hasKnownDistance());
        assertTrue(sirius.hasConstellation());
        assertEquals(0xFFCAD7FF, sirius.getSpectralColor()); // A-type white color
    }

    @Test
    public void testKnownStar_Sun() {
        StarData sun = StarData.builder()
            .setId("sun")
            .setName("Sun")
            .addAlternateName("Sol")
            .setMagnitude(-26.74f)
            .setSpectralType("G2V")
            .setDistance(0.0000158f) // ~1 AU in light years
            .calculateSizeFromMagnitude()
            .build();

        assertTrue(sun.isNakedEyeVisible());
        assertEquals(0xFFFFF4EA, sun.getSpectralColor()); // G-type yellow
    }

    @Test
    public void testKnownStar_Polaris() {
        StarData polaris = StarData.builder()
            .setId("polaris")
            .setName("Polaris")
            .addAlternateName("North Star")
            .addAlternateName("Alpha Ursae Minoris")
            .setRa(37.954f)
            .setDec(89.264f)
            .setMagnitude(1.98f)
            .setSpectralType("F7")
            .setDistance(433.0f)
            .setConstellationId("UMi")
            .build();

        assertEquals(89.264f, polaris.getDec(), EPSILON);
        assertTrue(polaris.isNakedEyeVisible());
        assertEquals(0xFFF8F7FF, polaris.getSpectralColor()); // F-type yellow-white
    }

    @Test
    public void testKnownStar_Betelgeuse() {
        StarData betelgeuse = StarData.builder()
            .setId("betelgeuse")
            .setName("Betelgeuse")
            .addAlternateName("Alpha Orionis")
            .setRa(88.79f)
            .setDec(7.407f)
            .setMagnitude(0.42f)
            .setSpectralType("M1")
            .setDistance(700.0f)
            .setConstellationId("Ori")
            .build();

        assertTrue(betelgeuse.isNakedEyeVisible());
        assertEquals(0xFFFFCC6F, betelgeuse.getSpectralColor()); // M-type orange-red
    }
}
