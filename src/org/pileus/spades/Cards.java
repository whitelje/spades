package org.pileus.spades;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import android.content.Context;

public class Cards extends GLSurfaceView implements GLSurfaceView.Renderer
{
	/* Shader data */
	private final String vertSource =
		"attribute vec4 vPosition;"  +
		"void main() {"              +
		"  gl_Position = vPosition;" +
		"}";

	private final String fragSource =
		"precision mediump float;" +
		"uniform vec4 vColor;"     +
		"void main() {"            +
		"  gl_FragColor = vColor;" +
		"}";

	/* Drawing data */
	private final float coords[] = {
		0.0f,  0.5f, 0.0f,
		-0.5f, -0.5f, 0.0f,
		0.5f, -0.5f, 0.0f,
	};

	private final float color[] = { 1, 0, 0, 1 };

	/* Private data */
	private int         program;
	private FloatBuffer vertBuf;

	/* Private methods */
	private int loadShader(int type, String code)
	{
		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, code);
		GLES20.glCompileShader(shader);
		return shader;
	}

	/* GLSurfaceView Methods */
	public Cards(Context context)
	{
		super(context);
		Os.debug("Cards create");

		this.setEGLContextClientVersion(2);
		this.setRenderer(this);
		this.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	/* Renderer methods */
	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config)
	{
		/* Initialize shaders */
		int vertShader = this.loadShader(GLES20.GL_VERTEX_SHADER,   vertSource);
		int fragShader = this.loadShader(GLES20.GL_FRAGMENT_SHADER, fragSource);

		/* Link shaders into an OpenGL program */
		this.program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vertShader);
		GLES20.glAttachShader(program, fragShader);
		GLES20.glLinkProgram(program);

		/* Create Vertex Array  */
		ByteBuffer byteBuf = ByteBuffer.allocateDirect(coords.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());

		this.vertBuf = byteBuf.asFloatBuffer();
		this.vertBuf.put(coords);
		this.vertBuf.position(0);
	}

	@Override
	public void onDrawFrame(GL10 unused)
	{
		/* Turn on the program */
		GLES20.glUseProgram(program);
		int posHandle = GLES20.glGetAttribLocation(program, "vPosition");
		int clrHandle = GLES20.glGetUniformLocation(program, "vColor");

		/* Draw */
		GLES20.glClearColor(0, 0, 0, 1);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glEnableVertexAttribArray(posHandle);
		GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 3*4, vertBuf);
		GLES20.glUniform4fv(clrHandle, 1, color, 0);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);
		GLES20.glDisableVertexAttribArray(posHandle);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height)
	{
		GLES20.glViewport(0, 0, width, height);
	}
}
