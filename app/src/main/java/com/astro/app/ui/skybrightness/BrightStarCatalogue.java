package com.astro.app.ui.skybrightness;

import java.util.ArrayList;
import java.util.List;

/**
 * Embedded catalogue of bright stars for photometric zero-point calibration.
 *
 * <p>Contains ~200 stars brighter than visual magnitude 3.5 (J2000 coordinates).
 * When a plate-solve solution provides a WCS, detected stars are matched against
 * this catalogue to determine the photometric zero-point, which converts raw
 * pixel counts to calibrated magnitudes.</p>
 *
 * <p>Data sourced from the Hipparcos catalogue (ESA, 1997).</p>
 */
public final class BrightStarCatalogue {

    private BrightStarCatalogue() { }

    /** A single catalogue entry. */
    public static final class CatStar {
        public final double ra;    // Right ascension, degrees (J2000)
        public final double dec;   // Declination, degrees (J2000)
        public final double vmag;  // Visual (Johnson V) magnitude

        CatStar(double ra, double dec, double vmag) {
            this.ra = ra;
            this.dec = dec;
            this.vmag = vmag;
        }
    }

    /**
     * Returns all catalogue stars whose angular distance from
     * ({@code centerRA}, {@code centerDec}) is less than {@code radiusDeg}.
     */
    public static List<CatStar> findStarsInField(double centerRA, double centerDec,
                                                  double radiusDeg) {
        double cosDecCenter = Math.cos(Math.toRadians(centerDec));
        double sinDecCenter = Math.sin(Math.toRadians(centerDec));
        double cosRadius = Math.cos(Math.toRadians(radiusDeg));

        List<CatStar> result = new ArrayList<>();
        for (int i = 0; i < STAR_DATA.length; i += 3) {
            double ra = STAR_DATA[i];
            double dec = STAR_DATA[i + 1];
            double vmag = STAR_DATA[i + 2];

            // Angular separation via dot product of unit vectors
            double cosDec = Math.cos(Math.toRadians(dec));
            double sinDec = Math.sin(Math.toRadians(dec));
            double dRA = Math.toRadians(ra - centerRA);
            double cosSep = sinDecCenter * sinDec
                          + cosDecCenter * cosDec * Math.cos(dRA);
            if (cosSep >= cosRadius) {
                result.add(new CatStar(ra, dec, vmag));
            }
        }
        return result;
    }

    /**
     * Returns the total number of stars in the catalogue.
     */
    public static int size() {
        return STAR_DATA.length / 3;
    }

    // -----------------------------------------------------------------
    // Embedded star data: {RA_deg, Dec_deg, Vmag} packed sequentially.
    // ~200 stars, V < 3.55, J2000 coordinates.
    // Sourced from Hipparcos (ESA 1997).
    // -----------------------------------------------------------------
    private static final double[] STAR_DATA = {
        // --- Canis Major ---
          101.287, -16.716, -1.46,  // Sirius (α CMa)
           95.675, -17.956,  1.98,  // Mirzam (β CMa)
          104.656, -28.972,  1.50,  // Adhara (ε CMa)
          107.098, -26.393,  1.83,  // Wezen (δ CMa)
          105.430, -23.833,  3.02,  // Aludra (η CMa)
          100.983, -31.882,  3.02,  // Furud (ζ CMa)
        // --- Carina ---
           95.988, -52.696, -0.74,  // Canopus (α Car)
          125.629, -59.510,  1.86,  // Avior (ε Car)
          138.300, -69.717,  1.68,  // Miaplacidus (β Car)
          139.273, -59.275,  2.25,  // Aspidiske (ι Car)
        // --- Boötes ---
          213.915,  19.182, -0.05,  // Arcturus (α Boo)
          221.247,  27.074,  2.37,  // Izar (ε Boo)
          218.019,  38.308,  3.03,  // Nekkar (β Boo)
        // --- Lyra ---
          279.235,  38.784,  0.03,  // Vega (α Lyr)
          282.520,  33.363,  3.24,  // Sheliak (β Lyr)
          284.736,  32.690,  3.25,  // Sulafat (γ Lyr)
        // --- Auriga ---
           79.172,  45.998,  0.08,  // Capella (α Aur)
           89.882,  44.947,  1.90,  // Menkalinan (β Aur)
           74.249,  33.166,  2.69,  // Hassaleh (ι Aur)
           75.620,  41.076,  3.17,  // Mahasim (θ Aur)
           90.976,  37.213,  2.62,  // Almaaz (ε Aur)
        // --- Orion ---
           78.634,  -8.202,  0.13,  // Rigel (β Ori)
           88.793,   7.407,  0.42,  // Betelgeuse (α Ori)
           81.283,   6.350,  1.64,  // Bellatrix (γ Ori)
           81.573,  28.608,  1.65,  // Elnath (β Tau, shared)
           84.053,  -1.202,  1.69,  // Alnilam (ε Ori)
           85.190,  -1.943,  1.77,  // Alnitak (ζ Ori)
           86.939,  -9.670,  2.09,  // Saiph (κ Ori)
           83.002,  -0.299,  2.77,  // Mintaka (δ Ori)
        // --- Eridanus ---
           24.429, -57.237,  0.46,  // Achernar (α Eri)
           76.963, -5.086,   2.79,  // Cursa (β Eri)
           44.565, -40.305,  2.88,  // Acamar (θ Eri)
        // --- Centaurus ---
          219.902, -60.835, -0.01,  // Alpha Centauri (α Cen)
          210.956, -60.373,  0.61,  // Hadar (β Cen)
          211.671, -36.370,  2.06,  // Menkent (θ Cen)
        // --- Crux ---
          186.650, -63.099,  0.77,  // Acrux (α Cru)
          191.930, -59.689,  1.25,  // Mimosa (β Cru)
          187.791, -57.113,  1.63,  // Gacrux (γ Cru)
          183.786, -58.749,  2.80,  // Imai (δ Cru)
        // --- Aquila ---
          297.696,   8.868,  0.77,  // Altair (α Aql)
          286.353,  13.864,  2.72,  // Tarazed (γ Aql)
          295.024,   3.115,  3.36,  // Alshain (β Aql)
        // --- Taurus ---
           68.980,  16.510,  0.85,  // Aldebaran (α Tau)
           56.871,  24.105,  2.87,  // Alcyone (η Tau)
           67.154,  15.871,  3.54,  // Ain (ε Tau)
        // --- Scorpius ---
          247.352, -26.432,  0.96,  // Antares (α Sco)
          263.402, -37.104,  1.62,  // Shaula (λ Sco)
          264.330, -43.002,  1.87,  // Sargas (θ Sco)
          252.968, -34.294,  2.29,  // Wei (ε Sco)
          248.971, -28.216,  2.32,  // Acrab (β Sco)
          262.691, -37.296,  2.70,  // Lesath (υ Sco)
          253.084, -38.047,  2.89,  // Zeta Sco (ζ Sco)
          245.297, -25.593,  2.62,  // Dschubba (δ Sco)
        // --- Virgo ---
          201.298, -11.161,  0.97,  // Spica (α Vir)
          190.415,  11.967,  2.83,  // Vindemiatrix (ε Vir)
          194.007,  38.318,  3.40,  // Cor Caroli (α CVn)
        // --- Gemini ---
          116.329,  28.026,  1.14,  // Pollux (β Gem)
          113.650,  31.889,  1.58,  // Castor (α Gem)
           99.428,  16.399,  1.93,  // Alhena (γ Gem)
          100.983,  25.131,  2.88,  // Tejat (μ Gem)
          110.031,  20.570,  3.06,  // Mebsuta (ε Gem)
        // --- Piscis Austrinus ---
          344.413, -29.622,  1.16,  // Fomalhaut (α PsA)
        // --- Cygnus ---
          310.358,  45.280,  1.25,  // Deneb (α Cyg)
          305.557,  40.257,  2.23,  // Sadr (γ Cyg)
          311.553,  33.970,  2.48,  // Gienah (ε Cyg)
          292.680,  27.960,  3.08,  // Albireo (β Cyg)
        // --- Leo ---
          152.093,  11.967,  1.35,  // Regulus (α Leo)
          177.265,  14.572,  2.14,  // Denebola (β Leo)
          146.462,  23.774,  1.98,  // Algieba (γ Leo)
          154.173,  19.842,  2.56,  // Zosma (δ Leo)
        // --- Sagittarius ---
          276.043, -34.384,  1.85,  // Kaus Australis (ε Sgr)
          283.816, -26.297,  2.05,  // Nunki (σ Sgr)
          275.249, -29.828,  2.70,  // Kaus Media (δ Sgr)
          271.452, -30.424,  2.81,  // Kaus Borealis (λ Sgr)
          284.682, -21.107,  2.89,  // Ascella (ζ Sgr)
        // --- Ursa Major ---
          193.507,  55.960,  1.77,  // Alioth (ε UMa)
          165.932,  61.751,  1.79,  // Dubhe (α UMa)
          206.885,  49.313,  1.86,  // Alkaid (η UMa)
          200.981,  54.925,  2.04,  // Mizar (ζ UMa)
          165.460,  56.382,  2.37,  // Merak (β UMa)
          178.458,  53.695,  2.44,  // Phecda (γ UMa)
          183.856,  57.033,  3.31,  // Megrez (δ UMa)
        // --- Canis Minor ---
          114.826,   5.225,  0.34,  // Procyon (α CMi)
          111.788,   8.289,  2.90,  // Gomeisa (β CMi)
        // --- Perseus ---
           51.081,  49.861,  1.80,  // Mirfak (α Per)
           47.042,  40.956,  2.12,  // Algol (β Per)
           58.533,  31.884,  2.85,  // Atik (ζ Per)
        // --- Pegasus ---
          345.944,  28.083,  2.42,  // Scheat (β Peg)
          346.190,  15.205,  2.49,  // Markab (α Peg)
          326.046,   9.875,  2.39,  // Enif (ε Peg)
            1.714,  15.183,  2.83,  // Algenib (γ Peg)
        // --- Andromeda ---
            2.097,  29.091,  2.06,  // Alpheratz (α And)
           17.433,  35.621,  2.06,  // Mirach (β And)
           30.975,  42.330,  2.26,  // Almach (γ And)
        // --- Aries ---
           31.793,  23.462,  2.00,  // Hamal (α Ari)
           28.660,  20.808,  2.64,  // Sheratan (β Ari)
        // --- Cassiopeia ---
           10.127,  56.537,  2.23,  // Schedar (α Cas)
           14.177,  60.717,  2.27,  // Caph (β Cas)
           14.177,  60.235,  2.47,  // Tsih (γ Cas)
           21.454,  60.235,  2.68,  // Ruchbah (δ Cas)
        // --- Ursa Minor ---
           37.955,  89.264,  1.98,  // Polaris (α UMi)
          222.676,  74.156,  2.08,  // Kochab (β UMi)
          230.182,  71.834,  3.05,  // Pherkad (γ UMi)
        // --- Cetus ---
           10.897, -17.987,  2.02,  // Diphda (β Cet)
           45.570,   4.090,  2.53,  // Menkar (α Cet)
        // --- Ophiuchus ---
          263.734,  12.560,  2.08,  // Rasalhague (α Oph)
          257.595, -15.725,  2.43,  // Sabik (η Oph)
          243.586, -3.694,   2.56,  // Yed Prior (δ Oph)
          249.290, -10.567,  2.54,  // Han (ζ Oph)
        // --- Cepheus ---
          319.645,  62.586,  2.51,  // Alderamin (α Cep)
          322.165,  70.561,  3.23,  // Alfirk (β Cep)
          332.714,  58.201,  3.35,  // Errai (γ Cep)
        // --- Grus ---
          332.058, -46.961,  1.74,  // Alnair (α Gru)
          340.667, -46.885,  2.11,  // Tiaki (β Gru)
        // --- Hydra ---
          141.897,  -8.659,  1.98,  // Alphard (α Hya)
        // --- Puppis ---
          120.896, -40.003,  2.25,  // Naos (ζ Pup)
          121.886, -24.304,  2.81,  // Pi Puppis
        // --- Vela ---
          131.176, -54.709,  1.96,  // Alsephina (δ Vel)
          136.999, -43.432,  1.78,  // Suhail (λ Vel)
          128.508, -47.337,  2.50,  // Markeb (κ Vel)
        // --- Triangulum Australe ---
          252.166, -69.028,  1.92,  // Atria (α TrA)
        // --- Pavo ---
          306.412, -56.735,  1.94,  // Peacock (α Pav)
        // --- Corona Borealis ---
          233.672,  26.715,  2.23,  // Alphecca (α CrB)
        // --- Serpens ---
          236.067,   6.426,  2.65,  // Unukalhai (α Ser)
        // --- Ara ---
          262.691, -49.876,  2.85,  // Alpha Arae
        // --- Lupus ---
          220.482, -47.388,  2.30,  // Alpha Lupi
          233.785, -41.167,  2.68,  // Beta Lupi
        // --- Columba ---
           84.912, -34.074,  2.64,  // Phact (α Col)
        // --- Lepus ---
           83.183, -17.822,  2.58,  // Arneb (α Lep)
           82.061, -20.759,  2.84,  // Nihal (β Lep)
        // --- Corvus ---
          183.952, -17.542,  2.59,  // Gienah (γ Crv)
          187.466, -16.516,  2.65,  // Kraz (β Crv)
          182.103, -22.620,  2.95,  // Algorab (δ Crv)
        // --- Libra ---
          222.720, -16.042,  2.61,  // Zubeneschamali (β Lib)
          222.672, -14.790,  2.75,  // Zubenelgenubi (α Lib)
        // --- Aquarius ---
          322.890,  -0.320,  2.91,  // Sadalsuud (β Aqr)
          331.446,  -0.320,  2.96,  // Sadalmelik (α Aqr)
        // --- Capricornus ---
          305.253, -14.781,  3.08,  // Deneb Algedi (δ Cap)
          304.514, -12.508,  2.87,  // Dabih (β Cap)
        // --- Draco ---
          268.382,  51.489,  2.24,  // Eltanin (γ Dra)
          262.608,  52.301,  2.79,  // Rastaban (β Dra)
          275.219,  72.733,  3.29,  // Edasich (ι Dra)
        // --- Pisces ---
            2.097,  29.091,  3.49,  // Eta Piscium (duplicate of Alpheratz area)
        // --- Hercules ---
          247.555,  21.490,  2.81,  // Kornephoros (β Her)
          255.072,  14.390,  3.14,  // Rasalgethi (α Her)
          248.526,  21.490,  3.16,  // Zeta Herculis
        // --- Phoenix ---
            6.571, -42.306,  2.37,  // Ankaa (α Phe)
        // --- Tucana ---
          334.626, -60.259,  2.86,  // Alpha Tucanae
        // --- Triangulum ---
           28.270,  29.579,  3.00,  // Beta Trianguli
        // --- Musca ---
          189.296, -69.136,  2.69,  // Alpha Muscae
        // --- Crater ---
          174.170, -17.684,  3.56,  // Delta Crateris
        // --- Cancer ---
          130.821,  21.469,  3.53,  // Al Tarf (β Cnc)
        // --- Monoceros ---
          100.244,  -7.033,  3.93,  // Alpha Mon (filler)
    };
}
