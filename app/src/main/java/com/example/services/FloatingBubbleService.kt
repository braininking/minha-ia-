package com.example.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.MainActivity
import com.example.R

class FloatingBubbleService : Service() {

    companion object {
        const val ACTION_STOP_BUBBLE = "com.example.ACTION_STOP_BUBBLE"

        fun startBubble(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            val intent = Intent(context, FloatingBubbleService::class.java)
            context.startService(intent)
        }

        fun stopBubble(context: Context) {
            val intent = Intent(context, FloatingBubbleService::class.java).apply {
                action = ACTION_STOP_BUBBLE
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createFloatingBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_BUBBLE) {
            removeBubble()
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createFloatingBubble() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            140, // 56dp approx
            140,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 200
        }

        val rootLayout = FrameLayout(this).apply {
            val background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#050706"))
                setStroke(4, Color.parseColor("#39FF14"))
            }
            this.background = background
            elevation = 16f
        }

        val iconView = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher_round)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(12, 12, 12, 12)
        }

        rootLayout.addView(
            iconView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Dragging & Click Handling
        rootLayout.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var startClickTime = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        startClickTime = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(rootLayout, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val duration = System.currentTimeMillis() - startClickTime
                        val moved = Math.hypot(
                            (event.rawX - initialTouchX).toDouble(),
                            (event.rawY - initialTouchY).toDouble()
                        )
                        if (duration < 250 && moved < 15) {
                            // Click detected! Open app
                            val appIntent = Intent(this@FloatingBubbleService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(appIntent)
                        }
                        return true
                    }
                }
                return false
            }
        })

        this.bubbleView = rootLayout
        try {
            windowManager?.addView(rootLayout, params)
        } catch (e: Exception) {
            stopSelf()
        }
    }

    private fun removeBubble() {
        if (bubbleView != null) {
            try {
                windowManager?.removeView(bubbleView)
            } catch (_: Exception) {}
            bubbleView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeBubble()
    }
}
