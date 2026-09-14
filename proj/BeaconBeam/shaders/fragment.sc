$input v_color, v_fog, v_texCoords

#include <bgfx_shader.sh>
#include <estn/functionLib.h>

uniform vec4 OverlayColor;
uniform vec4 TileLightColor;

SAMPLER2D_AUTOREG(s_BeaconTexture);

void main(){
	vec4 color = texture2D(s_BeaconTexture, v_texCoords);

#ifdef TRANSPARENT_PASS
	color.rgb = mix(color, OverlayColor, OverlayColor.a).rgb;
	color *= v_color;
#else
	color *= v_color;

	color.rgb = mix(color, OverlayColor, OverlayColor.a).rgb;

	color.rgb *= mix(emissValue, 1.0, maxC(TileLightColor));

	color.rgb = toneA(color.rgb);

#ifdef HDR
	color = rgb2hdr(color);
#endif

	color.rgb = mix(color.rgb, v_fog.rgb, v_fog.a);
#endif

	gl_FragColor = color;
}
