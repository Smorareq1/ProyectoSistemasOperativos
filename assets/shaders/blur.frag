// High-quality Gaussian blur (9-tap kernel, horizontal or vertical)
#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_dir;        // (1/w, 0) for horizontal, (0, 1/h) for vertical
uniform float u_radius;    // blur radius multiplier

// 9-tap Gaussian weights (sigma ~3.0)
const int SAMPLES = 9;
const float weights[9] = float[](
    0.10855, 0.13135, 0.10406, 0.07216,
    0.04380, 0.02328, 0.01083, 0.00441, 0.00157
);

void main() {
    vec3 result = texture2D(u_texture, v_texCoords).rgb * weights[0];
    for (int i = 1; i < SAMPLES; i++) {
        vec2 offset = u_dir * float(i) * u_radius;
        result += texture2D(u_texture, v_texCoords + offset).rgb * weights[i];
        result += texture2D(u_texture, v_texCoords - offset).rgb * weights[i];
    }
    gl_FragColor = vec4(result, 1.0);
}
