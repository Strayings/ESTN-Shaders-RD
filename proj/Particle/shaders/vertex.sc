$input a_color0, a_position, a_texcoord0
#ifdef INSTANCING__ON
$input i_data1, i_data2, i_data3
#endif
$output v_color0, v_fog, v_texcoord0

#include <bgfx_shader.sh>
#include <estn/functionLib.h>
#include <estn/rd.h>

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;

void main(){
#ifdef INSTANCING__ON
	mat4 model = instanceModel(i_data1, i_data2, i_data3);
#else
	mat4 model = u_model[0];
#endif
	vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;
	vec4 clipPos = mul(u_viewProj, vec4(worldPos, 1.0));

	v_color0 = a_color0;
	v_fog = vec4(FogColor.rgb, clamp(((clipPos.z / FogAndDistanceControl.z) - FogAndDistanceControl.x) / (FogAndDistanceControl.y - FogAndDistanceControl.x), 0.0, 1.0));
	v_texcoord0 = a_texcoord0;

	gl_Position = clipPos;
}
