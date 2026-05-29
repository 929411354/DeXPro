package com.dexpro.launcher.widget

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.Toast
import kotlin.math.max
import kotlin.math.min

/**
 * A responsive grid that hosts system AppWidgets on the DeX desktop.
 *
 * Uses GridLayout with cell-based positioning.
 * Each widget occupies one or more cells based on its minWidth/minHeight.
 *
 * Cells are roughly 180dp × 180dp (scaled to physical pixels).
 * Grid is 4 columns wide, unlimited rows.
 */
class WidgetGridLayout(context: Context) : GridLayout(context) {

    companion object {
        private const val MAX_COLUMNS = 4
        private const val CELL_SIZE_DP = 180f
    }

    private val cellSizePx: Int
    private var currentColumn = 0
    private var currentRow = 0

    init {
        val density = context.resources.displayMetrics.density
        cellSizePx = (CELL_SIZE_DP * density).toInt()

        columnCount = MAX_COLUMNS
        orientation = HORIZONTAL
    }

    /**
     * Add a widget view to the grid.
     * Automatically calculates column span.
     */
    fun addWidget(widgetView: AppWidgetHostView): Boolean {
        val info = widgetView.appWidgetInfo
        val spanX = calculateSpan(info.minWidth, MAX_COLUMNS)
        val spanY = calculateSpan(info.minHeight, MAX_COLUMNS)  // using same logic for height

        // Check if widget fits in current row
        if (currentColumn + spanX > MAX_COLUMNS) {
            currentColumn = 0
            currentRow++
        }

        val params = LayoutParams().apply {
            width = 0
            height = 0
            columnSpec = spec(currentColumn, spanX)
            rowSpec = spec(currentRow, spanY)
            setGravity(Gravity.FILL)
            setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        }

        // Wrap widget in a container for long-press delete
        val container = FrameLayout(context).apply {
            addView(widgetView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            tag = widgetView.appWidgetId

            setOnLongClickListener {
                val widgetId = tag as? Int ?: return@setOnLongClickListener false
                showDeleteDialog(widgetId)
                true
            }
        }

        addView(container, params)
        currentColumn += spanX

        return true
    }

    private fun showDeleteDialog(widgetId: Int) {
        val manager = AppWidgetHostManager.getInstance(context)
        val info = manager.getWidgetInfo(widgetId)
        val name = info?.label ?: "Widget"

        android.app.AlertDialog.Builder(context)
            .setTitle("Remove Widget")
            .setMessage("Remove \"$name\" from desktop?")
            .setPositiveButton("Remove") { _, _ ->
                removeWidgetById(widgetId)
                manager.deleteWidget(widgetId)
                Toast.makeText(context, "Widget removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun removeWidgetById(widgetId: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.tag == widgetId) {
                removeViewAt(i)
                return
            }
        }
    }

    private fun calculateSpan(minSize: Int, maxCells: Int): Int {
        if (minSize <= 0) return 1
        val span = (minSize.toFloat() / cellSizePx.toFloat() + 0.5f).toInt()
        return max(1, min(span, maxCells))
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    /**
     * Get the GridLayout.LayoutParams for the "Add Widget" button.
     */
    fun getButtonLayoutParams(): GridLayout.LayoutParams {
        if (currentColumn >= MAX_COLUMNS) {
            currentColumn = 0
            currentRow++
        }

        return LayoutParams().apply {
            width = cellSizePx
            height = cellSizePx
            columnSpec = spec(currentColumn, 1)
            rowSpec = spec(currentRow, 1)
            setGravity(Gravity.CENTER)
            setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        }.also {
            currentColumn++
        }
    }
}