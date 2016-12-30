package com.aiolos.vivek.texturefill;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static android.opengl.GLES10.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES10.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glTexParameterf;

public class SquareWithTexture {

    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec2 a_TextureCoordinates;" +
                    "varying vec2 v_TextureCoordinates;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    " v_TextureCoordinates = a_TextureCoordinates;" +
                    "}";
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    "uniform sampler2D u_TextureUnit;"+
                    "varying vec2 v_TextureCoordinates;" +
                    "void main() {" +
                    "   gl_FragColor = texture2D(u_TextureUnit, v_TextureCoordinates);" +
                    "}";
    private final FloatBuffer vertexBuffer;
    private final FloatBuffer textureBuffer;
    private final ShortBuffer drawListBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    static final int COORDS_PER_VERTEX = 3;
    static float squareCoords[] = {

            -1.0f,  1.0f, 0.0f,   // top left
            1.0f, 1.0f, 0.0f,  // top right
            1.0f, -1.0f, 0.0f,   // bottom right
            -1.0f, -1.0f, 0.0f   // bottom left
    };

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    final float[] previewTextureCoordinateData =
            {
                    0.0f,0.0f, // top left
                    1.0f,0.0f, // Top-right
                    1.0f,1.0f, // Bottom-right
                    0.0f,1.0f,  // Bottom-left
            };
    private int textureDataHandle;
    private int textureUniformHandle;
    private int textureCoordinateHandle;
    private final OpenGLES20Activity openGLES20Activity;

    private final short drawOrder[] = { 0, 1, 2, 0, 2, 3 };
    float color[] = { 0.2f, 0.709803922f, 0.898039216f, 1.0f };
    private int loadTexture(final int resourceId){    final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeResource(openGLES20Activity.getResources(), resourceId, options);

            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GL_TEXTURE_2D, textureHandle[0]);

            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

            // Set filtering
            GLES20.glTexParameteri(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];}

    public SquareWithTexture(OpenGLES20Activity openGLES20Activity) {
        this.openGLES20Activity = openGLES20Activity;
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        ByteBuffer texCoordinates = ByteBuffer.allocateDirect(previewTextureCoordinateData.length * 4);
        texCoordinates.order(ByteOrder.nativeOrder());
        textureBuffer = texCoordinates.asFloatBuffer();
        textureBuffer.put(previewTextureCoordinateData);
        textureBuffer.position(0);


        // prepare shaders and OpenGL program
        int vertexShader = MyGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        textureDataHandle = loadTexture(R.drawable.puppy);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
    }

    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        textureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TextureCoordinates");
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 2, GLES20.GL_FLOAT, false,
                0, textureBuffer);
        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);

        textureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_TextureUnit");
        MyGLRenderer.checkGlError("glGetUniformLocation");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GL_TEXTURE_2D, textureDataHandle);
        GLES20.glUniform1i(textureUniformHandle, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }}