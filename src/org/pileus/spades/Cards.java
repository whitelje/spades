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
	private final String vertSource =
		"attribute vec4 a_position;"  +
		"attribute vec2 a_texCoord;"  +
		"varying   vec2 v_texCoord;"  +
		"void main() {"               +
		"  gl_Position = a_position;" +
		"  v_texCoord  = a_texCoord;" +
		"}";

	private final String fragSource =
		"precision mediump   float;"      +
		"varying   vec2      v_texCoord;" +
		"uniform   sampler2D s_texture;"  +
		"uniform   vec4      a_color;"    +
		"void main() {"                   +
		//"  gl_FragColor = a_color;"       +
		"  gl_FragColor = texture2D("     +
		"    s_texture, v_texCoord);"     +
		"}";

	/* Drawing data */
	private final float  vertCoords[] = {
		-0.5f,  0.5f, 0.0f,
		-0.5f, -0.5f, 0.0f,
		 0.5f, -0.5f, 0.0f,
		 0.5f,  0.5f, 0.0f,
	};

	private final float  texCoords[] = {
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
	private Resources    res;
	private int          program;

	private FloatBuffer  vertBuf;
	private FloatBuffer  coordBuf;

	private int          vertHandle;
	private int          coordHandle;
	private int          texHandle;
	private int          colorHandle;

	private int          faces[] = new int[52];
	private int          backs[] = new int[2];
	private int          test[]  = new int[1];

	/* Private methods */
	private int loadShader(int type, String code)
	{
		Os.debug("Cards: loadShader");

		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, code);
		GLES20.glCompileShader(shader);
		return shader;
	}

	public int loadTexture(int id)
	{
		Os.debug("Cards: loadTexture");

		final int[] tex = new int[1];

		// Load the bitmap
		Bitmap bitmap = BitmapFactory.decodeResource(this.res, id);
		Os.debug("Cards: loadTexture - bitmap=" + bitmap);

		// Copy into OpenGL
		GLES20.glGenTextures(1, tex, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

		// Free the bitmap ??
		//bitmap.recycle();

		return tex[0];
	}

	/* GLSurfaceView Methods */
	public Cards(Context context)
	{
		super(context);
		Os.debug("Cards: create");

		this.res = context.getResources();

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
		this.coordHandle = GLES20.glGetAttribLocation(program, "a_texCoord");
		this.texHandle   = GLES20.glGetUniformLocation(program, "s_texture");
		this.colorHandle = GLES20.glGetUniformLocation(program, "a_color");

		// ???
		//this.coordBuf  = FloatBuffer.wrap(this.texCoords);

		/* Create vertex array  */
		ByteBuffer vertBytes = ByteBuffer.allocateDirect(vertCoords.length * 4);
		vertBytes.order(ByteOrder.nativeOrder());

		this.vertBuf = vertBytes.asFloatBuffer();
		this.vertBuf.put(vertCoords);
		this.vertBuf.position(0);

		/* Create texture coord array */
		ByteBuffer coordBytes = ByteBuffer.allocateDirect(texCoords.length * 4);
		coordBytes.order(ByteOrder.nativeOrder());

		this.coordBuf = coordBytes.asFloatBuffer();
		this.coordBuf.put(texCoords);
		this.coordBuf.position(0);

		/* Load textures */
		this.test[0] = this.loadTexture(R.drawable.card_as);
		//this.test[0] = this.staticTexture();

		/* Debug */
		Os.debug("Cards: onSurfaceCreate");
		Os.debug("Cards:     tex=" + this.test[0]);
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
		GLES20.glEnableVertexAttribArray(this.coordHandle);
		GLES20.glVertexAttribPointer(this.vertHandle,  3, GLES20.GL_FLOAT, false, 3*4, this.vertBuf);
		GLES20.glVertexAttribPointer(this.coordHandle, 2, GLES20.GL_FLOAT, false, 2*4, this.coordBuf);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, this.test[0]);
		GLES20.glUniform1i(this.texHandle, 0);

		GLES20.glUniform4fv(this.colorHandle, 1, this.color, 0);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
		GLES20.glDisableVertexAttribArray(this.coordHandle);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height)
	{
		Os.debug("Cards: onSurfaceChanged");

		GLES20.glViewport(0, 0, width, height);
	}
}
