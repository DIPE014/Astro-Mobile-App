package com.astro.app.core.math;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Unit tests for the TimeUtils functions.
 *
 * <p>Tests cover Julian day calculations and sidereal time calculations
 * which are fundamental to determining the positions of celestial objects
 * at any given time and location.</p>
 */
public class TimeUtilsTest {

    private static final double EPSILON = 1e-5;
    private static final float FLOAT_EPSILON = 1e-4f;

    // ============================================
    // Julian Day Calculation Tests
    // ============================================

    @Test
    public void testJulianDay_J2000Epoch() {
        // J2000.0 epoch: January 1, 2000 at 12:00 UT
        // Julian Day should be 2451545.0
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2000, Calendar.JANUARY, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date j2000 = cal.getTime();

        double jd = TimeUtilsKt.julianDay(j2000);

        assertEquals(2451545.0, jd, 0.001);
    }

    @Test
    public void testJulianDay_January1_2000_Midnight() {
        // January 1, 2000 at 00:00 UT
        // Julian Day should be 2451544.5
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        double jd = TimeUtilsKt.julianDay(date);

        assertEquals(2451544.5, jd, 0.001);
    }

    @Test
    public void testJulianDay_UnixEpoch() {
        // Unix Epoch: January 1, 1970 00:00:00 UT
        // Julian Day should be 2440587.5
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date unixEpoch = cal.getTime();

        double jd = TimeUtilsKt.julianDay(unixEpoch);

        assertEquals(2440587.5, jd, 0.001);
    }

    @Test
    public void testJulianDay_July4_2000() {
        // July 4, 2000 at 12:00 UT
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2000, Calendar.JULY, 4, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        double jd = TimeUtilsKt.julianDay(date);

        // Expected: 2451730.0 (approximately)
        assertEquals(2451730.0, jd, 0.001);
    }

    @Test
    public void testJulianDay_WithHoursMinutesSeconds() {
        // January 1, 2000 at 06:00 UT (quarter day)
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2000, Calendar.JANUARY, 1, 6, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        double jd = TimeUtilsKt.julianDay(date);

        // Should be 0.25 days after midnight
        assertEquals(2451544.75, jd, 0.001);
    }

    @Test
    public void testJulianDay_IncreasesByOnePerDay() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2000, Calendar.JANUARY, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date day1 = cal.getTime();

        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date day2 = cal.getTime();

        double jd1 = TimeUtilsKt.julianDay(day1);
        double jd2 = TimeUtilsKt.julianDay(day2);

        assertEquals(1.0, jd2 - jd1, EPSILON);
    }

    @Test
    public void testJulianDay_LeapYear() {
        // February 29, 2000 (leap year)
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2000, Calendar.FEBRUARY, 29, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        double jd = TimeUtilsKt.julianDay(date);

        // This should be a valid date in a leap year
        assertTrue(jd > 2451545.0); // After J2000
        assertTrue(jd < 2451700.0); // Before mid-2000
    }

    // ============================================
    // Julian Centuries Tests
    // ============================================

    @Test
    public void testJulianCenturies_AtJ2000() {
        // At J2000.0, julian centuries should be 0
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2000, Calendar.JANUARY, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date j2000 = cal.getTime();

        double jc = TimeUtilsKt.julianCenturies(j2000);

        assertEquals(0.0, jc, 0.001);
    }

    @Test
    public void testJulianCenturies_OneYearAfterJ2000() {
        // One year after J2000: ~0.01 centuries
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2001, Calendar.JANUARY, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        double jc = TimeUtilsKt.julianCenturies(date);

        // ~365.25 days / 36525 days per century = ~0.01
        assertEquals(0.01, jc, 0.001);
    }

    @Test
    public void testJulianCenturies_TenYearsAfterJ2000() {
        // Ten years after J2000: ~0.1 centuries
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2010, Calendar.JANUARY, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        double jc = TimeUtilsKt.julianCenturies(date);

        assertEquals(0.1, jc, 0.001);
    }

    @Test
    public void testJulianCenturies_BeforeJ2000() {
        // One year before J2000: ~-0.01 centuries
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(1999, Calendar.JANUARY, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        double jc = TimeUtilsKt.julianCenturies(date);

        // Should be negative (before J2000)
        assertTrue(jc < 0);
        assertEquals(-0.01, jc, 0.001);
    }

    // ============================================
    // Gregorian Date Conversion Tests
    // ============================================

    @Test
    public void testGregorianDate_FromJ2000() {
        // Convert J2000.0 back to Gregorian date
        double jd = 2451545.0; // J2000.0

        Date date = TimeUtilsKt.gregorianDate(jd);
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.setTime(date);

        assertEquals(2000, cal.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testGregorianDate_RoundTrip() {
        // Round trip: Date -> Julian Day -> Date
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2020, Calendar.JUNE, 15, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date original = cal.getTime();

        double jd = TimeUtilsKt.julianDay(original);
        Date recovered = TimeUtilsKt.gregorianDate(jd);

        Calendar recoveredCal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        recoveredCal.setTime(recovered);

        assertEquals(2020, recoveredCal.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, recoveredCal.get(Calendar.MONTH));
        assertEquals(15, recoveredCal.get(Calendar.DAY_OF_MONTH));
    }

    // ============================================
    // Mean Sidereal Time Tests
    // ============================================

    @Test
    public void testMeanSiderealTime_AtGreenwich() {
        // At Greenwich (longitude 0), LST = GST
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2000, Calendar.JANUARY, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date j2000 = cal.getTime();

        float lst = TimeUtilsKt.meanSiderealTime(j2000, 0.0f);

        // At J2000.0, GST should be approximately 280.46 degrees
        assertEquals(280.46f, lst, 0.1f);
    }

    @Test
    public void testMeanSiderealTime_EastLongitude() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2000, Calendar.JANUARY, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        float lstGreenwich = TimeUtilsKt.meanSiderealTime(date, 0.0f);
        float lstEast90 = TimeUtilsKt.meanSiderealTime(date, 90.0f);

        // LST at 90E should be 90 degrees more than at Greenwich
        float diff = lstEast90 - lstGreenwich;
        // Normalize to 0-360 range
        if (diff < 0) diff += 360;
        assertEquals(90.0f, diff, 0.1f);
    }

    @Test
    public void testMeanSiderealTime_WestLongitude() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2000, Calendar.JANUARY, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        float lstGreenwich = TimeUtilsKt.meanSiderealTime(date, 0.0f);
        float lstWest90 = TimeUtilsKt.meanSiderealTime(date, -90.0f);

        // LST at 90W should be 90 degrees less than at Greenwich
        float diff = lstGreenwich - lstWest90;
        // Normalize
        if (diff < 0) diff += 360;
        assertEquals(90.0f, diff, 0.1f);
    }

    @Test
    public void testMeanSiderealTime_IncreasesDuringDay() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date midnight = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY, 12);
        Date noon = cal.getTime();

        float lstMidnight = TimeUtilsKt.meanSiderealTime(midnight, 0.0f);
        float lstNoon = TimeUtilsKt.meanSiderealTime(noon, 0.0f);

        // Over 12 hours, sidereal time increases by about 180.5 degrees
        // (slightly more than 180 because sidereal day is shorter)
        float diff = lstNoon - lstMidnight;
        if (diff < 0) diff += 360;
        assertTrue(diff > 180.0f);
        assertTrue(diff < 182.0f);
    }

    @Test
    public void testMeanSiderealTime_AlwaysPositive() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2010, Calendar.MARCH, 15, 6, 30, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        // Test at various longitudes
        float[] longitudes = {-180.0f, -90.0f, 0.0f, 90.0f, 180.0f};
        for (float lon : longitudes) {
            float lst = TimeUtilsKt.meanSiderealTime(date, lon);
            assertTrue("LST should be >= 0 at longitude " + lon, lst >= 0.0f);
            assertTrue("LST should be < 360 at longitude " + lon, lst < 360.0f);
        }
    }

    // ============================================
    // Normalize Hours Tests
    // ============================================

    @Test
    public void testNormalizeHours_AlreadyNormalized() {
        double result = TimeUtilsKt.normalizeHours(12.0);

        assertEquals(12.0, result, EPSILON);
    }

    @Test
    public void testNormalizeHours_Negative() {
        double result = TimeUtilsKt.normalizeHours(-3.0);

        assertEquals(21.0, result, EPSILON);
    }

    @Test
    public void testNormalizeHours_Over24() {
        double result = TimeUtilsKt.normalizeHours(27.0);

        assertEquals(3.0, result, EPSILON);
    }

    @Test
    public void testNormalizeHours_Exactly24() {
        double result = TimeUtilsKt.normalizeHours(24.0);

        assertEquals(0.0, result, EPSILON);
    }

    @Test
    public void testNormalizeHours_Zero() {
        double result = TimeUtilsKt.normalizeHours(0.0);

        assertEquals(0.0, result, EPSILON);
    }

    @Test
    public void testNormalizeHours_LargeNegative() {
        double result = TimeUtilsKt.normalizeHours(-50.0);

        // -50 + 72 = 22 (adding 3 * 24)
        assertEquals(22.0, result, EPSILON);
    }

    // ============================================
    // Clock Time From Hours Tests
    // ============================================

    @Test
    public void testClockTimeFromHrs_Noon() {
        int[] hms = TimeUtilsKt.clockTimeFromHrs(12.0);

        assertEquals(12, hms[0]); // hours
        assertEquals(0, hms[1]);  // minutes
        assertEquals(0, hms[2]);  // seconds
    }

    @Test
    public void testClockTimeFromHrs_WithMinutes() {
        int[] hms = TimeUtilsKt.clockTimeFromHrs(12.5);

        assertEquals(12, hms[0]); // hours
        assertEquals(30, hms[1]); // minutes
        assertEquals(0, hms[2]);  // seconds
    }

    @Test
    public void testClockTimeFromHrs_Midnight() {
        int[] hms = TimeUtilsKt.clockTimeFromHrs(0.0);

        assertEquals(0, hms[0]);
        assertEquals(0, hms[1]);
        assertEquals(0, hms[2]);
    }

    @Test
    public void testClockTimeFromHrs_QuarterPastThree() {
        // 3:15:00 = 3.25 hours
        int[] hms = TimeUtilsKt.clockTimeFromHrs(3.25);

        assertEquals(3, hms[0]);
        assertEquals(15, hms[1]);
        assertEquals(0, hms[2]);
    }

    // ============================================
    // Constants Tests
    // ============================================

    @Test
    public void testMinutesPerHour() {
        assertEquals(60.0, TimeUtilsKt.MINUTES_PER_HOUR, EPSILON);
    }

    @Test
    public void testSecondsPerHour() {
        assertEquals(3600.0, TimeUtilsKt.SECONDS_PER_HOUR, EPSILON);
    }

    @Test
    public void testHoursToDegrees() {
        // 1 hour = 15 degrees (360/24)
        assertEquals(15.0f, TimeUtilsKt.HOURS_TO_DEGREES, FLOAT_EPSILON);
    }

    // ============================================
    // Astronomical Date Tests
    // ============================================

    @Test
    public void testJulianDay_VernalEquinox2020() {
        // Vernal equinox 2020: March 20, 2020 around 03:50 UT
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2020, Calendar.MARCH, 20, 3, 50, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date vernalEquinox = cal.getTime();

        double jd = TimeUtilsKt.julianDay(vernalEquinox);

        // Expected JD is around 2458928.66
        assertEquals(2458928.66, jd, 0.01);
    }

    @Test
    public void testJulianDay_SummerSolstice2020() {
        // Summer solstice 2020: June 20, 2020 around 21:44 UT
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2020, Calendar.JUNE, 20, 21, 44, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date solstice = cal.getTime();

        double jd = TimeUtilsKt.julianDay(solstice);

        // Expected JD is around 2459021.41 (21:44 UT = 0.906 day fraction)
        assertEquals(2459021.41, jd, 0.01);
    }

    @Test
    public void testSiderealTimeIncreaseRate() {
        // Sidereal day is about 23h 56m 4s, so in 24 solar hours,
        // sidereal time advances by about 360.986 degrees (one full rotation
        // plus about 1 degree)

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        cal.set(2000, Calendar.JUNE, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date day1 = cal.getTime();

        cal.add(Calendar.DAY_OF_MONTH, 1);
        Date day2 = cal.getTime();

        float lst1 = TimeUtilsKt.meanSiderealTime(day1, 0.0f);
        float lst2 = TimeUtilsKt.meanSiderealTime(day2, 0.0f);

        // After one solar day, sidereal time should advance by ~360.986 degrees
        // which means it should be about 0.986 degrees more than yesterday
        float diff = lst2 - lst1;
        if (diff < 0) diff += 360;

        // Should be close to 0.986 degrees (one solar day = one sidereal day + ~4 min)
        assertEquals(0.986f, diff, 0.01f);
    }
}
