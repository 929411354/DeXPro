package com.dexpro.launcher.window

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.dexpro.launcher.DeXWindowManager
import com.dexpro.launcher.R

class WindowDecorator(
    context: Context,
    private val windowManager: DeXWindowManager,
    val packageName: String
) : FrameLayout(context) {

    companion object {
        private const val TITLE_BAR_HEIGHT = 48
        private const val RESIZE_HANDLE_SIZE = 20
        private const val SHADOW_RADIUS = 8f
        private const val BORDER_WIDTH = 2f
    }

    private val titleBar = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setBackgroundColor(Color.parseColor("#1E1E1E"))
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            TITLE_BAR_HEIGHT.dpToPx()
        )
    }

    val titleText = android.widget.TextView(context).apply {
        text = packageName
        setTextColor(Color.WHITE)
        layoutParams = LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            marginStart = 8.dpToPx()
        }
    }

    private val btnMinimize = ImageButton(context).apply {
        setImageResource(R.drawable.ic_minimize)
        setBackgroundColor(Color.TRANSPARENT)
        layoutParams = LinearLayout.LayoutParams(
            TITLE_BAR_HEIGHT.dpToPx(), TITLE_BAR_HEIGHT.dpToPx()
        )
        setOnClickListener { minimizeWindow() }
    }

    private val btnMaximize = ImageButton(context).apply {
        setImageResource(R.drawable.ic_maximize)
        setBackgroundColor(Color.TRANSPARENT)
        layoutParams = LinearLayout.LayoutParams(
            TITLE_BAR_HEIGHT.dpToPx(), TITLE_BAR_HEIGHT.dpToPx()
        )
        setOnClickListener { toggleMaximize() }
    }

    private val btnClose = ImageButton(context).apply {
        setImageResource(R.drawable.ic_close)
        setBackgroundColor(Color.TRANSPARENT)
        layoutParams = LinearLayout.LayoutParams(
            TITLE_BAR_HEIGHT.dpToPx(), TITLE_BAR_HEIGHT.dpToPx()
        )
        setOnClickListener { closeWindow() }
    }

    private val shadowPaint = Paint().apply {
        color = Color.parseColor("#40000000")
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#3F51B5")
        strokeWidth = BORDER_WIDTH
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val resizeHandles = listOf(
        ResizeHandle(ResizeEdge.LEFT_TOP),
        ResizeHandle(ResizeEdge.TOP),
        ResizeHandle(ResizeEdge.RIGHT_TOP),
        ResizeHandle(ResizeEdge.LEFT),
        ResizeHandle(ResizeEdge.RIGHT),
        ResizeHandle(ResizeEdge.LEFT_BOTTOM),
        ResizeHandle(ResizeEdge.BOTTOM),
        ResizeHandle(ResizeEdge.RIGHT_BOTTOM)
    )

    private var isDragging = false
    private var isResizing = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var originalBounds = Rect()
    private var activeResizeEdge: ResizeEdge? = null

    init {
        setWillNotDraw(false)
        isFocusable = true
        isFocusableInTouchMode = true

        titleBar.addView(titleText)
        titleBar.addView(btnMinimize)
        titleBar.addView(btnMaximize)
        titleBar.addView(btnClose)
        addView(titleBar)

        updateTitle()
        updateButtons()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(),
            SHADOW_RADIUS, SHADOW_RADIUS, shadowPaint)
        canvas.drawRoundRect(BORDER_WIDTH / 2, BORDER_WIDTH / 2,
            width - BORDER_WIDTH / 2, height - BORDER_WIDTH / 2,
            SHADOW_RADIUS, SHADOW_RADIUS, borderPaint)

        if (windowManager.getWindowMeta(packageName)?.isMaximized != true) {
            resizeHandles.forEach { handle -> handle.draw(canvas, this) }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isInTitleBar(event.x, event.y)) {
                    startDragging(event); return true
                }
                val edge = getResizeEdgeAt(event.x, event.y)
                if (edge != null) { startResizing(event, edge); return true }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - dragStartX; val dy = event.y - dragStartY
                    moveWindow(dx.toInt(), dy.toInt())
                    dragStartX = event.x; dragStartY = event.y; return true
                }
                if (isResizing) {
                    val dx = event.x - dragStartX; val dy = event.y - dragStartY
                    resizeWindow(dx.toInt(), dy.toInt())
                    dragStartX = event.x; dragStartY = event.y; return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false; isResizing = false; activeResizeEdge = null; return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isInTitleBar(x: Float, y: Float) = y <= TITLE_BAR_HEIGHT.dpToPx()

    private fun getResizeEdgeAt(x: Float, y: Float): ResizeEdge? =
        resizeHandles.find { it.contains(x, y, this) }?.edge

    private fun startDragging(event: MotionEvent) {
        isDragging = true; dragStartX = event.x; dragStartY = event.y
        originalBounds = windowManager.getWindowMeta(packageName)?.bounds ?: Rect()
        bringToFront(); windowManager.focusWindow(packageName)
    }

    private fun startResizing(event: MotionEvent, edge: ResizeEdge) {
        isResizing = true; activeResizeEdge = edge
        dragStartX = event.x; dragStartY = event.y
        originalBounds = windowManager.getWindowMeta(packageName)?.bounds ?: Rect()
        bringToFront(); windowManager.focusWindow(packageName)
    }

    private fun moveWindow(dx: Int, dy: Int) {
        windowManager.moveWindow(packageName, dx, dy); updatePosition()
    }

    private fun resizeWindow(dx: Int, dy: Int) {
        val edge = activeResizeEdge ?: return
        val meta = windowManager.getWindowMeta(packageName) ?: return
        val newBounds = Rect(meta.bounds)
        val sw = context.resources.displayMetrics.widthPixels
        val sh = context.resources.displayMetrics.heightPixels

        when (edge) {
            ResizeEdge.LEFT -> {
                newBounds.left = (newBounds.left + dx).coerceAtLeast(0)
                if (newBounds.width() < 200) newBounds.left = newBounds.right - 200
            }
            ResizeEdge.RIGHT -> {
                newBounds.right = (newBounds.right + dx).coerceAtMost(sw)
                if (newBounds.width() < 200) newBounds.right = newBounds.left + 200
            }
            ResizeEdge.TOP -> {
                newBounds.top = (newBounds.top + dy).coerceAtLeast(0)
                if (newBounds.height() < 200) newBounds.top = newBounds.bottom - 200
            }
            ResizeEdge.BOTTOM -> {
                newBounds.bottom = (newBounds.bottom + dy).coerceAtMost(sh)
                if (newBounds.height() < 200) newBounds.bottom = newBounds.top + 200
            }
            ResizeEdge.LEFT_TOP -> {
                newBounds.left = (newBounds.left + dx).coerceAtLeast(0)
                newBounds.top = (newBounds.top + dy).coerceAtLeast(0)
                if (newBounds.width() < 200) newBounds.left = newBounds.right - 200
                if (newBounds.height() < 200) newBounds.top = newBounds.bottom - 200
            }
            ResizeEdge.RIGHT_TOP -> {
                newBounds.right = (newBounds.right + dx).coerceAtMost(sw)
                newBounds.top = (newBounds.top + dy).coerceAtLeast(0)
                if (newBounds.width() < 200) newBounds.right = newBounds.left + 200
                if (newBounds.height() < 200) newBounds.top = newBounds.bottom - 200
            }
            ResizeEdge.LEFT_BOTTOM -> {
                newBounds.left = (newBounds.left + dx).coerceAtLeast(0)
                newBounds.bottom = (newBounds.bottom + dy).coerceAtMost(sh)
                if (newBounds.width() < 200) newBounds.left = newBounds.right - 200
                if (newBounds.height() < 200) newBounds.bottom = newBounds.top + 200
            }
            ResizeEdge.RIGHT_BOTTOM -> {
                newBounds.right = (newBounds.right + dx).coerceAtMost(sw)
                newBounds.bottom = (newBounds.bottom + dy).coerceAtMost(sh)
                if (newBounds.width() < 200) newBounds.right = newBounds.left + 200
                if (newBounds.height() < 200) newBounds.bottom = newBounds.top + 200
            }
        }

        windowManager.resizeWindow(packageName, newBounds); updatePosition()
    }

    private fun minimizeWindow() {
        windowManager.minimizeCurrentWindow(); isVisible = false
    }

    private fun toggleMaximize() {
        val meta = windowManager.getWindowMeta(packageName) ?: return
        if (meta.isMaximized) {
            windowManager.snapWindow(packageName, SnapEdge.RESTORE)
        } else {
            windowManager.snapWindow(packageName, SnapEdge.MAXIMIZE)
        }
        updateButtons()
    }

    private fun closeWindow() {
        windowManager.closeWindow(packageName)
        (parent as? ViewGroup)?.removeView(this)
    }

    fun updateTitle() {
        val appName = try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) { packageName }
        titleText.text = appName
    }

    fun updateButtons() {
        val meta = windowManager.getWindowMeta(packageName)
        btnMaximize.setImageResource(
            if (meta?.isMaximized == true) R.drawable.ic_restore else R.drawable.ic_maximize
        )
    }

    fun updatePosition() {
        val meta = windowManager.getWindowMeta(packageName) ?: return
        val bounds = meta.bounds
        layoutParams = LayoutParams(bounds.width(), bounds.height()).apply {
            leftMargin = bounds.left; topMargin = bounds.top
        }
        requestLayout()
    }

    private fun Int.dpToPx(): Int = (this * context.resources.displayMetrics.density).toInt()
}

enum class ResizeEdge {
    LEFT, RIGHT, TOP, BOTTOM,
    LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM
}

class ResizeHandle(val edge: ResizeEdge) {
    fun getBounds(parent: View): Rect {
        val size = 20.dpToPx(parent.context)
        val pw = parent.width; val ph = parent.height
        return when (edge) {
            ResizeEdge.LEFT -> Rect(0, ph / 2 - size / 2, size, ph / 2 + size / 2)
            ResizeEdge.RIGHT -> Rect(pw - size, ph / 2 - size / 2, pw, ph / 2 + size / 2)
            ResizeEdge.TOP -> Rect(pw / 2 - size / 2, 0, pw / 2 + size / 2, size)
            ResizeEdge.BOTTOM -> Rect(pw / 2 - size / 2, ph - size, pw / 2 + size / 2, ph)
            ResizeEdge.LEFT_TOP -> Rect(0, 0, size, size)
            ResizeEdge.RIGHT_TOP -> Rect(pw - size, 0, pw, size)
            ResizeEdge.LEFT_BOTTOM -> Rect(0, ph - size, size, ph)
            ResizeEdge.RIGHT_BOTTOM -> Rect(pw - size, ph - size, pw, ph)
        }
    }

    fun contains(x: Float, y: Float, parent: View): Boolean =
        getBounds(parent).contains(x.toInt(), y.toInt())

    fun draw(canvas: Canvas, parent: View) {
        val bounds = getBounds(parent)
        val paint = Paint().apply {
            color = Color.parseColor("#3F51B5")
            style = Paint.Style.FILL; isAntiAlias = true
        }
        canvas.drawRect(bounds, paint)
    }

    private fun Int.dpToPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
}
