package com.sentaroh.android.DriveRecorder.Log;

import static com.sentaroh.android.DriveRecorder.Constants.*;
import android.content.Context;

import com.sentaroh.android.DriveRecorder.GlobalParameters;
import com.sentaroh.android.Utilities.CommonGlobalParms;
import com.sentaroh.android.Utilities.LogUtil.CommonLogReceiver;

public class LogReceiver extends CommonLogReceiver{
	@Override
	public void setLogParms(Context c, CommonGlobalParms cgp) {
		GlobalParameters tgp=new GlobalParameters();
		tgp.loadSettingParms(c);
		
		cgp.setDebugLevel(tgp.settingsDebugLevel);
		cgp.setLogLimitSize(2*1024*1024);
		cgp.setLogMaxFileCount(tgp.settingsLogMaxFileCount);
		cgp.setLogEnabled(tgp.settingsLogEnabled);
		cgp.setLogDirName(tgp.settingsLogFileDir);
		cgp.setLogFileName(tgp.settingsLogFileName);
		cgp.setApplicationTag(APPLICATION_TAG);
		cgp.setLogIntent(BROADCAST_LOG_RESET,
				BROADCAST_LOG_DELETE,
				BROADCAST_LOG_FLUSH,
				BROADCAST_LOG_ROTATE,
				BROADCAST_LOG_SEND,
				BROADCAST_LOG_CLOSE);

	};

}
