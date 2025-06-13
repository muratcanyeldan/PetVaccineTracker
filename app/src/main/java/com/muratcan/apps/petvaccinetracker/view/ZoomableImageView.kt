package com.muratcan.apps.petvaccinetracker.view

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private val startPoint = PointF()
    private val midPoint = PointF()
    private var initialDistance = 1f
    private var mode = NONE
    private val matrixValues = FloatArray(9)
    private val minimumScale = 1f
    private val maximumScale = 5f
    private var currentScale = 1f
    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    init {
        init(context)
    }

    private fun init(context: Context) {
        scaleType = ScaleType.MATRIX
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                startPoint.set(event.x, event.y)
                mode = DRAG
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                initialDistance = getDistance(event)
                if (initialDistance > 10f) {
                    savedMatrix.set(matrix)
                    getMidPoint(midPoint, event)
                    mode = ZOOM
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when (mode) {
                    DRAG -> {
                        matrix.set(savedMatrix)
                        val dx = event.x - startPoint.x
                        val dy = event.y - startPoint.y
                        matrix.postTranslate(dx, dy)
                    }

                    ZOOM -> {
                        val newDist = getDistance(event)
                        if (newDist > 10f) {
                            matrix.set(savedMatrix)
                            val scale = newDist / initialDistance
                            matrix.postScale(scale, scale, midPoint.x, midPoint.y)
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                mode = NONE
                performClick()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
            }
        }

        matrix.getValues(matrixValues)
        currentScale = matrixValues[Matrix.MSCALE_X]

        // Limit scale
        when {
            currentScale < minimumScale -> matrix.setScale(minimumScale, minimumScale)
            currentScale > maximumScale -> matrix.setScale(maximumScale, maximumScale)
        }

        imageMatrix = matrix
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun getDistance(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(x * x + y * y)
    }

    private fun getMidPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor
            val newScale = currentScale * scaleFactor

            if (newScale >= minimumScale && newScale <= maximumScale) {
                currentScale = newScale
                matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                imageMatrix = matrix
            }
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val targetScale = if (currentScale == minimumScale) maximumScale else minimumScale
            matrix.setScale(targetScale, targetScale, e.x, e.y)
            currentScale = targetScale
            imageMatrix = matrix
            return true
        }
    }

    @Suppress("unused")
    fun resetZoom() {
        matrix.reset()
        currentScale = 1f
        imageMatrix = matrix
    }
} 