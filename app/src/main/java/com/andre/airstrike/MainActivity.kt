package com.andre.airstrike

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import kotlin.math.sin

class MainActivity : Activity() {
    private lateinit var game: FlightView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        game = FlightView(this)
        setContentView(game)
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (game.key(keyCode, true)) return true
        return super.onKeyDown(keyCode, event)
    }
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        game.key(keyCode, false)
        return super.onKeyUp(keyCode, event)
    }
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (game.motion(event)) return true
        return super.onGenericMotionEvent(event)
    }
}

private class FlightView : View {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var speed = 0.45f
    private var steer = 0f
    private var pitch = 0f
    private var boost = false
    private var frame = 0L
    private var last = System.nanoTime()
    private var slowTouch = false
    private var boostTouch = false
    private var downX = 0f
    private var downY = 0f

    constructor(context: android.content.Context) : super(context) {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun key(code: Int, pressed: Boolean): Boolean {
        when (code) {
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_SPACE -> boost = pressed
            KeyEvent.KEYCODE_DPAD_LEFT -> steer = if (pressed) -0.7f else 0f
            KeyEvent.KEYCODE_DPAD_RIGHT -> steer = if (pressed) 0.7f else 0f
            KeyEvent.KEYCODE_DPAD_UP -> pitch = if (pressed) -0.4f else 0f
            KeyEvent.KEYCODE_DPAD_DOWN -> pitch = if (pressed) 0.4f else 0f
            KeyEvent.KEYCODE_BUTTON_R2 -> speed = if (pressed) 0.95f else 0.45f
            KeyEvent.KEYCODE_BUTTON_L2 -> speed = if (pressed) 0.2f else 0.45f
            else -> return false
        }
        return true
    }

    fun motion(e: MotionEvent): Boolean {
        if (e.source and android.view.InputDevice.SOURCE_JOYSTICK == android.view.InputDevice.SOURCE_JOYSTICK && e.action == MotionEvent.ACTION_MOVE) {
            steer = e.getAxisValue(MotionEvent.AXIS_X).coerceIn(-1f, 1f)
            pitch = e.getAxisValue(MotionEvent.AXIS_Y).coerceIn(-1f, 1f)
            val rt = e.getAxisValue(MotionEvent.AXIS_RTRIGGER)
            val lt = e.getAxisValue(MotionEvent.AXIS_LTRIGGER)
            if (rt > 0.1f) speed = (0.35f + rt * 0.65f).coerceIn(0.15f, 1f)
            if (lt > 0.1f) speed = (0.55f - lt * 0.4f).coerceAtLeast(0.15f)
            return true
        }
        return false
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                downX = e.x; downY = e.y
                slowTouch = e.x < width * 0.25f
                boostTouch = e.x > width * 0.75f
                if (slowTouch) speed = (speed - 0.15f).coerceAtLeast(0.15f)
                if (boostTouch) speed = (speed + 0.2f).coerceAtMost(1f)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (downX < width * 0.4f) {
                    steer = ((e.x - downX) / (width * 0.25f)).coerceIn(-1f, 1f)
                    pitch = ((e.y - downY) / (height * 0.3f)).coerceIn(-1f, 1f)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                steer = 0f; pitch = 0f; slowTouch = false; boostTouch = false
                return true
            }
        }
        return true
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val dt = ((System.nanoTime() - last) / 1e9f).coerceAtMost(0.05f)
        last = System.nanoTime()
        frame += 1
        if (boost) speed = (speed + dt * 0.35f).coerceAtMost(1f)
        val w = width.toFloat(); val h = height.toFloat()
        c.drawColor(Color.rgb(55, 135, 190))
        p.color = Color.rgb(190, 220, 235); p.alpha = 180
        for (i in 0..5) {
            val x = ((i * 227f + frame * speed * 2f) % (w + 180f)) - 90f
            c.drawOval(x, h * (0.15f + (i % 3) * 0.12f), x + 90, h * (0.20f + (i % 3) * 0.12f), p)
        }
        p.color = Color.rgb(35, 105, 85)
        val horizon = h * 0.72f + pitch * 25
        c.drawRect(0f, horizon, w, h, p)
        p.color = Color.rgb(30, 75, 60)
        for (i in 0..14) {
            val x = ((i * 173f - frame * speed * 5f) % (w + 120f) + w + 120f) % (w + 120f) - 60f
            c.drawCircle(x, horizon + 25 + (i % 4) * 12f, 9f, p)
        }
        val cx = w * 0.5f + steer * w * 0.18f
        val cy = h * 0.57f + pitch * 35f + sin(frame / 15.0).toFloat() * 4
        p.color = Color.rgb(225, 232, 240)
        c.save(); c.rotate(-steer * 24f, cx, cy)
        c.drawRoundRect(cx - 13, cy - 58, cx + 13, cy + 55, 12f, 12f, p)
        p.color = Color.rgb(35, 105, 220)
        c.drawRect(cx - 82, cy - 5, cx + 82, cy + 5, p)
        c.drawRect(cx - 33, cy + 32, cx + 33, cy + 38, p)
        c.restore()
        p.color = Color.WHITE; p.textSize = 30f; p.isFakeBoldText = true
        c.drawText("AIRSTRIKE", 28f, 42f, p)
        p.textSize = 19f
        c.drawText("SPEED  ${(speed * 240).toInt()} km/h", 30f, 72f, p)
        p.textSize = 16f
        c.drawText("LEFT: STEER     RIGHT: BOOST", 30f, h - 24f, p)
        c.drawRoundRect(w - 150, 22f, w - 28, 64f, 12f, 12f, p)
        p.color = Color.BLACK; p.textSize = 18f
        c.drawText("BOOST", w - 125, 50f, p)
        postInvalidateOnAnimation()
    }
}
