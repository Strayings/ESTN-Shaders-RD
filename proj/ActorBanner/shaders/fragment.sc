$input v_color0, v_fog, v_light, v_texcoords

#include <bgfx_shader.sh>
#include <estn/functionLib.h>

uniform vec4 ActorFPEpsilon;
uniform vec4 FogColor;
uniform vec4 FogControl;
uniform vec4 HudOpacity;

SAMPLER2D_AUTOREG(s_MatTexture);

void main(){
#if defined(DEPTH_ONLY_PASS) || defined(DEPTH_ONLY_OPAQUE_PASS)
#ifdef DEPTH_ONLY_PASS
	gl_FragColor = vec4_splat(0.0);
#else
	gl_FragColor = vec4(mix(vec3_splat(1.0), v_fog.rgb, v_fog.a), 1.0);
#endif
#else
	vec4 diffuse = texture2D(s_MatTexture, v_texcoords.xy);
	vec4 base = texture2D(s_MatTexture, v_texcoords.zw);

#ifdef TINTING__ENABLED
	base.a = mix(diffuse.r * diffuse.a, diffuse.a, v_color0.a);
	base.rgb *= v_color0.rgb;
#endif

#ifdef ALPHA_TEST_PASS
	if(base.a < ActorFPEpsilon.x){
		discard;
	}
#endif

	float dayA = dayAmount(FogColor);
	float nightA = nightAmount(FogColor);
	float rainA = rainAmount(FogControl);
	bool underwater = isUnderwater(FogControl);

	vec3 shadowAmbient = shadowCol * shadow_B * base.rgb;

	float uv1x = maxC(v_light);
	float uv2x = clamp2(pow(uv1x, lightShrp) * lightSize);
	float shadow_v = shdAlpha * 1.2 * (1.0 - uv2x) * mix(1.0, 0.64, max(nightA, rainA));
	base.rgb *= sqrt(v_light.rgb);
	base.rgb = base.rgb * (1.0 - shadow_v) + shadowAmbient * shadow_v;

#ifdef UNDERWATER_CAUSTIC
	if(underwater){
		base.rgb *= FogColor.rgb * (1.0 - uv2x) + uv2x;
	}
#endif

	base.rgb = toneA(base.rgb);
	vec3 grey_s = A_Saturation(base.rgb, monoSatE * (1.0 - uv2x) + uv2x);

	if(!underwater){
		base.rgb = FogControl.x < 1.0 && FogControl.x > 0.0 ? base.rgb * (1.0 - rainA) + grey_s * mix(mono_B, 1.0, uv2x) * rainA : base.rgb;
	}

#ifdef HDR
	base = rgb2hdr(base);
#endif

	base.rgb = mix(base.rgb, v_fog.rgb, v_fog.a);

#ifdef TRANSPARENT_PASS
	base.a *= HudOpacity.x;
#endif
	gl_FragColor = base;
#endif
}
