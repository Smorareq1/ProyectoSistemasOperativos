// Extract bright pixels for bloom
#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_threshold;

void main() {
    vec4 color = texture2D(u_texture, v_texCoords);
    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > u_threshold) {
        gl_FragColor = color * (brightness - u_threshold) / (1.0 - u_threshold);
    } else {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
    }
}
