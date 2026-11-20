package com.group14.notes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class EmptyNotesView extends View {

    private Paint paint;

    public EmptyNotesView(Context context) {
        super(context);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // Draw notebook background
        paint.setColor(Color.rgb(230, 240, 255));
        canvas.drawRoundRect(
                centerX - 100,
                centerY - 100,
                centerX + 100,
                centerY + 100,
                20, 20, paint
        );

        // Draw notebook lines
        paint.setColor(Color.BLUE);
        paint.setStrokeWidth(5);

        canvas.drawLine(
                centerX - 60, centerY - 40,
                centerX + 60, centerY - 40, paint
        );

        canvas.drawLine(
                centerX - 60, centerY,
                centerX + 60, centerY, paint
        );

        canvas.drawLine(
                centerX - 60, centerY + 40,
                centerX + 60, centerY + 40, paint
        );

        // Draw pencil
        paint.setColor(Color.rgb(255, 165, 0));
        canvas.drawRect(
                centerX + 75,
                centerY - 120,
                centerX + 95,
                centerY + 30,
                paint
        );
    }
}
