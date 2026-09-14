#ifndef ESTN_RD_H
#define ESTN_RD_H

uniform vec4 SubPixelOffset;

vec4 jitterVertexPosition(vec3 worldPosition){
	mat4 offsetProj = u_proj;
#if BGFX_SHADER_LANGUAGE_GLSL
	offsetProj[2][0] += SubPixelOffset.x;
	offsetProj[2][1] -= SubPixelOffset.y;
#else
	offsetProj[0][2] += SubPixelOffset.x;
	offsetProj[1][2] -= SubPixelOffset.y;
#endif
	return mul(offsetProj, mul(u_view, vec4(worldPosition, 1.0)));
}

mat4 instanceModel(vec4 d1, vec4 d2, vec4 d3){
	return mtxFromRows(d1, d2, d3, vec4(0.0, 0.0, 0.0, 1.0));
}

vec2 unpackAtlasUV(vec2 packed){
	uvec2 u = uvec2(round(packed * 65535.0));
	vec2 uv = vec2(float((u.x & 32767u) << uint(1)), float((u.y & 32767u) << uint(1))) * vec2_splat(1.525902189314365386962890625e-05);
	uv.x += 3.0517578125e-05 * ((2.0 * float((u.x & 32768u) >> uint(15))) - 1.0);
	uv.y += 3.0517578125e-05 * ((2.0 * float((u.y & 32768u) >> uint(15))) - 1.0);
	return uv;
}

vec2 unpackLightmapUV(vec2 packed){
	uvec2 u = uvec2(round(packed * 65535.0));
	return vec2(uvec2(u.y >> 4u, u.y) & uvec2(15u, 15u)) * vec2_splat(0.066666670143604278564453125);
}

float unpackMaskTinting(vec2 packed){
	uvec2 u = uvec2(round(packed * 65535.0));
	return (u.y & 256u) != 0u ? 1.0 : 0.0;
}

#endif
