package com.andre.airstrike

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import kotlin.math.min
import kotlin.math.sqrt

class MainActivity : Activity() {
    private lateinit var flight: FlightSurface
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        flight = FlightSurface(this)
        val root = FrameLayout(this)
        root.addView(flight, FrameLayout.LayoutParams(-1,-1))
        root.addView(TouchControls(this, flight), FrameLayout.LayoutParams(-1,-1))
        setContentView(root)
    }
    override fun onKeyDown(keyCode:Int,event:KeyEvent):Boolean {
        if (flight.controllerKey(keyCode,true)) return true
        return super.onKeyDown(keyCode,event)
    }
    override fun onKeyUp(keyCode:Int,event:KeyEvent):Boolean {
        if (flight.controllerKey(keyCode,false)) return true
        return super.onKeyUp(keyCode,event)
    }
    override fun onGenericMotionEvent(event:MotionEvent):Boolean {
        if (event.source and android.view.InputDevice.SOURCE_JOYSTICK == android.view.InputDevice.SOURCE_JOYSTICK && event.action == MotionEvent.ACTION_MOVE) {
            flight.joystick(event.getAxisValue(MotionEvent.AXIS_X),event.getAxisValue(MotionEvent.AXIS_Y))
            val rt=event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
            val lt=event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
            if(rt>0.08f) flight.throttle=(0.45f+rt*0.55f).coerceIn(0f,1f)
            if(lt>0.08f) flight.throttle=(0.55f-lt*0.45f).coerceIn(0f,1f)
            return true
        }
        return super.onGenericMotionEvent(event)
    }
}

private class TouchControls(context:android.content.Context, private val game:FlightSurface):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    private var activePointer=-1
    private var joyX=0f
    private var joyY=0f
    init { isClickable=true }
    override fun onDraw(c:Canvas) {
        super.onDraw(c)
        val w=width.toFloat(); val h=height.toFloat()
        paint.color=Color.argb(150,8,18,32); paint.style=Paint.Style.FILL
        c.drawRoundRect(RectF(22f,18f,260f,78f),18f,18f,paint)
        paint.color=Color.WHITE; paint.textSize=27f; paint.isFakeBoldText=true
        c.drawText("AIRSTRIKE",42f,56f,paint)
        paint.textSize=17f; paint.isFakeBoldText=false
        c.drawText("3D FLIGHT  •  TOUCH OR DUALSENSE",42f,99f,paint)
        val jx=w*0.15f; val jy=h*0.76f; val jr=min(w*0.095f,h*0.17f)
        paint.color=Color.argb(90,8,18,32); c.drawCircle(jx,jy,jr,paint)
        paint.color=Color.argb(210,225,239,250); paint.style=Paint.Style.STROKE; paint.strokeWidth=4f
        c.drawCircle(jx,jy,jr,paint); c.drawLine(jx-jr*0.65f,jy,jx+jr*0.65f,jy,paint); c.drawLine(jx,jy-jr*0.65f,jx,jy+jr*0.65f,paint)
        paint.style=Paint.Style.FILL; paint.color=Color.argb(220,50,145,230)
        c.drawCircle(jx+joyX*jr*0.55f,jy+joyY*jr*0.55f,jr*0.30f,paint)
        paint.color=Color.WHITE; paint.textSize=16f; c.drawText("STEER",jx-25f,jy+jr+25f,paint)
        button(c,w*0.78f,h*0.77f,"BRAKE",Color.rgb(55,110,170))
        button(c,w*0.91f,h*0.77f,"BOOST",Color.rgb(210,105,35))
        button(c,w*0.91f,h*0.48f,"THROTTLE",Color.rgb(35,145,100))
        paint.color=Color.argb(210,255,255,255); paint.textSize=15f
        c.drawText("L2 BRAKE   R2 THROTTLE   ✕ BOOST",w*0.58f, h-22f,paint)
    }
    private fun button(c:Canvas,cx:Float,cy:Float,label:String,color:Int) {
        val r=min(width*0.055f,height*0.105f)
        paint.style=Paint.Style.FILL; paint.color=Color.argb(175,8,18,32); c.drawCircle(cx,cy,r+5f,paint)
        paint.color=color; c.drawCircle(cx,cy,r,paint)
        paint.color=Color.WHITE; paint.textSize=if(label=="THROTTLE") 13f else 16f; paint.isFakeBoldText=true
        val tw=paint.measureText(label); c.drawText(label,cx-tw/2f,cy+6f,paint); paint.isFakeBoldText=false
    }
    override fun onTouchEvent(e:MotionEvent):Boolean {
        val w=width.toFloat(); val h=height.toFloat()
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN,MotionEvent.ACTION_POINTER_DOWN -> {
                val i=e.actionIndex; val x=e.getX(i); val y=e.getY(i)
                if(x<w*0.36f) { activePointer=e.getPointerId(i); updateJoy(x,y); return true }
                applyButton(x,y,true); return true
            }
            MotionEvent.ACTION_MOVE -> {
                if(activePointer!=-1) {
                    val i=(0 until e.pointerCount).firstOrNull{e.getPointerId(it)==activePointer}
                    if(i!=null) updateJoy(e.getX(i),e.getY(i))
                }
                return true
            }
            MotionEvent.ACTION_UP,MotionEvent.ACTION_POINTER_UP,MotionEvent.ACTION_CANCEL -> {
                val i=e.actionIndex
                if(e.getPointerId(i)==activePointer) { activePointer=-1; joyX=0f; joyY=0f; game.joystick(0f,0f); invalidate() }
                val x=e.getX(i); val y=e.getY(i); applyButton(x,y,false)
                return true
            }
        }
        return true
    }
    private fun updateJoy(x:Float,y:Float) {
        val cx=width*0.15f; val cy=height*0.76f; val radius=min(width*0.095f,height*0.17f)
        joyX=((x-cx)/radius).coerceIn(-1f,1f); joyY=((y-cy)/radius).coerceIn(-1f,1f)
        game.joystick(joyX,joyY); invalidate()
    }
    private fun applyButton(x:Float,y:Float,down:Boolean) {
        val w=width.toFloat(); val h=height.toFloat()
        if(x>w*0.84f && y>h*0.64f) game.boost=down
        else if(x>w*0.70f && x<w*0.85f && y>h*0.64f) game.throttle=if(down) 0.15f else 0.55f
        else if(x>w*0.84f && y<h*0.64f) game.throttle=if(down) 1f else 0.55f
    }
}
