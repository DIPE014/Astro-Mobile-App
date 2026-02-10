cmake_minimum_required(VERSION 3.22.1)
project(astrometry_native)

# Compiler flags
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -fPIC -Wno-unused-parameter -Wno-pointer-sign -Wno-format")

# Definitions
add_definitions(
    -DHAVE_NETPBM=0
    -DHAVE_CFITSIO=0
    -DSIMPLEXY_REENTRANT
)

# Include paths
include_directories(
    ${CMAKE_SOURCE_DIR}/astrometry/include
    ${CMAKE_SOURCE_DIR}/astrometry/include/astrometry
    ${CMAKE_SOURCE_DIR}/astrometry/gsl-an
    ${CMAKE_SOURCE_DIR}/astrometry/util
    ${CMAKE_SOURCE_DIR}/astrometry/solver
    ${CMAKE_SOURCE_DIR}/astrometry/libkd
    ${CMAKE_SOURCE_DIR}/astrometry/qfits-an
)

# GSL-AN sources (math library)
# Note: *_source.c files are template files included by other files, not compiled directly
file(GLOB GSL_SYS_SOURCES astrometry/gsl-an/sys/*.c)
file(GLOB GSL_ERR_SOURCES astrometry/gsl-an/err/*.c)
file(GLOB GSL_VECTOR_SOURCES astrometry/gsl-an/vector/*.c)
file(GLOB GSL_MATRIX_SOURCES astrometry/gsl-an/matrix/*.c)
file(GLOB GSL_BLAS_SOURCES astrometry/gsl-an/blas/*.c)
file(GLOB GSL_CBLAS_SOURCES astrometry/gsl-an/cblas/*.c)
file(GLOB GSL_LINALG_SOURCES astrometry/gsl-an/linalg/*.c)
file(GLOB GSL_PERMUTATION_SOURCES astrometry/gsl-an/permutation/*.c)
file(GLOB GSL_BLOCK_SOURCES astrometry/gsl-an/block/*.c)

# Exclude *_source.c files (they are templates included by other files)
# Also exclude linalg files that are #include'd by others (givens.c, apply_givens.c, svdstep.c)
file(GLOB GSL_SOURCE_TEMPLATES astrometry/gsl-an/*/*_source.c)
set(GSL_LINALG_INCLUDED
    ${CMAKE_SOURCE_DIR}/astrometry/gsl-an/linalg/givens.c
    ${CMAKE_SOURCE_DIR}/astrometry/gsl-an/linalg/apply_givens.c
    ${CMAKE_SOURCE_DIR}/astrometry/gsl-an/linalg/svdstep.c
)
set(GSL_ALL_SOURCES
    ${GSL_SYS_SOURCES}
    ${GSL_ERR_SOURCES}
    ${GSL_VECTOR_SOURCES}
    ${GSL_MATRIX_SOURCES}
    ${GSL_BLAS_SOURCES}
    ${GSL_CBLAS_SOURCES}
    ${GSL_LINALG_SOURCES}
    ${GSL_PERMUTATION_SOURCES}
    ${GSL_BLOCK_SOURCES}
)
list(REMOVE_ITEM GSL_ALL_SOURCES ${GSL_SOURCE_TEMPLATES})
list(REMOVE_ITEM GSL_ALL_SOURCES ${GSL_LINALG_INCLUDED})
set(GSL_SOURCES ${GSL_ALL_SOURCES})

# QFITS-AN sources (FITS file handling)
set(QFITS_SOURCES
    astrometry/qfits-an/anqfits.c
    astrometry/qfits-an/qfits_card.c
    astrometry/qfits-an/qfits_convert.c
    astrometry/qfits-an/qfits_error.c
    astrometry/qfits-an/qfits_float.c
    astrometry/qfits-an/qfits_header.c
    astrometry/qfits-an/qfits_image.c
    astrometry/qfits-an/qfits_memory.c
    astrometry/qfits-an/qfits_rw.c
    astrometry/qfits-an/qfits_table.c
    astrometry/qfits-an/qfits_time.c
    astrometry/qfits-an/qfits_tools.c
    astrometry/qfits-an/qfits_byteswap.c
    astrometry/qfits-an/md5.c
)

# LIBKD sources (KD-tree for spatial indexing)
# Note: kdtree_internal*.c files are #include'd by kdint_*.c, not compiled directly
set(LIBKD_SOURCES
    astrometry/libkd/kdtree.c
    astrometry/libkd/kdtree_dim.c
    astrometry/libkd/kdtree_fits_io.c
    astrometry/libkd/dualtree.c
    astrometry/libkd/dualtree_rangesearch.c
    astrometry/libkd/kdint_ddd.c
    astrometry/libkd/kdint_fff.c
    astrometry/libkd/kdint_ddu.c
    astrometry/libkd/kdint_duu.c
    astrometry/libkd/kdint_dds.c
    astrometry/libkd/kdint_dss.c
    astrometry/libkd/kdint_lll.c
)

# UTIL sources (star detection + utilities)
set(UTIL_SOURCES
    # Star detection (simplexy)
    astrometry/util/simplexy.c
    astrometry/util/dallpeaks.c
    astrometry/util/dcen3x3.c
    astrometry/util/dfind.c
    astrometry/util/dmedsmooth.c
    astrometry/util/dpeaks.c
    astrometry/util/dselip.c
    astrometry/util/dsigma.c
    astrometry/util/dsmooth.c
    astrometry/util/ctmf.c
    astrometry/util/image2xy.c
    astrometry/util/resample.c
    astrometry/util/dobjects.c
    # Base utilities
    astrometry/util/starutil.c
    astrometry/util/mathutil.c
    astrometry/util/bl.c
    astrometry/util/bl-sort.c
    astrometry/util/bt.c
    astrometry/util/healpix.c
    astrometry/util/healpix-utils.c
    astrometry/util/permutedsort.c
    astrometry/util/ioutils.c
    astrometry/util/fileutils.c
    astrometry/util/errors.c
    astrometry/util/log.c
    astrometry/util/datalog.c
    astrometry/util/tic.c
    astrometry/util/an-endian.c
    # SIP/WCS
    astrometry/util/sip.c
    astrometry/util/sip-utils.c
    astrometry/util/fit-wcs.c
    astrometry/util/gslutils.c
    astrometry/util/matchobj.c
    astrometry/util/starxy.c
    # FITS utilities
    astrometry/util/fitsioutils.c
    astrometry/util/fitsbin.c
    astrometry/util/fitsfile.c
    astrometry/util/fitstable.c
    astrometry/util/sip_qfits.c
)

# SOLVER sources
set(SOLVER_SOURCES
    astrometry/solver/solver.c
    astrometry/solver/verify.c
    astrometry/solver/tweak.c
    astrometry/solver/tweak2.c
    astrometry/solver/quad-utils.c
    astrometry/solver/pnpoly.c
    astrometry/solver/solvedfile.c
    astrometry/solver/codefile.c
    astrometry/solver/catalog.c
    # These are in util/
    astrometry/util/index.c
    astrometry/util/codekd.c
    astrometry/util/starkd.c
    astrometry/util/quadfile.c
    astrometry/util/multiindex.c
)

# Build static libraries
add_library(gsl_an STATIC ${GSL_SOURCES})
add_library(qfits_an STATIC ${QFITS_SOURCES})
add_library(libkd STATIC ${LIBKD_SOURCES})
add_library(anutil STATIC ${UTIL_SOURCES})
add_library(solver STATIC ${SOLVER_SOURCES})

# Link dependencies
target_link_libraries(anutil gsl_an)
target_link_libraries(solver anutil libkd qfits_an gsl_an)

# Main JNI library
add_library(astrometry_native SHARED
    jni/astrometry_jni.c
)

target_link_libraries(astrometry_native
    solver
    anutil
    libkd
    qfits_an
    gsl_an
    log
    m
)
