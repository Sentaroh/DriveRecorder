package com.sentaroh.android.DriveRecorder;

import static com.sentaroh.android.DriveRecorder.Constants.*;

import com.sentaroh.android.DriveRecorder.Log.LogUtil;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.RemoteViews;

public class WidgetService {
	
	private static GlobalParameters mGp=null;
	private static LogUtil mLog=null;
	
	private static RemoteViews mRvToggleRecorder=null;
	private static ComponentName mCnToggleRecorder=null;

	private static AppWidgetManager mWidgetManager=null;

	private static Bitmap mRecorderIconStart=null, mRecorderIconStop=null, mRecorderIconStartStop=null;
	
	private static Context mContext=null;
	
	public WidgetService(Context c, GlobalParameters gp, LogUtil log) {
		mContext=c;
		mGp=gp;
		mLog=log;
		mWidgetManager=AppWidgetManager.getInstance(mContext);
		
		mRecorderIconStart=BitmapFactory.decodeResource(mContext.getResources(), R.drawable.recorder_started);
		mRecorderIconStartStop=BitmapFactory.decodeResource(mContext.getResources(), R.drawable.recorder_start_stop);
		mRecorderIconStop=BitmapFactory.decodeResource(mContext.getResources(), R.drawable.recorder_stopped);
		
		initialyzeWidget();
	}
	
	
	final static private void initialyzeWidget() { 
		createToggleRecorderRemoteView();
	    setToggleRecorderButtonIcon(mRvToggleRecorder,false,mGp.isRecording);
		setToggleRecorderButtonIntent();
		
		updateToggleRecorderWidget();
	};

	public void processWidgetIntent(Intent intent, String action) {
    	if (action.equals(WIDGET_RECORDER_ENABLE)) {
    		createToggleRecorderRemoteView();
    	} else if (action.equals(WIDGET_RECORDER_UPDATE)) {
    		if (mRvToggleRecorder==null) createToggleRecorderRemoteView();
    		setToggleRecorderButtonIntent();
    		setToggleRecorderButtonIcon(mRvToggleRecorder,false,mGp.isRecording);
    		updateToggleRecorderWidget();
    	} else if (action.equals(WIDGET_RECORDER_DISABLE)) {
    		removeToggleRecorderRemoteView();
    	}
	}
	
	public void updateIcon(boolean start) {
		setToggleRecorderButtonIcon(mRvToggleRecorder,false,start);
		updateToggleRecorderWidget();
	}

	public void setIconStartStop() {
		setToggleRecorderButtonIcon(mRvToggleRecorder,true,false);
		updateToggleRecorderWidget();
	}

	static final private void setToggleRecorderButtonIcon(final RemoteViews rv,
			final boolean on_off, final boolean start) {
		if (rv==null) return;
		if (on_off) {
			rv.setImageViewBitmap(R.id.device_layout_toggle_recorder_btn, mRecorderIconStartStop);
		} else {
	    	if (start) {
	    		rv.setImageViewBitmap(R.id.device_layout_toggle_recorder_btn, mRecorderIconStart);
	    	} else {
	    		rv.setImageViewBitmap(R.id.device_layout_toggle_recorder_btn, mRecorderIconStop);
	    	}
		}
    };
    
    static final private void createToggleRecorderRemoteView() {
        int[] wids =mWidgetManager.getAppWidgetIds(new ComponentName(mContext, WidgetProviderRecorder.class));
        if (wids!=null && wids.length>0) {
			mRvToggleRecorder = new RemoteViews(mContext.getPackageName(), R.layout.widget_layout_recorder);
			mCnToggleRecorder = new ComponentName(mContext, WidgetProviderRecorder.class);
			mLog.addDebugMsg(1,"I","ToggleRecorder RemoteViews created");
        }
    };

    static final private void removeToggleRecorderRemoteView() {
    	if (mRvToggleRecorder!=null) {
        	Intent intent = new Intent();
        	intent.setAction(TOGGLE_RECORDER_INTENT);
        	PendingIntent pi = 
        		PendingIntent.getBroadcast(mContext, 0, intent,PendingIntent.FLAG_CANCEL_CURRENT);
        	mRvToggleRecorder.setOnClickPendingIntent(R.id.device_layout_toggle_recorder_btn, pi);
    		mRvToggleRecorder = null;
    		mCnToggleRecorder = null;
    		mLog.addDebugMsg(1,"I","ToggleRecorder RemoteViews was removed");
    	}
    };

    static final private void updateToggleRecorderWidget() {
	    if (mRvToggleRecorder!=null) 
	    	mWidgetManager.updateAppWidget(mCnToggleRecorder, mRvToggleRecorder);
	};

	static final private void setToggleRecorderButtonIntent() {
    	if (mRvToggleRecorder==null) return;
//    	util.addDebugMsg(1,"I","setToggleRecorderButtonIntent entered");
        Intent intent = new Intent();
    	intent.setAction(TOGGLE_RECORDER_INTENT);
    	PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 
    			PendingIntent.FLAG_UPDATE_CURRENT);
    	mRvToggleRecorder.setOnClickPendingIntent(R.id.device_layout_toggle_recorder_btn, pendingIntent);
    };

}
