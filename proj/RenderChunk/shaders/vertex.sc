$input a_color0, a_position, a_texcoord0, a_texcoord1
#ifdef INSTANCING__ON
$input i_data1, i_data2, i_data3
#endif
$output v_color0, v_atmoFog, v_fog, v_screenPos, v_ditheringAndMaskTinting, v_lightmapUV, v_texcoord0, v_worldPos, v_position

#include <bgfx_shader.sh>
#include <estn/functionLib.h>
#include <estn/rd.h>

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 MeshContext;
uniform vec4 RenderChunkFogAlpha;
uniform vec4 ViewPositionAndTime;

float rainAmountSafe(vec4 fogControl){ return clamp2(pow(max((0.7 - fogControl.x) / (fogControl.y - fogControl.x), 0.0), 3.0)); }

void main(){
#ifdef INSTANCING__ON
	mat4 model = instanceModel(i_data1, i_data2, i_data3);
#else
	mat4 model = u_model[0];
#endif
	vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;
	vec4 color = a_color0;
#ifdef RENDER_AS_BILLBOARDS__ON
	worldPos += vec3_splat(0.5);
	vec3 viewDir = normalize(worldPos - ViewPositionAndTime.xyz);
	vec3 boardPlane = normalize(cross(vec3(0.0, 1.0, 0.0), viewDir));
	worldPos = worldPos - ((cross(viewDir, boardPlane) * (a_color0.z - 0.5)) + (boardPlane * (a_color0.x - 0.5)));
	color = vec4_splat(1.0);
#endif

	highp float time = ViewPositionAndTime.w;
	vec3 world_pos = worldPos - ViewPositionAndTime.xyz;
	vec4 pos = jitterVertexPosition(worldPos);
	gl_Position = pos;

	vec4 fogCtl = mix(FogAndDistanceControl, vec4(0.99, 1.0, 100000.0, 100000.0), MeshContext.x > 0.5 ? 1.0 : 0.0);
	float cameraDepth = length(world_pos);

	float dayA = dayAmount(FogColor);
	float nightA = nightAmount(FogColor);
	float rainA = rainAmountSafe(FogAndDistanceControl);
	vec2 screenPos = pos.xy / (pos.z + 1.0);
	vec3 tiledPos = mod(fract(a_position / 16.0) * pi, vec3_splat(pi));
	vec2 uv1 = unpackLightmapUV(a_texcoord1);

	float heightFog = mix(maxHeight, minHeight, rainA);
	float nightFog = mix(densityMax, densityMin, nightA) * ((1.0 - rainA) + rainA * densityRain);
	float height1 = world_pos.y < 0.0 ? -(world_pos.y / heightFog) * abs(world_pos.y / heightFog) : 0.0;
	float height2 = world_pos.y < 0.0 ? -(world_pos.y / 45.0) * abs(world_pos.y / 45.0) : 0.0;
	float fog1 = cameraDepth / (fogCtl.z / nightFog);
	float fog2 = cameraDepth / heightFog;
	float fog3 = cameraDepth / 24.0;
	float atmoFog = clamp(mix(fog1 * fog1, sqrt(fog2), height1), 0.0, 0.7);
	float beamFog = clamp(mix(fog3, sqrt(fog2), height2), 0.0, 0.64) * dayA * pow(1.0 - nightA, 4.0);
#ifdef SHADOW_FOG
	float shdFog = clamp2(mix(fog1 * fog1, sqrt(fog2), height1)) * mix(1.0, 0.64, nightA);
#else
	float shdFog = 0.0;
#endif

	float waveFar = smoothstep(36.0, 24.0, cameraDepth);
#if (defined(ALPHA_TEST_PASS) || defined(DEPTH_ONLY_PASS)) && defined(PLANT_WAVES)
	float wind = GENWAVEC(vec3(tiledPos.xz, time), vec3(1.0, 1.0, 1.6), 0.75 * uv1.y);
	gl_Position.x += isPlant(color) && !(isBlock(color)) ? GENWAVES(vec4(tiledPos, time), vec4(6.0, -6.0, 6.0, 3.0), 0.025) + mix(0.014, -0.028, wind) : 0.0;
#endif

#if defined(WATER_WAVES) && defined(TRANSPARENT_PASS)
	gl_Position.y += color.a < 0.95 && !(isBlock(color)) ? GENWAVEC(vec4(tiledPos, time), vec4(4.0, 4.0, 4.0, 3.0), 0.064 * waveFar) : 0.0;
#endif

#ifdef UNDERWATER_WAVES
	if(isUnderwater(FogAndDistanceControl)){
		gl_Position.x += GENWAVEC(vec3(screenPos, time), vec3(12.0, 6.0, 1.0), 0.016 * (pos.z + 1.0));
	}
#endif

	float len = cameraDepth / fogCtl.z;
	len += RenderChunkFogAlpha.x;
	vec4 fogColor;
	fogColor.rgb = mix(mix(mix(fd_color, fs_color, dayA), fn_color, nightA), FogColor, rainA).rgb;
	fogColor.a = clamp((len - fogCtl.x) / (fogCtl.y - fogCtl.x), 0.0, 1.0);
	if(isUnderwater(FogAndDistanceControl)){
		fogColor.rgb = FogColor.rgb;
	}

#ifdef TRANSPARENT_PASS
	if(a_color0.a < 0.95){
		float cameraDist = cameraDepth / fogCtl.w;
		float alphaFadeOut = clamp(cameraDist, 0.0, 1.0);
		color.a = mix(a_color0.a, 1.0, alphaFadeOut);
	}
#endif

	v_texcoord0 = unpackAtlasUV(a_texcoord0);
	v_lightmapUV = uv1;
	v_ditheringAndMaskTinting = vec2(0.0, unpackMaskTinting(a_texcoord1));
	v_color0 = color;
	v_fog = fogColor;
	v_atmoFog = vec3(atmoFog, beamFog, shdFog);
	v_screenPos = screenPos;
	v_worldPos = world_pos;
	v_position = vec4(a_position, a_color0.a);
}
