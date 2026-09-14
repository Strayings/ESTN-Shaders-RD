$input v_texcoord0, v_position

#include <bgfx_shader.sh>
#include <estn/functionLib.h>

uniform vec4 SunMoonColor;
uniform vec4 FogColor;

SAMPLER2D_AUTOREG(s_SunMoonTexture);

float smoothstepR(float e0, float e1, float x){
	float t = clamp2((x - e0) / (e1 - e0));
	return t * t * (3.0 - 2.0 * t);
}

void main(){
	vec4 diffuse = texture2D(s_SunMoonTexture, v_texcoord0);

#ifdef ALPHA_TEST_PASS
	if(diffuse.a < 0.5)
		discard;
#endif

	highp vec2 position = v_position;
	float dayA = dayAmount(FogColor);
	float nightA = nightAmount(FogColor);

	float sunRange = smoothstepR(0.64, -0.24, length(position));
	vec3 sunCol = mix(mix(sd_color, ss_color, dayA), sn_color, nightA).rgb;

	float S = 1.0 - length(pow(abs(position / mix(sizeSun / 1000.0, sizeMoon / 1000.0, nightA)), vec2_splat(float(sunPow))));
	S = pow(clamp2(S * 1.8), 1.8);
	vec3 ssCol = mix(sunCol * 2.0, vec3_splat(1.0), S);
	vec4 newCol = vec4(mix(sunCol * sunRange, ssCol, S), mix(sunRange, S, S));

#ifdef CSUN
	gl_FragColor = newCol * SunMoonColor;
#else
	gl_FragColor = diffuse * SunMoonColor;
#endif
}
