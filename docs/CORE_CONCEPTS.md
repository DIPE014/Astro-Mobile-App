# Core Concepts: How the Astronomy App Works

This document explains the core math, star database, and time travel system in simple terms.

---

## 1. Celestial Coordinate System (RA/Dec)

Think of the sky as a giant sphere surrounding Earth. Every star has a fixed "address" on this sphere:

### Right Ascension (RA)
- Like **longitude** on Earth, but for the sky
- Measured in degrees (0° to 360°) or hours (0h to 24h)
- 1 hour = 15 degrees (because 360° ÷ 24h = 15°/h)
- Measured **eastward** from a reference point (vernal equinox)

### Declination (Dec)
- Like **latitude** on Earth
- Measured in degrees (-90° to +90°)
- 0° = celestial equator (directly above Earth's equator)
- +90° = north celestial pole (above Earth's north pole)
- -90° = south celestial pole

### Example: Sirius
```
RA  = 6h 45m 9s  = 101.29°
Dec = -16° 43'   = -16.72°
```

---

## 2. Converting Coordinates to 3D (Vector Math)

To do calculations, we convert RA/Dec to a 3D point on a unit sphere:

```
x = cos(RA) × cos(Dec)
y = sin(RA) × cos(Dec)
z = sin(Dec)
```

This gives us a point on a sphere with radius 1, which makes math easier.

### To convert back:
```
RA  = atan2(y, x)
Dec = asin(z)
```

---

## 3. Why Stars Don't Move (Much)

Stars are **so far away** that their positions appear fixed on the celestial sphere.

- The closest star (Proxima Centauri) is 4.24 light-years away
- Even over a human lifetime, star positions barely change
- We store star positions as fixed RA/Dec coordinates

**What DOES change**: Which part of the sky you see, based on:
- Your location on Earth
- The time of day/year
- Where you're pointing

---

## 4. How We Know Where You're Looking

### The Problem
Your phone has sensors, but they measure in "phone coordinates". We need to convert to "sky coordinates".

### The Solution: Transformation Matrix

**Step 1: Get phone orientation from sensors**
- Accelerometer → which way is "down" (gravity)
- Magnetometer → which way is "north" (magnetic field)
- Together → phone's orientation in space

**Step 2: Calculate sky orientation from time + location**
- Your latitude/longitude → where on Earth you are
- Current time → Earth's rotation angle
- Together → which RA/Dec is directly overhead (zenith)

**Step 3: Build transformation matrix**
```
celestial_direction = TransformMatrix × phone_direction
```

This 3×3 matrix converts any direction in phone coordinates to celestial coordinates.

---

## 5. Search Arrow Guidance (Navigation)

When you select a planet or star from search, the app shows a guidance arrow. The arrow is **roll-aware** so it matches the screen orientation even if the phone is tilted.

### How the Arrow Direction is Computed
1. **Line of sight vector**: where the phone is pointing (from sensor matrix math).
2. **Perpendicular vector**: the "up" direction on screen (also from the sensor matrix).
3. **Target vector**: RA/Dec converted into a 3D unit vector.

We project the target vector onto the view plane using the **right** and **up** axes, then convert that into a screen angle:

```
right = up × view
screenRight = -dot(targetRel, right)
screenUp = dot(targetRel, up)
arrowAngle = atan2(screenUp, screenRight)
```

This avoids left/right mirroring and keeps the arrow stable regardless of roll.

### When the Arrow Hides
If the target is inside the camera field of view, we **hide only the arrow** (to reduce jitter) but keep search mode active. The highlight ring on the target remains visible so the user can finish visually.

---

## 5. The Star Database

### Storage Format: Protocol Buffers (Protobuf)

Stars are stored in binary files for efficiency:
- `stars.binary` - ~9000 individual stars
- `constellations.binary` - 88 constellation definitions
- `messier.binary` - Deep sky objects (galaxies, nebulae)

### Star Data Structure
```
StarData {
    id: "HIP12345"           // Catalog ID
    name: "Sirius"           // Common name
    ra: 101.29               // Right Ascension (degrees)
    dec: -16.72              // Declination (degrees)
    magnitude: -1.46         // Brightness (lower = brighter)
    color: 0xFFCAD7FF        // Display color (ARGB)
}
```

### Magnitude Scale
- **Negative** = very bright (Sun = -26, Sirius = -1.46)
- **0 to 1** = brightest stars visible (Vega = 0.03)
- **2 to 4** = easily visible stars
- **5 to 6** = faint, barely visible to naked eye
- **> 6** = need telescope

### Loading Flow
```
stars.binary (on disk)
    ↓ parse protobuf
List<PointElementProto>
    ↓ convert to model
List<StarData>
    ↓ filter by magnitude
Visible stars on screen
```

---

## 6. Planet Calculations (Orbital Mechanics)

Unlike stars, **planets move**! We calculate their positions using orbital mechanics.

### The Six Orbital Elements

Every planet's orbit is defined by 6 numbers:

| Element | Symbol | Meaning |
|---------|--------|---------|
| Semi-major axis | a | Size of orbit (AU) |
| Eccentricity | e | How elliptical (0=circle, 1=parabola) |
| Inclination | i | Tilt of orbit plane |
| Ascending node | Ω | Where orbit crosses ecliptic going north |
| Perihelion | ω | Where planet is closest to Sun |
| Mean longitude | L | Position along orbit |

### How Planet Position is Calculated

**Step 1: Get orbital elements for the date**
```kotlin
// Elements change slowly over time
a = 0.387 + 0.00000037 × t  // Mercury's semi-major axis
```

**Step 2: Calculate true anomaly (where in orbit)**
```
Mean Anomaly → Eccentric Anomaly → True Anomaly
(Uses Newton-Raphson iteration)
```

**Step 3: Calculate heliocentric position (relative to Sun)**
```
r = a(1 - e²) / (1 + e×cos(anomaly))  // distance
x, y, z = convert to 3D coordinates
```

**Step 4: Convert to geocentric (relative to Earth)**
```
planet_from_earth = planet_from_sun - earth_from_sun
```

**Step 5: Convert to RA/Dec**
```
Apply ecliptic-to-equatorial rotation (23.4° tilt)
Convert 3D vector to RA/Dec
```

---

## 7. Time Travel System

### How It Works

The app has a `TimeTravelClock` that can return:

1. **Real time** - Actual current time
2. **Frozen time** - A specific moment, doesn't advance
3. **Offset time** - Running from a different starting point

```java
public long getTime() {
    if (!isTimeTravelActive) {
        return System.currentTimeMillis();  // Real time
    }
    if (isFrozen) {
        return frozenTimeMillis;  // Locked to specific moment
    }
    return System.currentTimeMillis() + offsetMillis;  // Offset time
}
```

### What Changes with Time

| Object | Time Effect |
|--------|-------------|
| Stars | None - fixed positions |
| Planets | Position changes (orbital motion) |
| Moon | Position changes rapidly |
| Sun | Apparent position changes |
| Sky rotation | Which stars are visible changes |

### Julian Day System

Astronomers use "Julian Day" - a continuous day count since 4713 BC:

```
JD = 2451545.0  →  January 1, 2000, 12:00 UT (J2000 epoch)
JD = 2460000.0  →  February 24, 2023
```

This makes date math easy: tomorrow = today + 1.0

### Sidereal Time (Earth's Rotation)

**Sidereal time** tells us which RA is overhead right now:

```
Local Sidereal Time = Global Sidereal Time + Your Longitude
```

- Earth rotates 360° in ~23h 56m (sidereal day)
- This is why different stars are visible at different times

---

## 8. Putting It All Together

### When You Open the App

```
1. Load star database (once)
   stars.binary → List<StarData>

2. Get current time
   TimeTravelClock.getTime() → Date

3. Get your location
   GPS → latitude, longitude

4. Calculate sky orientation
   time + location → zenith RA/Dec

5. Get phone orientation
   sensors → transformation matrix

6. For each star:
   - Is it above horizon? (visibility check)
   - Convert RA/Dec to screen position
   - Draw if visible

7. For each planet:
   - Calculate current RA/Dec (from orbital elements + time)
   - Convert to screen position
   - Draw if visible
```

### When You Time Travel

```
1. User picks new date/time
2. TimeTravelClock updates its offset
3. All position calculations use new time
4. Planets recalculate to new positions
5. Sky rotation changes (different LST)
6. Screen redraws with new sky
```

---

## 9. Key Formulas Reference

### Coordinate Conversion
```
// RA/Dec to 3D vector
x = cos(RA) × cos(Dec)
y = sin(RA) × cos(Dec)
z = sin(Dec)

// 3D vector to RA/Dec
RA = atan2(y, x)
Dec = asin(z)
```

### Angular Distance (Haversine)
```
d = 2 × asin(√(sin²(Δdec/2) + cos(dec1)×cos(dec2)×sin²(Δra/2)))
```

### Sidereal Time
```
GST = 280.461° + 360.98564737° × (JD - 2451545.0)
LST = GST + longitude
```

### Visibility Check
```
// Northern hemisphere observer at latitude φ
Circumpolar (always visible): dec > 90° - φ
Never visible: dec < φ - 90°
```

---

## 10. Summary

| Concept | Key Idea |
|---------|----------|
| RA/Dec | Sky coordinates, like lat/long for stars |
| Stars | Fixed positions, loaded from database |
| Planets | Calculated from orbital mechanics + time |
| Deep Sky Objects | Galaxies, clusters, nebulae from Messier catalog |
| Transformation | Sensor data → screen via matrix math |
| Time Travel | Change the "current time" for all calculations |
| Sidereal Time | Earth's rotation determines visible sky |
| Tonight's Highlights | Filter visible objects by altitude threshold |
| Trajectories | Sample planet positions over time to show orbital path |
| Gestures | Pinch-zoom, manual pan, long-press for trajectory |

The app essentially answers: **"Given my location, the time, and where I'm pointing, what celestial objects should I see?"**

---

## 11. Deep Sky Objects (Messier Catalog)

### What Are DSOs?

Deep Sky Objects are celestial objects beyond our solar system that aren't individual stars:

| Type | Examples | Shape Icon |
|------|----------|------------|
| **Galaxies** | M31 (Andromeda), M51 (Whirlpool) | Diamond |
| **Star Clusters** | M45 (Pleiades), M13 (Hercules Cluster) | Square |
| **Nebulae** | M42 (Orion Nebula), M1 (Crab Nebula) | Glowing circle |

### Data Source

DSOs are loaded from `messier.binary` (protobuf format), same pattern as stars and constellations:

```
messier.binary → ProtobufParser.parseMessierObjects()
    → MessierRepositoryImpl (lazy cache)
    → SkyCanvasView.drawDSOs()
```

Each object has: name, RA/Dec position, color, size, and shape type (galaxy/cluster/nebula).

### Visibility Rendering

DSOs are rendered between the star layer and planet layer, using shape-coded icons to distinguish types at a glance.

---

## 12. Tonight's Highlights

### How It Works

`TonightsHighlights.compute()` finds objects visible right now:

1. **Get observer context** - latitude, longitude, current time
2. **Calculate Local Sidereal Time** - determines which RA is overhead
3. **For each object** - compute altitude using the Alt/Az formula
4. **Filter by altitude threshold**:
   - Planets: above 5°
   - Bright stars (mag < 1.5): above 5°
   - Constellations: above 20°
   - Deep sky objects: above 10°
5. **Sort** - planets first, then by altitude descending

### Alt/Az from RA/Dec

The same formula used everywhere in the app:

```
Hour Angle = LST - RA
sin(Alt) = sin(Dec)sin(Lat) + cos(Dec)cos(Lat)cos(HA)
cos(Az) = (sin(Dec) - sin(Lat)sin(Alt)) / (cos(Lat)cos(Alt))
```

---

## 13. Planet Trajectory Visualization

When you long-press a planet, the app shows its path over 60 days (±30 from now):

1. **Sample positions** - Call `Universe.getRaDec(body, date)` at 2-day intervals
2. **Project to screen** - Convert each RA/Dec to screen coordinates via Alt/Az
3. **Draw path** - Connect points with an orange line
4. **Show markers** - Dots at each sample, with time labels for key positions
5. **Highlight current** - Larger marker at today's position

This helps users understand how planets move against the fixed star background (retrograde motion, conjunctions, etc.).

---

## 14. Gesture System

### Pinch-to-Zoom

The `ScaleGestureDetector` adjusts field of view:
- **Pinch in** → smaller FOV → more zoomed in (min 20°)
- **Pinch out** → larger FOV → wider view (max 120°)
- `pixelsPerDegree = screenSize / FOV` controls the projection scale

### Manual Pan

When the user drags, the view enters **manual mode**:
- Sensor orientation is overridden
- Drag updates `manualAzimuth` and `manualAltitude`
- Double-tap exits manual mode and returns to sensor tracking

### Smart Selection

Objects are detected using `getObjectsInReticle()`:
- Checks stars, planets, constellations, and DSOs within the center reticle circle
- **1 object** → show info panel directly
- **2-4 objects** → horizontal chip strip at bottom
- **5+ objects** → scrollable bottom sheet
