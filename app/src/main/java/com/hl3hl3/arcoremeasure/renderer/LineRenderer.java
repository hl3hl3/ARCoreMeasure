package com.hl3hl3.arcoremeasure.renderer;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.google.ar.core.examples.java.helloar.rendering.ShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 *
 * https://stackoverflow.com/questions/16027455/what-is-the-easiest-way-to-draw-line-using-opengl-es-android
 *
 * Created by user on 2017/9/25.
 */

public class LineRenderer {

    private final int mProgram;

    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    // the matrix must be included as a modifier of gl_Position
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";

    // Use to access and set the view transformation
    private int mMVPMatrixHandle;

    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";


    private FloatBuffer vertexBuffer;

    static final int COORDS_PER_VERTEX = 3;
    static float coordinates[] = { // in counterclockwise order:
            0.0f,  0.0f, 0.0f, // point 1
            1.0f,  0.0f, 0.0f, // point 2
    };

    float color[] = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

    private int mPositionHandle;
    private int mColorHandle;

    private final int vertexCount = coordinates.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4;

    private int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    public LineRenderer() {
        // initialize vertex byte buffer for shape coordinates
        // number ofr coordinate values * 4 bytes per float
        ByteBuffer bb = ByteBuffer.allocateDirect(coordinates.length * 4);
        bb.order(ByteOrder.nativeOrder());  // use the device hardware's native byte order
        vertexBuffer = bb.asFloatBuffer();  // create a floating point buffer from the ByteBuffer
        vertexBuffer.put(coordinates);  // add the coordinate to the FloatBuffer
        vertexBuffer.position(0);   // set the buffer to read the first coordinate

        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();    // create empty OpenGL ES Program
        GLES20.glAttachShader(mProgram, vertexShader);  // add the shader to program
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram); // create OpenGL ES program executables
        Matrix.setIdentityM(mModelMatrix, 0);
    }

    public void setVerts(float v0, float v1, float v2, float v3, float v4, float v5) {
        coordinates[0] = v0;
        coordinates[1] = v1;
        coordinates[2] = v2;
        coordinates[3] = v3;
        coordinates[4] = v4;
        coordinates[5] = v5;

        vertexBuffer.put(coordinates);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);
    }

    public void setColor(float red, float green, float blue, float alpha) {
        color[0] = red;
        color[1] = green;
        color[2] = blue;
        color[3] = alpha;
    }

    // Temporary matrices allocated here to reduce number of allocations for each frame.
    private float[] mModelMatrix = new float[16];
    private float[] mModelViewMatrix = new float[16];
    private float[] mModelViewProjectionMatrix = new float[16];
    final String TAG = "Line";

    public void draw(float[] cameraView, float[] cameraPerspective) {
        ShaderUtil.checkGLError(TAG, "Before draw");

        // Build the ModelView and ModelViewProjection matrices
        // for calculating object position and light.
        Matrix.multiplyMM(mModelViewMatrix, 0, cameraView, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mModelViewProjectionMatrix, 0, cameraPerspective, 0, mModelViewMatrix, 0);

        // add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);
        ShaderUtil.checkGLError(TAG, "After glBindBuffer");

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        ShaderUtil.checkGLError(TAG, "After glBindBuffer");

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        ShaderUtil.checkGLError(TAG, "After glGetAttribLocation");

        // enable a handle to the vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        ShaderUtil.checkGLError(TAG, "After glEnableVertexAttribArray");

        // prepare the coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);
        ShaderUtil.checkGLError(TAG, "After glVertexAttribPointer");

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);
        ShaderUtil.checkGLError(TAG, "After glUniform4fv");

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mModelViewProjectionMatrix, 0);
        ShaderUtil.checkGLError(TAG, "After glUniformMatrix4fv");

        // Draw the line
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, vertexCount);
        ShaderUtil.checkGLError(TAG, "After glDrawArrays");

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        ShaderUtil.checkGLError(TAG, "After draw");
    }


}
