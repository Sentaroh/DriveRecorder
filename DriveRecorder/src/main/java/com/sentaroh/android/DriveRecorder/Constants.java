package com.sentaroh.android.DriveRecorder;

public class Constants {
	public final static String APPLICATION_TAG="DriveRecorder";
	public final static String PACKAGE_NAME="com.sentaroh.android.DriveRecorder";
	public static final String DEFAULT_PREFS_FILENAME="default_preferences";
	
	public final static String RECORD_VIDEO_QUALITY_LOW="LOW";
	public final static String RECORD_VIDEO_QUALITY_MEDIUM="MEDIUM";
	public final static String RECORD_VIDEO_QUALITY_HIGH="HIGH";
	
	public final static String RECORD_VIDEO_DEFAULT_BIT_RATE="8";
	
//	public static final String SVC_CAMERA_STOP_PREVIEW="SVC_CAMERA_STOP_PREVIEW";
//	public static final String SVC_CAMERA_START_PREVIEW="SVC_CAMERA_START_PREVIEW";
//	public static final String SVC_CAMERA_START_RECORDER="SVC_CAMERA_START_RECORDER";
//	public static final String SVC_CAMERA_STOP_RECORDER="SVC_CAMERA_STOP_RECORDER";
//	public static final String SVC_CAMERA_SWITCH_RECORDER="SVC_CAMERA_SWITCH_RECORDER";
//	
	public static final String TOGGLE_RECORDER_INTENT="com.sentaroh.android.DriveRecorder.WIDGET_RECORDER_TOGGLE_INTENT";
	
	public static final String WIDGET_RECORDER_PREFIX="WIDGET_RECORDER_";
	public static final String WIDGET_RECORDER_UPDATE="WIDGET_RECORDER_UPDATE";
	public static final String WIDGET_RECORDER_DISABLE="WIDGET_RECORDER_DISABLE";
	public static final String WIDGET_RECORDER_ENABLE="WIDGET_RECORDER_ENABLE";
	public static final String WIDGET_RECORDER_DELETE="WIDGET_RECORDER_DELETE";
	
	public static final String BROADCAST_SERVICE_HEARTBEAT=
			"com.sentaroh.android.DriveRecorder.ACTION_SERVICE_HEARTBEAT";

	public static final String BROADCAST_LOG_SEND=PACKAGE_NAME+".ACTION_LOG_SEND";
	public static final String BROADCAST_LOG_RESET=PACKAGE_NAME+".ACTION_LOG_RESET";
	public static final String BROADCAST_LOG_ROTATE=PACKAGE_NAME+".ACTION_LOG_ROTATE";
	public static final String BROADCAST_LOG_DELETE=PACKAGE_NAME+".ACTION_LOG_DELETE";
	public static final String BROADCAST_LOG_FLUSH=PACKAGE_NAME+".ACTION_LOG_FLUSH";
	public static final String BROADCAST_LOG_CLOSE=PACKAGE_NAME+".ACTION_LOG_CLOSE";

}
