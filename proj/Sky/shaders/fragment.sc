$input v_color0, v_position

#include <bgfx_shader.sh>
#include <estn/functionLib.h>

uniform vec4 SkyColor;
uniform vec4 FogColor;
uniform vec4 FogAndDistanceControl;
uniform vec4 ViewPositionAndTime;

#if defined(CCLOUDS) && defined(DCLOUDS)
vec4 cloudMap(highp vec2 uv){
	uv = floor(uv * 256.0) * (1.0 / 256.0);
	float n = vnoise(uv, 0.0, 16.0) * 0.55 + vnoise(uv, 0.0, 32.0) * 0.3 + vnoise(uv, 0.0, 64.0) * 0.15;
	return vec4(vec3_splat(1.0), smoothstep(0.5, 0.6, n));
}
#endif

void main(){
	vec4 color = v_color0;

	if(!isUnderwater(FogAndDistanceControl)){
		float Value = maxC(SkyColor);
		Value = sqrt(clamp2((Value - 0.2) / (1.0 - 0.2)));

		float dayA = dayAmount(FogColor);
		float nightA = nightAmount(FogColor);
		float rainA = clamp2(pow(max((0.7 - FogAndDistanceControl.x) / (FogAndDistanceControl.y - FogAndDistanceControl.x), 0.0), 3.0));

		vec3 dir = normalize(v_position);
		float lenXZ = max(length(dir.xz), 0.0001);
		vec2 hdir = dir.xz / lenXZ;
		float tanE = dir.y / lenXZ;
		bool below = dir.y < 0.0;

		float cloudLen = 0.18 / max(tanE, 0.001);
		float capLen = 0.5 / max(0.45 - tanE, 0.001);
		vec3 position = below ? vec3(hdir.x * capLen, 0.5, hdir.y * capLen) : vec3(hdir.x * cloudLen, -0.5, hdir.y * cloudLen);

#if defined(CCLOUDS) && defined(DCLOUDS)
		highp float time = ViewPositionAndTime.w;
		highp vec2 texPos = fract(position.xz * 0.12 - vec2_splat(mod(time * 0.00025, 1.0)));
		vec4 cloudTex = cloudMap(texPos);
		vec4 cloudCol = mix(MIX2(cn_color, cs_color, cd_color, Value), FogColor + vec4(0.12, 0.12, 0.12, 0.0), rainA);
#else
		vec4 cloudCol = vec4_splat(0.0);
		vec4 cloudTex = vec4_splat(0.0);
#endif
		float gradient0 = 1.0 - smoothstep(0.13, 0.52, length(position.xz));
		float gradient1 = 1.0 - smoothstep(1.2, 2.4, length(position.xz));
		vec4 final = mix(mix(mix(fd_color, fs_color, dayA), fn_color, nightA), FogColor, rainA);
		final.a = below ? gradient0 : gradient1 * cloudTex.a;
		final.rgb = below ? final.rgb : cloudCol.rgb * cloudTex.rgb;

		color.rgb = mix(color.rgb, final.rgb, final.a);
	}

	gl_FragColor = color;
}
