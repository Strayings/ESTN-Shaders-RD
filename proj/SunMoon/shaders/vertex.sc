$input a_position, a_texcoord0
#ifdef INSTANCING__ON
$input i_data1, i_data2, i_data3
#endif
$output v_texcoord0, v_position

#include <bgfx_shader.sh>
#include <estn/functionLib.h>
#include <estn/rd.h>

void main(){
#ifdef INSTANCING__ON
	mat4 model = instanceModel(i_data1, i_data2, i_data3);
#else
	mat4 model = u_model[0];
#endif
#ifdef CSUN
	vec3 N = a_position * vec3(12.0, 0.25, 12.0);
	N.y -= 0.32;
#else
	vec3 N = a_position;
#endif
	vec3 worldPos = mul(model, vec4(N, 1.0)).xyz;

	v_position = a_position.xz;
	v_texcoord0 = a_texcoord0;

	gl_Position = mul(u_viewProj, vec4(worldPos, 1.0));
}
