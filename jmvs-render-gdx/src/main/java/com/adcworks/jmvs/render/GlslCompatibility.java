package com.adcworks.jmvs.render;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Configures libGDX shader compilation for an OpenGL 3.2 core profile context (required on macOS), so that
 * shaders written in GLSL ES 1.0 style (libGDX's own and JMVS's) compile as GLSL 1.50.
 */
public final class GlslCompatibility {

    private GlslCompatibility() {
    }

    /**
     * Must be called before any {@link ShaderProgram} (including SpriteBatch's) is created.
     */
    public static void useCoreProfileShaders() {
        ShaderProgram.prependVertexCode = "#version 150\n#define varying out\n#define attribute in\n"
                + "#define texture2D texture\n";
        ShaderProgram.prependFragmentCode = "#version 150\n#define varying in\n#define texture2D texture\n"
                + "#define gl_FragColor fragColor\nout vec4 fragColor;\n";
    }
}
