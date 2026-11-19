package com.andre.airstrike

import android.content.Context
import android.opengl.GLSurfaceView
import android.opengl.GLES20
import android.opengl.Matrix
import android.view.MotionEvent
import kotlin.math.max
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FlightSurface(context: Context) : GLSurfaceView(context) {
    @Volatile var steer = 0f
    @Volatile var pitch = 0f
    @Volatile var throttle = 0.55f
    @Volatile var boost = false
    private val renderer3d = PlaneRenderer(this)
    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer3d)
        renderMode = RENDERMODE_CONTINUOUSLY
        isFocusable = true
    }
    fun controllerKey(code: Int, down: Boolean): Boolean {
        when (code) {
            android.view.KeyEvent.KEYCODE_BUTTON_A, android.view.KeyEvent.KEYCODE_SPACE -> boost = down
            android.view.KeyEvent.KEYCODE_BUTTON_R2 -> throttle = if (down) 1f else 0.55f
            android.view.KeyEvent.KEYCODE_BUTTON_L2 -> throttle = if (down) 0.2f else 0.55f
            android.view.KeyEvent.KEYCODE_DPAD_LEFT -> steer = if (down) -0.8f else 0f
            android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> steer = if (down) 0.8f else 0f
            android.view.KeyEvent.KEYCODE_DPAD_UP -> pitch = if (down) -0.6f else 0f
            android.view.KeyEvent.KEYCODE_DPAD_DOWN -> pitch = if (down) 0.6f else 0f
            else -> return false
        }
        return true
    }
    fun joystick(x: Float, y: Float) { steer = x.coerceIn(-1f,1f); pitch = y.coerceIn(-1f,1f) }
}

private class PlaneRenderer(private val surface: FlightSurface) : GLSurfaceView.Renderer {
    private var program = 0
    private var pos = 0
    private var mvpLoc = 0
    private var colorLoc = 0
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    private val vertices = floatArrayOf(
        -1f,-1f,1f, 1f,-1f,1f, 1f,1f,1f, -1f,-1f,1f, 1f,1f,1f, -1f,1f,1f,
        -1f,-1f,-1f, -1f,1f,-1f, 1f,1f,-1f, -1f,-1f,-1f, 1f,1f,-1f, 1f,-1f,-1f,
        -1f,1f,-1f, -1f,1f,1f, 1f,1f,1f, -1f,1f,-1f, 1f,1f,1f, 1f,1f,-1f,
        -1f,-1f,-1f, 1f,-1f,-1f, 1f,-1f,1f, -1f,-1f,-1f, 1f,-1f,1f, -1f,-1f,1f,
        1f,-1f,-1f, 1f,1f,-1f, 1f,1f,1f, 1f,-1f,-1f, 1f,1f,1f, 1f,-1f,1f,
        -1f,-1f,-1f, -1f,-1f,1f, -1f,1f,1f, -1f,-1f,-1f, -1f,1f,1f, -1f,1f,-1f
    )
    private val buffer = java.nio.ByteBuffer.allocateDirect(vertices.size * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(vertices); position(0) }
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.40f,0.70f,0.94f,1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        val vs = "attribute vec4 aPosition; uniform mat4 uMVP; void main(){ gl_Position=uMVP*aPosition; }"
        val fs = "precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }"
        val v = shader(GLES20.GL_VERTEX_SHADER,vs); val f = shader(GLES20.GL_FRAGMENT_SHADER,fs)
        program = GLES20.glCreateProgram().also { GLES20.glAttachShader(it,v); GLES20.glAttachShader(it,f); GLES20.glLinkProgram(it) }
        pos = GLES20.glGetAttribLocation(program,"aPosition")
        mvpLoc = GLES20.glGetUniformLocation(program,"uMVP")
        colorLoc = GLES20.glGetUniformLocation(program,"uColor")
    }
    private fun shader(type:Int, code:String):Int {
        val s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,code); GLES20.glCompileShader(s); return s
    }
    override fun onSurfaceChanged(gl:GL10?, width:Int, height:Int) {
        GLES20.glViewport(0,0,width,height)
        Matrix.perspectiveM(projection,0,52f,max(1f,width.toFloat()/max(1,height)),0.1f,120f)
    }
    override fun onDrawFrame(gl:GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)
        Matrix.setLookAtM(view,0,0f,3.2f,9.5f,0f,0f,0f,0f,1f,0f)
        // Green terrain and distant runway.
        box(0f,-3.0f,-12f,90f,0.35f,90f,0f,0f,0f,0.16f,0.43f,0.23f,1f)
        box(0f,-2.78f,-10f,2.3f,0.025f,34f,0f,0f,0f,0.22f,0.23f,0.24f,1f)
        for (i in 0..7) box(0f,-2.74f,-31f+i*5f,0.10f,0.02f,1.4f,0f,0f,0f,0.95f,0.91f,0.72f,1f)
        // Detailed low-poly jet aircraft: fuselage, swept wings, tail, canopy and engines.
        val bank = -surface.steer*24f
        val nose = surface.pitch*13f
        box(0f,0f,0f,0.30f,0.28f,2.05f,nose,bank,0f,0.72f,0.76f,0.82f,1f)
        box(0f,0.02f,-2.12f,0.23f,0.22f,0.48f,nose,bank,0f,0.84f,0.86f,0.89f,1f)
        box(0f,0.23f,-0.55f,0.20f,0.18f,0.62f,0f,0f,0f,0.10f,0.28f,0.43f,1f)
        box(0f,-0.03f,0.10f,2.65f,0.075f,0.72f,0f,0f,0f,0.18f,0.30f,0.42f,1f)
        box(0f,0.01f,1.55f,1.05f,0.06f,0.38f,0f,0f,0f,0.72f,0.76f,0.82f,1f)
        box(0f,0.32f,1.50f,0.10f,0.48f,0.48f,0f,0f,0f,0.66f,0.70f,0.77f,1f)
        box(-0.72f,-0.16f,1.35f,0.34f,0.22f,0.78f,0f,0f,0f,0.12f,0.14f,0.17f,1f)
        box(0.72f,-0.16f,1.35f,0.34f,0.22f,0.78f,0f,0f,0f,0.12f,0.14f,0.17f,1f)
        box(0f,-0.27f,2.2f,0.48f,0.11f,0.25f,0f,0f,0f,0.95f,0.42f,0.10f,1f)
        // Cloud-like distant objects add depth and scale.
        for(i in 0..5) box((i-2.5f)*5.5f,2.8f,-24f-(i%2)*8f,1.3f,0.35f,0.7f,0f,0f,0f,0.93f,0.97f,1f,1f)
    }
    private fun box(x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,rx:Float,ry:Float,rz:Float,r:Float,g:Float,b:Float,a:Float) {
        Matrix.setIdentityM(model,0); Matrix.translateM(model,0,x,y,z)
        Matrix.rotateM(model,0,rx,1f,0f,0f); Matrix.rotateM(model,0,ry,0f,0f,1f); Matrix.rotateM(model,0,rz,0f,1f,0f)
        Matrix.scaleM(model,0,sx,sy,sz)
        Matrix.multiplyMM(mvp,0,view,0,model,0); Matrix.multiplyMM(mvp,0,projection,0,mvp,0)
        GLES20.glUniformMatrix4fv(mvpLoc,1,false,mvp,0); GLES20.glUniform4f(colorLoc,r,g,b,a)
        buffer.position(0); GLES20.glEnableVertexAttribArray(pos); GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,0,buffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,36); GLES20.glDisableVertexAttribArray(pos)
    }
}
