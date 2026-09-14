$input v_color0, v_fog, v_texcoord0

#include <bgfx_shader.sh>
#include <estn/functionLib.h>

uniform vec4 FogColor;
uniform vec4 FogAndDistanceControl;

SAMPLER2D_AUTOREG(s_ParticleTexture);

void main(){
	vec4 diffuse = texture2D(s_ParticleTexture, v_texcoord0);

#ifdef ALPHA_TEST_PASS
	if(diffuse.a < 0.5){
		discard;
	}
#endif

	diffuse = diffuse * v_color0;

#ifdef ALPHA_TEST_PASS
	diffuse.a = 1.0;
#endif

	float rainA = clamp2(pow(max((0.7 - FogAndDistanceControl.x) / (FogAndDistanceControl.y - FogAndDistanceControl.x), 0.0), 3.0));
	bool underwater = isUnderwater(FogAndDistanceControl);

	float uv1x = 1.0;
	float uv2x = clamp2(pow(uv1x, lightShrp) * lightSize);

	diffuse.rgb *= emissValue;

#ifdef UNDERWATER_CAUSTIC
	if(underwater){
		diffuse.rgb *= FogColor.rgb * (1.0 - uv2x) + uv2x;
	}
#endif

	diffuse.rgb = toneA(diffuse.rgb);
	vec3 grey_s = A_Saturation(diffuse.rgb, monoSatE * (1.0 - uv2x) + uv2x);

	if(!underwater){
		diffuse.rgb = FogAndDistanceControl.x < 1.0 && FogAndDistanceControl.x > 0.0 ? diffuse.rgb * (1.0 - rainA) + grey_s.rgb * mix(mono_B, 1.0, uv2x) * rainA : diffuse.rgb;
	}

#ifdef HDR
	diffuse = rgb2hdr(diffuse);
#endif

	diffuse.rgb = mix(diffuse.rgb, v_fog.rgb, v_fog.a);

	gl_FragColor = diffuse;
}
