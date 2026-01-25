package com.astro.app.core.renderer;

import android.opengl.GLES20;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for loading, compiling, and managing OpenGL ES 2.0 shaders.
 *
 * <p>This class encapsulates the shader compilation and linking process, providing
 * a clean interface for creating and using shader programs in the sky renderer.</p>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * ShaderProgram shader = new ShaderProgram(vertexShaderCode, fragmentShaderCode);
 * if (shader.isValid()) {
 *     shader.use();
 *     int positionHandle = shader.getAttribLocation("aPosition");
 *     int matrixHandle = shader.getUniformLocation("uMVPMatrix");
 *     // ... set attributes and uniforms
 * }
 * }</pre>
 *
 * <p>Remember to call {@link #release()} when the shader is no longer needed
 * to free GPU resources.</p>
 */
public class ShaderProgram {

    private static final String TAG = "ShaderProgram";

    /** The OpenGL program ID */
    private int programId;

    /** Cache for attribute locations */
    private final Map<String, Integer> attributeLocations = new HashMap<>();

    /** Cache for uniform locations */
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    /** Whether the shader program was successfully compiled and linked */
    private boolean valid;

    /**
     * Compiles and links the given vertex and fragment GLSL sources into an OpenGL shader program and initializes this ShaderProgram.
     *
     * @param vertexShaderCode   GLSL source for the vertex shader
     * @param fragmentShaderCode GLSL source for the fragment shader
     */
    public ShaderProgram(@NonNull String vertexShaderCode, @NonNull String fragmentShaderCode) {
        programId = createProgram(vertexShaderCode, fragmentShaderCode);
        valid = (programId != 0);
        if (!valid) {
            Log.e(TAG, "Failed to create shader program");
        }
    }

    /**
     * Indicates whether the shader program was created successfully and is usable.
     *
     * @return true if the program was created successfully and can be used, false otherwise.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Get the OpenGL program identifier for this ShaderProgram.
     *
     * @return the OpenGL program ID, or 0 if the program is invalid or has been released
     */
    public int getProgramId() {
        return programId;
    }

    /**
     * Activates this shader program for rendering.
     *
     * <p>Call this before setting attributes and uniforms.</p>
     */
    public void use() {
        if (valid) {
            GLES20.glUseProgram(programId);
        }
    }

    /**
     * Gets the location of a vertex attribute.
     *
     * <p>Locations are cached for efficient repeated lookups.</p>
     *
     * @param name The attribute name in the shader
     * @return The attribute location, or -1 if not found
     */
    public int getAttribLocation(@NonNull String name) {
        if (!valid) return -1;

        Integer cached = attributeLocations.get(name);
        if (cached != null) {
            return cached;
        }

        int location = GLES20.glGetAttribLocation(programId, name);
        if (location == -1) {
            Log.w(TAG, "Attribute not found: " + name);
        }
        attributeLocations.put(name, location);
        return location;
    }

    /**
     * Gets the location of a uniform variable.
     *
     * <p>Locations are cached for efficient repeated lookups.</p>
     *
     * @param name The uniform name in the shader
     * @return The uniform location, or -1 if not found
     */
    public int getUniformLocation(@NonNull String name) {
        if (!valid) return -1;

        Integer cached = uniformLocations.get(name);
        if (cached != null) {
            return cached;
        }

        int location = GLES20.glGetUniformLocation(programId, name);
        if (location == -1) {
            Log.w(TAG, "Uniform not found: " + name);
        }
        uniformLocations.put(name, location);
        return location;
    }

    /**
     * Set the float uniform with the given name for this shader program.
     *
     * @param name  the GLSL uniform variable name
     * @param value the float value to assign to the uniform
     */
    public void setUniform1f(@NonNull String name, float value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GLES20.glUniform1f(location, value);
        }
    }

    /**
     * Sets an integer uniform value.
     *
     * @param name  The uniform name
     * @param value The integer value
     */
    public void setUniform1i(@NonNull String name, int value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GLES20.glUniform1i(location, value);
        }
    }

    /**
     * Sets the shader program's vec2 uniform to the given components.
     *
     * @param name the name of the uniform variable in the shader
     * @param x    the first (x) component of the vec2
     * @param y    the second (y) component of the vec2
     */
    public void setUniform2f(@NonNull String name, float x, float y) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GLES20.glUniform2f(location, x, y);
        }
    }

    /**
     * Sets the vec3 uniform identified by name in the currently active shader program.
     *
     * If the uniform is not found or the program is not valid, the call has no effect.
     *
     * @param name the uniform variable name in the shader
     * @param x the X component
     * @param y the Y component
     * @param z the Z component
     */
    public void setUniform3f(@NonNull String name, float x, float y, float z) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GLES20.glUniform3f(location, x, y, z);
        }
    }

    /**
     * Set the shader uniform with the given name to the specified four-component vector.
     *
     * If the uniform is not found in the program, this method performs no operation.
     *
     * @param name the uniform variable name in the shader program
     * @param x    the X component
     * @param y    the Y component
     * @param z    the Z component
     * @param w    the W component
     */
    public void setUniform4f(@NonNull String name, float x, float y, float z, float w) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GLES20.glUniform4f(location, x, y, z, w);
        }
    }

    /**
     * Assigns a 4x4 matrix to the specified uniform.
     *
     * If the named uniform is not found or not active, the call is ignored.
     *
     * @param name   the uniform variable name in the shader
     * @param matrix a 16-element float array containing the matrix in column-major order; values are read starting at index 0
     */
    public void setUniformMatrix4fv(@NonNull String name, @NonNull float[] matrix) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
        }
    }

    /**
     * Sets the 4x4 matrix uniform identified by name using values from the provided float array starting at the given offset.
     *
     * @param name   the uniform variable name in the shader
     * @param matrix float array containing the matrix values (column-major order)
     * @param offset index in the array where the 4x4 matrix begins
     */
    public void setUniformMatrix4fv(@NonNull String name, @NonNull float[] matrix, int offset) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GLES20.glUniformMatrix4fv(location, 1, false, matrix, offset);
        }
    }

    /**
     * Releases all resources associated with this shader program.
     *
     * <p>Call this when the shader is no longer needed.</p>
     */
    public void release() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId);
            programId = 0;
            valid = false;
        }
        attributeLocations.clear();
        uniformLocations.clear();
    }

    /**
     * Create and link an OpenGL ES shader program from vertex and fragment GLSL sources.
     *
     * Deletes the compiled shader objects after linking; if compilation or linking fails the program
     * is deleted and the function returns 0.
     *
     * @param vertexSource   GLSL source for the vertex shader
     * @param fragmentSource GLSL source for the fragment shader
     * @return The linked OpenGL program ID, or 0 if compilation or linking failed
     */
    private static int createProgram(@NonNull String vertexSource, @NonNull String fragmentSource) {
        Log.d(TAG, "Creating shader program...");

        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            Log.e(TAG, "Vertex shader compilation failed, cannot create program");
            return 0;
        }

        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (fragmentShader == 0) {
            Log.e(TAG, "Fragment shader compilation failed, cannot create program");
            GLES20.glDeleteShader(vertexShader);
            return 0;
        }

        int program = GLES20.glCreateProgram();
        if (program == 0) {
            Log.e(TAG, "Could not create program (glCreateProgram returned 0)");
            checkGLError("glCreateProgram");
            GLES20.glDeleteShader(vertexShader);
            GLES20.glDeleteShader(fragmentShader);
            return 0;
        }

        GLES20.glAttachShader(program, vertexShader);
        checkGLError("glAttachShader vertex");

        GLES20.glAttachShader(program, fragmentShader);
        checkGLError("glAttachShader fragment");

        GLES20.glLinkProgram(program);
        checkGLError("glLinkProgram");

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            String infoLog = GLES20.glGetProgramInfoLog(program);
            Log.e(TAG, "Could not link program: " + infoLog);
            Log.e(TAG, "Vertex shader source:\n" + vertexSource);
            Log.e(TAG, "Fragment shader source:\n" + fragmentSource);
            GLES20.glDeleteProgram(program);
            program = 0;
        } else {
            Log.d(TAG, "Shader program linked successfully (id=" + program + ")");
        }

        // Shaders can be deleted after linking
        GLES20.glDeleteShader(vertexShader);
        GLES20.glDeleteShader(fragmentShader);

        return program;
    }

    /**
     * Compiles a shader from source code.
     *
     * @param type   The shader type (GL_VERTEX_SHADER or GL_FRAGMENT_SHADER)
     * @param source The GLSL source code
     * @return The shader ID, or 0 on failure
     */
    private static int compileShader(int type, @NonNull String source) {
        String typeStr = (type == GLES20.GL_VERTEX_SHADER) ? "vertex" : "fragment";
        Log.d(TAG, "Compiling " + typeStr + " shader...");

        int shader = GLES20.glCreateShader(type);
        if (shader == 0) {
            Log.e(TAG, "Could not create shader type " + type + " (" + typeStr + ")");
            checkGLError("glCreateShader");
            return 0;
        }

        GLES20.glShaderSource(shader, source);
        checkGLError("glShaderSource");

        GLES20.glCompileShader(shader);
        checkGLError("glCompileShader");

        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] != GLES20.GL_TRUE) {
            String infoLog = GLES20.glGetShaderInfoLog(shader);
            Log.e(TAG, "Could not compile " + typeStr + " shader: " + infoLog);
            Log.e(TAG, "Shader source:\n" + source);
            GLES20.glDeleteShader(shader);
            return 0;
        }

        Log.d(TAG, typeStr + " shader compiled successfully");
        return shader;
    }

    /**
     * Logs the most recent OpenGL error, if one occurred.
     *
     * Maps the GL error code to a human-readable name and writes an error log
     * optionally prefixed with the provided operation description.
     *
     * @param operation an optional label describing the GL operation to include in the log; may be null
     */
    public static void checkGLError(@Nullable String operation) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String errorString;
            switch (error) {
                case GLES20.GL_INVALID_ENUM:
                    errorString = "GL_INVALID_ENUM";
                    break;
                case GLES20.GL_INVALID_VALUE:
                    errorString = "GL_INVALID_VALUE";
                    break;
                case GLES20.GL_INVALID_OPERATION:
                    errorString = "GL_INVALID_OPERATION";
                    break;
                case GLES20.GL_OUT_OF_MEMORY:
                    errorString = "GL_OUT_OF_MEMORY";
                    break;
                case GLES20.GL_INVALID_FRAMEBUFFER_OPERATION:
                    errorString = "GL_INVALID_FRAMEBUFFER_OPERATION";
                    break;
                default:
                    errorString = "Unknown error " + error;
            }
            String msg = operation != null
                    ? operation + ": " + errorString
                    : "GL error: " + errorString;
            Log.e(TAG, msg);
        }
    }
}