// Instanced sphere: unit-sphere vertex scaled by the instance radius and moved to the instance centre.
attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec4 a_center;
attribute vec4 a_color;

uniform mat4 u_projView;

varying vec3 v_normal;
varying vec3 v_world;
varying vec4 v_color;

void main() {
    vec3 world = a_center.xyz + a_position * a_center.w;
    v_normal = a_normal;
    v_world = world;
    v_color = a_color;
    gl_Position = u_projView * vec4(world, 1.0);
}
