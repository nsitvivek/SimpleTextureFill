/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aiolos.vivek.texturefill;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES10.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES10.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glTexParameterf;

/**
 * Provides drawing instructions for a GLSurfaceView object. This class
 * must override the OpenGL ES drawing lifecycle methods:
 * <ul>
 *   <li>{@link GLSurfaceView.Renderer#onSurfaceCreated}</li>
 *   <li>{@link GLSurfaceView.Renderer#onDrawFrame}</li>
 *   <li>{@link GLSurfaceView.Renderer#onSurfaceChanged}</li>
 * </ul>
 */
public class MyGLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "MyGLRenderer";
    private static final int numFrames = 3;
    private List<SquareWithTexture> mSquare;
    OpenGLES20Activity openGLES20Activity;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    private int[] mTextureDataHandle;

    public MyGLRenderer(OpenGLES20Activity openGLES20Activity) {
        this.openGLES20Activity = openGLES20Activity;
        mTextureDataHandle = new int[numFrames];
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        mSquare = new ArrayList<>();
        for (int i = 0; i < numFrames; i++) {
            loadTexture(R.drawable.puppy, i);
        }

        for (int i = 0; i < numFrames; i++) {
            SquareWithTexture sq = new SquareWithTexture(openGLES20Activity, mTextureDataHandle[i], i, numFrames);
            mSquare.add(sq);
        }
    }

    private void loadTexture(final int resourceId, int index) {

        GLES20.glGenTextures(1, mTextureDataHandle, index);

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   // No pre-scaling

        // Read in the resource
        final Bitmap bitmap = BitmapFactory.decodeResource(openGLES20Activity.getResources(), resourceId, options);

        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GL_TEXTURE_2D, mTextureDataHandle[index]);

        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

        // Set filtering
        GLES20.glTexParameteri(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

        // Recycle the bitmap, since its data has been loaded into OpenGL.
        bitmap.recycle();


        if (mTextureDataHandle[index] == 0) {
            throw new RuntimeException("Error loading texture.");
        }
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        // Draw square
        for (int i = 0; i < numFrames; i++) {
            mSquare.get(i).draw(mMVPMatrix);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

    }

    /**
     * Utility method for compiling a OpenGL shader.
     *
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

}