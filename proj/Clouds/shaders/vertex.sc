$input a_color0, a_position, a_texcoord0
#ifdef INSTANCING__ON
$input i_data1, i_data2, i_data3
#endif
$output v_color0

#include <bgfx_shader.sh>
#include <estn/functionLib.h>
#include <estn/rd.h>

uniform vec4 CloudColor;
uniform vec4 DistanceControl;
uniform vec4 FogColor;
uniform vec4 FogAndDistanceControl;

CONST(float fogNear) = 0.9;

void main(){
	float Value = pow(clamp2(maxC(CloudColor) * 1.2), 1.2);
	Value = clamp2((Value - 0.35) / (1.0 - 0.35));
	float rainA = clamp2(pow(max((0.7 - FogAndDistanceControl.x) / (FogAndDistanceControl.y - FogAndDistanceControl.x), 0.0), 3.0));

#ifdef INSTANCING__ON
	mat4 model = instanceModel(i_data1, i_data2, i_data3);
#else
	mat4 model = u_model[0];
#endif
	vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;
	gl_Position = jitterVertexPosition(worldPos);

	float depth = length(worldPos) / DistanceControl.x;
	float fog = clamp2(depth - fogNear);
	float gradient = clamp2(a_position.y);

	vec4 color = vec4(vec3_splat(1.0), a_color0.a);
#ifdef CCLOUDS
	color.a *= mix(gradientA1, gradientA2, gradient);
	color.rgb *= mix(gradientB1, gradientB2, gradient);
	color *= mix(MIX2(cn_color, cs_color, cd_color, Value), FogColor + vec4(0.12, 0.12, 0.12, 0.0), rainA);
#else
	color *= mix(MIX2(cn_color, cs_color, cd_color, Value), FogColor + vec4(0.12, 0.12, 0.12, 0.0), rainA);
#endif
	color.a *= 1.0 - fog;

	v_color0 = color;
}
