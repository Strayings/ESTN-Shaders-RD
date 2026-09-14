$input v_color0, v_atmoFog, v_fog, v_screenPos, v_ditheringAndMaskTinting, v_lightmapUV, v_texcoord0, v_worldPos, v_position

#include <bgfx_shader.sh>
#include <estn/functionLib.h>

SAMPLER2D_AUTOREG(s_LightMapTexture);
SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_SeasonsTexture);

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 ViewPositionAndTime;

#ifdef CUSTOM_ANGLE
#define ANGLE ((customAngle / 360.0) * pi)
#else
#define ANGLE ((terrainTime - 0.5) * pi)
#endif

float rainAmountSafe(vec4 fogControl){ return clamp2(pow(max((0.7 - fogControl.x) / (fogControl.y - fogControl.x), 0.0), 3.0)); }

void main(){
#if defined(DEPTH_ONLY_PASS) || defined(DEPTH_ONLY_OPAQUE_PASS)
#ifdef DEPTH_ONLY_PASS
	if(texture2D(s_MatTexture, v_texcoord0).a < 0.5){
		discard;
	}
	gl_FragColor = vec4_splat(0.0);
#else
	gl_FragColor = vec4(mix(vec3_splat(1.0), v_fog.rgb, v_fog.a), 1.0);
#endif
#else
	vec2 uv0 = v_texcoord0;
	vec2 uv1 = v_lightmapUV;
	vec4 color = v_color0;
	float originAlpha = v_position.w;
	vec3 world_pos = v_worldPos;
	vec3 position = v_position.xyz;
	vec2 screenPos = v_screenPos;
	float atmoFog = v_atmoFog.x;
	float beamFog = v_atmoFog.y;
	float shdFog = v_atmoFog.z;
	highp float time = ViewPositionAndTime.w;
	bool underwater = isUnderwater(FogAndDistanceControl);

	vec4 albedo = texture2D(s_MatTexture, uv0);
	float texAlpha = albedo.a;

	vec3 T = normalize(dFdx(position));
	vec3 B = normalize(dFdy(position));
	vec3 N = normalize(cross(T, B));

#ifdef NORMAL_MAPS
	float d0 = maxC(A_Contrast(albedo.rgb, normDetail));
	float d1 = maxC(A_Contrast(texture2D(s_MatTexture, uv0 + vec2(delta, 0.0)).rgb, normDetail));
	float d2 = maxC(A_Contrast(texture2D(s_MatTexture, uv0 + vec2(0.0, delta)).rgb, normDetail));
	float dx = (d0 - d1) / delta;
	float dy = (d0 - d2) / delta;
	vec3 normalMap = normalize(vec3(dx, dy, 5.0 * normDetail));
	float detailFar = min(length(world_pos) / 16.0, 1.0);
	N = (T * normalMap.x + B * normalMap.y + N * normalMap.z) * (1.0 - detailFar) + N * detailFar;
#endif

	float dayA = dayAmount(FogColor);
	float nightA = nightAmount(FogColor);
	float rainA = rainAmountSafe(FogAndDistanceControl);
	float terrainTime = pow(maxC(texture2D(s_LightMapTexture, vec2(0.0, 1.0))), 5.0);
	float terraDim0 = maxC(texture2D(s_LightMapTexture, vec2(0.0, 1.0)));
	float terraDim1 = maxC(texture2D(s_LightMapTexture, vec2(0.0, 0.0)));
	float shdMap = maxC(texture2D(s_LightMapTexture, vec2(0.0, uv1.y)));
	vec2 uv2 = clamp2(pow(uv1, vec2(lightShrp, 1.0)) * vec2(lightSize, 1.0));

	float beamFar = clamp2(abs(-world_pos.x) / (FogAndDistanceControl.z / 1.6));
	float sunBeam = pow(vnoise(atan2(world_pos.y, world_pos.z) / (pi * 2.0) * 75.1), 1.75) * 1.75;
	sunBeam = smoothstep(1.0, 0.2, length(screenPos * screenPos * screenPos)) * mix(clamp2(sunBeam) * smoothstep(0.2, 1.0, length(world_pos.yz) / 16.0), 1.0, beamFar * beamFar * beamFar);

#ifdef ALPHA_TEST_PASS
	if(albedo.a < 0.5){
		discard;
	}
#endif

	vec4 inColor = color;

#ifdef TRANSPARENT_PASS
	albedo.a *= inColor.a;
#endif

	float emissive = smoothstep(0.864, 1.0, clamp2(mix(0.45, maxC(albedo), 2.5))) * maxC(albedo);
	albedo.rgb *= texture2D(s_LightMapTexture, uv2).rgb;
#ifdef EMISSIVE_MAPS
	vec2 uv1L = (uv1 * 15.0 + 0.5) / 16.0;
	albedo.rgb *= isBlock(color) ? mix(1.0, emissValue * mix(0.4, 1.0, emissive), smoothstep(0.8564, 0.8745, uv1L.x) * mix(1.0 - shdMap * shdMap * shdMap * shdMap * shdMap, mix(1.0, 0.4, uv1.y), rainA)) : 1.0;
#endif

#if defined(SEASONS__ON) && !defined(TRANSPARENT_PASS)
	vec2 uv = inColor.xy;
	albedo.rgb *= mix(vec3_splat(1.0), texture2D(s_SeasonsTexture, uv).rgb * 2.0, inColor.b);
	albedo.rgb *= inColor.aaa;
	albedo.a = 1.0;
#else
#if !defined(ALPHA_TEST_PASS) && !defined(TRANSPARENT_PASS)
	albedo.a = inColor.a;
#endif
	vec4 hsvCol = rgb2hsv(inColor);
	float colAmb = normalize(vec2(hsvCol.z, 0.56)).x;
	inColor.rgb = isBlock(color) ? sqrt(color.rgb) : hsv2rgb(vec4(hsvCol.xy, colAmb, 1.0)).rgb;
#ifndef ALPHA_TEST_PASS
	if(v_ditheringAndMaskTinting.y > 0.5){
		albedo.rgb = mix(albedo.rgb, albedo.rgb * inColor.rgb, texAlpha) * inColor.a;
		albedo.a = 1.0;
	}
	else
#endif
	{
		albedo.rgb *= inColor.rgb;
	}
#endif

	vec3 shadowAmbient = shadowCol * shadow_B * mix(albedo.rgb, vec3_splat(1.0), shdFog);

	float shdX = 0.0;
	float shdY = 0.0;
#ifdef NORM_SHADOWS
	shdX = max(N.y, N.z) < 0.5 && !(clamp2(abs(N.z)) > 0.75) ? 1.0 : 0.0;
#endif
#ifdef EXTRA_SHADOWS
	shdX = isBlock(color) ? 1.0 - (maxC(color) - 0.64) * 31.0 : shdX;
#endif
#ifdef BASIC_SHADOWS
	shdY = 1.0 - (uv1.y - 0.867) * 124.0;
#endif
	float shadow_v = clamp2(max(shdX, shdY)) * shdAlpha;

#ifdef SHADOW_FOG
	shadow_v = clamp2(mix(shadow_v, shadow_v * 1.4, shdFog));
#endif

	albedo.rgb = mix(albedo.rgb, shadowAmbient, shadow_v * (1.0 - uv2.x) * pow(mix(1.0, shdAlpha, max(dayA, rainA)), 1.4));

#if (!defined(ALPHA_TEST_PASS) && defined(WATER_NOISE)) || defined(UNDERWATER_CAUSTIC)
	float waterNoi = 0.0;
#if defined(ALPHA_TEST_PASS) || !defined(WATER_NOISE)
	if(underwater)
#endif
	{
#if NOISE_T
		waterNoi = voronoi2D(fract(position.xz / 16.0), time * 0.27, 3.0);
#else
		waterNoi = vnoise(fract(position.xz / 16.0), time * 0.08, 6.0) * 0.9;
#endif
#ifdef LAYERED_NOISE
		waterNoi += vnoise(fract(position.xz / 16.0), time * 0.2, 45.0) * 0.15;
#endif
	}
#endif

#if !defined(ALPHA_TEST_PASS) && defined(WATER_NOISE)
	albedo.rgb *= originAlpha < 0.95 && originAlpha != 0.0 && !(isBlock(color)) ? clamp2(((waterNoi * waterNoi * waterNoi * waterNoi * 1.2) + 0.36) * (1.0 - atmoFog) + atmoFog) : 1.0;
#endif

#ifdef UNDERWATER_CAUSTIC
	if(underwater){
		float causticNoi = clamp2((waterNoi * waterNoi * waterNoi * waterNoi * 1.8) + 0.32);
		albedo.rgb *= mix(mix(causticNoi, 1.0, uv1.y * 1.24), mix(mix(causticNoi, 1.0, 0.75), 1.0, uv1.y * 1.24), uv1.x);
		albedo.rgb *= mix(max(FogColor.rgb, hsv2rgb(vec4(rgb2hsv(FogColor).x, 0.75, 1.0, 1.0)).rgb), vec3_splat(1.0), max(uv1.x, uv1.y));
	}
#endif

	if(!underwater){
		albedo.rgb = mix(albedo.rgb + (lightCol * uv2.x), albedo.rgb, mix(shdMap, 0.5, rainA));
	}

	albedo.rgb = toneA(albedo.rgb);

	vec3 grey_s = A_Saturation(albedo.rgb, mix(monoSat, 1.0, uv2.x));

	if(!underwater){
		float rainExp = rainA * uv2.y;
		albedo.rgb = FogAndDistanceControl.x < 1.0 && FogAndDistanceControl.x > 0.0 ? mix(albedo.rgb, grey_s * mix(mono_B, 1.0, uv2.x), rainExp) : albedo.rgb;
	}

#if !defined(ALPHA_TEST_PASS) && defined(SPECULAR)
	vec3 lPos = normalize(vec3(rot2d(vec2(100.0, 0.0), ANGLE), 0.0) + world_pos);
	vec3 rPos = reflect(-lPos, N);
	float spec = max(dot(rPos, normalize(-world_pos)), 0.0);
	vec3 baseSpec = isBlock(color) ? A_Saturation(texture2D(s_MatTexture, uv0).rgb, 0.6) : texture2D(s_MatTexture, uv0).rgb * A_Saturation(inColor.rgb, 0.5);
	albedo += pow(spec, 16.0) * sqrt(rgb2hsv(vec4(baseSpec, 1.0)).y * 1.3) * mix(mix(mix(sld_color, sls_color, dayA), sln_color, nightA), vec4_splat(0.0), rainA) * (1.0 - clamp2(max(shdX, shdY)));
#endif

#ifdef HDR
	albedo = rgb2hdr(albedo);
#endif

#ifdef ATMO
	vec3 atmoCol = isDimen(terraDim0, terraDim1) ? FogColor.rgb * 2.0 : mix(mix(mix(ad_color, as_color, dayA), an_color, nightA), mix(vec4_splat(0.24), FogColor, maxC(FogColor)), rainA).rgb;
	albedo.rgb = mix(albedo.rgb, atmoCol, atmoFog);
#endif

#ifdef BEAMS
	albedo.rgb = isDimen(terraDim0, terraDim1) ? albedo.rgb : mix(albedo.rgb, A_Saturation(FogColor.rgb * 1.6, 1.2) * sunBeam, beamFog * uv1.y);
#endif

#ifdef DEBUG
	albedo = vec4(floor(position) / 16.0, 1.0);
	albedo.rgb = mix(mix(albedo.rgb, vec3(0.0, 0.0, 1.0), atmoFog * 1.16), vec3(1.0, 1.0, 0.0), beamFog * 1.44);
#endif

	albedo.rgb = mix(albedo.rgb, isDimen(terraDim0, terraDim1) ? FogColor.rgb : v_fog.rgb, v_fog.a);

	gl_FragColor = albedo;
#endif
}
