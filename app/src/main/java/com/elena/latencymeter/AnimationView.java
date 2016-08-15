package com.elena.latencymeter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.SharedPreferences.Editor;

@SuppressLint({ "DrawAllocation", "ClickableViewAccessibility" })
public class AnimationView extends View {

	public static final String TAG = "LatencyMeter";
	Paint paint, paintText, paintTouch, paintStat, autoPaint, paintAxis, paintPoint;
	Paint wrongMove;

    public SharedPreferences userPref;

	Bitmap bm;

	int bm_offsetX, bm_offsetY;

    float circleWide;

	double rawA;
	float startAngle;
	float touchAngle;
	float sweepAngle;
	float delta;
    float ballAngle = 0;

	boolean touchActive;
    //boolean mRunMode;

	Path animPath;
	Path touchPath;

	Point point = new Point();

	PathMeasure pathMeasure;
	float pathLength;

	float step; // distance each step
	public static float distance; // distance moved

	float[] pos;
	float[] tan;

	Matrix matrix;
	public static int count = -1;
	long millis1, millis2, millis3, millis4;
	long time360 = 0;
	long dispatchTime = 0;
    public static int touchCount = 0;
	public static double speed = 0;
    public static double latency;
    public static double averageLatency;
    public static double averageEvRate;
    public static double averageDispatchLatency;
    public static double averageOutputLatency = 0;
    public static double median, minL, maxL;
    public static double minChart, maxChart;
    public static double medianEv, minEv, maxEv;
    public static double stdevLatency;
    public static double eventRate;
    public static double stdevEv;

    public static List<Double> myLatency = new ArrayList<Double>();
    public static List<Long> dispatchLatency = new ArrayList<Long>();
    public static List<Double> outputLatency = new ArrayList<Double>();
    public static List<Double> evRate = new ArrayList<Double>();

	public static int screenWidth;
	public static int screenHeight;
	public static float screenDpi;

	int cX, cY;
	int radius, radMin, radMax;
	int touchDistance;
	int touchDelta;
    int sampleShow;
    float xStep;

    int x1, x2, y1, y2;
    int axisPad = 20;
    int layoutPads = 20;
    public static float winStartX, winEndX;

	double alpha, theta;

    float autoX1, autoX2, autoY1, autoY2;

    public static double newX = 0;
    public static double newY = 0;
    public static double prevX;
    public static double prevY;
    public static boolean isAutoDone = false;
	double ballA, ballB, touchA, touchB;
	TextView tvSpeed, tvIC, tvMin, tvMax, tvMed, tvStd;
    TextView tvDisp, tvOut, tvTotal, tvEvRate;
    TextView tvIC_w, tvMin_w, tvMax_w, tvMed_w, tvStd_w;
    TextView tvDisp_w, tvOut_w, tvTotal_w;
    int currFps = 59;
    int lowEdge;
    int multiplier;
	public static int samples;

    public static int windowStart;
    public static int windowEnd;
    public static boolean isStartMoving = false;
    public static boolean isEndMoving = false;

    float textSize;

    public static String noiseState;
    public static boolean showChart;
    Button restart;

    private RectF oval = null;
    private double eventRatePrev = -1;
    private int statsTextColorPrev = Color.TRANSPARENT;
    private static final int STATS_COLOR1 = Color.parseColor("#008000");
    private static final int STATS_COLOR2 = Color.parseColor("#FFA500");

	public AnimationView(Context context) {
		super(context);
		initMyView();
	}

	public AnimationView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initMyView();
	}

	public AnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initMyView();
	}

	public void initMyView() {

		bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_ball_2);

		bm_offsetX = bm.getWidth() / 2;
		bm_offsetY = bm.getHeight() / 2;

		animPath = new Path();
		touchPath = new Path();
        showChart = false;

		DisplayMetrics displaymetrics = new DisplayMetrics();
		WindowManager wm = ((Activity) getContext()).getWindowManager();
		wm.getDefaultDisplay().getMetrics(displaymetrics);

		screenWidth = displaymetrics.widthPixels;
		screenHeight = displaymetrics.heightPixels;
		screenDpi = displaymetrics.density;


		cX = (screenWidth - bm_offsetX / 2 - 10) / 2;
		cY = screenHeight - cX - 2 * bm_offsetY;
		// cY = (screenHeight - bm_offsetY / 2 - 10) / 2;

		radius = cX - bm_offsetX;
        radMin = radius - bm_offsetX / 2;
        radMax = radius + bm_offsetX / 2;

		paint = new Paint();
		//paint.setColor(Color.BLUE);
		paint.setStrokeWidth(bm_offsetX);
		paint.setStyle(Paint.Style.STROKE);
        noiseState = setPathColor();

        autoPaint = new Paint();
        autoPaint.setColor(Color.parseColor("#81d8d0"));
        autoPaint.setStrokeWidth(bm_offsetX / 4);
        autoPaint.setStyle(Paint.Style.STROKE);

		paintTouch = new Paint();
		paintTouch.setColor(Color.GRAY);
		paintTouch.setStrokeWidth(5);
		paintTouch.setStyle(Paint.Style.FILL_AND_STROKE);

		paintText = new Paint();
		paintText.setColor(Color.parseColor("#FFA500"));
		paintText.setStrokeWidth(4);
		paintText.setTextSize(70 * screenDpi / 4);
		paintText.setStyle(Paint.Style.FILL_AND_STROKE);

		wrongMove = new Paint();
		wrongMove.setColor(Color.RED);
		wrongMove.setStrokeWidth(10);
		wrongMove.setStyle(Paint.Style.STROKE);

		//count = -1;
		//touchActive = false;

		point.x = (int) cX;
		point.y = (int) cY;

		animPath.addCircle(cX, cY, radius, Direction.CW);

		pathMeasure = new PathMeasure(animPath, false);
		pathLength = pathMeasure.getLength();

		//distance = 0;

		pos = new float[2];
		tan = new float[2];

		matrix = new Matrix();

        x1 = axisPad;
        x2 = screenWidth - axisPad;
        y1 = cY - radMax;
        y2 = cY + radMax;

        paintAxis = new Paint();
        paintAxis.setStyle(Paint.Style.FILL_AND_STROKE);

        paintPoint = new Paint();
        paintPoint.setColor(Color.BLUE);
        paintPoint.setStyle(Paint.Style.FILL_AND_STROKE);

        textSize = 60 * screenDpi / 4;
        paintStat = new Paint();
        paintStat.setColor(Color.BLACK);
        //paintStat.setColor(Color.parseColor("#F45823"));
        paintStat.setStrokeWidth(2);
        paintStat.setTextSize(textSize);
        paintStat.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        paintStat.setStyle(Paint.Style.FILL_AND_STROKE);

        oval = new RectF();
	}

    void lookupViews() {
        restart = (Button) this.getRootView().findViewById(R.id.buttonRestart);
        tvSpeed = (TextView) this.getRootView().findViewById(R.id.textViewSpeed);
		tvIC = (TextView) this.getRootView().findViewById(R.id.textViewIC);
		tvDisp = (TextView) this.getRootView().findViewById(R.id.textViewDisp);
		tvOut = (TextView) this.getRootView().findViewById(R.id.textViewOut);
		tvTotal = (TextView) this.getRootView().findViewById(R.id.textViewTotal);
        tvEvRate = (TextView) this.getRootView().findViewById(R.id.textViewEvRate);
        tvMin = (TextView) this.getRootView().findViewById(R.id.textViewMin);
        tvMed = (TextView) this.getRootView().findViewById(R.id.textViewMed);
        tvMax = (TextView) this.getRootView().findViewById(R.id.textViewMax);
        tvStd = (TextView) this.getRootView().findViewById(R.id.textViewStd);

        tvIC_w = (TextView) this.getRootView().findViewById(R.id.textViewIC_w);
        tvDisp_w = (TextView) this.getRootView().findViewById(R.id.textViewDisp_w);
        tvOut_w = (TextView) this.getRootView().findViewById(R.id.textViewOut_w);
        tvTotal_w = (TextView) this.getRootView().findViewById(R.id.textViewTotal_w);
        tvMin_w = (TextView) this.getRootView().findViewById(R.id.textViewMin_w);
        tvMed_w = (TextView) this.getRootView().findViewById(R.id.textViewMed_w);
        tvMax_w = (TextView) this.getRootView().findViewById(R.id.textViewMax_w);
        tvStd_w = (TextView) this.getRootView().findViewById(R.id.textViewStd_w);
    }

    void setStatsTextColor(int color) {
        if (color != statsTextColorPrev) {
            tvDisp.setTextColor(color);
            tvTotal.setTextColor(color);
            tvMin.setTextColor(color);
            tvMed.setTextColor(color);
            tvMax.setTextColor(color);
            tvStd.setTextColor(color);
            tvIC.setTextColor(color);
            tvDisp_w.setTextColor(color);
            tvTotal_w.setTextColor(color);
            tvMin_w.setTextColor(color);
            tvMed_w.setTextColor(color);
            tvMax_w.setTextColor(color);
            tvStd_w.setTextColor(color);
            tvIC_w.setTextColor(color);
            statsTextColorPrev = color;
        }
    }

    void updateStatsViews() {
        if (averageLatency > 0) {
            tvTotal.setTypeface(Typeface.DEFAULT_BOLD);
            tvTotal_w.setTypeface(Typeface.DEFAULT_BOLD);
            tvIC.setText(String.format("%.2f", averageLatency
                    - averageOutputLatency - averageDispatchLatency) + " ms");
            tvTotal.setText(String.format("%.2f", averageLatency)
                    + " ms");
            showChart = true;
        } else {
            tvTotal.setTypeface(Typeface.DEFAULT);
            tvTotal_w.setTypeface(Typeface.DEFAULT);
            tvIC.setText(" --");
            tvTotal.setText(" --");
            showChart = false;
        }

        tvMin.setText(String.format("%.2f", minL) + " ms");
        tvMax.setText(String.format("%.2f", maxL) + " ms");
        tvMed.setText(String.format("%.2f", median) + " ms");
        tvStd.setText(String.format("%.2f", stdevLatency) + " ms");

        if (averageDispatchLatency == 0) {
            tvDisp.setText(" --");
        } else {
            tvDisp.setText(String.format("%.2f", averageDispatchLatency) + " ms");
        }

        if (averageOutputLatency > 0) {
            tvOut.setText(String.format("%.2f", averageOutputLatency) + " ms");
            tvOut.setTextColor(Color.parseColor("#008000"));
            tvOut.setTypeface(Typeface.DEFAULT);
            tvOut_w.setTextColor(Color.parseColor("#008000"));
            tvOut_w.setTypeface(Typeface.DEFAULT);
            // isAutoDone = true;
        } else {
            tvOut.setText(" --");
            tvOut.setTextColor(Color.RED);
            tvOut.setTypeface(Typeface.DEFAULT_BOLD);
            tvOut_w.setTextColor(Color.RED);
            tvOut_w.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

	@SuppressLint("NewApi")
    @Override
	protected void onDraw(Canvas canvas) {

        //Log.d(TAG, "multiplier and samples: " + multiplier + "..." + samples);

        if (showChart) {
            restart.setVisibility(VISIBLE);
            recalcStats();
            paintAxis.setColor(Color.BLACK);
            paintAxis.setStrokeWidth(3);
            canvas.drawLine(x1,y2,x2,y2,paintAxis);
            canvas.drawLine(x1,y1-20,x1,y2,paintAxis);
            double yMax = Math.round(maxChart + 1 );
            double yMin = Math.round(minChart - 1);
            //float xStep = (x2 - x1)/(samples+1);
            sampleShow = samples / (x2 - x1 - layoutPads) + 1;
            xStep = sampleShow * (x2 - x1 - layoutPads)/(samples+1);
            Log.d(TAG, "axis and step: " + (x2-x1 - layoutPads) + ".." + xStep);
            /*
            if (xStep < 1.0) {
                sampleShow = 2;
                xStep = 2 * (x2 - x1)/(samples+1);
                //Log.d(TAG, "step " + xStep);
            } else {
                sampleShow = 1;
            }
            */
            float coef = (float)(yMax - yMin) / (y2 - y1);
            int n=0;
            double avgSample;
            double sumSample;
            for (int i=0; i < samples; i+=sampleShow) {
                float pointX = x1 + n*xStep;
                if (xStep < 4) {
                    paintPoint.setStrokeWidth(xStep);
                } else if (xStep < 8) {
                    paintPoint.setStrokeWidth(xStep-1);
                } else {
                    paintPoint.setStrokeWidth(xStep-2);
                }

                sumSample = 0;
                for (int k = sampleShow-1; k >= 0; k--) {
                    if (i+k < myLatency.size()) {
                        sumSample += myLatency.get(i + k);
                    }
                }
                avgSample = sumSample / sampleShow;
                float pointY = (float)(y2 + yMin/coef - avgSample / coef);

                canvas.drawLine(pointX, y2 ,pointX, pointY, paintPoint);
                for (int k = sampleShow-1; k >= 0; k--) {
                if ((i+k)==windowStart) {
                    paintAxis.setColor(Color.parseColor("#FFA500"));
                    paintAxis.setStrokeWidth(4);
                    canvas.drawLine(pointX, y1, pointX, y2, paintAxis);
                    canvas.drawText("#" + Integer.toString(windowStart+1), pointX + 5, y1 + 60, paintStat);
                    winStartX = pointX;
                }
                    if ((i+k)==windowEnd) {
                        //Log.d(TAG, "index, winEnd: " + (i+k) + ".." + windowEnd);
                        paintAxis.setColor(Color.parseColor("#FFA500"));
                        paintAxis.setStrokeWidth(4);
                        canvas.drawLine(pointX, y1, pointX, y2, paintAxis);
                        canvas.drawText("#" + Integer.toString(windowEnd+1), pointX - textSize*3, y1 + 60, paintStat);
                        winEndX = pointX;
                    }
                }
                n++;
                //Log.d(TAG, "coef, x, y: " + coef + ".." + pointX + ".." + pointY);
            }
            paintAxis.setColor(Color.GREEN);
            paintAxis.setStrokeWidth(4);
            //canvas.drawLine(x1, (float) (y2 + yMin / coef - median / coef), x2, (float) (y2 + yMin / coef - median / coef), paintAxis);
            canvas.drawLine(x1, (float) (y2 + yMin / coef - averageLatency / coef), x2, (float) (y2 + yMin / coef - averageLatency / coef), paintAxis);
            paintAxis.setColor(Color.MAGENTA);
            canvas.drawLine(x1, (float) (y2 + yMin / coef - (averageLatency - stdevLatency) / coef), x2, (float) (y2 + yMin / coef - (averageLatency - stdevLatency) / coef), paintAxis);
            canvas.drawLine(x1, (float) (y2 + yMin / coef - (averageLatency + stdevLatency) / coef), x2, (float) (y2 + yMin / coef - (averageLatency + stdevLatency) / coef), paintAxis);

            //canvas.drawText(Double.toString(yMax), x1 + textSize*2, y1 + 10, paintStat);
            //canvas.drawText(Double.toString(yMin), x1 + textSize*2, y2 - 5, paintStat);

            paintAxis.setColor(Color.RED);
            canvas.drawLine(x1, (float) (y2 + yMin / coef - minL / coef), x2, (float) (y2 + yMin / coef - minL / coef), paintAxis);
            canvas.drawLine(x1, (float) (y2 + yMin / coef - maxL / coef), x2, (float) (y2 + yMin / coef - maxL / coef), paintAxis);
            canvas.drawText("min", x1 + 5, (float)(y2 + yMin / coef - minL / coef - 10), paintStat);
            canvas.drawText("max", x1 + 5, (float)(y2 + yMin / coef - maxL / coef - 10), paintStat);

            //canvas.drawText("median", x1 + 5, (float) (y2 + yMin / coef - median / coef -10), paintStat);
            canvas.drawText("avg", x1 + 5, (float) (y2 + yMin / coef - averageLatency / coef -10), paintStat);
            canvas.drawText("-stdev", x1 + 5, (float) (y2 + yMin / coef - (averageLatency - stdevLatency) / coef -10), paintStat);
            canvas.drawText("+stdev", x1 + 5, (float) (y2 + yMin / coef - (averageLatency + stdevLatency) / coef -10), paintStat);
            if (sampleShow == 1) {
                canvas.drawText("n=" + Integer.toString(samples), x2 - textSize * 6, y2 - 5, paintStat);
            } else {
                canvas.drawText("avg of each " + sampleShow + " samples, total n="+Integer.toString(samples), x2-textSize*20, y2-5, paintStat);
            }

            ///////////////////////////
            if (eventRatePrev != eventRate) {
                if (eventRate == 0) {
                    tvEvRate.setText("event rate: --");
                } else {
                    tvEvRate.setText("event rate: " + String.format("%.2f", eventRate)
                            + " Hz");
                }
                eventRatePrev = eventRate;
            }

            if (averageOutputLatency > 0) {
                tvOut.setText(String.format("%.2f", averageOutputLatency) + " ms");
                //tvOut.setTextColor(Color.parseColor("#008000"));
                //tvOut.setTypeface(Typeface.DEFAULT);
                //tvOut_w.setTextColor(Color.parseColor("#008000"));
                //tvOut_w.setTypeface(Typeface.DEFAULT);
                // isAutoDone = true;
            }

            if (averageLatency > 0) {
                tvTotal.setTypeface(Typeface.DEFAULT_BOLD);
                tvTotal_w.setTypeface(Typeface.DEFAULT_BOLD);
                tvIC.setText(String.format("%.2f", averageLatency
                        - averageOutputLatency - averageDispatchLatency) + " ms");
                tvTotal.setText(String.format("%.2f", averageLatency)
                        + " ms");
                showChart = true;
            }

            tvMin.setText(String.format("%.2f", minL) + " ms");
            tvMax.setText(String.format("%.2f", maxL) + " ms");
            tvMed.setText(String.format("%.2f", median) + " ms");
            tvStd.setText(String.format("%.2f", stdevLatency) + " ms");

            if (averageDispatchLatency == 0) {
                tvDisp.setText(" --");
            } else {
                tvDisp.setText(String.format("%.2f", averageDispatchLatency) + " ms");
            }
            //////////////////////////

        } else {
            restart.setVisibility(INVISIBLE);
            showChart = false;
            canvas.drawPath(animPath, paint);
            final float ballDir = MainActivity.clockWise ? 1.0f : -1.0f;

            if (!isAutoDone) {
                lowEdge = 5;
            } else {
                lowEdge = (int) averageOutputLatency;
            }

            double tmpX = newX;
            double tmpY = newY;

            newX = cX + radius * Math.cos(ballAngle);
            newY = cY + radius * Math.sin(ballAngle);
            //noiseState = setPathColor();
            //Log.d(TAG, "call color function");
            //tvSpeed.setText("speed\n" + String.format("%.2f", speed)
            //        + " rad/s" + "\nNState: " + noiseState);

            if (!isAutoDone) {
                autoX1 = (float) (cX + radMin * Math.cos(ballAngle));
                autoY1 = (float) (cY + radMin * Math.sin(ballAngle));
                autoX2 = (float) (cX + radMax * Math.cos(ballAngle));
                autoY2 = (float) (cY + radMax * Math.sin(ballAngle));
                canvas.drawLine(autoX1, autoY1, autoX2, autoY2, autoPaint);
            }


            pos[0] = (float) newX;
            pos[1] = (float) newY;
            matrix.reset();

            if (isAutoDone) {
                matrix.postTranslate(pos[0] - bm_offsetX, pos[1] - bm_offsetY);

                canvas.drawBitmap(bm, matrix, null);
            }
            prevX = tmpX;
            prevY = tmpY;

            // /calculate lines from center to ball and to touch
            if (!((pos[0] - cX) == 0) && !((pos[1] - cY) == 0)) {
                ballA = (pos[1] - cY) / (pos[0] - cX);
                // ballB = (pos[0] * cY - cX * pos[1]) / (pos[0] - cX);
            } else {
                ballA = 0;
            }
            if (!((point.x - cX) == 0) && !((point.y - cY) == 0)) {
                touchA = (point.y - cY) / (point.x - cX);
                // touchB = cY + (point.y - cY) * (-cX) / (point.x - cX);
            } else {
                touchA = 0;
            }
/*
            if (MainActivity.showSector) {
                paintTouch.setColor(Color.GREEN);
                canvas.drawLine(point.x, point.y, cX, cY, paintTouch);
            }
            */
            paintTouch.setColor(Color.GRAY);

            theta = Math
                    .acos(((pos[0] - cX) * (point.x - cX) + (pos[1] - cY)
                            * (point.y - cY))
                            / (Math.sqrt((pos[0] - cX) * (pos[0] - cX)
                            + (pos[1] - cY) * (pos[1] - cY)) * Math
                            .sqrt((point.x - cX)
                                    * (point.x - cX)
                                    + (point.y - cY)
                                    * (point.y - cY))));
            touchDistance = (int) Math.sqrt(Math.pow(cX - point.x, 2)
                    + Math.pow(cY - point.y, 2));
            if (MainActivity.mDensity < 2 || MainActivity.mDensity > 3.8) {
                touchDelta = (int) (1.5 * Math.abs(touchDistance - radius));
            } else {
                touchDelta = 2 * Math.abs(touchDistance - radius);
            }

            // ////////////////try to fill sector///////////
            oval.set((float) (cX - radius),
                    (float) (cY - radius), (float) (cX + radius),
                    (float) (cY + radius));
            if (!(pos[1] == cY)) {
                rawA = Math.atan2(pos[1] - cY, pos[0] - cX);
            } else if (pos[0] < cX) {
                rawA = Math.PI;
            } else if (pos[0] > cX) {
                rawA = 0;
            }

            if (Math.toDegrees(rawA) < 0) {
                startAngle = (float) Math.toDegrees(rawA) + 360;
            } else {
                startAngle = (float) Math.toDegrees(rawA);
            }
            if (!(point.y == cY)) {
                if (Math.toDegrees(Math.atan2(point.y - cY, point.x - cX)) < 0) {
                    touchAngle = (float) Math.toDegrees(Math.atan2(point.y
                            - cY, point.x - cX)) + 360;
                } else {
                    touchAngle = (float) Math.toDegrees(Math.atan2(point.y
                            - cY, point.x - cX));
                }
            } else if (point.x > cX) {
                touchAngle = 360;
            } else if (point.x < cX) {
                touchAngle = (float) Math.toDegrees(Math.PI);
            }
            if (MainActivity.clockWise) {
                // ////////////////grey - correct
                if (((touchAngle < startAngle) && (touchAngle < 357))
                        || ((touchAngle > startAngle) && (touchAngle > 270) && (startAngle < 90))) {
                    sweepAngle = (-1) * (float) Math.toDegrees(theta);
                }
                // ///////////red - invalid
                if (((touchAngle > startAngle) && (touchAngle < 270))
                        || ((touchAngle < startAngle) && (touchAngle < 90) && (startAngle > 270))) {
                    sweepAngle = (float) Math.toDegrees(theta);
                }
            } else {
                if (((touchAngle > startAngle) && (startAngle > 3))
                        || ((touchAngle < startAngle) && (touchAngle < 90) && (startAngle > 270))) {
                    sweepAngle = (float) Math.toDegrees(theta);
                }
                if (((touchAngle < startAngle) && (touchAngle > 90))
                        || ((touchAngle > startAngle) && (touchAngle > 270) && (startAngle < 90))) {
                    sweepAngle = (-1) * (float) Math.toDegrees(theta);
                }
            }

            int statsColor;
            // ////////////////
            // ///////////////change theta to alpha again if needed
            if ((touchActive && (ballDir * sweepAngle) > 0)
                    || (touchDelta > bm_offsetX)) {

                paintText.setColor(Color.RED);
                paintTouch.setColor(Color.RED);
                statsColor = Color.RED;
                if (touchActive && (touchDelta > bm_offsetX)) {
                    canvas.drawCircle(cX, cY, radius - bm_offsetX / 2,
                            wrongMove);
                    canvas.drawCircle(cX, cY, radius + bm_offsetX / 2,
                            wrongMove);

                }
            } else if (touchDelta <= bm_offsetX) {

                paintText.setColor(Color.BLACK);
                paintTouch.setColor(Color.GRAY);
                statsColor = STATS_COLOR2;
            }

            // /////////////////////
            if (touchActive && MainActivity.showSector && isAutoDone) {
                canvas.drawArc(oval, startAngle, sweepAngle, true,
                        paintTouch);
            }
            // /////////////
            if (speed > 0 && theta > 0 && (ballDir * sweepAngle) < 0
                    && (touchDelta <= bm_offsetX)) {
                latency = theta * 1000.0 / speed;
                if (latency > lowEdge && latency < 50
                        && outputLatency.size() < 200 && !isAutoDone) { // 30

                    outputLatency.add(latency);
                    count = outputLatency.size();
                }
                //Log.d(TAG, "..." + latency + "..." + averageOutputLatency + "..." + dispatchTime + "..." + noiseState);
                if (latency > lowEdge && latency < 220
                        && myLatency.size() < samples && isAutoDone
                        && ((noiseState.equals("1") && ((latency
                        - averageOutputLatency - dispatchTime) < (multiplier * 1000 / eventRate))) || noiseState.isEmpty() ||
                        noiseState.equals("2"))) { // 30
                    // is
                    // set
                    // to
                    // exclude
                    // "cheating"
                    // samples
                    myLatency.add(latency);
                    //Log.d(TAG, "latency added: " + latency);
                    //count = myLatency.size();
                }
            } else {
                latency = 0;
            }
            if (median > 0) {
                statsColor = STATS_COLOR1;
            } else {
                statsColor = STATS_COLOR2;
            }
            setStatsTextColor(statsColor);

            if (eventRatePrev != eventRate) {
                if (eventRate == 0) {
                    tvEvRate.setText("event rate: --");
                } else {
                    tvEvRate.setText("event rate: " + String.format("%.2f", eventRate)
                            + " Hz");
                }
                eventRatePrev = eventRate;
            }

            if (touchActive && myLatency.size() < samples && isAutoDone) {
                paintText.setColor(STATS_COLOR2);
                paintText.setStrokeWidth(2);
                paintText.setTextSize(80 * screenDpi / 4);
                canvas.drawText("" + (samples - myLatency.size()), cX - 40,
                        cY, paintText);
            } else if (touchActive && isAutoDone) {
                paintText.setColor(STATS_COLOR1);
                paintText.setStrokeWidth(4);
                paintText.setTextSize(100 * screenDpi / 4);
                canvas.drawText("DONE", cX - 80, cY, paintText);
            }

            ballAngle += ballDir * speed / currFps;
            invalidate();
        }


	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		int action = event.getAction();
        if (!showChart) {

            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    point.x = (int) event.getX();
                    point.y = (int) event.getY();
                    touchActive = true;
                    resetValues();
                    //Log.d(TAG, "start counting, autoDone " + isAutoDone);
                    millis3 = SystemClock.elapsedRealtime();
                    break;
                case MotionEvent.ACTION_MOVE:
                    point.x = (int) event.getX();
                    point.y = (int) event.getY();
                    touchActive = true;

                    if (isAutoDone) {
                        touchCount += event.getHistorySize();
                        try {
                            dispatchTime = SystemClock.uptimeMillis()
                                    - event.getEventTime();
                            dispatchLatency.add(dispatchTime);

                        } catch (IllegalArgumentException e) {
                            Log.d(TAG, e.toString());
                        }

                        millis4 = SystemClock.elapsedRealtime();
                        eventRate = touchCount * 1000.0 / (millis4 - millis3);
                        evRate.add(eventRate);
                        //Log.d(TAG, "rate added: " + eventRate);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    point.x = (int) cX;
                    point.y = (int) cY;
                    alpha = 0;
                    theta = 0;
                    touchActive = false;
                    //Log.d(TAG, "touch up happened");
                    //count = -1;
                    // dispatchTime = 0;

                    if (!isAutoDone && outputLatency.size() == 200) {

                        double sumOutput = 0;
                        double[] numArrayOutput = new double[outputLatency.size()];
                        for (int i = 50; i < outputLatency.size(); i++) {
                            sumOutput += outputLatency.get(i);
                            numArrayOutput[i] = outputLatency.get(i);

                        }
                        averageOutputLatency = MainActivity.displayTransmissionDelay + sumOutput * 1.0 / (outputLatency.size() - 50);
                        //averageOutputLatency = sumOutput * 1.0 / (outputLatency.size());
                        prevX = cX;
                        prevY = cY;
                        isAutoDone = true;
                        count = 500;
                        //the hack to fix "sticky touch" issue when Auto Mode is done
                        for (int i = 0; i < 7; i++) {
                            simulateTouchCancel(1);
                            simulateTouchCancel(0);

                            //Log.d(TAG, "touch up is done");
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        //setMode(false);

                    }
                    recalcStats();

                    break;

            }
        } else {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if ((Math.abs(event.getX() - winStartX) < 80 || Math.abs(event.getX() - winEndX) < 80)
                            &&
                            (event.getY() > y1 && event.getY() < y2)) {
                        if (Math.abs(event.getX() - winStartX) <= Math.abs(event.getX() - winEndX)) {
                            isEndMoving = false;
                            isStartMoving = true;
                        } else {
                            isEndMoving = true;
                            isStartMoving = false;
                        }
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if ((Math.abs(event.getX() - winStartX) < 80 || Math.abs(event.getX() - winEndX) < 80)
                            &&
                            (event.getY() > y1 && event.getY() < y2) && !isStartMoving && !isEndMoving) {
                        if (Math.abs(event.getX() - winStartX) <= Math.abs(event.getX() - winEndX)) {
                            isEndMoving = false;
                            isStartMoving = true;
                        } else {
                            isEndMoving = true;
                            isStartMoving = false;
                        }
                    }
                    //if (Math.abs(event.getX() - winStartX) < 80 &&
                    if (event.getY() > y1 && event.getY() < y2 && isStartMoving) {
                        if (event.getX() < x1) {
                            windowStart = 0;
                        } else {
                            windowStart = (int) ((event.getX() - x1) * sampleShow / xStep);
                        }
                    }
                    //if (Math.abs(event.getX() - winEndX) < 80 &&
                    if  (event.getY() > y1 && event.getY() < y2 && isEndMoving) {
                        if (event.getX() > x1 + xStep * samples / sampleShow) {
                            windowEnd = samples - 1;
                        } else {
                            windowEnd = (int) ((event.getX() - x1) * sampleShow / xStep);
                        }
                    }
                    if (windowStart >= windowEnd) {
                        if (windowEnd < samples - 2 && windowStart < samples - 2) {
                            windowStart += 1;
                        } else {
                            windowStart = samples - 1;
                            windowEnd = samples - 2;
                        }
                        int tmpVar;
                        tmpVar = windowEnd;
                        windowEnd = windowStart;
                        windowStart = tmpVar;
                        if (windowStart < 0) {
                            windowStart = 0;
                        }
                        if (windowEnd > samples - 1) {
                            windowEnd = samples - 1;
                        }
                    }
                    //Log.d(TAG, "window: " + windowStart + ".." + windowEnd);
                    setSamplingWindow(windowStart, windowEnd);
                    this.invalidate();
                    break;
                case MotionEvent.ACTION_CANCEL:
                    isEndMoving = false;
                    isStartMoving = false;
                    break;
                case MotionEvent.ACTION_UP:
                    isEndMoving = false;
                    isStartMoving = false;
                    break;
            }
        }
		return true;
	}

    public static void recalcStats() {

            //Log.d(TAG, "array: " + myLatency.size());
            if (myLatency.size() == samples) {

                double sum = 0;
                double sum2 = 0;
                double devSum = 0;
                double[] numArray = new double[myLatency.size()];
                double[] numWindowArray = new double[windowEnd - windowStart + 1];
                for (int i = 0; i < myLatency.size(); i++) {
                    //sum += myLatency.get(i);
                    numArray[i] = myLatency.get(i);
                }
                for (int i = windowStart; i <= windowEnd; i++) {
                    sum += myLatency.get(i);
                    numWindowArray[i - windowStart] = myLatency.get(i);
                }
                //averageLatency = sum * 1.0 / (myLatency.size());
                averageLatency = sum * 1.0 / (windowEnd - windowStart + 1);

                if (isAutoDone) {
                    for (int i = 0; i < dispatchLatency.size(); i++) {
                        sum2 += dispatchLatency.get(i);

                    }
                    averageDispatchLatency = sum2 * 1.0 / (dispatchLatency.size());
                }

                for (int i = windowStart; i <= windowEnd; i++) {
                    devSum += Math.pow(myLatency.get(i) - averageLatency, 2);
                }
                //stdevLatency = Math.sqrt(devSum * 1.0 / myLatency.size());
                stdevLatency = Math.sqrt(devSum * 1.0 / (windowEnd - windowStart + 1));
                Arrays.sort(numArray);
                Arrays.sort(numWindowArray);
                minChart = numArray[0];
                maxChart = numArray[numArray.length - 1];
                minL = numWindowArray[0];
                maxL = numWindowArray[numWindowArray.length - 1];
                int middle = (numWindowArray.length) / 2;
                if (numWindowArray.length % 2 == 0) {
                    double medianA = numWindowArray[middle];
                    double medianB = numWindowArray[middle - 1];
                    median = ((double) (medianA + medianB) / 2);
                } else {
                    median = numWindowArray[middle + 1];
                }
            }

        MainActivity.updateStats();
    }

	public void setBallSpeed(float bSpeed) {
		speed = bSpeed;
        multiplier = MainActivity.multiplier;
        //samples = 1000;
        samples = MainActivity.samples;
        //windowStart = MainActivity.windowStart - 1;
        //windowEnd = MainActivity.windowEnd - 1;
        showChart = false;
        tvSpeed.setText(String.format("speed\n%.2f rad/s", speed));
	}

    public void setSamplingWindow (int winStart, int winEnd) {
        windowStart = winStart;
        windowEnd = winEnd;
        userPref = MainActivity.userPref;
        Editor editor = userPref.edit();
        editor.putString("start", Integer.toString(windowStart+1));
        editor.putString("end", Integer.toString(windowEnd+1));
        editor.commit();

    }

    public static void resetValues() {
        myLatency.clear();
        dispatchLatency.clear();
        outputLatency.clear();
        median = 0;
        averageLatency = 0;
        averageDispatchLatency = 0;
        if (!isAutoDone) {
            averageOutputLatency = 0;
        }
        minL = 0;
        maxL = 0;
        stdevLatency = 0;
        eventRate = 0;
        touchCount = 0;

        MainActivity.updateStats();
    }

    @SuppressLint("NewApi")
    public String setPathColor() {

        FileInputStream is;
        BufferedReader reader;
        String readOut = "";
        String line;
        File file = new File(MainActivity.touchFWPath + "/f54/d10_noise_state");

        if (file.exists()) {
            try {
                is = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(is));
                //line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    //Log.d(TAG, "read from file: " + line);
                    readOut += line;
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!readOut.isEmpty()) {
            //Log.d(TAG, "reading noise state: " + readOut);

            if (readOut.equals("2")) {
                paint.setColor(Color.parseColor("#ffa500"));
				//Log.d(TAG, "set color to yellow");
            }
            else {
                paint.setColor(Color.BLUE);
				//Log.d(TAG, "set color to blue");
            }

            //this.invalidate();
        } else {
            paint.setColor(Color.BLUE);
        }

        return readOut;
    }

    private void simulateTouchCancel (int simMode) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        //Log.d(TAG, "differ " + (eventTime - downTime)*1.0 + " ms" );
        int metaState = 0;
        int action;//
        // = MotionEvent.ACTION_CANCEL;
        if (simMode == 1) {
            action = MotionEvent.ACTION_DOWN;
        } else {
            action = MotionEvent.ACTION_UP;
        }

        MotionEvent motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                cX,
                cY,
                metaState);

        this.dispatchTouchEvent(motionEvent);
    }

}