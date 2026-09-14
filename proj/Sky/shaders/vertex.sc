$input a_position
#ifdef INSTANCING__ON
$input i_data1, i_data2, i_data3
#endif
$output v_color0, v_position

#include <bgfx_shader.sh>
#include <estn/functionLib.h>
#include <estn/rd.h>

uniform vec4 SkyColor;
uniform vec4 FogColor;
uniform vec4 FogAndDistanceControl;

void main(){
	vec4 hsv = rgb2hsv(SkyColor);
	float Value = clamp2((hsv.z - 0.1) / (1.0 - 0.1));
	float rainA = clamp2(pow(max((0.7 - FogAndDistanceControl.x) / (FogAndDistanceControl.y - FogAndDistanceControl.x), 0.0), 3.0));

#ifdef INSTANCING__ON
	mat4 model = instanceModel(i_data1, i_data2, i_data3);
#else
	mat4 model = u_model[0];
#endif

	vec3 sphere = a_position;
	float gradient = length(sphere.xz);
	sphere.y -= gradient * mix(skyMax, skyMin, MIX2(0.0, 1.0, 0.0, hsv.z));

	vec3 worldPos = mul(model, vec4(sphere, 1.0)).xyz;
	gl_Position = mul(u_viewProj, vec4(worldPos, 1.0));

	vec4 sky = mix(MIX2(n_color, s_color, d_color, Value), FogColor, rainA);

	if(isUnderwater(FogAndDistanceControl)){
		v_color0 = FogColor;
	}else{
		v_color0 = MIX2(sky, vec4(FogColor.rgb * 1.8, 1.0), FogColor, gradient);
	}

	v_position = sphere;
}
