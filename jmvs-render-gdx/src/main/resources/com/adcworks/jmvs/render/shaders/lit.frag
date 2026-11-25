// Two-sided Blinn-Phong lighting with one directional light. Colour alpha > 1.5 marks the selected atom.
#ifdef GL_ES
precision mediump float;
#endif

uniform vec3 u_cameraPos;
uniform vec3 u_lightDir;

varying vec3 v_normal;
varying vec3 v_world;
varying vec4 v_color;

void main() {
    vec3 n = normalize(v_normal);
    vec3 v = normalize(u_cameraPos - v_world);
    if (dot(n, v) < 0.0) {
        n = -n;
    }
    vec3 l = normalize(u_lightDir);
    float diffuse = max(dot(n, l), 0.0);
    float specular = pow(max(dot(n, normalize(l + v)), 0.0), 48.0);
    vec3 base = v_color.rgb;
    if (v_color.a > 1.5) {
        base = mix(base, vec3(1.0, 0.95, 0.2), 0.65);
    }
    vec3 colour = base * (0.28 + 0.72 * diffuse) + vec3(0.3) * specular;
    gl_FragColor = vec4(colour, 1.0);
}
