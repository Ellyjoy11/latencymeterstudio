package com.elena.latencymeter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
//import java.util.concurrent.Semaphore;

public class MainActivity extends Activity {

	public static int screenWidth;
	public static int screenHeight;
    public static float mDensity;
	public static boolean clockWise;
	public static boolean showSector;
    public static int displayTransmissionDelay;
    public static int oldDisplayTransmissionDelay;
    public static int multiplier=30;
    public static int samples;
    public static int oldSamples;
    public static int windowStart;
    public static int windowEnd;
    private static MainActivity myActivity;
    public static MonitorNoiseState mThreadForData;

	public static final String TAG = "LatencyMeter";
	public String appVersion;

    private String manufacturer, model_name, device_name;
    private static File[] list1;
    private static File[] list2;

	SeekBar speedBar;
	static AnimationView myView;
	CheckBox mCheckBox, mCheckBox2;

	TextView touchInfo;
    public static String touchFWPath;
    public static String panel;
    public static String touchCfg;
    public static String productInfo;

    float defaultSpeed;

    private boolean isBackFromSettings;
    public static SharedPreferences userPref;

    Button restart;
    ImageButton playPause;

    private static RandomAccessFile noiseF;
    private static int nState;
    static Handler mHandler;
    Message msg;
    int readOut = 0;
    int prevNoiseState;

	@SuppressLint("NewApi")
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        userPref = PreferenceManager
                .getDefaultSharedPreferences(this);

		try {
			appVersion = this.getPackageManager().getPackageInfo(
					this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			Log.d(TAG, "App version not found " + e.getMessage());
		}

		setContentView(R.layout.activity_main);

        restart = (Button) this.findViewById(R.id.buttonRestart);
        playPause = (ImageButton) this.findViewById(R.id.playPause);

        manufacturer = Build.MANUFACTURER;
        model_name = Build.MODEL;
        device_name = Build.DEVICE;

        setTitle(manufacturer + " " + device_name + " [" + model_name + "]");

		mCheckBox = (CheckBox) findViewById(R.id.checkBox);
		if (mCheckBox.isChecked()) {
			clockWise = false;
		} else {
			clockWise = true;
		}

		mCheckBox2 = (CheckBox) findViewById(R.id.checkBox2);
		if (mCheckBox2.isChecked()) {
			showSector = true;
		} else {
			showSector = false;
		}

		speedBar = (SeekBar) findViewById(R.id.speedBar);
		myView = (AnimationView) findViewById(R.id.animView);
		myView.lookupViews();
        setPauseButton();
        myView.count = -1;

		defaultSpeed = (float) (speedBar.getProgress()) * 10.0f
				/ (float) (speedBar.getMax());

		speedBar.setOnSeekBarChangeListener(speedBarOnSeekBarChangeListener);

        touchInfo = (TextView) findViewById(R.id.infoText);

        touchFWPath = getTouchFWPath();
        touchCfg = getTouchCfg();
        panel = getPanelType();

        try {
            noiseF = new RandomAccessFile(touchFWPath + "/f54/d10_noise_state", "r");
            noiseF.seek(0);
            nState = Character.getNumericValue ((char) noiseF.read());
        } catch (IOException e) {
            nState = 0;
            e.printStackTrace();
        }
        Log.d(TAG, "initial noise state " + nState);
        myView.setCircleColor(nState);

        String touchInfoText = "";

        if (!touchCfg.isEmpty()) {
            touchInfoText = "Product: " + productInfo + "  Config: " + touchCfg;
        }
        if (!panel.isEmpty()) {
            touchInfoText += " Panel: " + panel;
        }

			touchInfo.setText(touchInfoText);

        myActivity = this;

        /////////////
        mHandler = new Handler() {
            @Override
            public void handleMessage (Message msg) {
                //super.handleMessage(msg);
                //TODO
                Log.d(TAG, "Handler's got message to process");
                        myView.setCircleColor(msg.what);
                    }
            };

                ////////////
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        myActivity = null;
    }

    static void updateStats() {
        if (myActivity != null && AnimationView.isAutoDone) {
            myActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myView.updateStatsViews();
                }
            });
        }
    }

    @Override
	public void onResume() {
		super.onResume();
        isBackFromSettings = false;
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		screenWidth = displaymetrics.widthPixels;
		screenHeight = displaymetrics.heightPixels;
        mDensity = displaymetrics.density;

        displayTransmissionDelay = Integer.parseInt(userPref.getString("trans", "13"));
        if (displayTransmissionDelay != oldDisplayTransmissionDelay) {
            oldDisplayTransmissionDelay = displayTransmissionDelay;
            myView.showChart = false;
            restart.setVisibility(View.INVISIBLE);
            AnimationView.isAutoDone = false;
            AnimationView.resetValues();
            AnimationView.count = -1;
        }

        //multiplier = Integer.parseInt(userPref.getString("multi", "10"));
        samples = Integer.parseInt(userPref.getString("samples", "1000"));
        if (samples > oldSamples) {
            oldSamples = samples;
            AnimationView.resetValues();
            AnimationView.showChart = false;
            restart.setVisibility(View.INVISIBLE);
        }
        windowStart = Integer.parseInt(userPref.getString("start", "1"));
        windowEnd = Integer.parseInt(userPref.getString("end", Integer.toString(samples)));

        if (!(windowStart > 0 && windowEnd <= samples)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(
                    "Sampling window must be set between 1 and total samples number!")
                    .setTitle("Oops...");

            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            isBackFromSettings = true;
                            Intent intent = new Intent(getApplicationContext(),
                                    SetPreferences.class);
                            startActivity(intent);
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        if (windowStart >= windowEnd) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(
                    "Start of sampling window must be less than its end!")
                    .setTitle("Oops...");

            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            isBackFromSettings = true;
                            Intent intent = new Intent(getApplicationContext(),
                                    SetPreferences.class);
                            startActivity(intent);
                        }
                    });

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        myView.setBallSpeed(defaultSpeed);
        myView.setSamplingWindow(windowStart-1, windowEnd-1);
        myView.invalidate();

        if (!AnimationView.isAutoDone) {
            Toast.makeText(
                    this,
                    "When the ball appears,\nkeep your finger on it",
                    Toast.LENGTH_LONG).show();
        }
        onModeAuto();

        if (nState > 0) {
            mThreadForData = new MonitorNoiseState();
            mThreadForData.start();
        }

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

			builder.setMessage("\u00a9 2014-2016 Elena Last").setTitle(
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
		}

        if (item.getItemId() == R.id.action_settings) {
            isBackFromSettings = true;
            Intent intent = new Intent(this, com.elena.latencymeter.SetPreferences.class);
            startActivity(intent);
            return true;
        }
			return super.onOptionsItemSelected(item);

	}

	OnSeekBarChangeListener speedBarOnSeekBarChangeListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			float defaultSpeed = (float) (speedBar.getProgress()) * 10.0f
					/ (float) (speedBar.getMax());
			myView.setBallSpeed(defaultSpeed);
            myView.showChart = false;
            myView.resetValues();
            restart.setVisibility(View.INVISIBLE);
			myView.invalidate();

            onModeAuto();

            if (nState > 0 && !mThreadForData.isAlive()) {
                mThreadForData = new MonitorNoiseState();
                mThreadForData.start();
            }
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
        onModeAuto();
	}

	public void boxHideClicked(View view) {
		if (mCheckBox2.isChecked()) {
			showSector = true;
		} else {
			showSector = false;
		}
	}

    public void restartClicked(View view) {
        AnimationView.showChart = false;
        AnimationView.resetValues();

        AnimationView.isAutoDone = false;
            Toast.makeText(
                    this,
                    "When the ball appears,\nkeep your finger on it",
                    Toast.LENGTH_LONG).show();

        AnimationView.count = -1;
        restart.setVisibility(View.INVISIBLE);
        myView.invalidate();
        onModeAuto();

        if (nState > 0 && !mThreadForData.isAlive()) {
            mThreadForData = new MonitorNoiseState();
            mThreadForData.start();
        }

    }

    public void setPauseButton() {

        final int buttonSize = (int) (myView.screenDpi * 50 + 0.5f);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(buttonSize, buttonSize);
        params.leftMargin = myView.cX - buttonSize/2;
        params.topMargin = myView.cY - buttonSize/2;
        playPause.setLayoutParams(params);
        Log.d(TAG, "button set");

    }


    public void pauseClicked(View view) {
        myView.setIsPlaying(!myView.isPlaying);
        if (myView.isPlaying) {
            playPause.setImageResource(R.drawable.stop);
            myView.invalidate();
        } else {
            playPause.setImageResource(R.drawable.play);
        }
    }

    public void onModeAuto() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!AnimationView.isAutoDone && myActivity != null) {
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

        if (!isBackFromSettings) {
            finish();
        }
        super.onPause();
    }

    public static String getTouchFWPath() {

        String touchPath = "/sys/bus/i2c/devices/";
        String exactFolder = "";
        String fwPattern1 = "(?i).*(\\w{1}-\\w{4}).*";

        File rootSyna = new File("/sys/bus/i2c/drivers/synaptics_dsx_i2c");
        list1 = rootSyna.listFiles();
        File rootAtm = new File("/sys/bus/i2c/drivers/atmel_mxt_ts");
        list2 = rootAtm.listFiles();

        if (list1 != null) {

            for (File f : list1) {
                if (f.isDirectory()) {
                    Log.d(TAG, "checking: " + f.toString());
                    if (f.toString().matches(fwPattern1)) {
                        exactFolder = f.toString().replaceAll(fwPattern1, "$1");
                    }
                }
            }
        }
        if (list2 != null && exactFolder == "") {
                for (File f : list2) {
                    if (f.isDirectory()) {
                        Log.d(TAG, "checking: " + f.toString());
                        if (f.toString().matches(fwPattern1)) {
                            exactFolder = f.toString().replaceAll(fwPattern1, "$1");
                        }
                    }
                }
            }

        touchPath += exactFolder;
        Log.d(TAG, "determined touch fw path: " + touchPath);
        return touchPath;
    }

    public static String getTouchCfg() {
        String touchCfg = "";
        String catIC = "";
        catIC = readFile(touchFWPath + "/ic_ver");
        String cfgPattern = "(?i).*Config\\s+ID:\\s+(\\w+).*";
        String prodInfo = "(?i).*Product\\s+ID:\\s+([\\w|\\(|\\)]+)Build.*";//([\w|\(|\)]+)

        if (catIC.matches(prodInfo)) {
            productInfo = catIC.replaceAll(prodInfo, "$1");
        }
        if (catIC.matches(cfgPattern)) {
            touchCfg = catIC.replaceAll(cfgPattern, "$1");
        }
        //Log.d(TAG, "product info: " + productInfo);
        return touchCfg;
    }

    public static String readFile(String fileToRead) {
        FileInputStream is;
        BufferedReader reader;
        String readOut = "";
        String line;
        final File file = new File(fileToRead);

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

        return readOut;
    }

    @SuppressLint("NewApi")
    public static String getPanelType() {
        //TODO
        String panel = readFile("/sys/class/graphics/fb0/panel_supplier");
        String tmpVersion = readFile("/sys/class/graphics/fb0/panel_ver");
        if (!panel.isEmpty() && !tmpVersion.isEmpty()) {
            panel += "-v" + tmpVersion.substring(tmpVersion.length() - 3, tmpVersion.length() - 2); // begin index inclusive, end index exclusive!!!
        }
        return panel;
    }

    //////////////
    public class MonitorNoiseState extends Thread {

        MonitorNoiseState() {}
        ///
        @Override
        public void run() {
            android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);

            prevNoiseState = nState;

                while (!myView.showChart && myActivity != null) {

                    try {
                        noiseF.seek(0);
                        readOut = Character.getNumericValue ((char) noiseF.read());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //Log.d(TAG, "current noise state: " + readOut);
                    if (readOut > 0) {
                        if (readOut != prevNoiseState) {
                            prevNoiseState = readOut;
                            msg = mHandler.obtainMessage(readOut);
                            mHandler.sendMessage(msg);
                            //Log.d(TAG, "noise state has been changed!!!");
                        }
                    }
                    try {
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        }

    }
    /////////////

}
