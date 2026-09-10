package com.blackrussia.laws

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var menuView: View? = null
    private var textView: View? = null

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var menuParams: WindowManager.LayoutParams? = null
    private var textParams: WindowManager.LayoutParams? = null

    private val CHANNEL_ID = "overlay_channel"
    private val NOTIFICATION_ID = 1001

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        prepareTextFiles()
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Black Russia Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Плавающий оверлей законов"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Black Russia Laws")
            .setContentText("Оверлей активен. Нажмите на кружок.")
            .setSmallIcon(R.drawable.ic_bear)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun prepareTextFiles() {
        val filesDir = getExternalFilesDir(null) ?: filesDir
        val zakonyFile = File(filesDir, "zakony.txt")
        val pravilaFile = File(filesDir, "pravila.txt")

        if (!zakonyFile.exists()) {
            assets.open("zakony.txt").use { input ->
                FileOutputStream(zakonyFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        if (!pravilaFile.exists()) {
            assets.open("pravila.txt").use { input ->
                FileOutputStream(pravilaFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun loadText(fileName: String): String {
        val filesDir = getExternalFilesDir(null) ?: filesDir
        val file = File(filesDir, fileName)
        return if (file.exists()) {
            file.readText(Charsets.UTF_8)
        } else {
            try {
                assets.open(fileName).bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                "Файл не найден. Создайте $fileName в папке приложения."
            }
        }
    }

    private fun showBubble() {
        if (bubbleView != null) return

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        bubbleView = inflater.inflate(R.layout.overlay_bubble, null)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }

        // Drag support
        bubbleView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = true

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = bubbleParams!!.x
                        initialY = bubbleParams!!.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) isClick = false
                        bubbleParams!!.x = initialX + dx
                        bubbleParams!!.y = initialY + dy
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            toggleMenu()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(bubbleView, bubbleParams)
    }

    private fun toggleMenu() {
        if (menuView != null) {
            hideMenu()
            return
        }
        showMenu()
    }

    private fun showMenu() {
        hideText()
        if (menuView != null) return

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        menuView = inflater.inflate(R.layout.overlay_menu, null)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (bubbleParams?.x ?: 50) + 70
            y = bubbleParams?.y ?: 300
        }

        menuView?.findViewById<Button>(R.id.btnZakony)?.setOnClickListener {
            showTextPanel("Законы", loadText("zakony.txt"))
        }
        menuView?.findViewById<Button>(R.id.btnPravila)?.setOnClickListener {
            showTextPanel("Правила", loadText("pravila.txt"))
        }
        menuView?.findViewById<Button>(R.id.btnCloseMenu)?.setOnClickListener {
            hideMenu()
        }

        windowManager.addView(menuView, menuParams)
    }

    private fun hideMenu() {
        menuView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            menuView = null
        }
    }

    private fun showTextPanel(title: String, content: String) {
        hideMenu()
        hideText()

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        textView = inflater.inflate(R.layout.overlay_text, null)

        textView?.findViewById<TextView>(R.id.tvTitle)?.text = title
        textView?.findViewById<TextView>(R.id.tvContent)?.text = content
        textView?.findViewById<Button>(R.id.btnBack)?.setOnClickListener {
            hideText()
            showMenu()
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        textParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        // Flags for scrollable content
        textParams!!.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

        windowManager.addView(textView, textParams)
    }

    private fun hideText() {
        textView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            textView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideText()
        hideMenu()
        bubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            bubbleView = null
        }
    }
}
