// Plain lit mesh (ribbons) with per-vertex colour.
attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec4 a_color;

uniform mat4 u_projView;

varying vec3 v_normal;
varying vec3 v_world;
varying vec4 v_color;

void main() {
    v_normal = a_normal;
    v_world = a_position;
    v_color = a_color;
    gl_Position = u_projView * vec4(a_position, 1.0);
}
