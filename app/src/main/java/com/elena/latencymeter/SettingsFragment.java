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

    private final String[] keys = { "trans", "samples", "multi" };
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
    }

}
