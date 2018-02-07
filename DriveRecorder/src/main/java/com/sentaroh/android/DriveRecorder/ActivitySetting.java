package com.sentaroh.android.DriveRecorder;

import java.util.List;

import static com.sentaroh.android.DriveRecorder.Constants.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

@SuppressLint("NewApi")
public class ActivitySetting extends PreferenceActivity{
	private static boolean DEBUG_ENABLE=false;
	private static Context mContext=null;
	private static PreferenceFragment mPrefFrag=null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onCreate entered");
        if (Build.VERSION.SDK_INT>=11) return;
	};

    @Override
    public void onStart(){
        super.onStart();
        if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onStart entered");
    };
 
    @Override
    public void onResume(){
        super.onResume();
        if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onResume entered");
        setTitle(R.string.settings_main_title);
    };
 
    @Override
    public void onBuildHeaders(List<Header> target) {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onBuildHeaders entered");
        loadHeadersFromResource(R.xml.settings_frag, target);
    };

//    @Override
//    public boolean isMultiPane () {
//    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity isMultiPane entered");
//        return true;
//    };

    @Override
    public boolean onIsMultiPane () {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onIsMultiPane entered");
        return true;
    };

	@Override  
	protected void onPause() {  
	    super.onPause();  
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onPause entered");
	};

	@Override
	final public void onStop() {
		super.onStop();
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onStop entered");
	};

	@Override
	final public void onDestroy() {
		super.onDestroy();
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onDestroy entered");
	};

	private static void initSettingValueAfterHc(SharedPreferences shared_pref, String key_string) {
		initSettingValue(mPrefFrag.findPreference(key_string),shared_pref,key_string);
	};

	private static void initSettingValue(Preference pref_key, 
			SharedPreferences shared_pref, String key_string) {
		
		if (!checkUiSettings(pref_key,shared_pref, key_string,mContext))
		if (!checkVideoSettings(pref_key,shared_pref, key_string,mContext))
		if (!checkMiscSettings(pref_key,shared_pref, key_string,mContext))				    	
		   	checkOtherSettings(pref_key,shared_pref, key_string,mContext);
	};

	private static SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =   
		    new SharedPreferences.OnSharedPreferenceChangeListener() {  
		    public void onSharedPreferenceChanged(SharedPreferences shared_pref, 
		    		String key_string) {
		    	Preference pref_key=mPrefFrag.findPreference(key_string);
				if (!checkUiSettings(pref_key,shared_pref, key_string,mContext))
				if (!checkVideoSettings(pref_key,shared_pref, key_string,mContext))
				if (!checkMiscSettings(pref_key,shared_pref, key_string,mContext))				    	
				  	checkOtherSettings(pref_key,shared_pref, key_string,mContext);
		    }
	};

	private static boolean checkUiSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
		if (key_string.equals(c.getString(R.string.settings_device_orientation_portrait))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_video_playback_keep_aspect_ratio))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_video_record_start_stop_by_volume_key))) {
    		isChecked=true;
    	}

		return isChecked;
	};

	private static boolean checkMiscSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
		if (key_string.equals(c.getString(R.string.settings_debug_enable))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_exit_cleanly))) {
    		isChecked=true;
    	} 
		return isChecked;
	};


	@SuppressLint("SdCardPath")
	private static boolean checkVideoSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
		if (key_string.equals(c.getString(R.string.settings_video_folder))) {
    		isChecked=true;
    		pref_key.setSummary(shared_pref.getString(key_string, "/mnt/sdcard/DriveRecorder/Videos/"));
    	} else if (key_string.equals(c.getString(R.string.settings_recording_duration))) {
    		isChecked=true;
    		String ts=shared_pref.getString(key_string,"3");
    		String[] ts_label= c.getResources().getStringArray(R.array.settings_recording_duration_list_entries);
    		if (ts.equals("1")) {
        		pref_key.setSummary(ts_label[0]);
    		} else if (ts.equals("2")) {
        		pref_key.setSummary(ts_label[1]);
    		} else if (ts.equals("3")) {
        		pref_key.setSummary(ts_label[2]);
    		} else if (ts.equals("5")) {
        		pref_key.setSummary(ts_label[3]);
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_max_video_keep_generation))) {
    		isChecked=true;
    		String ts=shared_pref.getString(key_string,"100");
    		String[] ts_label= c.getResources().getStringArray(R.array.settings_max_video_keep_generation_list_entries);
    		if (ts.equals("10")) {
        		pref_key.setSummary(ts_label[0]);
    		} else if (ts.equals("50")) {
        		pref_key.setSummary(ts_label[1]);
    		} else if (ts.equals("100")) {
        		pref_key.setSummary(ts_label[2]);
    		} else if (ts.equals("200")) {
        		pref_key.setSummary(ts_label[3]);
    		} else if (ts.equals("300")) {
        		pref_key.setSummary(ts_label[4]);
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_video_record_quality))) {
    		isChecked=true;
    		String ts=shared_pref.getString(key_string,RECORD_VIDEO_QUALITY_HIGH);
    		String[] ts_label= c.getResources().getStringArray(R.array.settings_video_record_quality_list_entries);
    		if (ts.equals(RECORD_VIDEO_QUALITY_LOW)) {
        		pref_key.setSummary(ts_label[0]);
    		} else if (ts.equals(RECORD_VIDEO_QUALITY_MEDIUM)) {
        		pref_key.setSummary(ts_label[1]);
    		} else if (ts.equals(RECORD_VIDEO_QUALITY_HIGH)) {
        		pref_key.setSummary(ts_label[2]);
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_video_record_bitrate))) {
    		isChecked=true;
    		String ts=shared_pref.getString(key_string,RECORD_VIDEO_DEFAULT_BIT_RATE);
    		String[] ts_label= c.getResources().getStringArray(R.array.settings_video_record_bitrate_list_entries);
    		pref_key.setSummary(ts_label[Integer.parseInt(ts)]);
    	} else if (key_string.equals(c.getString(R.string.settings_video_stabilization_enabled))) {
    		isChecked=true;
    		if (!shared_pref.contains(key_string)) {
    			shared_pref.edit().putBoolean(key_string, true).commit();
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_record_sound))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_video_scene_mode_action_enabled))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_start_auto_focus_after_video_record_started))) {
    		isChecked=true;
    	}
    	return isChecked;
	};

	private static boolean checkOtherSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
//		Log.v("","other key="+key_string);
		boolean isChecked = true;
    	if (pref_key!=null) {
    		pref_key.setSummary(
	    		c.getString(R.string.settings_default_current_setting)+
	    		shared_pref.getString(key_string, "0"));
    	} else {
    		Log.v("TextFileBrowserSettings","key not found. key="+key_string);
    	}
    	return isChecked;
	};

    public static class SettingsUi extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentUi onCreate entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_ui);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);

    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_device_orientation_portrait));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_video_playback_keep_aspect_ratio));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_video_record_start_stop_by_volume_key));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentUi onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentUi onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };
	
    public static class SettingsVideo extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentScan onCreate entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_video);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

			SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_video_folder));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_recording_duration));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_max_video_keep_generation));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_video_record_quality));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_video_record_bitrate));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_video_stabilization_enabled));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_record_sound));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_video_scene_mode_action_enabled));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_start_auto_focus_after_video_record_started));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentScan onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentScan onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };

	
    public static class SettingsMisc extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentMisc onCreate entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_misc);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);

    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_debug_enable));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_exit_cleanly));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentMisc onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentMisc onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };

}