#ifndef ESTN_FUNCTION_LIB_H
#define ESTN_FUNCTION_LIB_H

#include <estn/presetDefinitions.h>

#define pi 3.14159265358979

float clamp2(float x){ return clamp(x, 0.0, 1.0); }
vec2 clamp2(vec2 x){ return clamp(x, vec2_splat(0.0), vec2_splat(1.0)); }
vec3 clamp2(vec3 x){ return clamp(x, vec3_splat(0.0), vec3_splat(1.0)); }
vec4 clamp2(vec4 x){ return clamp(x, vec4_splat(0.0), vec4_splat(1.0)); }
float maxC(vec3 x){ return max(x.r, max(x.g, x.b)); }
float maxC(vec4 x){ return max(x.r, max(x.g, x.b)); }

vec3 A_Saturation(vec3 col, float a){
	return (col.r * 0.2125 + col.g * 0.7154 + col.b * 0.0721) * (1.0 - a) + col * a;
}

vec3 A_Exposure(vec3 col, float amount){
	return clamp2(col + (amount - 1.0));
}

vec3 A_Contrast(vec3 col, float a){
	return clamp2(0.5 * (1.0 - a) + col * a);
}

vec3 toneA(vec3 base){
	return A_Exposure(A_Contrast(A_Saturation(base, saturation), contrast), exposure) * brightness;
}

vec4 rgb2hsv(vec4 c){
	vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
	vec4 p = c.g < c.b ? vec4(c.bg, K.wz) : vec4(c.gb, K.xy);
	vec4 q = c.r < p.x ? vec4(p.xyw, c.r) : vec4(c.r, p.yzx);
	float d = q.x - min(q.w, q.y);
	float e = 1e-10;
	return vec4(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x, c.a);
}

vec4 hsv2rgb(vec4 c){
	vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
	vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
	return vec4(c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y), c.a);
}

bool isWater(vec4 col){
	vec4 hsv = rgb2hsv(col);
	return hsv.x >= 0.397 && hsv.x <= 0.722;
}

bool isSwamp(vec4 col){
	vec4 hsv = rgb2hsv(col);
	return hsv.x >= 0.184 && hsv.x <= 0.234 && hsv.z <= 0.441;
}

bool isBlock(vec4 col){
	return rgb2hsv(col).y == 0.0;
}

bool isPlant(vec4 col){
	vec4 hsv = rgb2hsv(col);
	return hsv.x >= 0.15 && hsv.x <= 0.4;
}

bool isDimen(float col0, float col1){
	return col0 == col1;
}

vec4 rgb2hdr(vec4 col){
	return vec4(col.rgb * SV * (1.0 - col.rgb) + col.rgb * HV * col.rgb, col.a);
}

float dayAmount(vec4 fogColor){ return pow(clamp2(1.0 - fogColor.b * 1.2), 0.5); }
float nightAmount(vec4 fogColor){ return pow(clamp2(1.0 - fogColor.r * 1.5), 1.2); }
float rainAmount(vec4 fogControl){ return clamp2(pow((0.7 - fogControl.x) / (fogControl.y - fogControl.x), 3.0)); }
bool isUnderwater(vec4 fogControl){ return fogControl.x < 0.0001; }

#define MIX2(x, y, z, w) ((w) < 0.5 ? mix(x, y, clamp2((w) / 0.5)) : mix(y, z, clamp2(((w) - 0.5) / 0.5)))
#define GENWAVES(x, y, z) (sin(dot(x, y)) * (z))
#define GENWAVEC(x, y, z) (cos(dot(x, y)) * (z))

vec2 rot2d(vec2 v, float a){
	float c = cos(a);
	float s = sin(a);
	return vec2(v.x * c - v.y * s, v.x * s + v.y * c);
}

#define s0 vec4(12.9898, 4.1414, 78.233, 314.13)
#define s1 vec4(0.1031, 1.1031, 2.1031, 3.1031)

float rand12(highp vec2 n){
	return fract(sin(dot(n, s0.xy)) * 1e4);
}

vec2 rand22(highp vec2 n){
	return fract(sin(vec2(dot(n, s0.xy), dot(n, s0.zw))) * 1e4);
}

vec3 rand32(highp vec2 n){
	return fract(sin(vec3(dot(n, s0.xy), dot(n, s0.yz), dot(n, s0.zw))) * 1e4);
}

vec3 rand33(highp vec3 n){
	return fract(sin(vec3(dot(n, s0.xyz), dot(n, s0.yzw), dot(n, s0.zwx))) * 1e4);
}

float hash11(highp float p){
	highp float p1 = fract(p * s1.x);
	p1 *= p1 + 33.33;
	return fract(p1 * p1 * 2.0);
}

float vnoise(highp float p){
	highp float i = floor(p);
	float f = fract(p);
	return mix(hash11(i), hash11(i + 1.0), f * f * f * (f * (f * 6.0 - 15.0) + 10.0));
}

float vnoise(highp vec2 p, highp float time, highp float tiles){
	p = p * tiles + time;
	vec2 i = floor(p);
	vec2 f = fract(p);
	vec2 u = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);
	vec2 t = vec2_splat(tiles);
	return mix(mix(rand12(mod(i, t)), rand12(mod(i + vec2(1.0, 0.0), t)), u.x), mix(rand12(mod(i + vec2(0.0, 1.0), t)), rand12(mod(i + vec2_splat(1.0), t)), u.x), u.y);
}

float voronoi2D(highp vec2 uv, highp float time, highp float tiles){
	uv *= tiles;
	float dist = 1.0;
	vec2 t = vec2_splat(tiles);
	for(int x = 0; x <= 1; x++){
		for(int y = 0; y <= 1; y++){
			vec2 o = vec2(float(x), float(y));
			vec2 p = floor(uv) + o;
			float d = length(0.27 * sin(rand22(mod(p, t)) * 12.0 + time) + o - fract(uv));
			dist = min(d, dist);
		}
	}
	return dist;
}

vec2 pix2D(vec2 uv, float pixSize){
	float pix = pixSize / 500.0;
	return pix * floor(uv / pix);
}

#endif
