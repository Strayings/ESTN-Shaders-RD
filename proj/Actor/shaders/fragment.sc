$input v_color0, v_fog, v_light, v_texcoord0

#include <bgfx_shader.sh>
#include <estn/functionLib.h>

uniform vec4 ActorFPEpsilon;
uniform vec4 ChangeColor;
uniform vec4 ColorBased;
uniform vec4 FogColor;
uniform vec4 FogControl;
uniform vec4 HudOpacity;
uniform vec4 MatColor;
uniform vec4 MultiplicativeTintColor;
uniform vec4 OverlayColor;
uniform vec4 TintedAlphaTestEnabled;
uniform vec4 UseAlphaRewrite;

SAMPLER2D_AUTOREG(s_MatTexture);
SAMPLER2D_AUTOREG(s_MatTexture1);

#if defined(EMISSIVE__EMISSIVE)
#define NEEDS_DISCARD(C, A) (dot(vec4(C.rgb, A), vec4_splat(1.0)) < ActorFPEpsilon.x)
#elif defined(EMISSIVE__EMISSIVE_ONLY)
#define NEEDS_DISCARD(C, A) ((A) < ActorFPEpsilon.x || (A) > 1.0 - ActorFPEpsilon.x)
#elif defined(CHANGE_COLOR__ON) || defined(CHANGE_COLOR__MULTI)
#define NEEDS_DISCARD(C, A) ((A) < ActorFPEpsilon.x)
#else
#define NEEDS_DISCARD(C, A) ((A) < 0.5)
#endif

void main(){
#if defined(DEPTH_ONLY_PASS)
	gl_FragColor = vec4_splat(0.0);
	return;
#else
#if defined(DEPTH_ONLY_OPAQUE_PASS)
	gl_FragColor = vec4(mix(vec3_splat(1.0), v_fog.rgb, v_fog.a), 1.0);
	return;
#else
	vec4 color = MatColor * texture2D(s_MatTexture, v_texcoord0);

#ifdef MASKED_MULTITEXTURE__ON
	vec4 tex1 = texture2D(s_MatTexture1, v_texcoord0);
	float maskedTexture = (tex1.r + tex1.g + tex1.b) * (1.0 - tex1.a) > 0.0 ? 1.0 : 0.0;
	color = mix(tex1, color, maskedTexture);
#endif

#ifdef ALPHA_TEST_PASS
	float testAlpha = mix(color.a, color.a * OverlayColor.a, TintedAlphaTestEnabled.x);
	if(NEEDS_DISCARD(color, testAlpha))
		discard;
#endif

#ifdef CHANGE_COLOR__MULTI
	vec2 colorMask = color.rg;
	color.rgb = colorMask.rrr * ChangeColor.rgb;
	color.rgb = mix(color.rgb, colorMask.ggg * MultiplicativeTintColor.rgb, vec3_splat(ceil(colorMask.g)));
#else
#ifdef CHANGE_COLOR__ON
	color.rgb = mix(color.rgb, color.rgb * ChangeColor.rgb, vec3_splat(color.a));
	color.a *= ChangeColor.a;
#endif
#endif

#ifdef ALPHA_TEST_PASS
	color.a = max(UseAlphaRewrite.x, color.a);
#endif

	color.rgb *= mix(vec3_splat(1.0), v_color0.rgb, vec3_splat(ColorBased.x));

	color.rgb = mix(color.rgb, OverlayColor.rgb, vec3_splat(OverlayColor.a));

	float dayA = dayAmount(FogColor);
	float nightA = nightAmount(FogColor);
	float rainA = rainAmount(FogControl);
	bool underwater = isUnderwater(FogControl);

	vec3 shadowAmbient = shadowCol * shadow_B * color.rgb;
	float uv1x = maxC(v_light);
	float uv2x = clamp2(pow(uv1x, lightShrp) * lightSize);
	float shadow_v = shdAlpha * 1.2 * (1.0 - uv2x) * mix(1.0, 0.64, max(nightA, rainA));

#if defined(EMISSIVE__EMISSIVE) || defined(EMISSIVE__EMISSIVE_ONLY)
	color.rgb *= vec3_splat(emissValue * (1.0 - color.a)) + sqrt(v_light.rgb) * color.a;
	#ifdef BASIC_SHADOWS
		shadow_v *= color.a;
		color.rgb = color.rgb * (1.0 - shadow_v) + shadowAmbient * shadow_v;
	#endif
#else
	color.rgb *= sqrt(v_light.rgb);
	#ifdef BASIC_SHADOWS
		color.rgb = color.rgb * (1.0 - shadow_v) + shadowAmbient * shadow_v;
	#endif
#endif

#ifdef UNDERWATER_CAUSTIC
	if(underwater){
		color.rgb *= FogColor.rgb * (1.0 - uv2x) + vec3_splat(uv2x);
	}
#endif

	color.rgb = toneA(color.rgb);
	vec3 grey_s = A_Saturation(color.rgb, monoSatE * (1.0 - uv2x) + uv2x);

	if(!underwater){
		color.rgb = FogControl.x < 1.0 && FogControl.x > 0.0 ? color.rgb * (1.0 - rainA) + grey_s * mix(mono_B, 1.0, uv2x) * rainA : color.rgb;
	}

#ifdef HDR
	color = rgb2hdr(color);
#endif

	color.rgb = mix(color.rgb, v_fog.rgb, vec3_splat(v_fog.a));

#ifdef TRANSPARENT_PASS
	color.a *= HudOpacity.x;
#endif

#ifdef OPAQUE_PASS
	color.a = 1.0;
#endif

	gl_FragColor = color;
#endif
#endif
}
