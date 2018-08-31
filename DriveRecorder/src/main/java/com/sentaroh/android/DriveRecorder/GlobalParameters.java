package com.sentaroh.android.DriveRecorder;


import static com.sentaroh.android.DriveRecorder.Constants.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.sentaroh.android.Utilities.CommonGlobalParms;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.RingtoneManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

@SuppressWarnings("unused")
public class GlobalParameters extends CommonGlobalParms{
	public boolean settingsDebugEnabled=true;
	public int settingsDebugLevel=1;
	public boolean settingsExitCleanly=true;

	public boolean settingsLogEnabled=false;
	
	public boolean isRecording=false;
	
	public boolean screenIsLocked=false;
	
	public int usedCameraId=0;
	public int numberOfCamera=1;
	
	public String logFileDir=null;
//	public String settingsLogTag="BluetoothWidget";
	@SuppressLint("SdCardPath")
	public String settingsLogFileDir="/mnt/sdcard/DriveRecorder/";
	public String settingsLogFileName="DriveRecorder_log";
	public int settingsLogFileBufferSize=1024*32;
	public int settingsLogMaxFileCount=10;
	
	public boolean settingsDeviceOrientationPortrait=false;
	public boolean settingsRecordSound=true;
	
	public boolean settingsVideoStabilizationEnabled=true;

	public int settingsRecordingDuration=3;
	public int settingsMaxVideoKeepGeneration=100;
	
	public String settingsVideoBitRate="0";

	public String settingsRecordVideoQuality=RECORD_VIDEO_QUALITY_LOW;//1280_720;//720_480;
	
	public boolean settingsVideoPlaybackKeepAspectRatio=false;
	
	public boolean settingsVideoStartStopByVolumeKey=true;
	
//	public int settingsVideoFrameRate=30;
    
	public String videoRecordDir="", videoFileNamePrefix="drive_record_", videoArchiveDir="";;
	public String currentRecordedFileName="";
	
	public int settingHeartBeatIntervalTime=1000*60*1;
	
	public ArrayList<ThumnaiCachelListItem> thumnailCacheList=null;
	
	public boolean settingsStartAutoFocusAfterVideoRecordStarted=false;
	
	public boolean settingsSceneModeActionEnabled=false;
    
	public void setLogOptionEnabled(Context c, boolean enabled) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		prefs.edit().putBoolean(c.getString(R.string.settings_debug_enable),enabled).commit();
		settingsLogEnabled=enabled;
	};

	public void loadSettingParms(Context c) {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		String vf=Environment.getExternalStorageDirectory().toString()+"/DriveRecorder/videos/";
		
		videoArchiveDir=Environment.getExternalStorageDirectory().toString()+"/DriveRecorder/archive/";
		
		videoRecordDir=
				prefs.getString(c.getString(R.string.settings_video_folder),vf);
		settingsDebugEnabled=
				prefs.getBoolean(c.getString(R.string.settings_debug_enable),false);
		if (settingsDebugEnabled) {
			settingsDebugLevel=1;
			settingsLogEnabled=true;
		} else {
			settingsDebugLevel=0;
			settingsLogEnabled=false;
		}
		
		settingsVideoBitRate=
				prefs.getString(c.getString(R.string.settings_video_record_bitrate),RECORD_VIDEO_DEFAULT_BIT_RATE);
		
		settingsRecordSound=
				prefs.getBoolean(c.getString(R.string.settings_record_sound),true);
		
		settingsVideoStabilizationEnabled=
				prefs.getBoolean(c.getString(R.string.settings_video_stabilization_enabled),true);
		
		settingsVideoPlaybackKeepAspectRatio=
				prefs.getBoolean(c.getString(R.string.settings_video_playback_keep_aspect_ratio),true);
		settingsVideoStartStopByVolumeKey=
				prefs.getBoolean(c.getString(R.string.settings_video_record_start_stop_by_volume_key),true);
		
		settingsSceneModeActionEnabled=
				prefs.getBoolean(c.getString(R.string.settings_video_scene_mode_action_enabled),false);
				
		settingsExitCleanly=
				prefs.getBoolean(c.getString(R.string.settings_exit_cleanly),false);
		settingsRecordingDuration=Integer.parseInt(
				prefs.getString(c.getString(R.string.settings_recording_duration),"3"));
		settingsMaxVideoKeepGeneration=Integer.parseInt(
				prefs.getString(c.getString(R.string.settings_max_video_keep_generation),"100"));
		
		settingsDeviceOrientationPortrait=
				prefs.getBoolean(c.getString(R.string.settings_device_orientation_portrait),false);

		settingsRecordVideoQuality=
				prefs.getString(c.getString(R.string.settings_video_record_quality),RECORD_VIDEO_QUALITY_HIGH);
		
		settingsStartAutoFocusAfterVideoRecordStarted=
				prefs.getBoolean(c.getString(R.string.settings_start_auto_focus_after_video_record_started),false);
		
		setDebugLevel(settingsDebugLevel);
		setLogLimitSize(2*1024*1024);
		setLogMaxFileCount(10);
		setLogEnabled(settingsLogEnabled);
		setLogDirName(settingsLogFileDir);
		setLogFileName(settingsLogFileName);
		setApplicationTag(APPLICATION_TAG);
//		setLogIntent(BROADCAST_LOG_RESET,
//				BROADCAST_LOG_DELETE,
//				BROADCAST_LOG_FLUSH,
//				BROADCAST_LOG_ROTATE,
//				BROADCAST_LOG_SEND,
//				BROADCAST_LOG_CLOSE);
	};
	
	public void initSettingParms(Context c) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		String vf=Environment.getExternalStorageDirectory().toString()+"/DriveRecorder/videos/";
		if (prefs.getString(c.getString(R.string.settings_video_folder),"").equals("")) {
			prefs.edit().putString(c.getString(R.string.settings_video_folder),vf).commit();
			
			prefs.edit().putBoolean(c.getString(R.string.settings_debug_enable),false).commit();
			prefs.edit().putBoolean(c.getString(R.string.settings_exit_cleanly),false).commit();
			
			prefs.edit().putString(c.getString(R.string.settings_recording_duration),"3").commit();
			prefs.edit().putString(c.getString(R.string.settings_max_video_keep_generation),"100").commit();
			prefs.edit().putBoolean(c.getString(R.string.settings_record_sound),true).commit();
			
			prefs.edit().putBoolean(c.getString(R.string.settings_video_stabilization_enabled),true).commit();
			
			prefs.edit().putBoolean(c.getString(R.string.settings_video_playback_keep_aspect_ratio),true).commit();
			prefs.edit().putBoolean(c.getString(R.string.settings_video_record_start_stop_by_volume_key),true).commit();
		}
	};
	
	public boolean thumnailCacheListModified=false;
    public boolean housekeepThumnailCache() {
    	synchronized(thumnailCacheList) {
        	File rf=new File(videoRecordDir);
        	File[] rfl=rf.listFiles();
        	if (rfl!=null) {
            	for (int i=0;i<rfl.length;i++) {
            		byte[] ba=getThumnailCache(rfl[i].getPath());
//            		Log.v("","ba="+ba+", fp="+rfl[i].getPath());
            		if (ba==null) {
            			addThumnailCache(rfl[i].getPath());
            			thumnailCacheListModified=true;
            		}
            	}
        	}
        	
        	File af=new File(videoArchiveDir);
        	File[] afl=af.listFiles();
        	if (afl!=null) {
            	for (int i=0;i<afl.length;i++) {
            		byte[] ba=getThumnailCache(afl[i].getPath());
//            		Log.v("","ba="+ba+", fp="+afl[i].getPath());
            		if (ba==null) {
            			addThumnailCache(afl[i].getPath());
            			thumnailCacheListModified=true;
            		}
            	}
        	}

        	if (thumnailCacheList.size()>0) {
            	for(int i=thumnailCacheList.size()-1;i>=0;i--) {
            		ThumnaiCachelListItem tli=thumnailCacheList.get(i);
                	File tlf=new File(tli.file_path);
                	if (!tlf.exists()) {
                		thumnailCacheList.remove(i);
                		thumnailCacheListModified=true;
                	}
            	}
        	}
    	}
    	return thumnailCacheListModified;
    };
	
    public void addThumnailCache(String fp) {
    	if (thumnailCacheList==null) return;
    	ThumnaiCachelListItem tli=new ThumnaiCachelListItem();
		tli.file_path=fp;
		Bitmap bm=ThumbnailUtils.createVideoThumbnail(fp, MediaStore.Images.Thumbnails.MICRO_KIND);
		if (bm!=null) {
    		ByteArrayOutputStream baos=new ByteArrayOutputStream();
    		bm.compress(CompressFormat.PNG, 50, baos);
    		try {
				baos.flush();
	    		baos.close();
	    		tli.thumnail_byte_array=baos.toByteArray();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		synchronized(thumnailCacheList) {
			thumnailCacheList.add(tli);
			thumnailCacheListModified=true;
		}
    };

    public boolean removeThumnailCache(String fp) {
    	if (thumnailCacheList==null) return false;
    	boolean result=false;
    	synchronized(thumnailCacheList) {
        	for (int i=0;i<thumnailCacheList.size();i++) {
        		if (thumnailCacheList.get(i).file_path.equals(fp)) {
        			thumnailCacheList.remove(i);
        			thumnailCacheListModified=true;
        			result=true;
        			break;
        		}
        	}
    	}
    	return result;
    };

    public byte[] getThumnailCache(String fp) {
    	if (thumnailCacheList==null) return null;
    	byte[] result=null;
    	synchronized(thumnailCacheList) {
        	for (int i=0;i<thumnailCacheList.size();i++) {
//        		Log.v("","tn path="+thumnailCacheList.get(i).file_path+", fp="+fp);
        		if (thumnailCacheList.get(i).file_path.equals(fp)) {
        			result=thumnailCacheList.get(i).thumnail_byte_array;
        			break;
        		}
        	}
    	}
    	return result;
    };

    public void loadThumnaiCachelList() {
    	thumnailCacheList=new ArrayList<ThumnaiCachelListItem>();
    	synchronized(thumnailCacheList) {
        	File lf=new File(Environment.getExternalStorageDirectory().toString()+"/DriveRecorder/thumnail_cache");
        	if (lf.exists()) {
            	try {
        			FileInputStream fis=new FileInputStream(lf);
        			BufferedInputStream bis=new BufferedInputStream(fis,1024*256);
        			ObjectInputStream ois=new ObjectInputStream(bis);
        			int l_cnt=ois.readInt();
        	    	for(int i=0;i<l_cnt;i++) {
        	    		ThumnaiCachelListItem tli=new ThumnaiCachelListItem();
        	    		tli.file_path=ois.readUTF();
        	    		int b_cnt=ois.readInt();
        	    		if (b_cnt!=0) {
        	    			tli.thumnail_byte_array=new byte[b_cnt];
        	    			ois.readFully(tli.thumnail_byte_array);
        	    		}
                    	File tlf=new File(tli.file_path);
                    	if (tlf.exists()) {
                    		thumnailCacheList.add(tli);
                    	}
        	    	}
        	    	ois.close();
        		} catch (FileNotFoundException e) {
        			e.printStackTrace();
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
            	thumnailCacheListModified=false;
        	}
    	}
    };
    
    public void saveThumnailCacheList() {
    	synchronized(thumnailCacheList) {
        	File lf=new File(Environment.getExternalStorageDirectory().toString()+"/DriveRecorder/");
        	if (!lf.exists()) lf.mkdirs();
        	lf=new File(Environment.getExternalStorageDirectory().toString()+"/DriveRecorder/thumnail_cache");
        	lf.delete();
        	try {
    			if (thumnailCacheList.size()>0) {
    				Collections.sort(thumnailCacheList,new Comparator<ThumnaiCachelListItem>(){
    					@Override
    					public int compare(ThumnaiCachelListItem lhs, ThumnaiCachelListItem rhs) {
    						return lhs.file_path.compareToIgnoreCase(rhs.file_path);
    					}
    				});
        			FileOutputStream fos=new FileOutputStream(lf);
        			BufferedOutputStream bos=new BufferedOutputStream(fos,1024*256);
        			ObjectOutputStream oos=new ObjectOutputStream(bos);
        			oos.writeInt(thumnailCacheList.size());
        	    	for(int i=0;i<thumnailCacheList.size();i++) {
        	    		ThumnaiCachelListItem tli=thumnailCacheList.get(i);
        	    		if (!tli.file_path.equals("")) {
        		    		oos.writeUTF(tli.file_path);
        		    		if (tli.thumnail_byte_array!=null) {
        		    			oos.writeInt(tli.thumnail_byte_array.length);
        			    		oos.write(tli.thumnail_byte_array,0,tli.thumnail_byte_array.length);
        		    		} else {
        		    			oos.writeInt(0);
        		    		}
        	    		}
        	    	}
        	    	oos.flush();
        	    	oos.close();
    			}
    		} catch (FileNotFoundException e) {
    			e.printStackTrace();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
        	thumnailCacheListModified=false;
    	}
    };

}
class ThumnaiCachelListItem {
	public String file_path="";
	public byte[] thumnail_byte_array=null;
}

