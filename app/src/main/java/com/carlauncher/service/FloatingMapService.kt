package com.carlauncher.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.carlauncher.R

class FloatingMapService : Service() {
    
    companion object {
        private const val TAG = "FloatingMapService"
    }
    
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingMapService created")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createFloatingWindow()
    }
    
    private fun createFloatingWindow() {
        try {
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_map_window, null)
            
            params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 100
            }
            
            setupTouchListener()
            setupCloseButton()
            
            floatingView?.let { view ->
                windowManager.addView(view, params)
                Log.d(TAG, "Floating window added")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create floating window", e)
        }
    }
    
    private fun setupTouchListener() {
        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.let { layoutParams ->
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        floatingView?.let { view ->
                            windowManager.updateViewLayout(view, layoutParams)
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupCloseButton() {
        floatingView?.findViewById<ImageView>(R.id.floating_map_close)?.setOnClickListener {
            hideFloatingWindow()
            stopSelf()
        }
    }
    
    fun showFloatingWindow() {
        try {
            floatingView?.let { view ->
                if (!view.isShown) {
                    params?.let { layoutParams ->
                        windowManager.addView(view, layoutParams)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show floating window", e)
        }
    }
    
    fun hideFloatingWindow() {
        try {
            floatingView?.let { view ->
                if (view.isShown) {
                    windowManager.removeView(view)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide floating window", e)
        }
    }
    
    fun resizeWindow(width: Int, height: Int) {
        params?.let { layoutParams ->
            layoutParams.width = width
            layoutParams.height = height
            floatingView?.let { view ->
                if (view.isShown) {
                    windowManager.updateViewLayout(view, layoutParams)
                }
            }
        }
    }
    
    fun moveWindow(x: Int, y: Int) {
        params?.let { layoutParams ->
            layoutParams.x = x
            layoutParams.y = y
            floatingView?.let { view ->
                if (view.isShown) {
                    windowManager.updateViewLayout(view, layoutParams)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "FloatingMapService destroyed")
        hideFloatingWindow()
        floatingView = null
        params = null
    }
}
