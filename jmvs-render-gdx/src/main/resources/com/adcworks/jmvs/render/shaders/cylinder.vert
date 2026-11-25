// Instanced cylinder: unit cylinder (radius 1, y from 0 to 1) stretched between a_start and a_endRadius.xyz.
attribute vec3 a_position;
attribute vec3 a_normal;
attribute vec3 a_start;
attribute vec4 a_endRadius;
attribute vec4 a_color;

uniform mat4 u_projView;

varying vec3 v_normal;
varying vec3 v_world;
varying vec4 v_color;

void main() {
    vec3 axis = a_endRadius.xyz - a_start;
    float len = length(axis);
    vec3 y = len > 0.0001 ? axis / len : vec3(0.0, 1.0, 0.0);
    vec3 helper = abs(y.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);
    vec3 x = normalize(cross(y, helper));
    vec3 z = cross(x, y);
    float r = a_endRadius.w;
    vec3 world = a_start + y * (a_position.y * len) + (x * a_position.x + z * a_position.z) * r;
    v_normal = x * a_normal.x + z * a_normal.z;
    v_world = world;
    v_color = a_color;
    gl_Position = u_projView * vec4(world, 1.0);
}
