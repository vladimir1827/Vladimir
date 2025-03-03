package com.example.axel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class LineChartView extends View {

    private Paint linePaint, gridPaint, textPaint;
    private List<Float> xValues = new ArrayList<>();
    private List<Float> yValues = new ArrayList<>();
    private List<Float> zValues = new ArrayList<>();
    private List<Float> totalValues = new ArrayList<>(); // Убрать, если не используется

    private static final float AXIS_MIN = -1.0f; // Минимальное значение по оси Y
    private static final float AXIS_MAX = 1.0f;  // Максимальное значение по оси Y
    private static final float STEP = 0.25f;     // Шаг сетки
    private static final int MAX_POINTS = 100;   // Максимальное количество точек на графике

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Настройка кисти для линий графика
        linePaint = new Paint();
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        // Настройка кисти для сетки
        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setAntiAlias(true);

        // Настройка кисти для текста
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(24f);
        textPaint.setAntiAlias(true);
    }

    // Добавление новой точки данных
    public void addDataPoint(float x, float y, float z, float total) {
        xValues.add(x);
        yValues.add(y);
        zValues.add(z);
        totalValues.add(total); // Убрать, если не используется

        // Удаляем старые точки, если их слишком много
        if (xValues.size() > MAX_POINTS) xValues.remove(0);
        if (yValues.size() > MAX_POINTS) yValues.remove(0);
        if (zValues.size() > MAX_POINTS) zValues.remove(0);
        if (totalValues.size() > MAX_POINTS) totalValues.remove(0); // Убрать, если не используется

        // Перерисовываем график
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int padding = 50; // Отступы от краев

        // Отрисовка сетки
        drawGrid(canvas, width, height, padding);

        // Отрисовка графиков
        drawLineChart(canvas, xValues, Color.BLUE, width, height, padding);      // Ось X (синий)
        drawLineChart(canvas, yValues, Color.RED, width, height, padding);      // Ось Y (красный)
        drawLineChart(canvas, zValues, Color.GREEN, width, height, padding);    // Ось Z (зеленый)
        // Убрана отрисовка графика для totalValues (розовая линия)
    }

    // Отрисовка сетки
    private void drawGrid(Canvas canvas, int width, int height, int padding) {
        float graphHeight = height - 2 * padding;
        float graphWidth = width - 2 * padding;

        // Горизонтальные линии сетки
        for (float i = AXIS_MIN; i <= AXIS_MAX; i += STEP) {
            float y = padding + graphHeight * (1 - (i - AXIS_MIN) / (AXIS_MAX - AXIS_MIN));
            canvas.drawLine(padding, y, width - padding, y, gridPaint);
            canvas.drawText(String.format("%.1f", i), padding - 45, y + 10, textPaint); // Подписи значений по оси Y
        }

        // Вертикальные линии сетки
        int horizontalSteps = 10;
        for (int i = 0; i <= horizontalSteps; i++) {
            float x = padding + i * (graphWidth / horizontalSteps);
            canvas.drawLine(x, padding, x, height - padding, gridPaint);
        }
    }

    // Отрисовка графика для одного набора данных
    private void drawLineChart(Canvas canvas, List<Float> values, int color, int width, int height, int padding) {
        if (values.size() < 2) return;

        float graphHeight = height - 2 * padding;
        float graphWidth = width - 2 * padding;

        linePaint.setColor(color);

        int maxVisiblePoints = Math.min(values.size(), MAX_POINTS);
        float pointSpacing = graphWidth / (MAX_POINTS - 1);

        for (int i = 1; i < maxVisiblePoints; i++) {
            float startX = padding + (i - 1) * pointSpacing;
            float startY = padding + graphHeight * (1 - (values.get(i - 1) - AXIS_MIN) / (AXIS_MAX - AXIS_MIN));
            float stopX = padding + i * pointSpacing;
            float stopY = padding + graphHeight * (1 - (values.get(i) - AXIS_MIN) / (AXIS_MAX - AXIS_MIN));

            canvas.drawLine(startX, startY, stopX, stopY, linePaint);
        }
    }
}