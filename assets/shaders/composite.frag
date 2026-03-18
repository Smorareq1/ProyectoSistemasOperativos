// HD-2D Final Composite: scene + DoF + bloom + vignette + color grading + atmosphere
#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;

uniform sampler2D u_scene;       // sharp pixel-art scene
uniform sampler2D u_bloom;       // bloom (blurred bright pixels)
uniform sampler2D u_dof;         // blurred scene for depth-of-field

// Bloom
uniform float u_bloomIntensity;  // 0.6

// Depth-of-field (tilt-shift)
uniform float u_focusCenter;     // Y center of sharp band (0.40)
uniform float u_focusRange;      // half-width of transition zone (0.18)
uniform float u_dofStrength;     // max blur mix (0.75)

// Vignette
uniform float u_vignetteRadius;
uniform float u_vignetteSoftness;

// Color grading
uniform float u_warmth;
uniform float u_contrast;
uniform float u_saturation;

// Atmosphere
uniform float u_time;            // for subtle animation

// --- Tilt-shift depth-of-field ---
float calcDofFactor(vec2 uv) {
    float dist = abs(uv.y - u_focusCenter);
    // Asymmetric: bottom (foreground) blurs faster than top (background)
    float range = uv.y > u_focusCenter ? u_focusRange * 1.2 : u_focusRange * 0.9;
    return smoothstep(range * 0.4, range, dist) * u_dofStrength;
}

// --- Film grain ---
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float filmGrain(vec2 uv, float time) {
    float g = hash(uv * 500.0 + vec2(time * 0.1, time * 0.07));
    return (g - 0.5) * 0.04; // very subtle
}

// --- Chromatic aberration (subtle, at edges) ---
vec3 chromaticAberration(sampler2D tex, vec2 uv, float strength) {
    vec2 center = uv - 0.5;
    float dist = length(center);
    float aberration = dist * dist * strength;
    vec2 dir = normalize(center) * aberration;
    float r = texture2D(tex, uv + dir).r;
    float g = texture2D(tex, uv).g;
    float b = texture2D(tex, uv - dir).b;
    return vec3(r, g, b);
}

void main() {
    vec2 uv = v_texCoords;

    // Sample textures
    vec3 scene = texture2D(u_scene, uv).rgb;
    vec3 bloom = texture2D(u_bloom, uv).rgb;
    vec3 dof = texture2D(u_dof, uv).rgb;

    // 1. Tilt-shift depth of field (THE key HD-2D effect)
    float dofFactor = calcDofFactor(uv);
    vec3 color = mix(scene, dof, dofFactor);

    // 2. Subtle chromatic aberration at DoF boundaries
    float caStrength = dofFactor * 0.003;
    if (caStrength > 0.0005) {
        vec3 caColor = chromaticAberration(u_scene, uv, caStrength);
        color = mix(color, mix(caColor, dof, dofFactor), 0.3);
    }

    // 3. Bloom with light scattering (downward pooling + warm tint)
    vec3 warmBloom = bloom * vec3(1.15, 1.0, 0.80); // warm-tinted bloom
    // Light scattering: sample bloom slightly below for gravity effect
    vec2 scatterUV = uv + vec2(0.0, 0.003);
    vec3 scatteredBloom = texture2D(u_bloom, scatterUV).rgb * vec3(1.1, 0.95, 0.75);
    vec3 finalBloom = mix(warmBloom, scatteredBloom, 0.3);
    color += finalBloom * u_bloomIntensity;

    // Extra bloom color bleed in dark areas (warm light fills shadows)
    float sceneLum = dot(scene, vec3(0.2126, 0.7152, 0.0722));
    float bloomBleed = (1.0 - sceneLum) * 0.15;
    color += bloom * vec3(1.2, 0.9, 0.6) * bloomBleed * u_bloomIntensity;

    // 4. Vignette (stronger, more cinematic)
    vec2 vigUV = uv - 0.5;
    float vigDist = length(vigUV * vec2(1.0, 0.85)); // slightly wider horizontally
    float vignette = smoothstep(u_vignetteRadius, u_vignetteRadius - u_vignetteSoftness, vigDist);
    vignette = vignette * 0.85 + 0.15; // never fully black
    color *= vignette;

    // 5. Color grading (HD-2D warm golden atmosphere)
    // Warm shift
    color.r *= 1.0 + u_warmth * 0.12;
    color.g *= 1.0 + u_warmth * 0.04;
    color.b *= 1.0 - u_warmth * 0.08;

    // Shadows get even warmer (like indoor firelight)
    float lum = dot(color, vec3(0.2126, 0.7152, 0.0722));
    float shadowWarmth = (1.0 - lum) * 0.08;
    color.r += shadowWarmth;
    color.g += shadowWarmth * 0.35;

    // Contrast (S-curve for more pleasing look)
    color = (color - 0.5) * u_contrast + 0.5;

    // Saturation
    float gray = dot(color, vec3(0.2126, 0.7152, 0.0722));
    color = mix(vec3(gray), color, u_saturation);

    // 6. Film grain (very subtle, adds texture)
    color += filmGrain(uv, u_time);

    // 7. Tone mapping (ACES-inspired, tuned for warm highlights)
    vec3 x = max(vec3(0.0), color);
    color = (x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14);

    // 8. Subtle color lift in shadows (cinematic look)
    color = max(color, vec3(0.015, 0.012, 0.022)); // slight blue-ish lift in blacks

    gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
