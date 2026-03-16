package gt.edu.url.so.proyectosistemasoperativos.common;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * HD-2D GPU post-processing pipeline:
 * 1. Bright-pass extraction
 * 2. Multi-pass Gaussian bloom (wider, softer glow)
 * 3. Depth-of-field tilt-shift blur (THE signature HD-2D effect)
 * 4. Final composite: scene + bloom + DoF + vignette + color grading + film grain
 */
public class PostProcessingPipeline implements Disposable {

    private final int width, height;

    private ShaderProgram brightExtract;
    private ShaderProgram blur;
    private ShaderProgram composite;

    // Bloom FBOs (half-resolution for soft look)
    private FrameBuffer fbBright;
    private FrameBuffer fbBlurA;
    private FrameBuffer fbBlurB;

    // DoF FBOs (half-resolution)
    private FrameBuffer fbDofA;
    private FrameBuffer fbDofB;

    private Mesh fullscreenQuad;
    private float time = 0;

    // --- Tunable parameters ---
    // Bloom
    private float bloomThreshold = 0.45f;
    private float bloomIntensity = 0.65f;
    private float bloomRadius = 2.2f;
    private int bloomPasses = 2; // number of H+V blur iterations

    // Depth of field
    private float dofRadius = 4.5f;
    private float focusCenter = 0.40f;  // Y position of focus (0=top, 1=bottom)
    private float focusRange = 0.18f;   // transition zone half-width
    private float dofStrength = 0.75f;  // max blur amount

    // Vignette
    private float vignetteRadius = 0.72f;
    private float vignetteSoftness = 0.50f;

    // Color grading
    private float warmth = 1.2f;
    private float contrast = 1.12f;
    private float saturation = 1.20f;

    public PostProcessingPipeline(int width, int height) {
        this.width = width;
        this.height = height;

        ShaderProgram.pedantic = false;

        String vert = Gdx.files.internal("shaders/passthrough.vert").readString();
        brightExtract = new ShaderProgram(vert, Gdx.files.internal("shaders/bright_extract.frag").readString());
        if (!brightExtract.isCompiled()) {
            Gdx.app.error("PostFX", "bright_extract: " + brightExtract.getLog());
        }
        blur = new ShaderProgram(vert, Gdx.files.internal("shaders/blur.frag").readString());
        if (!blur.isCompiled()) {
            Gdx.app.error("PostFX", "blur: " + blur.getLog());
        }
        composite = new ShaderProgram(vert, Gdx.files.internal("shaders/composite.frag").readString());
        if (!composite.isCompiled()) {
            Gdx.app.error("PostFX", "composite: " + composite.getLog());
        }

        // Half-resolution for bloom and DoF (softer + faster)
        int bw = width / 2;
        int bh = height / 2;

        fbBright = createFBO(bw, bh);
        fbBlurA = createFBO(bw, bh);
        fbBlurB = createFBO(bw, bh);
        fbDofA = createFBO(bw, bh);
        fbDofB = createFBO(bw, bh);

        // Full-screen quad (clip-space)
        fullscreenQuad = new Mesh(true, 4, 6,
                new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));
        fullscreenQuad.setVertices(new float[]{
                -1, -1, 0, 0,
                 1, -1, 1, 0,
                 1,  1, 1, 1,
                -1,  1, 0, 1
        });
        fullscreenQuad.setIndices(new short[]{0, 1, 2, 2, 3, 0});
    }

    private FrameBuffer createFBO(int w, int h) {
        FrameBuffer fb = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        // Use linear filtering for smooth bloom/DoF sampling
        fb.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return fb;
    }

    /**
     * Render the full HD-2D post-processing pipeline.
     */
    public void render(Texture sceneTexture) {
        time += Gdx.graphics.getDeltaTime();
        int bw = width / 2;
        int bh = height / 2;

        // === BLOOM PIPELINE ===
        // Pass 1: Extract bright pixels
        fbBright.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        brightExtract.bind();
        brightExtract.setUniformf("u_threshold", bloomThreshold);
        sceneTexture.bind(0);
        brightExtract.setUniformi("u_texture", 0);
        fullscreenQuad.render(brightExtract, GL20.GL_TRIANGLES);
        fbBright.end();

        // Multi-pass bloom blur for wider, softer glow
        FrameBuffer src = fbBright;
        for (int pass = 0; pass < bloomPasses; pass++) {
            float radius = bloomRadius * (1.0f + pass * 0.5f);

            // Horizontal blur
            fbBlurA.begin();
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            blur.bind();
            blur.setUniformf("u_dir", 1.0f / bw, 0f);
            blur.setUniformf("u_radius", radius);
            src.getColorBufferTexture().bind(0);
            blur.setUniformi("u_texture", 0);
            fullscreenQuad.render(blur, GL20.GL_TRIANGLES);
            fbBlurA.end();

            // Vertical blur
            fbBlurB.begin();
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            blur.bind();
            blur.setUniformf("u_dir", 0f, 1.0f / bh);
            blur.setUniformf("u_radius", radius);
            fbBlurA.getColorBufferTexture().bind(0);
            blur.setUniformi("u_texture", 0);
            fullscreenQuad.render(blur, GL20.GL_TRIANGLES);
            fbBlurB.end();

            src = fbBlurB;
        }
        // Bloom result is now in fbBlurB

        // === DEPTH-OF-FIELD PIPELINE ===
        // Blur the entire scene for DoF (larger radius for obvious tilt-shift)
        // H blur
        fbDofA.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        blur.bind();
        blur.setUniformf("u_dir", 1.0f / bw, 0f);
        blur.setUniformf("u_radius", dofRadius);
        sceneTexture.bind(0);
        blur.setUniformi("u_texture", 0);
        fullscreenQuad.render(blur, GL20.GL_TRIANGLES);
        fbDofA.end();

        // V blur
        fbDofB.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        blur.bind();
        blur.setUniformf("u_dir", 0f, 1.0f / bh);
        blur.setUniformf("u_radius", dofRadius);
        fbDofA.getColorBufferTexture().bind(0);
        blur.setUniformi("u_texture", 0);
        fullscreenQuad.render(blur, GL20.GL_TRIANGLES);
        fbDofB.end();

        // Second DoF pass for even softer blur
        fbDofA.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        blur.bind();
        blur.setUniformf("u_dir", 1.0f / bw, 0f);
        blur.setUniformf("u_radius", dofRadius * 0.8f);
        fbDofB.getColorBufferTexture().bind(0);
        blur.setUniformi("u_texture", 0);
        fullscreenQuad.render(blur, GL20.GL_TRIANGLES);
        fbDofA.end();

        fbDofB.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        blur.bind();
        blur.setUniformf("u_dir", 0f, 1.0f / bh);
        blur.setUniformf("u_radius", dofRadius * 0.8f);
        fbDofA.getColorBufferTexture().bind(0);
        blur.setUniformi("u_texture", 0);
        fullscreenQuad.render(blur, GL20.GL_TRIANGLES);
        fbDofB.end();
        // DoF blurred scene is now in fbDofB

        // === FINAL COMPOSITE ===
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        composite.bind();

        // Bind textures
        sceneTexture.bind(0);
        composite.setUniformi("u_scene", 0);
        fbBlurB.getColorBufferTexture().bind(1);
        composite.setUniformi("u_bloom", 1);
        fbDofB.getColorBufferTexture().bind(2);
        composite.setUniformi("u_dof", 2);

        // Uniforms
        composite.setUniformf("u_bloomIntensity", bloomIntensity);
        composite.setUniformf("u_focusCenter", focusCenter);
        composite.setUniformf("u_focusRange", focusRange);
        composite.setUniformf("u_dofStrength", dofStrength);
        composite.setUniformf("u_vignetteRadius", vignetteRadius);
        composite.setUniformf("u_vignetteSoftness", vignetteSoftness);
        composite.setUniformf("u_warmth", warmth);
        composite.setUniformf("u_contrast", contrast);
        composite.setUniformf("u_saturation", saturation);
        composite.setUniformf("u_time", time);

        fullscreenQuad.render(composite, GL20.GL_TRIANGLES);
    }

    // --- Setters for per-screen tuning ---
    public void setBloomThreshold(float t) { bloomThreshold = t; }
    public void setBloomIntensity(float i) { bloomIntensity = i; }
    public void setBloomRadius(float r) { bloomRadius = r; }
    public void setBloomPasses(int p) { bloomPasses = p; }
    public void setDofRadius(float r) { dofRadius = r; }
    public void setFocusCenter(float c) { focusCenter = c; }
    public void setFocusRange(float r) { focusRange = r; }
    public void setDofStrength(float s) { dofStrength = s; }
    public void setVignetteRadius(float r) { vignetteRadius = r; }
    public void setVignetteSoftness(float s) { vignetteSoftness = s; }
    public void setWarmth(float w) { warmth = w; }
    public void setContrast(float c) { contrast = c; }
    public void setSaturation(float s) { saturation = s; }

    @Override
    public void dispose() {
        brightExtract.dispose();
        blur.dispose();
        composite.dispose();
        fbBright.dispose();
        fbBlurA.dispose();
        fbBlurB.dispose();
        fbDofA.dispose();
        fbDofB.dispose();
        fullscreenQuad.dispose();
    }
}
