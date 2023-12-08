package com.example.inventorymanager.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.DetectedObject.Label;

/**
 * Object overlay to draw a box around the detected object and label it.
 */
public class ObjectOverlay extends View {
    private RectF rect = new RectF();
    private DetectedObject object;
    private Paint paint = new Paint();
    private Paint textpaint = new Paint();
    public ObjectOverlay(Context context) {
        super(context);
    }

    public ObjectOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ObjectOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setObject(DetectedObject object) {
        this.object = object;
        rect = new RectF(object.getBoundingBox());
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(8);

        textpaint.setColor(Color.WHITE);
        textpaint.setTextSize(50);
        textpaint.setStyle(Paint.Style.FILL);

        // Draw object details
        if (object != null) {
            // Draw object box
            canvas.drawRect(rect, paint);

            // Draw object name
            for (Label label : object.getLabels()) {
                String text = label.getText();
                float textWidth = textpaint.measureText(text);
                float x = rect.left + (rect.width() - textWidth) / 2;
                float y = rect.top - 30;
                canvas.drawText(text, x, y, textpaint);

                // Log the labels
                Log.d("ObjectLabel", "Detected label: " + label.getText());
            }
        }
    }
}
