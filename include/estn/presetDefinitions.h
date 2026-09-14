#ifndef ESTN_PRESET_DEFINITIONS_H
#define ESTN_PRESET_DEFINITIONS_H

/* Welcome to the Preset Dashboard 7.0 (RenderDragon edition)! Here is where you can make changes
to the shader pack's settings! Rebuild with `lazurite build proj` after editing.
Disable any feature by typing "//" in front of it (ex. // #define <FEATURE>) */

#define PLANT_WAVES
#define WATER_WAVES
//#define UNDERWATER_WAVES

#define BASIC_SHADOWS
#define NORM_SHADOWS
#define EXTRA_SHADOWS
#define HDR

// Noise type, 0 for cheap noise, 1 for the laggy voronoi. DO NOT DISABLE BY "//"
#define NOISE_T 1
#define LAYERED_NOISE
#define WATER_NOISE
#define UNDERWATER_CAUSTIC

#define ATMO
#define SHADOW_FOG

#define CSUN
#define CSTAR
#define CCLOUDS
// Second procedural cloud layer drawn on the sky dome (CCLOUDS MUST BE ON!)
#define DCLOUDS

#define BEAMS
//#define SPECULAR
//#define CUSTOM_ANGLE
#define EMISSIVE_MAPS
//#define NORMAL_MAPS

#define sunPow 3
#define sizeSun 18.0
#define sizeMoon 30.0
#define starB 3.0
#define starS 0.75

#define delta 0.000016
#define normDetail 0.005

#define lightCol vec3(0.15, 0.1, 0.05)
#define customAngle 180.0
#define emissValue 1.64

#define shadowCol vec3(0.0, 0.0, 0.64)
#define shadow_B 0.42
#define shdAlpha 0.64
#define lightShrp 3.0
#define lightSize 1.25

#define saturation 1.3
#define brightness 1.2
#define exposure 1.0
#define contrast 1.08

#define monoSat 0.6
#define monoSatE 1.0
#define mono_B 0.56

#define SV 1.36
#define HV 1.0

#define maxHeight 64.0
#define minHeight 32.0
#define densityMax 1.3
#define densityMin 1.2
#define densityRain 2.0

#define d_color vec4(0.21, 0.7, 1.0, 1.0)
#define n_color vec4(0.0, 0.1, 0.2, 1.0)
#define s_color vec4(0.375, 0.125, 0.75, 1.0)

#define cd_color vec4(1.0, 1.2, 1.2, 1.0)
#define cn_color vec4(0.0, 0.15, 0.3, 1.0)
#define cs_color vec4(0.75, 0.5, 1.0, 1.0)

#define fd_color vec4(0.2, 0.69, 1.0, 1.0)
#define fn_color vec4(0.0, 0.1, 0.2, 1.0)
#define fs_color vec4(0.8, 0.4, 0.1, 1.0)

#define ad_color vec4(0.6, 0.8, 1.2, 1.0)
#define an_color vec4(0.16, 0.24, 0.32, 1.0)
#define as_color vec4(0.6, 0.5, 0.7, 1.0)

#define sd_color vec4(0.4, 0.6, 0.8, 1.0)
#define sn_color vec4(0.3, 0.3, 0.6, 1.0)
#define ss_color vec4(1.0, 0.6, 0.0, 1.0)

#define sld_color vec4(1.0, 1.0, 0.8, 1.0)
#define sln_color vec4(0.3, 0.3, 0.6, 1.0)
#define sls_color vec4(1.0, 1.0, 0.8, 1.0)

#define skyMax 0.5
#define skyMin 0.125

#define gradientA1 1.0
#define gradientA2 0.64

#define gradientB1 1.0
#define gradientB2 0.8

//#define DEBUG

#endif
