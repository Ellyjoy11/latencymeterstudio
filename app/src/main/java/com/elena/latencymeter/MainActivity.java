package com.elena.latencymeter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

	public static int screenWidth;
	public static int screenHeight;
    public static float mDensity;
	public static boolean clockWise;
	public static boolean showSector;
    public static int displayTransmissionDelay;
	public static final String TAG = "LatencyMeter";
	public String appVersion;
	//public final String MYPREFS = "my shared prefs";
    SharedPreferences prefs;
    private String tmp;
    private String manufacturer, model_name, device_name;
    private static File[] list1;
    private static File[] list2;

	SeekBar speedBar;
	AnimationView myView;
	CheckBox mCheckBox, mCheckBox2;

	TextView touchInfo;
    public static String touchFWPath;
    public static String panel;
    public static String touchCfg;
    public static String productInfo;

	@SuppressLint("NewApi")
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        prefs = getPreferences(MODE_PRIVATE);

		try {
			appVersion = this.getPackageManager().getPackageInfo(
					this.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			Log.d(TAG, "App version not found " + e.getMessage());
		}

		setContentView(R.layout.activity_main);

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
		float defaultSpeed = (float) (speedBar.getProgress()) * 10.0f
				/ (float) (speedBar.getMax());
		myView.setBallSpeed(defaultSpeed);
        //modeAuto = true;
        //myView.setMode(modeAuto);

		speedBar.setOnSeekBarChangeListener(speedBarOnSeekBarChangeListener);

        touchInfo = (TextView) findViewById(R.id.infoText);
		//TODO read config ID etc.

        touchFWPath = getTouchFWPath();
        touchCfg = getTouchCfg();
        panel = getPanelType();
        String touchInfoText = "";

        if (!touchCfg.isEmpty()) {
            touchInfoText = "Product: " + productInfo + "  Config: " + touchCfg;
        }
        if (!panel.isEmpty()) {
            touchInfoText += " Panel: " + panel;
        }

			touchInfo.setText(touchInfoText);


	}

	@Override
	public void onResume() {
		super.onResume();
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		screenWidth = displaymetrics.widthPixels;
		screenHeight = displaymetrics.heightPixels;
        mDensity = displaymetrics.density;

        tmp = prefs.getString("displayTransmissionDelay", "13");
        displayTransmissionDelay = Integer.parseInt(tmp);

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
		} else if (item.getItemId() == R.id.action_settings) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Display transmission delay, ms");

            final EditText input = new EditText(MainActivity.this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            tmp = prefs.getString("displayTransmissionDelay", "13");
            input.setText(tmp);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String tmpValue = input.getText().toString();
                    //Log.d(TAG, "value is " + tmpValue + " ms");
                    SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                    editor.putString("displayTransmissionDelay", input.getText().toString());
                    editor.commit();

                    displayTransmissionDelay = Integer.parseInt(input.getText().toString());
                    //Log.d(TAG, "integer value is " + displayTransmissionDelay + " ms");

                    AnimationView.isAutoDone = false;
                    AnimationView.count = -1;
                    myView.invalidate();
                    onModeAuto();
                }
            });
            builder.setNegativeButton("Cancel",
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
            //AnimationView.isAutoDone = false;
            //AnimationView.count = -1;
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
		//Log.d(TAG, "direction clockWise: " + clockWise);
        //modeAuto = true;
        //myView.setMode(modeAuto);
		//AnimationView.isAutoDone = false;
        //AnimationView.count = -1;
		myView.invalidate();
        onModeAuto();
	}

	public void boxHideClicked(View view) {
		if (mCheckBox2.isChecked()) {
			showSector = true;
		} else {
			showSector = false;
		}
		//Log.d(TAG, "show sector: " + showSector);

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
        finish();
        super.onPause();
    }

    public static String getTouchFWPath() {

        String touchPath = "/sys/bus/i2c/devices/";// +"2-0020";
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


}
