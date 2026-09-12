package com.blackrussia.laws

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.text.Html
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat

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

    private lateinit var repository: NotesRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        repository = NotesRepository(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
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

        val notes = repository.loadNotes()
        if (notes.isEmpty()) return

        val density = resources.displayMetrics.density

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                (14 * density).toInt(),
                (14 * density).toInt(),
                (14 * density).toInt(),
                (14 * density).toInt()
            )
            setBackgroundResource(R.drawable.menu_bg)
            elevation = 10f * density
        }

        notes.forEach { note ->
            val btn = Button(this).apply {
                text = note.title
                setBackgroundResource(R.drawable.btn_menu_item)
                setTextColor(Color.WHITE)
                textSize = 14f
                isAllCaps = false
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(
                    (18 * density).toInt(),
                    (14 * density).toInt(),
                    (18 * density).toInt(),
                    (14 * density).toInt()
                )
                elevation = 2f * density
                layoutParams = LinearLayout.LayoutParams(
                    (180 * density).toInt(),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (8 * density).toInt()
                }
                setOnClickListener {
                    showTextPanel(note.title, note.content)
                }
            }
            container.addView(btn)
        }

        val closeBtn = Button(this).apply {
            text = "Закрыть"
            setBackgroundResource(R.drawable.btn_menu_close)
            setTextColor(Color.parseColor("#CCCCCC"))
            textSize = 13f
            isAllCaps = false
            setPadding(
                (18 * density).toInt(),
                (12 * density).toInt(),
                (18 * density).toInt(),
                (12 * density).toInt()
            )
            layoutParams = LinearLayout.LayoutParams(
                (180 * density).toInt(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { hideMenu() }
        }
        container.addView(closeBtn)

        scroll.addView(container)

        val maxHeightPx = (if (notes.size > 3) 300 else 420) * density
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                scroll,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    maxHeightPx.toInt()
                )
            )
        }

        menuView = wrapper

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

    private fun showTextPanel(title: String, contentHtml: String) {
        hideMenu()
        hideText()

        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        textView = inflater.inflate(R.layout.overlay_text, null)

        textView?.findViewById<TextView>(R.id.tvTitle)?.text = title
        val tvContent = textView?.findViewById<TextView>(R.id.tvContent)
        tvContent?.text = Html.fromHtml(contentHtml, Html.FROM_HTML_MODE_COMPACT)
        tvContent?.setLineSpacing(4f, 1.15f)

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
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

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
