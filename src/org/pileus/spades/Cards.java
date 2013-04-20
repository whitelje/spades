package org.pileus.spades;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;

public class Cards extends GLSurfaceView implements GLSurfaceView.Renderer
{
	/* Shader data */
	private final String vertSource
		= "attribute vec4 a_position;"
		+ "attribute vec2 a_mapping;"
		+ "varying   vec2 v_mapping;"
		+ "void main() {"
		+ "  gl_Position = a_position;"
		+ "  v_mapping   = a_mapping;"
		+ "}";

	private final String fragSource
		= "precision mediump   float;"
		+ "uniform   sampler2D u_texture;"
		+ "uniform   vec4      u_color;"
		+ "varying   vec2      v_mapping;"
		+ "void main() {"
		+ "  gl_FragColor = texture2D("
		+ "    u_texture, v_mapping);"
		+ "}";

	/* Drawing data */
	private final float  vertCoords[] = {
		-0.5f,  0.5f, 0.0f,
		-0.5f, -0.5f, 0.0f,
		 0.5f, -0.5f, 0.0f,
		 0.5f,  0.5f, 0.0f,
	};

	private final float  mapCoords[] = {
		0.0f, 0.0f,
		0.0f, 1.0f,
		1.0f, 1.0f,
		1.0f, 0.0f,
	};

	private final float  color[] = {
		1, 0, 0, 1
	};

	/* Cards data */
	private final String cards[] = {
		"As", "Ks", "Qs", "Js", "10s", "9s", "8s", "7s", "6s", "5s", "4s", "3s", "2s",
		"Ah", "Kh", "Qh", "Jh", "10h", "9h", "8h", "7h", "6h", "5h", "4h", "3h", "2h",
		"Ac", "Kc", "Qc", "Jc", "10c", "9c", "8c", "7c", "6c", "5c", "4c", "3c", "2c",
		"Ad", "Kd", "Qd", "Jd", "10d", "9d", "8d", "7d", "6d", "5d", "4d", "3d", "2d",
	};

	/* Private data */
	private Resources    res;         // app resources
	private int          program;     // opengl program

	private FloatBuffer  vertBuf;     // vertex position buffer
	private FloatBuffer  mapBuf;      // texture mapping coord buffer

	private int          vertHandle;  // vertex positions
	private int          mapHandle;   // texture mapping coords
	private int          texHandle;   // texture data
	private int          colorHandle; // color data

	private int          face[];      // card face textures
	private int          red;         // red card back
	private int          blue;        // blue card back

	/* Private methods */
	private int loadShader(int type, String code)
	{
		Os.debug("Cards: loadShader");

		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, code);
		GLES20.glCompileShader(shader);
		return shader;
	}

	private int loadTexture(String name)
	{
		Os.debug("Cards: loadTexture - " + name);

		final int[] tex = new int[1];

		/* Lookup the resource ID */
		int id = 0;
		try {
			id = R.drawable.class.getField(name).getInt(null);
		} catch(Exception e) {
			Os.debug("Cards: lookup failed for '" + name + "'", e);
			return 0;
		}

		/* Load the bitmap */
		Bitmap bitmap = BitmapFactory.decodeResource(this.res, id);

		/* Copy into OpenGL */
		GLES20.glGenTextures(1, tex, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

		return tex[0];
	}

	private FloatBuffer loadBuffer(float[] data)
	{
		ByteBuffer bytes = ByteBuffer.allocateDirect(data.length * 4);
		bytes.order(ByteOrder.nativeOrder());

		FloatBuffer buf = bytes.asFloatBuffer();
		buf.put(data);
		buf.position(0);

		return buf;
	}

	/* GLSurfaceView Methods */
	public Cards(Context context)
	{
		super(context);
		Os.debug("Cards: create");

		this.res = context.getResources();

		this.face  = new int[52];

		this.setEGLContextClientVersion(2);
		this.setRenderer(this);
		this.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

	/* Renderer methods */
	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config)
	{
		Os.debug("Cards: onSurfaceCreate");

		/* Initialize shaders */
		int vertShader = this.loadShader(GLES20.GL_VERTEX_SHADER,   vertSource);
		int fragShader = this.loadShader(GLES20.GL_FRAGMENT_SHADER, fragSource);

		/* Link shaders into an OpenGL program */
		this.program = GLES20.glCreateProgram();
		GLES20.glAttachShader(program, vertShader);
		GLES20.glAttachShader(program, fragShader);
		GLES20.glLinkProgram(program);

		/* Get shaders attributes */
		this.vertHandle  = GLES20.glGetAttribLocation(program, "a_position");
		this.mapHandle   = GLES20.glGetAttribLocation(program, "a_mapping");
		this.texHandle   = GLES20.glGetUniformLocation(program, "u_texture");
		this.colorHandle = GLES20.glGetUniformLocation(program, "u_color");

		/* Create vertex array  */
		this.vertBuf = this.loadBuffer(this.vertCoords);
		this.mapBuf  = this.loadBuffer(this.mapCoords);

		/* Load textures */
		for (int i = 0; i < 52; i++) {
			String name = "card_" + this.cards[i].toLowerCase();
			this.face[i] = this.loadTexture(name);
		}
		this.red  = this.loadTexture("card_red");
		this.blue = this.loadTexture("card_blue");

		/* Debug */
		Os.debug("Cards: onSurfaceCreate");
	}

	@Override
	public void onDrawFrame(GL10 unused)
	{
		Os.debug("Cards: onDrawFrame");

		/* Turn on the program */
		GLES20.glUseProgram(program);

		/* Draw */
		GLES20.glClearColor(0, 0, 0, 1);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

		GLES20.glEnableVertexAttribArray(this.vertHandle);
		GLES20.glEnableVertexAttribArray(this.mapHandle);
		GLES20.glVertexAttribPointer(this.vertHandle,  3, GLES20.GL_FLOAT, false, 3*4, this.vertBuf);
		GLES20.glVertexAttribPointer(this.mapHandle, 2, GLES20.GL_FLOAT, false, 2*4, this.mapBuf);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.red);
		GLES20.glUniform1i(this.texHandle, 0);

		GLES20.glUniform4fv(this.colorHandle, 1, this.color, 0);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
		GLES20.glDisableVertexAttribArray(this.mapHandle);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height)
	{
		Os.debug("Cards: onSurfaceChanged");

		GLES20.glViewport(0, 0, width, height);
	}
}
