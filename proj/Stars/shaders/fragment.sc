$input v_color0, v_position

#include <bgfx_shader.sh>
#include <estn/functionLib.h>

uniform vec4 StarsColor;
uniform vec4 ViewPositionAndTime;

void main(){
#ifdef CSTAR
	float twinkle = GENWAVEC(vec4(v_position, ViewPositionAndTime.w), vec4(1.0, 2.0, 1.0, starS), starB);
	gl_FragColor = vec4(rand33(floor(v_position)) * twinkle, twinkle);
#else
	gl_FragColor = vec4(v_color0.rgb * (StarsColor.rgb * v_color0.a), v_color0.a);
#endif
}
