$input a_position, a_color0, a_texcoord0, a_indices, a_normal
#ifdef INSTANCING__ON
$input i_data1, i_data2, i_data3
#endif
$output v_color0, v_fog, v_light, v_texcoord0

#include <bgfx_shader.sh>
#include <estn/functionLib.h>
#include <estn/rd.h>

uniform mat4 Bones[8];
uniform vec4 FogColor;
uniform vec4 FogControl;
uniform vec4 OverlayColor;
uniform vec4 TileLightColor;
uniform vec4 UVAnimation;

#define AMBIENT 0.45
#define XFAC (-0.1)
#define ZFAC 0.1

float lightIntensity(mat4 world, vec4 normal){
#ifdef FANCY__ON
	vec3 N = normalize(mul(world, normal)).xyz;
	N.y *= TileLightColor.w;
	float yLight = (1.0 + N.y) * 0.5;
	return yLight * (1.0 - AMBIENT) + N.x * N.x * XFAC + N.z * N.z * ZFAC + AMBIENT;
#else
	return 1.0;
#endif
}

void main(){
	mat4 World = mul(u_model[0], Bones[int(a_indices)]);
	float L = lightIntensity(World, vec4(a_normal.xyz, 0.0));
#ifdef INSTANCING__ON
	World = instanceModel(i_data1, i_data2, i_data3);
#endif
	vec3 worldPos = mul(World, vec4(a_position, 1.0)).xyz;
	vec4 pos = jitterVertexPosition(worldPos);
	gl_Position = pos;

	L += OverlayColor.a * 0.35;
	vec4 light = vec4(vec3_splat(L) * TileLightColor.xyz, 1.0);

	vec2 uv = UVAnimation.xy + (a_texcoord0 * UVAnimation.zw);

	vec4 fogColor = vec4(FogColor.rgb, clamp(((pos.z / FogControl.z) - FogControl.x) / (FogControl.y - FogControl.x), 0.0, 1.0));

#ifdef DEPTH_ONLY_PASS
	v_texcoord0 = vec2_splat(0.0);
	v_color0 = vec4_splat(0.0);
#else
	v_texcoord0 = uv;
	v_color0 = a_color0;
#endif
	v_light = light;
	v_fog = fogColor;
}
