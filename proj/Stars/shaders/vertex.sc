$input a_color0, a_position
#ifdef INSTANCING__ON
$input i_data1, i_data2, i_data3
#endif
$output v_color0, v_position

#include <bgfx_shader.sh>
#include <estn/functionLib.h>
#include <estn/rd.h>

void main(){
#ifdef INSTANCING__ON
	mat4 model = instanceModel(i_data1, i_data2, i_data3);
#else
	mat4 model = u_model[0];
#endif
	vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;

	v_color0 = a_color0;
	v_position = a_position;

	gl_Position = mul(u_viewProj, vec4(worldPos, 1.0));
}
