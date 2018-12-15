
#ifndef _APEX_PARAMS_H_
#define _APEX_PARAMS_H_

// The FUSE API has been changed a number of times.  So, our code
// needs to define the version of the API that we assume.  As of this
// writing, the most current API version is 26
#define FUSE_USE_VERSION 26

// need this to get pwrite().  I have to use setvbuf() instead of
// setlinebuf() later in consequence.
#define _XOPEN_SOURCE 500

#include <limits.h>
#include <stdio.h>
#include <glib.h>

struct apex_data{

    int MIN_PARAM;
    int MAX_PARAM;

    // int width;
    // int height;

    double mem_util;

    int lambda, sigma, rho, mu;

    // Array of blocks
    Garray *blocks;

    

    // This will contain the used, unused hastables
    // Block params and factors and their calculations,
    // Master file table, list etc
};


struct apex_state {
    apex_data *data;
    char *rootdir;
};
#define APEX_DATA ((struct apex_state *) fuse_get_context()->private_data)

#endif
