

/* This is auto-generated code. Edit at your own peril. */

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "cutest.h"


extern void test_depths(CuTest*);
extern void test_sorting(CuTest*);
extern void test_tweak_1(CuTest*);
extern void test_tweak_2(CuTest*);
extern void test_tchebytweak(CuTest*);
extern void test_solve_multiindex(CuTest*);
extern void test_predistort(CuTest*);


void RunAllTests(void) 
{
    CuString *output = CuStringNew();
    CuSuite* suite = CuSuiteNew();


    SUITE_ADD_TEST(suite, test_depths);
    SUITE_ADD_TEST(suite, test_sorting);
    SUITE_ADD_TEST(suite, test_tweak_1);
    SUITE_ADD_TEST(suite, test_tweak_2);
    SUITE_ADD_TEST(suite, test_tchebytweak);
    SUITE_ADD_TEST(suite, test_solve_multiindex);
    SUITE_ADD_TEST(suite, test_predistort);

    CuSuiteRun(suite);
    CuSuiteSummary(suite, output);
    CuSuiteDetails(suite, output);

    printf("%s\n", output->buffer);

    CuSuiteFree(suite);
    CuStringFree(output);
}

int main(int argc, char** args)
{
    if (argc > 1 && !strcmp(args[1], "-d")) {
        printf("Setting die on fail.\n");
        CuDieOnFail();
    }
    RunAllTests();
    return 0;
}
