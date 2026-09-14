$input a_position, a_color0, a_texcoord0
$output v_color, v_fog, v_texCoords

#include <bgfx_shader.sh>
#include <estn/functionLib.h>
#include <estn/rd.h>

uniform vec4 FogAndDistanceControl;
uniform vec4 FogColor;
uniform vec4 UVAnimation;

void main(){
	vec4 pos = jitterVertexPosition(a_position);

	v_color = a_color0;

#ifdef TRANSPARENT_PASS
	v_fog = vec4_splat(0.0);
	v_texCoords = a_texcoord0;
#else
	v_fog = vec4(FogColor.rgb, clamp(((pos.z / FogAndDistanceControl.z) - FogAndDistanceControl.x) / (FogAndDistanceControl.y - FogAndDistanceControl.x), 0.0, 1.0));
	v_texCoords = UVAnimation.xy + (a_texcoord0 * UVAnimation.zw);
#endif

	gl_Position = pos;
}
