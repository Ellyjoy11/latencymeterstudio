package com.elena.latencymeter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

/**
 * Created by elenalast on 10/29/15.
 */
public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    private final String[] keys = { "trans", "samples", "start", "end" };
    SharedPreferences userPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        userPref = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

        setSummary();
    }

    public void onPause() {
        super.onPause();
        Context context = getActivity();
        PreferenceManager.getDefaultSharedPreferences(context)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onResume() {
        super.onResume();
        setSummary();
        Context context = getActivity();
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        setSummary();
    }

    private void setSummary() {

        for (int i = 0; i < keys.length; i++) {
            String key_string = keys[i];
            Preference pref = (Preference) findPreference(key_string);
            String value = userPref.getString(key_string, "5");
            pref.setSummary(value);
        }
        if (Integer.parseInt(userPref.getString("end", "1000")) > Integer.parseInt(userPref.getString("samples", "1000"))) {
            Preference prefEnd = (Preference) findPreference("end");
            userPref.edit().putString("end", userPref.getString("samples", "1000")).commit();
            prefEnd.setSummary(userPref.getString("samples", "1000"));
        }
        if (Integer.parseInt(userPref.getString("start", "1")) >= Integer.parseInt(userPref.getString("end", "1000"))) {
            Preference prefStart = (Preference) findPreference("start");
            userPref.edit().putString("start", "1").commit();
            prefStart.setSummary("1");
        }
    }

}
