package com.elena.latencymeter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint({ "DrawAllocation", "ClickableViewAccessibility" })
public class AnimationView extends View {

	public static final String TAG = "LatencyMeter";
	Paint paint, paintText, paintTouch, paintStat;
	Paint wrongMove;

	Bitmap bm;

	int bm_offsetX, bm_offsetY;

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
	int touchCount = 0;
	double speed = 0;
	double latency;
	double averageLatency;
	double averageDispatchLatency;
    double averageOutputLatency = 0;
	double median, minL, maxL;
	double stdevLatency;
	double eventRate;

	List<Double> myLatency = new ArrayList<Double>();
	List<Long> dispatchLatency = new ArrayList<Long>();
    List<Double> outputLatency = new ArrayList<Double>();

	public static int screenWidth;
	public static int screenHeight;
	public static float screenDpi;
    public static int displayTransitionDelay = 13;

	int cX, cY;
	int radius;
	int touchDistance;
	int touchDelta;

	double alpha, theta;


	public static double newX = 0;
    public static double newY = 0;
    public static double prevX;
    public static double prevY;
    public static boolean isAutoDone = false;
	double ballA, ballB, touchA, touchB;
	TextView tv1, tv2, tv3, tv4, tv5, tv6, tv7;
    int currFps = 59;
    int lowEdge;

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

		paint = new Paint();
		paint.setColor(Color.BLUE);
		paint.setStrokeWidth(bm_offsetX);
		paint.setStyle(Paint.Style.STROKE);

		paintTouch = new Paint();
		paintTouch.setColor(Color.GRAY);
		paintTouch.setStrokeWidth(5);
		paintTouch.setStyle(Paint.Style.FILL_AND_STROKE);

		paintText = new Paint();
		paintText.setColor(Color.parseColor("#FFA500"));
		paintText.setStrokeWidth(4);
		paintText.setTextSize(70 * screenDpi / 4);
		paintText.setStyle(Paint.Style.FILL_AND_STROKE);

		paintStat = new Paint();
		paintStat.setColor(Color.parseColor("#FFA500"));
		paintStat.setStrokeWidth(2);
		paintStat.setTextSize(60 * screenDpi / 4);
		paintStat.setStyle(Paint.Style.FILL_AND_STROKE);

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

        Toast.makeText(
                getContext(),
                "When output latency row becomes green,\nkeep your finger on the ball",
                Toast.LENGTH_LONG).show();

	}

	@Override
	protected void onDraw(Canvas canvas) {

		tv1 = (TextView) this.getRootView().findViewById(R.id.textView1);
		tv2 = (TextView) this.getRootView().findViewById(R.id.textView2);
		tv3 = (TextView) this.getRootView().findViewById(R.id.textView3);
		tv4 = (TextView) this.getRootView().findViewById(R.id.textView4);
		tv5 = (TextView) this.getRootView().findViewById(R.id.textView5);
		tv6 = (TextView) this.getRootView().findViewById(R.id.textView6);
        tv7 = (TextView) this.getRootView().findViewById(R.id.textView7);

		canvas.drawPath(animPath, paint);

		tv1.setText("angular speed is " + String.format("%.2f", speed)
                + " rad/s");

        //if(!isAutoDone) {
        //    lowEdge = 1;
        //} else {
            lowEdge = 20;
        //}


        if (MainActivity.clockWise) {

			// ////////////!!!!!!!!!!!!!//////////////////
            prevX = newX;
            prevY = newY;

        newX = cX + radius * Math.cos(ballAngle);
        newY = cY + radius * Math.sin(ballAngle);


                pos[0] = (float) newX;
                pos[1] = (float) newY;
				matrix.reset();

				matrix.postTranslate(pos[0] - bm_offsetX, pos[1] - bm_offsetY);

				canvas.drawBitmap(bm, matrix, null);
            //prevX = newX;
            //prevY = newY;

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
				touchDelta = Math.abs(touchDistance - radius);

				// ////////////////try to fill sector///////////
				RectF oval = new RectF((float) (cX - radius),
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

				// ////////////////
				// ///////////////change theta to alpha again if needed
				if ((touchActive && sweepAngle > 0)
						|| (2 * touchDelta > bm_offsetX)) {

					paintText.setColor(Color.RED);
					paintTouch.setColor(Color.RED);
					tv4.setTextColor(Color.RED);
					tv6.setTextColor(Color.RED);
					if (touchActive && (2 * touchDelta > bm_offsetX)) {
						canvas.drawCircle(cX, cY, radius - bm_offsetX / 2,
								wrongMove);
						canvas.drawCircle(cX, cY, radius + bm_offsetX / 2,
								wrongMove);

					}
				} else if (2 * touchDelta <= bm_offsetX) {

					paintText.setColor(Color.BLACK);
					paintTouch.setColor(Color.GRAY);
					tv4.setTextColor(Color.parseColor("#FFA500"));
					tv6.setTextColor(Color.parseColor("#FFA500"));
				}

				// /////////////////////
				if (touchActive && MainActivity.showSector && isAutoDone) {
					canvas.drawArc(oval, startAngle, sweepAngle, true,
							paintTouch);
				}
				// /////////////
				if (speed > 0 && theta > 0 && sweepAngle < 0
						&& (2 * touchDelta <= bm_offsetX)) {
					latency = theta * 1000.0 / speed;
                    if (latency > lowEdge && latency < 50
                            && outputLatency.size() < 200 && !isAutoDone) { // 30

                        outputLatency.add(latency);
                        count = outputLatency.size();
                    }
					if (latency > lowEdge && latency < 220
							&& myLatency.size() < 1000 && isAutoDone) { // 30
															// is
															// set
															// to
						// exclude
						// "cheating"
						// samples
						myLatency.add(latency);
                        //count = myLatency.size();
					}
				} else {
					latency = 0;
				}
				if (median > 0) {

					tv3.setTextColor(Color.parseColor("#008000"));
					tv4.setTextColor(Color.parseColor("#008000"));
					tv6.setTextColor(Color.parseColor("#008000"));
                    tv2.setTextColor(Color.parseColor("#008000"));
				} else {

					tv3.setTextColor(Color.parseColor("#FFA500"));
					tv4.setTextColor(Color.parseColor("#FFA500"));
					tv6.setTextColor(Color.parseColor("#FFA500"));
                    tv2.setTextColor(Color.parseColor("#FFA500"));
				}

            if (eventRate == 0) {
                tv5.setText("event rate: N/a yet");
            } else {
                tv5.setText("event rate: " + String.format("%.2f", eventRate)
                        + " Hz");
            }

            if (averageOutputLatency > 0) {
                tv7.setText("average output latency: "
                        + String.format("%.2f", averageOutputLatency) + " ms");
                tv7.setTextColor(Color.parseColor("#008000"));
                tv7.setTypeface(Typeface.DEFAULT);
               // isAutoDone = true;
            } else {
                tv7.setText("Measuring output latency, please wait...");
                tv7.setTextColor(Color.RED);
                tv7.setTypeface(Typeface.DEFAULT_BOLD);
            }

				if (touchActive && myLatency.size() < 1000 && isAutoDone) {
					paintText.setColor(Color.parseColor("#FFA500"));
					paintText.setStrokeWidth(2);
					paintText.setTextSize(80 * screenDpi / 4);
					canvas.drawText("" + (1000 - myLatency.size()), cX - 40,
							cY, paintText);
				} else if (touchActive && isAutoDone) {
					paintText.setColor(Color.parseColor("#008000"));
					paintText.setStrokeWidth(4);
					paintText.setTextSize(100 * screenDpi / 4);
					canvas.drawText("DONE", cX - 80, cY, paintText);
				}

				tv4.setText("total average: " + String.format("%.2f", averageLatency)
						+ " ms   median: "
						+ String.format("%.2f", median) + " ms");
                if (averageLatency > 0) {
                    tv2.setText("average IC latency: " + String.format("%.2f", averageLatency
                            - averageOutputLatency - averageDispatchLatency) + " ms");
                } else {
                    tv2.setText("average IC latency: N/a yet");
                }

				tv6.setText("min: " + String.format("%.2f", minL)
						+ " ms   max: " + String.format("%.2f", maxL)
						+ "   stdev: " + String.format("%.2f", stdevLatency)
						+ " ms");

            if (averageDispatchLatency == 0) {
                tv3.setText("average dispatch latency: N/a yet");
            } else {
                tv3.setText("average dispatch latency: "
                        + String.format("%.2f", averageDispatchLatency) + " ms");
            }
            ballAngle += speed / currFps;

			// /////////////////!!!!!!!!!!!////////////////////////
		} else {
			// //////reverse all!!!!!!!!!!!!!!!//////////
            prevX = newX;
            prevY = newY;

            newX = cX + radius * Math.cos(ballAngle);
            newY = cY + radius * Math.sin(ballAngle);


            pos[0] = (float) newX;
            pos[1] = (float) newY;
            matrix.reset();
				matrix.postTranslate(pos[0] - bm_offsetX, pos[1] - bm_offsetY);

				canvas.drawBitmap(bm, matrix, null);
            //prevX = newX;
            //prevY = newY;

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
				touchDelta = Math.abs(touchDistance - radius);

				// ////////////////try to fill sector///////////
				RectF oval = new RectF((float) (cX - radius),
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

				if (((touchAngle > startAngle) && (startAngle > 3))
						|| ((touchAngle < startAngle) && (touchAngle < 90) && (startAngle > 270))) {
					sweepAngle = (float) Math.toDegrees(theta);
				}
				if (((touchAngle < startAngle) && (touchAngle > 90))
						|| ((touchAngle > startAngle) && (touchAngle > 270) && (startAngle < 90))) {
					sweepAngle = (-1) * (float) Math.toDegrees(theta);
				}

				// ////////////////
				// ///////////////change theta to alpha again if needed
				if ((touchActive && sweepAngle < 0)
						|| (2 * touchDelta > bm_offsetX)) {

					paintText.setColor(Color.RED);
					paintTouch.setColor(Color.RED);
					tv4.setTextColor(Color.RED);
					tv6.setTextColor(Color.RED);
					if (touchActive && (2 * touchDelta > bm_offsetX)) {
						canvas.drawCircle(cX, cY, radius - bm_offsetX / 2,
								wrongMove);
						canvas.drawCircle(cX, cY, radius + bm_offsetX / 2,
								wrongMove);

					}
				} else if (2 * touchDelta <= bm_offsetX) {

					paintText.setColor(Color.BLACK);
					paintTouch.setColor(Color.GRAY);
					tv4.setTextColor(Color.parseColor("#FFA500"));
					tv6.setTextColor(Color.parseColor("#FFA500"));
				}

				// /////////////////////
				if (touchActive && MainActivity.showSector && isAutoDone) {
					canvas.drawArc(oval, startAngle, sweepAngle, true,
                            paintTouch);
				}
				// /////////////


				if (speed > 0 && theta > 0 && sweepAngle > 0
						&& (2 * touchDelta <= bm_offsetX)) {
					latency = theta * 1000.0 / speed;

                    if (latency > lowEdge && latency < 50
                            && outputLatency.size() < 200 && !isAutoDone) { // 30

                        outputLatency.add(latency);
                        count = outputLatency.size();
                    }
					if (latency > lowEdge && latency < 220
							&& myLatency.size() < 1000 && isAutoDone) { // 30
															// is
															// set
															// to
						// exclude
						// "cheating"
						// samples
						myLatency.add(latency);
                        //count = myLatency.size();
					}
				} else {
					latency = 0;
				}
				if (median > 0) {
                    tv3.setTextColor(Color.parseColor("#008000"));
					tv4.setTextColor(Color.parseColor("#008000"));
					tv6.setTextColor(Color.parseColor("#008000"));
                    tv2.setTextColor(Color.parseColor("#008000"));
				} else {

					tv3.setTextColor(Color.parseColor("#FFA500"));
					tv4.setTextColor(Color.parseColor("#FFA500"));
					tv6.setTextColor(Color.parseColor("#FFA500"));
                    tv2.setTextColor(Color.parseColor("#FFA500"));
				}

            if (averageOutputLatency > 0) {
                tv7.setText("average output latency: "
                        + String.format("%.2f", averageOutputLatency) + " ms");
                tv7.setTextColor(Color.parseColor("#008000"));
                tv7.setTypeface(Typeface.DEFAULT);
               // isAutoDone = true;
            } else {
                tv7.setText("Measuring output latency, please wait...");
                tv7.setTextColor(Color.RED);
                tv7.setTypeface(Typeface.DEFAULT_BOLD);
            }

            if (eventRate == 0) {
                tv5.setText("event rate: N/a yet");
            } else {
                tv5.setText("event rate: " + String.format("%.2f", eventRate)
                        + " Hz");
            }

				if (touchActive && myLatency.size() < 1000 && isAutoDone) {
                    paintText.setColor(Color.parseColor("#FFA500"));
                    paintText.setStrokeWidth(3);
                    paintText.setTextSize(80 * screenDpi / 4);
                    canvas.drawText("" + (1000 - myLatency.size()), cX - 40,
                            cY, paintText);
                } else if (touchActive && isAutoDone) {
                    paintText.setColor(Color.parseColor("#008000"));
                    paintText.setStrokeWidth(6);
                    paintText.setTextSize(110 * screenDpi / 4);
                    canvas.drawText("DONE", cX - 80, cY, paintText);
                }

				tv4.setText("total average: " + String.format("%.2f", averageLatency)
						+ " ms   median: "
						+ String.format("%.2f", median) + " ms");
            if (averageLatency > 0) {
                tv2.setText("average IC latency: " + String.format("%.2f", averageLatency
                        - averageOutputLatency - averageDispatchLatency) + " ms");
            } else {
                tv2.setText("average IC latency: N/a yet");
            }

				tv6.setText("min: " + String.format("%.2f", minL)
						+ " ms   max: " + String.format("%.2f", maxL)
						+ "   stdev: " + String.format("%.2f", stdevLatency)
						+ " ms");
            if (averageDispatchLatency == 0) {
                tv3.setText("average dispatch latency: N/a yet");
            } else {
                tv3.setText("average dispatch latency: "
                        + String.format("%.2f", averageDispatchLatency) + " ms");
            }
                ballAngle -= speed / currFps;

			// /////////////////!!!!!!!!!!!////////////////////////

		}

		invalidate();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		int action = event.getAction();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			point.x = (int) event.getX();
			point.y = (int) event.getY();
			touchActive = true;
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
                averageOutputLatency = displayTransitionDelay + sumOutput * 1.0 / (outputLatency.size() - 50);
                //averageOutputLatency = sumOutput * 1.0 / (outputLatency.size());
                    prevX = cX;
                    prevY = cY;
                isAutoDone = true;
                count = 500;
                //the hack to fix "sticky touch" issue when Auto Mode is done
                for (int i=0; i < 5; i++) {
                    simulateTouchCancel(1);
                    simulateTouchCancel(0);
                }
                //Log.d(TAG, "touch up is done");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //setMode(false);

            }

			if (myLatency.size() == 1000) {

				double sum = 0;
				double sum2 = 0;
				double devSum = 0;
				double[] numArray = new double[myLatency.size()];
				for (int i = 0; i < myLatency.size(); i++) {
					sum += myLatency.get(i);
					numArray[i] = myLatency.get(i);

				}
				averageLatency = sum * 1.0 / (myLatency.size());
				if (isAutoDone) {
                    for (int i = 0; i < dispatchLatency.size(); i++) {
                        sum2 += dispatchLatency.get(i);

                    }
                    averageDispatchLatency = sum2 * 1.0 / (dispatchLatency.size());
                }
				for (int i = 0; i < myLatency.size(); i++) {
					devSum += Math.pow(myLatency.get(i) - averageLatency, 2);
				}
				stdevLatency = Math.sqrt(devSum * 1.0 / myLatency.size());
				Arrays.sort(numArray);

				minL = numArray[0];
				maxL = numArray[numArray.length - 1];
				int middle = (numArray.length) / 2;
				if (numArray.length % 2 == 0) {
					double medianA = numArray[middle];
					double medianB = numArray[middle - 1];
					median = ((double) (medianA + medianB) / 2);
				} else {
					median = numArray[middle + 1];
				}
                /*
                if (mRunMode && !isAutoDone) {
                    averageOutputLatency = averageLatency;
                    isAutoDone = true;
                }
                */
			}
			break;

		}

		return true;
	}

	public void setBallSpeed(float bSpeed) {
		speed = bSpeed;
	}

    //public void setMode(boolean runMode) {
    //    mRunMode = runMode;
    //}

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