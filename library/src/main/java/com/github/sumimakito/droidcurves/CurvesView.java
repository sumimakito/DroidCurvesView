/*
 *  Copyright (c) 2014-2016 Sumi Makito
 *  https://github.com/sumimakito
 *  Please read the license before using or modifying.
 */

package com.github.sumimakito.droidcurves;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class CurvesView extends View {
    private final String TAG = "CurvesView";

    private int cvBackgroundColor, cvCurveColor, cvHintLineColor, cvTextColor, cvTouchEffectColor;

    private int cvCtrlKnotCount, cvTextSize;
    private float cvCurveWeight, cvHintLineWeight;
    private boolean cvDrawText, cvDrawTouchEffect;
    private Typeface cvTextTypeface;

    private float columnMaskWidth = 0, columnMaskHeight = 0;
    private int opKnotIdx = -1;
    private float startPointY = -1;

    private ArrayList<PointF> knotsArray;
    private ArrayList<Float> valuesArray;

    private Paint pCurve, pMask, pLine, pText;
    private int viewWidth, viewHeight;
    private float textLabelGlobalY;
    private CurvesChangedListener curvesChangedListener;

    private float _dirtyFixXOffset;

    public CurvesView(Context context) {
        this(context, null, 0);
    }

    public CurvesView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CurvesView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        applyStyle(context, attrs, defStyleAttr);
        initArrays();
        reloadPaints();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CurvesView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        applyStyle(context, attrs, defStyleAttr);
        initArrays();
        reloadPaints();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rescaleView(w, h);
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (knotsArray.size() < 3) return;
        canvas.drawColor(cvBackgroundColor);
        canvas.drawLine(0, columnMaskHeight, viewWidth, 0, pLine);
        drawSplineCubicHermite(knotsArray, 0.05f, canvas, pCurve);

        if (cvDrawText) {
            for (int i = 0; i < cvCtrlKnotCount; i++) {
                canvas.drawText(Math.round(valuesArray.get(i) * 100) + "%", (i + 0.5f) * columnMaskWidth, textLabelGlobalY, pText);
            }
        }

        if (cvDrawTouchEffect && opKnotIdx >= 0) {
            canvas.drawRect(opKnotIdx * columnMaskWidth, 0,
                            (opKnotIdx + 1) * columnMaskWidth, columnMaskHeight,
                            pMask);
        }
    }

    private void applyStyle(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CurvesView, defStyleAttr, 0);

        cvBackgroundColor = a.getColor(R.styleable.CurvesView_cv_backgroundColor, Color.BLACK);
        cvTouchEffectColor = a.getColor(R.styleable.CurvesView_cv_touchEffectColor, Color.argb(30, 255, 255, 255));
        cvCurveColor = a.getColor(R.styleable.CurvesView_cv_curveColor, Color.WHITE);
        cvHintLineColor = a.getColor(R.styleable.CurvesView_cv_hintLineColor, Color.argb(50, 255, 255, 255));
        cvTextColor = a.getColor(R.styleable.CurvesView_cv_textColor, Color.WHITE);

        cvDrawText = a.getBoolean(R.styleable.CurvesView_cv_drawText, true);
        cvDrawTouchEffect = a.getBoolean(R.styleable.CurvesView_cv_drawTouchEffect, true);

        cvCtrlKnotCount = a.getInt(R.styleable.CurvesView_cv_ctrlPointCount, 5);

        cvCurveWeight = a.getDimensionPixelSize(R.styleable.CurvesView_cv_curveWeight,
                                                Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics())));
        cvHintLineWeight = a.getDimensionPixelSize(R.styleable.CurvesView_cv_hintLineWeight,
                                                   Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics())));
        cvTextSize = a.getDimensionPixelSize(R.styleable.CurvesView_cv_textSize,
                                             Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11, getResources().getDisplayMetrics())));
        int cvTypefaceID = a.getInt(R.styleable.CurvesView_cv_textTypeface, 0);
        cvTextTypeface = (cvTypefaceID == 0 ? Typeface.MONOSPACE : cvTypefaceID == 1 ? Typeface.DEFAULT : Typeface.DEFAULT_BOLD);
        a.recycle();
    }

    private void reloadPaints() {
        pText = new Paint();
        pText.setColor(cvTextColor);
        pText.setAntiAlias(true);
        pText.setTypeface(cvTextTypeface);
        pText.setTextSize(cvTextSize);
        pText.setTextAlign(Paint.Align.CENTER);

        pCurve = new Paint();
        pCurve.setColor(cvCurveColor);
        pCurve.setAntiAlias(true);
        pCurve.setFlags(pCurve.getFlags() | Paint.ANTI_ALIAS_FLAG);
        pCurve.setStrokeWidth(cvCurveWeight);
        pCurve.setDither(true);
        pCurve.setStyle(Paint.Style.STROKE);
        pCurve.setStrokeJoin(Paint.Join.ROUND);
        pCurve.setStrokeCap(Paint.Cap.ROUND);

        pLine = new Paint();
        pLine.setColor(cvHintLineColor);
        pLine.setAntiAlias(true);
        pLine.setStrokeWidth(cvHintLineWeight);

        pMask = new Paint();
        pMask.setColor(cvTouchEffectColor);
        pMask.setAntiAlias(true);
    }

    private void initArrays() {
        knotsArray = new ArrayList<>();
        valuesArray = new ArrayList<>();
        float valueInterval = 1.0f / (cvCtrlKnotCount - 1);
        for (int i = 0; i < cvCtrlKnotCount; i++) {
            valuesArray.add(i * valueInterval);
        }
    }

    private void rescaleView(int width, int height) {
        viewWidth = width;
        viewHeight = height;
        //Log.w(TAG, "W@" + width + ", H@" + height);
        columnMaskWidth = (float) width / (float) cvCtrlKnotCount;
        columnMaskHeight = height;
        _dirtyFixXOffset = viewWidth * 0.125f;
        knotsArray.clear();

        // initial curve
        double hypotenuse = Math.sqrt(Math.pow(width, 2) + Math.pow(height, 2));
        double cos = (double) width / hypotenuse;
        double sin = (double) height / hypotenuse;
        double pOffsetX = hypotenuse / ((double) (cvCtrlKnotCount - 1)) * cos;
        double pOffsetY = hypotenuse / ((double) (cvCtrlKnotCount - 1)) * sin;

        for (int i = 0; i < cvCtrlKnotCount; i++) {
            knotsArray.add(
                    new PointF(
                            Math.round(pOffsetX * i),
                            columnMaskHeight * (1.0f - valuesArray.get(i))
                    )
            );
        }

        textLabelGlobalY = columnMaskHeight - (pText.getTextSize() * 0.8f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                float x = event.getX();
                startPointY = event.getY();
                opKnotIdx = (int) Math.floor(x / columnMaskWidth);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaY = event.getY() - startPointY; // ↓:>0 ↑:<0
                float targetValue = valuesArray.get(opKnotIdx) - deltaY * 0.0001f;
                if (targetValue < 0.0f)
                    targetValue = 0.0f;
                else if (targetValue > 1.0f)
                    targetValue = 1.0f;
                valuesArray.set(opKnotIdx, targetValue);
                knotsArray.get(opKnotIdx).y = columnMaskHeight * (1.0f - targetValue); //+= deltaY * 0.1;

                if (curvesChangedListener != null)
                    curvesChangedListener.onCallback(opKnotIdx, targetValue);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                opKnotIdx = -1;
                startPointY = -1;
                invalidate();
                break;
        }
        return true;
    }

    private void drawSplineCubicHermite(List<PointF> knots, float delta, Canvas canvas, Paint paint) {
        Path path = new Path();
        int n = knots.size();
        float pX = 0, pY = 0;
        float lX0 = 0, lY0 = 0, lX1 = 0, lY1 = 0;
        int lFlg = -1;
        for (int i = 0; i < n; i++) {
            for (float t = 0; t < 1; t += delta) {

                if (lFlg == -1 && i == (n - 1) && t >= 0.5f) {
                    //Log.w(TAG, "set Flag to 0");
                    lFlg = 0;
                }

                float h00 = (1 + 2 * t) * (1 - t) * (1 - t);
                float h10 = t * (t - 1) * (t - 1);
                float h01 = t * t * (3 - 2 * t);
                float h11 = t * t * (t - 1);
                if (i == 0) {
                    PointF p0 = knots.get(i);
                    PointF p1 = knots.get(i + 1);
                    PointF p2 = null;
                    if (n > 2) {
                        p2 = knots.get(i + 2);
                    } else {
                        p2 = p1;
                    }
                    PointF m0 = tgp(p1, p0);
                    PointF m1 = tgp(p2, p0);

                    pX = h00 * p0.x + h10 * m0.x + h01 * p1.x + h11
                            * m1.x;
                    pY = h00 * p0.y + h10 * m0.y + h01 * p1.y + h11
                            * m1.y;
                    if (t == 0) path.moveTo(pX, limitY(pY));
                    else path.lineTo(pX, limitY(pY));
                } else if (i < n - 2) {
                    PointF p0 = knots.get(i - 1);
                    PointF p1 = knots.get(i);
                    PointF p2 = knots.get(i + 1);
                    PointF p3 = knots.get(i + 2);

                    PointF m0 = tgp(p2, p0);
                    PointF m1 = tgp(p3, p1);

                    pX = h00 * p1.x + h10 * m0.x + h01 * p2.x + h11
                            * m1.x;
                    pY = h00 * p1.y + h10 * m0.y + h01 * p2.y + h11
                            * m1.y;
                    path.lineTo(pX, limitY(pY));
                } else if (i == n - 1) {
                    if (n < 3) {
                        continue;
                    }
                    PointF p0 = knots.get(i - 2);
                    PointF p1 = knots.get(i - 1);
                    PointF p2 = knots.get(i);

                    PointF m0 = tgp(p2, p0);
                    PointF m1 = tgp(p2, p1);

                    pX = h00 * p1.x + h10 * m0.x + h01 * p2.x + h11
                            * m1.x;
                    pY = h00 * p1.y + h10 * m0.y + h01 * p2.y + h11
                            * m1.y;
                    path.lineTo(pX, limitY(pY));
                }

                if (lFlg == 0) {
                    lX0 = pX;
                    lY0 = limitY(pY);
                    lFlg = 1;
                    //Log.w(TAG, "set Flag to 1; lX0=" + lX0 + ", lY0=" + lY0);
                } else if (lFlg == 1) {
                    lX1 = pX;
                    lY1 = limitY(pY);
                    lFlg = 2;
                    //Log.w(TAG, "set Flag to 2; lX1=" + lX1 + ", lY1=" + lY1);
                }
            }
        }

        /*
         * Dirty-fix: expand line
         */

        float dfX = _dirtyFixXOffset;
        float dfY = dfX * (lY1 - lY0) / (lX1 - lX0);
        path.lineTo(lX1 + dfX, lY1 + dfY);

        canvas.drawPath(path, paint);
    }

    private PointF tgp(PointF p0, PointF p1) {
        return new PointF((p0.x - p1.x) / 2, (p0.y - p1.y) / 2);
    }

    private float limitY(float pY) {
        if (pY > columnMaskHeight - cvCurveWeight) return columnMaskHeight - cvCurveWeight;
        else if (pY < cvCurveWeight) return cvCurveWeight;
        else return pY;
    }


    public void setCurvesChangedListener(CurvesChangedListener listener) {
        this.curvesChangedListener = listener;
    }

    public void setCvBackgroundColor(int cvBackgroundColor) {
        this.cvBackgroundColor = cvBackgroundColor;
        reloadPaints();
        invalidate();
    }

    public void setCvCurveColor(int cvCurveColor) {
        this.cvCurveColor = cvCurveColor;
        reloadPaints();
        invalidate();
    }

    public void setCvCurveWeight(float cvCurveWeight) {
        this.cvCurveWeight = cvCurveWeight;
        reloadPaints();
        invalidate();
    }

    public void setCvDrawText(boolean cvDrawText) {
        this.cvDrawText = cvDrawText;
        invalidate();
    }

    public void setCvDrawTouchEffect(boolean cvDrawTouchEffect) {
        this.cvDrawTouchEffect = cvDrawTouchEffect;
        invalidate();
    }

    public void setCvHintLineColor(int cvHintLineColor) {
        this.cvHintLineColor = cvHintLineColor;
        reloadPaints();
        invalidate();
    }

    public void setCvHintLineWeight(float cvHintLineWeight) {
        this.cvHintLineWeight = cvHintLineWeight;
        reloadPaints();
        invalidate();
    }

    public void setCvTextColor(int cvTextColor) {
        this.cvTextColor = cvTextColor;
        reloadPaints();
        invalidate();
    }

    public void setCvTextSize(int cvTextSize) {
        this.cvTextSize = cvTextSize;
        reloadPaints();
        invalidate();
    }

    public void setCvTouchEffectColor(int cvTouchEffectColor) {
        this.cvTouchEffectColor = cvTouchEffectColor;
        reloadPaints();
        invalidate();
    }

    public void setCvTextTypeface(Typeface cvTextTypeface) {
        this.cvTextTypeface = cvTextTypeface;
        reloadPaints();
        invalidate();
    }

    public interface CurvesChangedListener {
        void onCallback(int controlKnotID, float newValue);
    }
}
