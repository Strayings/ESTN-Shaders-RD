$input a_position, a_color0, a_texcoord0, a_indices, a_normal
#ifdef INSTANCING__ON
$input i_data1, i_data2, i_data3
#endif

$output v_color0, v_fog, v_light, v_texcoords

#include <bgfx_shader.sh>
#include <estn/functionLib.h>
#include <estn/rd.h>

uniform mat4 Bones[8];
uniform vec4 BannerColors[7];
uniform vec4 BannerUVOffsetsAndScales[7];
uniform vec4 FogColor;
uniform vec4 FogControl;
uniform vec4 OverlayColor;
uniform vec4 TileLightColor;
uniform vec4 UVAnimation;

#define AMBIENT 0.45
#define XFAC -0.1
#define ZFAC 0.1

void main(){
	mat4 World = mul(u_model[0], Bones[int(a_indices)]);

	vec2 texcoord0 = UVAnimation.xy + (a_texcoord0 * UVAnimation.zw);

#ifdef INSTANCING__ON
	mat4 model = instanceModel(i_data1, i_data2, i_data3);
#else
	mat4 model = World;
#endif
	vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;
	vec4 pos = jitterVertexPosition(worldPos);

	float L = 1.0;
#ifdef FANCY__ON
	vec3 N = normalize(mul(World, vec4(a_normal.xyz, 0.0))).xyz;
	N.y *= TileLightColor.w;
	float yLight = (1.0 + N.y) * 0.5;
	L = yLight * (1.0 - AMBIENT) + N.x * N.x * XFAC + N.z * N.z * ZFAC + AMBIENT;
#endif
	L += OverlayColor.a * 0.35;
	vec4 light = vec4(vec3_splat(L) * TileLightColor.xyz, 1.0);

	int frameIndex = int(a_color0.w * 255.0);
	vec2 layerUV = (BannerUVOffsetsAndScales[frameIndex].zw * texcoord0) + BannerUVOffsetsAndScales[frameIndex].xy;
	vec2 baseUV = (BannerUVOffsetsAndScales[0].zw * texcoord0) + BannerUVOffsetsAndScales[0].xy;

	vec4 color = a_color0;
#ifdef TINTING__ENABLED
	color = BannerColors[frameIndex];
	color.a = 1.0;
	if(frameIndex > 0){
		color.a = 0.0;
	}
#endif

#ifdef DEPTH_ONLY_PASS
	v_color0 = vec4_splat(0.0);
#else
	v_color0 = color;
#endif
	v_fog = vec4(FogColor.rgb, clamp(((pos.z / FogControl.z) - FogControl.x) / (FogControl.y - FogControl.x), 0.0, 1.0));
	v_light = light;
	v_texcoords = vec4(layerUV.x, layerUV.y, baseUV.x, baseUV.y);
	gl_Position = pos;
}
