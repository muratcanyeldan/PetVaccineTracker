package com.muratcan.apps.petvaccinetracker.view;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class ZoomableImageView extends AppCompatImageView {
    private final Matrix matrix = new Matrix();
    private final Matrix savedMatrix = new Matrix();
    private final PointF startPoint = new PointF();
    private final PointF midPoint = new PointF();
    private float initialDistance = 1f;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    private final float[] matrixValues = new float[9];
    private final float minimumScale = 1f;
    private final float maximumScale = 5f;
    private float currentScale = 1f;
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public ZoomableImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomableImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomableImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                startPoint.set(event.getX(), event.getY());
                mode = DRAG;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                initialDistance = getDistance(event);
                if (initialDistance > 10f) {
                    savedMatrix.set(matrix);
                    getMidPoint(midPoint, event);
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    float dx = event.getX() - startPoint.x;
                    float dy = event.getY() - startPoint.y;
                    matrix.postTranslate(dx, dy);
                } else if (mode == ZOOM) {
                    float newDist = getDistance(event);
                    if (newDist > 10f) {
                        matrix.set(savedMatrix);
                        float scale = newDist / initialDistance;
                        matrix.postScale(scale, scale, midPoint.x, midPoint.y);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                mode = NONE;
                performClick();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }

        matrix.getValues(matrixValues);
        currentScale = matrixValues[Matrix.MSCALE_X];

        // Limit scale
        if (currentScale < minimumScale) {
            matrix.setScale(minimumScale, minimumScale);
        } else if (currentScale > maximumScale) {
            matrix.setScale(maximumScale, maximumScale);
        }

        setImageMatrix(matrix);
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private float getDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void getMidPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newScale = currentScale * scaleFactor;

            if (newScale >= minimumScale && newScale <= maximumScale) {
                currentScale = newScale;
                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                setImageMatrix(matrix);
            }
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float targetScale = (currentScale == minimumScale) ? maximumScale : minimumScale;
            matrix.setScale(targetScale, targetScale, e.getX(), e.getY());
            currentScale = targetScale;
            setImageMatrix(matrix);
            return true;
        }
    }

    @SuppressWarnings("unused")
    public void resetZoom() {
        matrix.reset();
        currentScale = 1f;
        setImageMatrix(matrix);
    }
} 