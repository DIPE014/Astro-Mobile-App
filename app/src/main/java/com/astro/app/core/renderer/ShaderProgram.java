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
     * Creates a shader program from vertex and fragment shader source code.
     *
     * @param vertexShaderCode   The vertex shader GLSL source code
     * @param fragmentShaderCode The fragment shader GLSL source code
     */
    public ShaderProgram(@NonNull String vertexShaderCode, @NonNull String fragmentShaderCode) {
        programId = createProgram(vertexShaderCode, fragmentShaderCode);
        valid = (programId != 0);
        if (!valid) {
            Log.e(TAG, "Failed to create shader program");
        }
    }

    /**
     * Checks if the shader program was successfully created.
     *
     * @return true if the program is valid and can be used
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns the OpenGL program ID.
     *
     * @return The program ID, or 0 if invalid
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
     * Sets a float uniform value.
     *
     * @param name  The uniform name
     * @param value The float value
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
     * Sets a vec2 uniform value.
     *
     * @param name The uniform name
     * @param x    X component
     * @param y    Y component
     */
    public void setUniform2f(@NonNull String name, float x, float y) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GLES20.glUniform2f(location, x, y);
        }
    }

    /**
     * Sets a vec3 uniform value.
     *
     * @param name The uniform name
     * @param x    X component
     * @param y    Y component
     * @param z    Z component
     */
    public void setUniform3f(@NonNull String name, float x, float y, float z) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GLES20.glUniform3f(location, x, y, z);
        }
    }

    /**
     * Sets a vec4 uniform value.
     *
     * @param name The uniform name
     * @param x    X component
     * @param y    Y component
     * @param z    Z component
     * @param w    W component
     */
    public void setUniform4f(@NonNull String name, float x, float y, float z, float w) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GLES20.glUniform4f(location, x, y, z, w);
        }
    }

    /**
     * Sets a 4x4 matrix uniform value.
     *
     * @param name   The uniform name
     * @param matrix The 16-element matrix array (column-major order)
     */
    public void setUniformMatrix4fv(@NonNull String name, @NonNull float[] matrix) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
        }
    }

    /**
     * Sets a 4x4 matrix uniform value with offset.
     *
     * @param name   The uniform name
     * @param matrix The matrix array
     * @param offset Starting offset in the array
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
     * Creates a shader program from vertex and fragment shader source code.
     *
     * @param vertexSource   Vertex shader GLSL source
     * @param fragmentSource Fragment shader GLSL source
     * @return The program ID, or 0 on failure
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
     * Checks for OpenGL errors and logs them.
     *
     * @param operation The operation being performed (for logging)
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
