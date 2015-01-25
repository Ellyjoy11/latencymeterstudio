package com.elena.latencymeter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

public class MainActivity extends Activity {

	public static int screenWidth;
	public static int screenHeight;
	public static boolean clockWise;
	public static boolean showSector;
    //public static boolean modeAuto = true;
	public static final String TAG = "LatencyMeter";
	public String appVersion;
	public static int speedValue;
	public final String MYPREFS = "my shared prefs";

	SeekBar speedBar;
	AnimationView myView;
	CheckBox mCheckBox, mCheckBox2, mCheckBox3;

    Thread mTouchThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

		try {
			appVersion = this.getPackageManager().getPackageInfo(
					this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			Log.d(TAG, "App version not found " + e.getMessage());
		}

		setContentView(R.layout.activity_main);



		mCheckBox = (CheckBox) findViewById(R.id.checkBox);
		if (mCheckBox.isChecked()) {
			clockWise = false;
		} else {
			clockWise = true;
		}

		mCheckBox2 = (CheckBox) findViewById(R.id.checkBox2);
		if (mCheckBox2.isChecked()) {
			showSector = false;
		} else {
			showSector = true;
		}

		speedBar = (SeekBar) findViewById(R.id.speedBar);
		myView = (AnimationView) findViewById(R.id.animView);
		float defaultSpeed = (float) (speedBar.getProgress()) * 10.0f
				/ (float) (speedBar.getMax());
		myView.setBallSpeed(defaultSpeed);
        //modeAuto = true;
        //myView.setMode(modeAuto);

		speedBar.setOnSeekBarChangeListener(speedBarOnSeekBarChangeListener);



	}

	@Override
	public void onResume() {
		super.onResume();
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		screenWidth = displaymetrics.widthPixels;
		screenHeight = displaymetrics.heightPixels;
        //AnimationView.isAutoDone = false;
        onModeAuto();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.about) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			builder.setMessage("\u00a9 2014-2015 Elena Last").setTitle(
					"Latency Meter v." + appVersion);

			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});

			AlertDialog dialog = builder.create();
			dialog.show();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}

	}

	OnSeekBarChangeListener speedBarOnSeekBarChangeListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			float defaultSpeed = (float) (speedBar.getProgress()) * 10.0f
					/ (float) (speedBar.getMax());
			myView.setBallSpeed(defaultSpeed);
            //modeAuto = true;
            //myView.setMode(modeAuto);
            AnimationView.isAutoDone = false;
            AnimationView.count = -1;
			myView.invalidate();
            onModeAuto();
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
		}

	};

	public void boxClicked(View view) {
		if (mCheckBox.isChecked()) {
			clockWise = false;
		} else {
			clockWise = true;
		}
		Log.d(TAG, "direction clockWise: " + clockWise);
        //modeAuto = true;
        //myView.setMode(modeAuto);
		AnimationView.isAutoDone = false;
        AnimationView.count = -1;
		myView.invalidate();
        onModeAuto();
	}

	public void boxHideClicked(View view) {
		if (mCheckBox2.isChecked()) {
			showSector = false;
		} else {
			showSector = true;
		}
		Log.d(TAG, "show sector: " + showSector);

	}

    public void onModeAuto() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!AnimationView.isAutoDone) {
                    simulateTouch(AnimationView.prevX, AnimationView.prevY, AnimationView.count);
                    //Log.d(TAG, "Ball coords: " + AnimationView.prevX + "; " + AnimationView.prevY + "count " + AnimationView.count);
                    }
            }
        }).start();
    }

    private void simulateTouch (double x, double y, int count) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis();
        //Log.d(TAG, "differ " + (eventTime - downTime)*1.0 + " ms" );
        int metaState = 0;
        int action; // = MotionEvent.ACTION_UP;
        if (count == -1) {
            action = MotionEvent.ACTION_DOWN;
        } else if (count < 200 && count > -1) {
            action = MotionEvent.ACTION_MOVE;
        } else if (count == 200) {
            action = MotionEvent.ACTION_UP;
           //modeAuto = false;
            //Log.d(TAG, "mode set to false");
            //x = 0; y = 0;
        } else {
            action = MotionEvent.ACTION_CANCEL;
        }
        MotionEvent motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                (float) x,
                (float) y,
                metaState);

        myView.dispatchTouchEvent(motionEvent);
    }

    @Override
    public void onPause() {
        //modeAuto = true;
        AnimationView.isAutoDone = false;
        AnimationView.count = -1;
        super.onPause();
    }

}
