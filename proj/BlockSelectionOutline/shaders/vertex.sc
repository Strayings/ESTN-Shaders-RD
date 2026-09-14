$input a_position

#include <bgfx_shader.sh>
#include <estn/functionLib.h>
#include <estn/rd.h>

void main(){
	gl_Position = jitterVertexPosition(a_position);
}
