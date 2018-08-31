package com.sentaroh.android.DriveRecorder;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.sentaroh.android.DriveRecorder.Log.LogFileListDialogFragment;
import com.sentaroh.android.DriveRecorder.Log.LogUtil;
import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.MiscUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ActivityMain extends AppCompatActivity {
    public AppCompatActivity mActivity=null;
    
    private int mRestartStatus=0;
    private Context mContext=null;

    private GlobalParameters mGp=null;
    
    private Handler mUiHandler=null;
    
    private LogUtil mLog=null;
    private CommonDialog mCommonDlg=null;

    private LinearLayout mMainUiView=null;
    
    private String mCurrentSelectedDayList="";
    private ListView mDayListView=null;
    private ListView mFileListView=null;
    private AdapterDayList mDayListAdapter=null;
    private AdapterFileList mFileListAdapter=null;
    
	@Override  
	protected void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
	};  

	@Override  
	protected void onRestoreInstanceState(Bundle savedInstanceState) {  
		super.onRestoreInstanceState(savedInstanceState);
		mRestartStatus=2;
	};

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    mLog.addDebugMsg(1,"I","onConfigurationChanged Entered, orientation="+newConfig.orientation);
	    
    	processConfigChanged();
	};

	private void processConfigChanged() {
	    int ps_dl_y=0;
	    int pos_dl_x=mDayListView.getFirstVisiblePosition();
		if (mDayListView.getChildAt(0)!=null) ps_dl_y=mDayListView.getChildAt(0).getTop();

	    int ps_fl_y=0;
	    int pos_fl_x=mFileListView.getFirstVisiblePosition();
		if (mFileListView.getChildAt(0)!=null) ps_fl_y=mFileListView.getChildAt(0).getTop();
	    
	    initView();
	    
	    mDayListView.setAdapter(mDayListAdapter);
	    mFileListView.setAdapter(mFileListAdapter);
	    
	    mDayListView.setSelectionFromTop(pos_dl_x, ps_dl_y);
	    mFileListView.setSelectionFromTop(pos_fl_x, ps_fl_y);

        setContextButtonListener();
        if (mFileListAdapter!=null) {
            if (mFileListAdapter.isShowCheckBox()) {
            	setContextButtonSelectMode();
            } else {
            	setContextButtonNormalMode();
            }
            mFileListAdapter.notifyDataSetChanged();
        }
        
        setDayListListener();
        setFileListListener();
	}
	
	private void initView() {
		mLog.addDebugMsg(1,"I","initView Entered");
		setContentView(R.layout.activity_main);
        mMainUiView=(LinearLayout)findViewById(R.id.main_ui_view);
        
        mDayListView=(ListView)findViewById(R.id.main_day_listview);
        mFileListView=(ListView)findViewById(R.id.main_file_listview);
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext=this.getApplicationContext();
        mUiHandler=new Handler();
        mGp=GlobalWorkArea.getGlobalParameters(mContext);

        if (mGp.settingsDeviceOrientationPortrait) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mActivity=this;
//        mGp.surfaceView = (SurfaceView) findViewById(R.id.surfaceView1);
//        mGp.surfaceHolder = mGp.surfaceView.getHolder();
//        mGp.surfaceHolder.addCallback(mSurfaceListener);
//        mGp.surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        
        mLog=new LogUtil(mContext, "Main", mGp);
        
        mLog.addDebugMsg(1, "I","onCreate entered");
        
        mCommonDlg=new CommonDialog(mContext, getSupportFragmentManager());
        
        mFileListAdapter=new AdapterFileList(this, R.layout.file_list_item, new ArrayList<FileListItem>());
        setFileListCheckBoxHandler(mFileListAdapter);

        Intent intent = new Intent(this, RecorderService.class);
        startService(intent);

        initView();
        
    };

    @Override
    public void onResume() {
    	super.onResume();
    	mLog.addDebugMsg(1, "I","onResume entered, restartStatus="+mRestartStatus);
		refreshOptionMenu();
    	if (mRestartStatus==1) {
        	if (isRecording()) {
        		showPreview();
        		setUiEnabled(false);
        	} else {
        		hidePreview();
        		setUiEnabled(true);
        	};
    	} else {
    		NotifyEvent ntfy=new NotifyEvent(mContext);
    		ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
			        setCallbackListener();
					setActivityStarted(true);
					if (mRestartStatus==0) {
						
					} else if (mRestartStatus==2) {
						
					}
	            	if (isRecording()) {
	            		showPreview();
	            		setUiEnabled(false);
	            	} else {
	            		hidePreview();
	            		setUiEnabled(true);
	            	};
			        mRestartStatus=1;
				}
				@Override
				public void negativeResponse(Context c, Object[] o) {}
    		});
    		openService(ntfy);
	        createDayList();
	        if (mDayListAdapter.getCount()>0) {
				mDayListAdapter.getItem(0).isSelected=true;
				mDayListAdapter.notifyDataSetChanged();
				mCurrentSelectedDayList=mDayListAdapter.getItem(0).folder_name;
				createFileList(mCurrentSelectedDayList);
		        setFileListListener();
		        setContextButtonNormalMode();
	        } else {
	        	setContextButtonNormalMode();
	        }
	        setContextButtonListener();
	        setDayListListener();
    	}
    };
    
    @Override
    public void onPause() {
    	super.onPause();
    	mLog.addDebugMsg(1,"I","onPause entered");
    	hidePreview();
    };

    @Override
    public void onStop() {
    	super.onStop();
    	mLog.addDebugMsg(1,"I","onStop entered");
    };

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	mLog.addDebugMsg(1,"I","onDestroy entered");
    	unsetCallbackListener();
    	closeService();
    	mLog.flushLog();
    };

	final private void refreshOptionMenu() {
		this.supportInvalidateOptionsMenu();
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mLog.addDebugMsg(1, "I","onCreateOptionsMenu entered");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_top, menu);
		return true;
	};
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		mLog.addDebugMsg(1,"i","main onKeyDown enterd, kc="+keyCode);
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				terminateApplication();
				return true;
				// break;
			default:
				return super.onKeyDown(keyCode, event);
				// break;
		}
	};

	private void terminateApplication() {
		if (mFileListAdapter!=null && mFileListAdapter.isShowCheckBox()) {
			mFileListAdapter.setShowCheckBox(false);
			mFileListAdapter.notifyDataSetChanged();
			setContextButtonNormalMode();
			return;
		}
		finish();
	};

	
	private void setStartStopBtnEnabled(boolean p) {
		mStartStopBtnEnabled=p;
	};

	private boolean mUiEnabled=true;
	private void setUiEnabled(boolean p) {
		mUiEnabled=p;
		if (p) mMainUiView.setVisibility(LinearLayout.VISIBLE);
		else  mMainUiView.setVisibility(LinearLayout.GONE);
	}
	@SuppressWarnings("unused")
	private boolean isUiEnabled() {
		return mUiEnabled;
	}
	
	private boolean isStartStopBtnEnabled() {
		return mStartStopBtnEnabled;
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	mLog.addDebugMsg(1, "I","onPrepareOptionsMenu entered");
    	super.onPrepareOptionsMenu(menu);
    	if (isStartStopBtnEnabled()) {
    		menu.findItem(R.id.menu_top_start_recorder).setEnabled(true);
    		menu.findItem(R.id.menu_top_stop_recorder).setEnabled(true);
    	} else {
    		menu.findItem(R.id.menu_top_start_recorder).setEnabled(false);
    		menu.findItem(R.id.menu_top_stop_recorder).setEnabled(false);
    	}
		if (mGp.usedCameraId==0) menu.findItem(R.id.menu_top_change_camera).setTitle("BACK");
		else menu.findItem(R.id.menu_top_change_camera).setTitle("FRONT");
    	if (!isRecording()) {
    		menu.findItem(R.id.menu_top_change_camera).setVisible(true);
    		menu.findItem(R.id.menu_top_start_recorder).setVisible(true);
    		menu.findItem(R.id.menu_top_stop_recorder).setVisible(false);
    		menu.findItem(R.id.menu_top_show_log).setVisible(true);
    		menu.findItem(R.id.menu_top_refresh).setVisible(true);
    		menu.findItem(R.id.menu_top_settings).setVisible(true);
    		menu.findItem(R.id.menu_top_about_drive_recorder).setVisible(true);
    		menu.findItem(R.id.menu_top_manage_log).setVisible(true);
    		menu.findItem(R.id.menu_top_start_autofocus).setVisible(false);
    	} else {
    		menu.findItem(R.id.menu_top_change_camera).setVisible(false);
    		menu.findItem(R.id.menu_top_start_recorder).setVisible(false);
    		menu.findItem(R.id.menu_top_stop_recorder).setVisible(true);
    		menu.findItem(R.id.menu_top_show_log).setVisible(false);
    		menu.findItem(R.id.menu_top_refresh).setVisible(false);
    		menu.findItem(R.id.menu_top_settings).setVisible(false);
    		menu.findItem(R.id.menu_top_about_drive_recorder).setVisible(false);
    		menu.findItem(R.id.menu_top_manage_log).setVisible(false);
    		if (isAutoFocusAvailable()) {
    			menu.findItem(R.id.menu_top_start_autofocus).setIcon(R.drawable.focus_successed).setVisible(true);
    		} else {
    			menu.findItem(R.id.menu_top_start_autofocus).setVisible(false);			
    		}
    	}
        return true;
    };
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mLog.addDebugMsg(1, "I","onOptionsItemSelected entered, id="+item.getItemId());
		switch (item.getItemId()) {
			case android.R.id.home:
				processHomeButtonPress();
				return true;
			case R.id.menu_top_change_camera:
		    	try {
		    		mLog.addDebugMsg(1, "I","Cahnge camera");
		    		mRecoderClient.aidlSwitchCamera(0);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
		    	refreshOptionMenu();
				return true;
			case R.id.menu_top_start_recorder:
				setStartStopBtnEnabled(false);
				setUiEnabled(false);
				showPreview();
				startRecorderThread();
				refreshOptionMenu();
				return true;
			case R.id.menu_top_stop_recorder:
				setStartStopBtnEnabled(false);
				stopRecorderThread();
//				hidePreview();
				refreshOptionMenu();
				return true;
			case R.id.menu_top_refresh:
				mCurrentSelectedDayList="";
				createDayList();
				if (mDayListAdapter.getCount()>0) {
					setDayListUnselected();
					mDayListAdapter.getItem(0).isSelected=true;
					mDayListAdapter.notifyDataSetChanged();
					mCurrentSelectedDayList=mDayListAdapter.getItem(0).folder_name;
					createFileList(mCurrentSelectedDayList);
				} else {
					if (mFileListAdapter!=null) {
						mFileListAdapter.clear();
						mFileListAdapter.notifyDataSetChanged();
					}
				}
				if (mFileListAdapter.isShowCheckBox()) setContextButtonSelectMode();
				else setContextButtonNormalMode();
				return true;
			case R.id.menu_top_show_log:
				invokeShowLogActivity();
				return true;
			case R.id.menu_top_settings:
				invokeSettingsActivity();
				return true;				
			case R.id.menu_top_manage_log:
				invokeLogManagement();
				return true;				
			case R.id.menu_top_about_drive_recorder:
				about();
				return true;				
			case R.id.menu_top_start_autofocus:
		    	try {
		    		mLog.addDebugMsg(1, "I","Start auto focus");
		    		mRecoderClient.aidlStartAutoFocus();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				return true;				
		}
		return false;
	};
	
	private void invokeLogManagement() {
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				boolean enabled=(Boolean)o[0];
				mGp.setLogOptionEnabled(mContext,enabled);
				applySettingParms();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
		});
		mLog.resetLogReceiver();
		LogFileListDialogFragment lfmf=LogFileListDialogFragment.newInstance(true,
				mContext.getString(R.string.msgs_log_file_list_title));
		lfmf.showDialog(getSupportFragmentManager(), lfmf, mGp, ntfy);

	};

	
	private void processHomeButtonPress() {
		if (mFileListAdapter.isShowCheckBox()) {
			mFileListAdapter.setShowCheckBox(false);
			mFileListAdapter.notifyDataSetChanged();
			setContextButtonNormalMode();
		}
	};

	private void about() {
		// common 繧ｫ繧ｹ繧ｿ繝�繝�繧､繧｢繝ｭ繧ｰ縺ｮ逕滓��
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.about_dialog);
		((TextView)dialog.findViewById(R.id.about_dialog_title)).setText(
			getString(R.string.msgs_about_drive_recorder)+" Ver "+getApplVersionName());
		final WebView func_view=(WebView)dialog.findViewById(R.id.about_dialog_function);
//	    func_view.setWebViewClient(new WebViewClient());
//	    func_view.getSettings().setJavaScriptEnabled(true); 
		func_view.getSettings().setSupportZoom(true);
//		func_view.setVerticalScrollbarOverlay(true);
		func_view.setBackgroundColor(Color.LTGRAY);
//		func_view.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
		func_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET); 
		func_view.setVerticalScrollBarEnabled(true);
		func_view.setScrollbarFadingEnabled(false);
		if (Build.VERSION.SDK_INT>10) {
			func_view.getSettings().setDisplayZoomControls(true); 
			func_view.getSettings().setBuiltInZoomControls(true);
		} else {
			func_view.getSettings().setBuiltInZoomControls(true);
		}
		func_view.loadUrl("file:///android_asset/"+
				getString(R.string.msgs_about_dlg_func_html));

		func_view.getSettings().setTextZoom(120);
		func_view.getSettings().setDisplayZoomControls(true);
		func_view.getSettings().setBuiltInZoomControls(true);
		
		final Button btnOk = (Button) dialog.findViewById(R.id.about_dialog_btn_ok);
		
		func_view.setVisibility(TextView.VISIBLE);
		
		CommonDialog.setDlgBoxSizeLimit(dialog,true);
		
		// OK繝懊ち繝ｳ縺ｮ謖�螳�
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		// Cancel繝ｪ繧ｹ繝翫�ｼ縺ｮ謖�螳�
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnOk.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();
				
	};

	private String getApplVersionName() {
		try {
		    String packegeName = getPackageName();
		    PackageInfo packageInfo = getPackageManager().getPackageInfo(packegeName, PackageManager.GET_META_DATA);
		    return packageInfo.versionName;
		} catch (NameNotFoundException e) {
			return "";
		}
	};

	
	private void invokeSettingsActivity() {
		Intent intent = new Intent(this, ActivitySetting.class);
		startActivityForResult(intent,0);
	}
	
	protected void onActivityResult(int rc, int resultCode, Intent data) {
		mLog.addDebugMsg(1, "I", "onActivityResult entered, rc="+rc+", result="+resultCode);
		if (rc==0) applySettingParms();
		else if (rc==1) refreshFileList();
	};
	
	private void applySettingParms() {
        mGp.loadSettingParms(this);
        if (mGp.settingsDeviceOrientationPortrait) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	};

	private void invokeShowLogActivity() {
		mLog.flushLog();
//		enableBrowseLogFileMenu=false;
		if (mLog.isLogFileExists()) {
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.setDataAndType(Uri.parse("file://"+mLog.getLogFilePath()), "text/plain");
			startActivity(intent);
		}
	};
	
	private void refreshFileList() {
		mLog.addDebugMsg(1, "I","refreshFileList entered");
		
	    int ps_dl_y=0;
	    int pos_dl_x=mDayListView.getFirstVisiblePosition();
		if (mDayListView.getChildAt(0)!=null) ps_dl_y=mDayListView.getChildAt(0).getTop();

	    int ps_fl_y=0;
	    int pos_fl_x=mFileListView.getFirstVisiblePosition();
		if (mFileListView.getChildAt(0)!=null) ps_fl_y=mFileListView.getChildAt(0).getTop();
	    
		createDayList();
		boolean found=false;
		for (int i=0;i<mDayListAdapter.getCount();i++) {
			if (mCurrentSelectedDayList.equals(mDayListAdapter.getItem(i).folder_name)) {
				found=true;
				break;
			}
		}
		if (found) {
			if (mCurrentSelectedDayList.equals("")) 
				mCurrentSelectedDayList=mDayListAdapter.getItem(0).folder_name;
			createFileList(mCurrentSelectedDayList);
		}
		else {
			if (mDayListAdapter.getCount()>0) {
				mCurrentSelectedDayList=mDayListAdapter.getItem(0).folder_name;
				createFileList(mCurrentSelectedDayList);
			} else {
				mFileListAdapter.clear();
				mFileListAdapter.notifyDataSetChanged();
			}
		}
		mDayListView.setSelectionFromTop(pos_dl_x, ps_dl_y);
		mFileListView.setSelectionFromTop(pos_fl_x, ps_fl_y);
	};

    private void setDayListListener() {
    	mDayListView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				boolean show_cb=mFileListAdapter.isShowCheckBox();
				for (int j = 0; j < mDayListAdapter.getCount(); j++) {
					mDayListAdapter.getItem(j).isSelected=false;
				}
				mDayListAdapter.getItem(position).isSelected=true;
				mDayListAdapter.notifyDataSetChanged();
//	            view.setBackgroundColor(Color.DKGRAY);
				createFileList(mDayListAdapter.getItem(position).folder_name);
				mFileListAdapter.setShowCheckBox(show_cb);
				mFileListAdapter.notifyDataSetChanged();
				if (show_cb) setContextButtonSelectMode();
				else setContextButtonNormalMode();
			}
    	});
    	
//    	mDayListView.setOnItemLongClickListener(new OnItemLongClickListener(){
//			@Override
//			public boolean onItemLongClick(AdapterView<?> parent, View view,
//					final int position, long id) {
//				mCcMenu.addMenuItem(String.format(
//					mContext.getString(R.string.msgs_main_ccmenu_day_delete),mDayListAdapter.getItem(position).folder_name),
//					R.drawable.menu_trash)
//			  		.setOnClickListener(new CustomContextMenuOnClickListener() {
//					  @Override
//					  public void onClick(CharSequence menuTitle) {
//						  if (mDayListAdapter.getItem(position).archive_folder) deleteAllArchiveFolderFile(position);
//						  else deleteAllRecordFoloderFile(position);
//					  }
//				});
//				mCcMenu.createMenu();
//				return true;
//			}
//    	});
    };


    private void setDayListUnselected() {
    	for (int i=0;i<mDayListAdapter.getCount();i++) mDayListAdapter.getItem(i).isSelected=false;
    }
    
	private int deleteMediaStoreItem(String fp) {
		int dc_image=0, dc_audio=0, dc_video=0, dc_files=0;
		String mt=isMediaFile(fp);
		if (mt!=null && 
				(mt.startsWith("audio") ||
				 mt.startsWith("video") ||
				 mt.startsWith("image") )) {
	    	ContentResolver cri = mContext.getContentResolver();
	    	ContentResolver cra = mContext.getContentResolver();
	    	ContentResolver crv = mContext.getContentResolver();
	    	ContentResolver crf = mContext.getContentResolver();
	    	dc_image=cri.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
	          		MediaStore.Images.Media.DATA + "=?", new String[]{fp} );
	       	dc_audio=cra.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
	           		MediaStore.Audio.Media.DATA + "=?", new String[]{fp} );
	       	dc_video=crv.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
	           		MediaStore.Video.Media.DATA + "=?", new String[]{fp} );
	        if(Build.VERSION.SDK_INT >= 11) {
	        	dc_files=crf.delete(MediaStore.Files.getContentUri("external"), 
	          		MediaStore.Files.FileColumns.DATA + "=?", new String[]{fp} );
	        }
//	        Log.v("","fp="+fp);
		} else {
//       		sendDebugLogMsg(1,"I","deleMediaStoreItem not MediaStore library. fn="+
//	       				fp+"");
		}
		return dc_image+dc_audio+dc_video+dc_files;
	};

	@SuppressLint("DefaultLocale")
	private static String isMediaFile(String fp) {
		String mt=null;
		String fid="";
		if (fp.lastIndexOf(".")>0) {
			fid=fp.substring(fp.lastIndexOf(".")+1,fp.length());
			fid=fid.toLowerCase();
		}
		mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
		if (mt==null) return "";
		else return mt;
	};
	
	private void setFileListCheckBoxHandler(AdapterFileList fa) {
    	NotifyEvent ntfy_cb=new NotifyEvent(mContext);
    	ntfy_cb.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				setContextButtonSelectMode();	
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
    	});
    	fa.setNotifyCheckBoxEventHandler(ntfy_cb);
	}

    private void setFileListListener() {
    	mFileListView.setOnItemClickListener(new OnItemClickListener(){
			@SuppressLint("DefaultLocale")
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				for (int j = 0; j < parent.getChildCount(); j++)
	                parent.getChildAt(j).setBackgroundColor(Color.TRANSPARENT);
				if (mFileListAdapter.isShowCheckBox()) {
	            	mFileListAdapter.getItem(position).isChecked=!mFileListAdapter.getItem(position).isChecked;
	            	mFileListAdapter.notifyDataSetChanged();
				} else {
					FileListItem fli=mFileListAdapter.getItem(position);
					Intent intent;
					intent = new Intent(mContext,ActivityVideoPlayer.class);
					intent.putExtra("archive",fli.archive_folder);
					if (fli.archive_folder) intent.putExtra("fd",mGp.videoArchiveDir);
					else intent.putExtra("fd",mGp.videoRecordDir);
					intent.putExtra("fn",fli.file_name);
					startActivityForResult(intent,1);
				}
			}
    	});
    	
    	mFileListView.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, final int pos, long id) {
				if (mFileListAdapter.getCount()==0) return true;
				if (!mFileListAdapter.getItem(pos).isChecked) {
					if (isFileListSelected()) {
						int down_sel_pos=-1, up_sel_pos=-1;
						int tot_cnt=mFileListAdapter.getCount();
						if (pos+1<=tot_cnt) {
							for(int i=pos+1;i<tot_cnt;i++) {
								if (mFileListAdapter.getItem(i).isChecked) {
									up_sel_pos=i;
									break;
								}
							}
						}
						if (pos>0) {
							for(int i=pos;i>=0;i--) {
								if (mFileListAdapter.getItem(i).isChecked) {
									down_sel_pos=i;
									break;
								}
							}
						}
//						Log.v("","up="+up_sel_pos+", down="+down_sel_pos);
						if (up_sel_pos!=-1 && down_sel_pos==-1) {
							for (int i=pos;i<up_sel_pos;i++) 
								mFileListAdapter.getItem(i).isChecked=true;
						} else if (up_sel_pos!=-1 && down_sel_pos!=-1) {
							for (int i=down_sel_pos+1;i<up_sel_pos;i++) 
								mFileListAdapter.getItem(i).isChecked=true;
						} else if (up_sel_pos==-1 && down_sel_pos!=-1) {
							for (int i=down_sel_pos+1;i<=pos;i++) 
								mFileListAdapter.getItem(i).isChecked=true;
						}
						mFileListAdapter.notifyDataSetChanged();
					} else {
						mFileListAdapter.setShowCheckBox(true);
						mFileListAdapter.getItem(pos).isChecked=true;
						mFileListAdapter.notifyDataSetChanged();
					}
					setContextButtonSelectMode();
				}
				return true;
			}
    	});
    	
    };
    
    private boolean isFileListSelected() {
    	boolean result=false;
    	for (int i=0;i<mFileListAdapter.getCount();i++) {
    		if (mFileListAdapter.getItem(i).isChecked) {
    			result=true;
    			break;
    		}
    	}
    	return result;
    };

    @SuppressWarnings("unused")
	private int getFileListSelectedCount() {
    	int result=0;
    	for (int i=0;i<mFileListAdapter.getCount();i++) {
    		if (mFileListAdapter.getItem(i).isChecked) {
    			result++;
    		}
    	}
    	return result;
    };

	private void setContextButtonListener() {
		LinearLayout ll_prof=(LinearLayout) findViewById(R.id.context_filelist_view);
        final ImageButton ib_delete=(ImageButton)ll_prof.findViewById(R.id.context_button_delete);
        final ImageButton ib_orientation=(ImageButton)ll_prof.findViewById(R.id.context_button_orientation);
        final ImageButton ib_share=(ImageButton)ll_prof.findViewById(R.id.context_button_share);
        final ImageButton ib_archive=(ImageButton)ll_prof.findViewById(R.id.context_button_archive);
        final ImageButton ib_select_all=(ImageButton)ll_prof.findViewById(R.id.context_button_select_all);
        final ImageButton ib_unselect_all=(ImageButton)ll_prof.findViewById(R.id.context_button_unselect_all);
        
        ib_delete.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				deleteConfirm();
			}
        });
    	final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		if (mGp.settingsDeviceOrientationPortrait) ib_orientation.setImageResource(R.drawable.orientation_landscape);
		else ib_orientation.setImageResource(R.drawable.orientation_portrait);
        ib_orientation.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				mGp.settingsDeviceOrientationPortrait=!mGp.settingsDeviceOrientationPortrait;
		        if (mGp.settingsDeviceOrientationPortrait) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		        
				prefs.edit().putBoolean(mContext.getString(R.string.settings_device_orientation_portrait),
						mGp.settingsDeviceOrientationPortrait).commit();
				if (mGp.settingsDeviceOrientationPortrait) ib_orientation.setImageResource(R.drawable.orientation_landscape);
				else ib_orientation.setImageResource(R.drawable.orientation_portrait);
			}
        });
        ib_share.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
			    Intent intent = new Intent();
			    intent.setAction(Intent.ACTION_SEND_MULTIPLE);
			    intent.putExtra(Intent.EXTRA_SUBJECT, "Add any subject");
			    intent.setType("video/mp4");

			    ArrayList<Uri> files = new ArrayList<Uri>();
				for (int i=0;i<mFileListAdapter.getCount();i++) {
					if (mFileListAdapter.getItem(i).isChecked) {
						Uri uri=null;
						if (mFileListAdapter.getItem(i).archive_folder) uri=Uri.parse(mGp.videoArchiveDir+mFileListAdapter.getItem(i).file_name);
						else uri=Uri.parse(mGp.videoRecordDir+mFileListAdapter.getItem(i).file_name);
					    files.add(uri);
					}
				}

			    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files); 
			    startActivity(Intent.createChooser(intent,
			    		mContext.getString(R.string.msgs_main_ccmenu_share_title)));
			    
				mFileListAdapter.setShowCheckBox(false);
				mFileListAdapter.notifyDataSetChanged();
				setContextButtonNormalMode();
			}
        });
        ib_archive.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				archiveRecordedVideo();
			}
        });

        ib_select_all.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				mFileListAdapter.setShowCheckBox(true);
				selectAllFileListItem();
				setContextButtonSelectMode();
			}
        });
        ib_unselect_all.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				unselectAllFileListItem();
				setContextButtonSelectMode();
			}
        });

	};

	private void deleteConfirm() {
		if (isFileListSelected()) {
			  String fn="", sep="";
			  for (int i=0;i<mFileListAdapter.getCount();i++) {
				  if (mFileListAdapter.getItem(i).isChecked) {
					  fn+=sep+mFileListAdapter.getItem(i).file_name;
					  sep="\n";
				  }
			  }
			  NotifyEvent ntfy=new NotifyEvent(mContext);
			  ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
					for (int i=mFileListAdapter.getCount()-1;i>=0;i--) {
						if (mFileListAdapter.getItem(i).isChecked) {
							String fp="";
							if (mFileListAdapter.getItem(i).archive_folder) {
								fp=mGp.videoArchiveDir+mFileListAdapter.getItem(i).file_name;
							} else {
								fp=mGp.videoRecordDir+mFileListAdapter.getItem(i).file_name;
							}
					    	File lf=new File(fp);
		        			mLog.addLogMsg("I", "File was deleted. name="+fp);
					        deleteMediaStoreItem(fp);
					        lf.delete();
					    	mFileListAdapter.remove(mFileListAdapter.getItem(i));
						}
					}
					if (mGp.housekeepThumnailCache()) mGp.saveThumnailCacheList();
					mFileListAdapter.setShowCheckBox(false);
					mFileListAdapter.notifyDataSetChanged();
			    	if (mFileListAdapter.getCount()==0) {
			    		createDayList();
				        if (mDayListAdapter.getCount()>0) {
				        	Handler hndl=new Handler();
				        	hndl.postDelayed(new Runnable(){
								@Override
								public void run() {
						    		createFileList(mDayListAdapter.getItem(0).folder_name);
						    		setContextButtonNormalMode();
								}
				        	}, 100);
				        } else {
				        	setContextButtonNormalMode();
				        }
			    	} else {
						setContextButtonNormalMode();
			    	}
				}
				@Override
				public void negativeResponse(Context c, Object[] o) {
				}
			  });
			  mCommonDlg.showCommonDialog(true, "W", 
					  mContext.getString(R.string.msgs_main_ccmenu_file_delete_file_confirm), fn, ntfy);
//		} else {
//			if (mDayListAdapter.getItem(mCurrentSelectedListViewPos).archive_folder) {
//				deleteAllArchiveFolderFile(mCurrentSelectedListViewPos);
//			} else {
//				deleteAllRecordFoloderFile(mCurrentSelectedListViewPos);
//			}
		}
	}
	
	private void archiveRecordedVideo() {
		String arch_list="", sep="";
		final ArrayList<FileListItem> al=new ArrayList<FileListItem>();
		for(int i=0;i<mFileListAdapter.getCount();i++) {
			if (mFileListAdapter.getItem(i).isChecked) {
				arch_list+=sep+mFileListAdapter.getItem(i).file_name;
				sep=",";
				al.add(mFileListAdapter.getItem(i));
			}
		}
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				for(int i=0;i<al.size();i++) {
					boolean result=moveFileToArchive(al.get(i));
					if (!result) {
						break;
					} else {
			    		createDayList();
				        if (mDayListAdapter.getCount()>0) {
				    		createFileList(mDayListAdapter.getItem(0).folder_name);
//				        	Handler hndl=new Handler();
//				        	hndl.postDelayed(new Runnable(){
//								@Override
//								public void run() {
//								}
//				        	}, 100);
				        }
						mGp.saveThumnailCacheList();
					}
				}
	        	Handler hndl=new Handler();
	        	hndl.postDelayed(new Runnable(){
					@Override
					public void run() {
						mFileListAdapter.setShowCheckBox(false);
						mFileListAdapter.notifyDataSetChanged();
						setContextButtonNormalMode();
					}
	        	}, 100);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		mCommonDlg.showCommonDialog(true, "W", 
				  mContext.getString(R.string.msgs_main_ccmenu_file_archive_file_confirm), arch_list, ntfy);
	};

	private boolean moveFileToArchive(FileListItem fli) {
		String fp=mGp.videoRecordDir+fli.file_name;
		String afp=mGp.videoArchiveDir+fli.file_name;
		File tlf=new File(mGp.videoArchiveDir);
		if (!tlf.exists()) tlf.mkdirs();
		
    	File lf=new File(fp);
    	boolean result=lf.renameTo(new File(afp));
    	if (result) {
    		mGp.removeThumnailCache(fp);
    		mGp.addThumnailCache(afp);
    		
    		mLog.addLogMsg("I", "File was archived. name="+fli.file_name);
	        deleteMediaStoreItem(fp);
	    	mFileListAdapter.remove(fli);
	    	scanMediaStoreFile(afp);
    	} else {
    		mLog.addLogMsg("E", "File can not archived. name="+fli.file_name);
			mCommonDlg.showCommonDialog(false, "E", 
					  mContext.getString(R.string.msgs_main_ccmenu_file_archive_error), fli.file_name, null);
    	}
    	return result;
	};
	
	private void setActionBarSelectMode(int sel_cnt, int total_cnt) {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
        String sel_txt=""+sel_cnt+"/"+total_cnt;
        actionBar.setTitle(sel_txt);
	};

	private void setActionBarNormalMode() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.app_name);
		actionBar.setHomeButtonEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(false);
	};

	
	private void setContextButtonSelectMode() {
        int sel_cnt=mFileListAdapter.getItemSelectedCount();
        int tot_cnt=mFileListAdapter.getCount();
		setActionBarSelectMode(sel_cnt, tot_cnt);
		
		LinearLayout ll_prof=(LinearLayout) findViewById(R.id.context_filelist_view);
        LinearLayout ll_delete=(LinearLayout)ll_prof.findViewById(R.id.context_button_delete_view);
        LinearLayout ll_orientation=(LinearLayout)ll_prof.findViewById(R.id.context_button_orientation_view);
        LinearLayout ll_share=(LinearLayout)ll_prof.findViewById(R.id.context_button_share_view);
        LinearLayout ll_archive=(LinearLayout)ll_prof.findViewById(R.id.context_button_archive_view);
        LinearLayout ll_select_all=(LinearLayout)ll_prof.findViewById(R.id.context_button_select_all_view);
        LinearLayout ll_unselect_all=(LinearLayout)ll_prof.findViewById(R.id.context_button_unselect_all_view);

        boolean sel=isFileListSelected();
        
        if (sel) ll_delete.setVisibility(LinearLayout.VISIBLE);
        else ll_delete.setVisibility(LinearLayout.GONE);
        
        ll_orientation.setVisibility(LinearLayout.VISIBLE);
        
        if (sel) ll_share.setVisibility(LinearLayout.VISIBLE);
        else ll_share.setVisibility(LinearLayout.GONE);
        
        if (sel) ll_archive.setVisibility(LinearLayout.VISIBLE);
        else ll_archive.setVisibility(LinearLayout.GONE);
        
        if (tot_cnt!=sel_cnt) ll_select_all.setVisibility(LinearLayout.VISIBLE);
        else ll_select_all.setVisibility(LinearLayout.GONE);

        if (sel) ll_unselect_all.setVisibility(LinearLayout.VISIBLE);
        else ll_unselect_all.setVisibility(LinearLayout.GONE);
        
        ll_prof.invalidate();
	};

	private void setContextButtonNormalMode() {
		setActionBarNormalMode();

		LinearLayout ll_prof=(LinearLayout) findViewById(R.id.context_filelist_view);
        LinearLayout ll_delete=(LinearLayout)ll_prof.findViewById(R.id.context_button_delete_view);
        LinearLayout ll_orientation=(LinearLayout)ll_prof.findViewById(R.id.context_button_orientation_view);
        LinearLayout ll_share=(LinearLayout)ll_prof.findViewById(R.id.context_button_share_view);
        LinearLayout ll_archive=(LinearLayout)ll_prof.findViewById(R.id.context_button_archive_view);
        LinearLayout ll_select_all=(LinearLayout)ll_prof.findViewById(R.id.context_button_select_all_view);
        LinearLayout ll_unselect_all=(LinearLayout)ll_prof.findViewById(R.id.context_button_unselect_all_view);

        ll_delete.setVisibility(LinearLayout.GONE);
        ll_orientation.setVisibility(LinearLayout.VISIBLE);
        ll_share.setVisibility(LinearLayout.GONE);
        ll_archive.setVisibility(LinearLayout.GONE);
        if (mFileListAdapter!=null && mFileListAdapter.getCount()>0) ll_select_all.setVisibility(LinearLayout.VISIBLE);
        else ll_select_all.setVisibility(LinearLayout.GONE);
        ll_unselect_all.setVisibility(LinearLayout.GONE);
        ll_prof.invalidate();
	};

//    private void sendFile(final LogFileListAdapter lfm_adapter) {
//		final String zip_file_name=mGlblParms.settingsLogFileDir+"log.zip";
//		
//		int no_of_files=0;
//		for (int i=0;i<lfm_adapter.getCount();i++) {
//			if (lfm_adapter.getItem(i).isChecked) no_of_files++;
//		}
//		final String[] file_name=new String[no_of_files];
//		int files_pos=0;
//		for (int i=0;i<lfm_adapter.getCount();i++) {
//			if (lfm_adapter.getItem(i).isChecked) {
//				file_name[files_pos]=lfm_adapter.getItem(i).log_file_path;
//				files_pos++;
//			}
//		}
//		final ThreadCtrl tc=new ThreadCtrl();
//		NotifyEvent ntfy=new NotifyEvent(mContext);
//		ntfy.setListener(new NotifyEventListener(){
//			@Override
//			public void positiveResponse(Context c, Object[] o) {
//			}
//			@Override
//			public void negativeResponse(Context c, Object[] o) {
//				tc.setDisabled();
//			}
//		});
//
//		final ProgressBarDialogFragment pbdf=ProgressBarDialogFragment.newInstance(
//				mContext.getString(R.string.msgs_log_file_list_dlg_send_zip_file_creating), 
//				"",
//				mContext.getString(R.string.msgs_common_dialog_cancel),
//				mContext.getString(R.string.msgs_common_dialog_cancel));
//		pbdf.showDialog(getFragmentManager(), pbdf, ntfy,true);
//		Thread th=new Thread() {
//			@Override
//			public void run() {
//				File lf=new File(zip_file_name);
//				lf.delete();
//				MiscUtil.createZipFile(tc,pbdf,zip_file_name,file_name);
//				if (tc.isEnabled()) {
//				    Intent intent=new Intent();
//				    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				    intent.setAction(Intent.ACTION_SEND);  
////				    intent.setType("message/rfc822");  
////				    intent.setType("text/plain");
//				    intent.setType("application/zip");
//				    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(lf)); 
//				    mFragment.getActivity().startActivity(intent);
//
//				    mUiHandler.post(new Runnable(){
//						@Override
//						public void run() {
//							lfm_adapter.setAllItemChecked(false);
//							lfm_adapter.setShowCheckBox(false);
//							lfm_adapter.notifyDataSetChanged();
//							setContextButtonNormalMode(lfm_adapter);
//						}
//				    });
//				} else {
//					lf.delete();
//
//					MessageDialogFragment mdf =MessageDialogFragment.newInstance(false, "W",
//							mContext.getString(R.string.msgs_log_file_list_dlg_send_zip_file_cancelled),
//			        		"");
//			        mdf.showDialog(mFragment.getFragmentManager(), mdf, null);
//
//				}
//				pbdf.dismiss();
//			};
//		};
//		th.start();
//    };
    
    private void scanMediaStoreFile(String fp) {
    	String[] paths = new String[] {fp};
    	MediaScannerConnection.scanFile(getApplicationContext(), paths, null, mOnScanCompletedListener);
    };
    
	private OnScanCompletedListener mOnScanCompletedListener=new OnScanCompletedListener(){
		@Override
		public void onScanCompleted(String path, Uri uri) {
			mLog.addDebugMsg(1,"I", "Scan completed path="+path+", uri="+uri);
		}
	};

    
    private void selectAllFileListItem() {
    	for(int i=0;i<mFileListAdapter.getCount();i++) mFileListAdapter.getItem(i).isChecked=true;
    	mFileListAdapter.notifyDataSetChanged();
    };

    private void unselectAllFileListItem() {
    	for(int i=0;i<mFileListAdapter.getCount();i++) mFileListAdapter.getItem(i).isChecked=false;
    	mFileListAdapter.notifyDataSetChanged();
    };

    private void createDayList() {
    	mLog.addDebugMsg(1, "I","createDayList entered");
//		String c_sel="";
//		if (mDayListAdapter!=null && mDayListAdapter.getCount()>1) {
//			for (int i=0;i<mDayListAdapter.getCount();i++) {
//				if (mDayListAdapter.getItem(i).isSelected) c_sel=mDayListAdapter.getItem(i).folder_name;
//			}
//		}

    	ArrayList<DayFolderListItem> fl=new ArrayList<DayFolderListItem>();
    	File lf=new File(mGp.videoRecordDir);
    	File[] tfl=lf.listFiles();
    	if (tfl!=null && tfl.length>0) {
    		ArrayList<String> sfl=new ArrayList<String>();
    		for (int i=0;i<tfl.length;i++) sfl.add(tfl[i].getName());
    		
    		Collections.sort(sfl);
    		
    		String c_day="";
    		for (int i=0;i<sfl.size();i++) {
    			String tfn=getDayValueFromFileName(sfl.get(i));
    			if (!c_day.equals(tfn)) {
    				DayFolderListItem dli=new DayFolderListItem();
    				dli.folder_name=tfn;
    				fl.add(dli);
    				c_day=tfn;
    				mLog.addDebugMsg(1, "I","createDayList Day "+tfn+" added");
    			}
    		}
    		
    		Collections.sort(fl, new Comparator<DayFolderListItem>(){
				@Override
				public int compare(DayFolderListItem lhs, DayFolderListItem rhs) {
					return lhs.folder_name.compareToIgnoreCase(rhs.folder_name);
				}
    		});
    		
    		for (int i=0;i<fl.size();i++) {
    			int cnt=0;
    			for(int j=0;j<sfl.size();j++) {
    				String tfn=getDayValueFromFileName(sfl.get(i));
    				if (tfn.equals(fl.get(i).folder_name)) {
    					cnt++;
    				}
    			}
    			fl.get(i).no_of_file=""+cnt+"繝輔ぃ繧､繝ｫ";
    		}
    	}
    	mDayListAdapter=new AdapterDayList(this, R.layout.day_list_item, fl);
    	mDayListView.setAdapter(mDayListAdapter);
    	
    	createDayArchiveList();
    	
    	if (mDayListAdapter.getCount()>0) {
//    		Log.v("","size="+mDayListAdapter.getCount()+", s="+mCurrentSelectedDayList);
			boolean found=false;
    		for (int i=0;i<mDayListAdapter.getCount();i++) {
    			if (mDayListAdapter.getItem(i).folder_name.equals(mCurrentSelectedDayList)) {
    				mDayListAdapter.getItem(i).isSelected=true;
    				found=true;
    			}
//    			Log.v("","key="+mDayListAdapter.getItem(i).folder_name+", result="+mDayListAdapter.getItem(i).isSelected);
    		}
//    		Log.v("","found="+found);
			if (!found) mDayListAdapter.getItem(0).isSelected=true;
    		mDayListAdapter.notifyDataSetChanged();
    	}

    };

    private void createDayArchiveList() {
    	mLog.addDebugMsg(1, "I","createDayArchiveList entered");
    	ArrayList<DayFolderListItem> fl=new ArrayList<DayFolderListItem>();
    	File lf=new File(mGp.videoArchiveDir);
    	File[] tfl=lf.listFiles();
    	if (tfl!=null && tfl.length>0) {
			DayFolderListItem dli=new DayFolderListItem();
			dli.folder_name=getString(R.string.msgs_main_folder_type_archive);
			dli.archive_folder=true;
    		dli.no_of_file=""+tfl.length+"繝輔ぃ繧､繝ｫ";
			fl.add(dli);
    		
        	mDayListAdapter.add(dli);
        	mDayListAdapter.notifyDataSetChanged();
    	}
    };
    
    private String getDayValueFromFileName(String fn) {
    	int f_pos=-1, l_pos=-1;
    	String rfn=null;
    	f_pos=fn.indexOf("_20");
    	if (f_pos>=0) {
        	String tfn=fn.substring(f_pos+1);
        	l_pos=tfn.indexOf("_");
        	if (l_pos>0) rfn=tfn.substring(0, l_pos);
//        	Log.v("","name="+fn+", f_pos="+f_pos+", l_pos="+l_pos);
    	}
    	return rfn;
    };
    
    private void createFileList(String sel_day) {
		if (sel_day.startsWith("20")) {
			createRecordFileList(sel_day);
		} else {
    		createArchiveFileList();
		}
    };
    
    private void createRecordFileList(String sel_day) {
    	mLog.addDebugMsg(1, "I","createRecordFileList entered, day="+sel_day);
    	ArrayList<FileListItem> fl=new ArrayList<FileListItem>();
    	File lf=new File(mGp.videoRecordDir);
    	File[] tfl=lf.listFiles();
    	ContentResolver crv = mContext.getContentResolver();
    	String[] query_proj=new String[] {MediaStore.Video.VideoColumns.DURATION};
    	if (tfl!=null && tfl.length>0) {
        	for (int i=0;i<tfl.length;i++) {
        		String tfn=getDayValueFromFileName(tfl[i].getName());
        		if (sel_day.equals(tfn) && !mGp.currentRecordedFileName.equals(tfl[i].getName())) {
        			FileListItem fli=new FileListItem();
        			fli.file_name=tfl[i].getName();
        			fli.file_size=MiscUtil.convertFileSize(tfl[i].length());
        			fli.thumbnail=readThumnailCache(tfl[i]);
        			Cursor cursor=crv.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, query_proj, "_data=?", 
        	    			new String[]{tfl[i].getPath()}, null);
        			if (cursor!=null && cursor.getCount()>0) {
            			cursor.moveToNext();
    			        int dur=Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)));
    					int mm=dur/1000/60;
    					int ss=(dur-(mm*1000*60))/1000;
    			        fli.duration=String.format("%02d",mm)+":"+String.format("%02d",ss);
        			}
                	cursor.close();
                	mLog.addDebugMsg(1, "I","createFileList File "+fli.file_name+" added");
        			fl.add(fli);	
        		}
    		}
    		Collections.sort(fl, new Comparator<FileListItem>(){
				@Override
				public int compare(FileListItem lhs, FileListItem rhs) {
					return lhs.file_name.compareToIgnoreCase(rhs.file_name);
				}
    		});
    	}
    	
    	mFileListAdapter=new AdapterFileList(this, R.layout.file_list_item, fl);
    	setFileListCheckBoxHandler(mFileListAdapter);
    	
    	mFileListView.setAdapter(mFileListAdapter);
    	mCurrentSelectedDayList=sel_day;
    };

    private void createArchiveFileList() {
    	mLog.addDebugMsg(1, "I","createArchiveFileList entered");
    	ArrayList<FileListItem> fl=new ArrayList<FileListItem>();
    	File lf=new File(mGp.videoArchiveDir);
    	File[] tfl=lf.listFiles();
    	ContentResolver crv = mContext.getContentResolver();
    	String[] query_proj=new String[] {MediaStore.Video.VideoColumns.DURATION};
    	if (tfl!=null && tfl.length>0) {
        	for (int i=0;i<tfl.length;i++) {
    			FileListItem fli=new FileListItem();
    			fli.archive_folder=true;
    			fli.file_name=tfl[i].getName();
    			fli.file_size=MiscUtil.convertFileSize(tfl[i].length());
    			fli.thumbnail=readThumnailCache(tfl[i]);
    			Cursor cursor=crv.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, query_proj, "_data=?", 
    	    			new String[]{tfl[i].getPath()}, null);
    			if (cursor!=null && cursor.getCount()>0) {
        			cursor.moveToNext();
			        int dur=Integer.parseInt(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)));
					int mm=dur/1000/60;
					int ss=(dur-(mm*1000*60))/1000;
			        fli.duration=String.format("%02d",mm)+":"+String.format("%02d",ss);
    			}
            	cursor.close();
            	mLog.addDebugMsg(1, "I","createFileList File "+fli.file_name+" added");
    			fl.add(fli);	
    		}
    		Collections.sort(fl, new Comparator<FileListItem>(){
				@Override
				public int compare(FileListItem lhs, FileListItem rhs) {
					return lhs.file_name.compareToIgnoreCase(rhs.file_name);
				}
    		});
    	}
    	
    	mFileListAdapter=new AdapterFileList(this, R.layout.file_list_item, fl);
    	setFileListCheckBoxHandler(mFileListAdapter);
    	mFileListView.setAdapter(mFileListAdapter);
    	mCurrentSelectedDayList=getString(R.string.msgs_main_folder_type_archive);
    };
    
    private boolean mThumnailListModified=false;

    
    
    private Bitmap readThumnailCache(File vf) {
    	Bitmap bm=null;
		byte[] ba=mGp.getThumnailCache(vf.getPath());
    	if (ba!=null) {
   			bm=BitmapFactory.decodeByteArray(ba, 0, ba.length);
    	}
    	mLog.addDebugMsg(1, "I","readThumnailCache File "+vf.getPath()+" Bitmap="+bm+", mThumnailListModified="+mThumnailListModified);
    	return bm;
    };

 	private boolean isAutoFocusAvailable() {
    	boolean result=false;
		try {
			if (mRecoderClient!=null) result=mRecoderClient.aidlIsAutoFocusAvailable();
			else mLog.addDebugMsg(1, "I","isAutoFocusAvailable is not excuted");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return result;
    };
    
    private boolean isRecording() {
    	boolean result=false;
    	try {
    		if (mRecoderClient!=null) result=mRecoderClient.aidlIsRecording();
    		else mLog.addDebugMsg(1, "I","isRecording is not excuted");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    	return result;
    };
    
    private void showPreview() {
    	try {
    		if (mRecoderClient!=null) mRecoderClient.aidlShowPreview();
    		else mLog.addDebugMsg(1, "I","showPreview is not excuted");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    };
    
    private void hidePreview() {
    	try {
    		if (mRecoderClient!=null) mRecoderClient.aidlHidePreview();
    		else mLog.addDebugMsg(1, "I","hidePreview is not excuted");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    };

    private void startRecorderThread() {
    	try {
			if (mRecoderClient!=null) mRecoderClient.aidlStartRecorderThread();
    		else mLog.addDebugMsg(1, "I","startRecorderThread is not excuted");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    };

    private void stopRecorderThread() {
    	try {
			if (mRecoderClient!=null) mRecoderClient.aidlStopRecorderThread();
    		else mLog.addDebugMsg(1, "I","stopRecorderThread is not excuted");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    };

    private void setActivityStarted(boolean started) {
    	try {
			if (mRecoderClient!=null) mRecoderClient.aidlSetActivityStarted(started);
    		else mLog.addDebugMsg(1, "I","aidlSetActivityStarted is not excuted");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
    };

    private boolean mStartStopBtnEnabled=true;
    
	private IRecorderCallback mRecorderCallbackStub=new IRecorderCallback.Stub() {
		@Override
		public void notifyRecordingStarted() throws RemoteException {
			mUiHandler.postDelayed(new Runnable(){
				@Override
				public void run() {
					setStartStopBtnEnabled(true);
					refreshOptionMenu();
				}
			},500);
		};
		@Override
		public void notifyRecordingStopped() throws RemoteException {
			mUiHandler.postDelayed(new Runnable(){
				@Override
				public void run() {
					setStartStopBtnEnabled(true);
					setUiEnabled(true);
					refreshOptionMenu();
			    	createDayList();
			        if (mDayListAdapter.getCount()>0) {
			        	Handler hndl=new Handler();
			        	hndl.postDelayed(new Runnable(){
							@Override
							public void run() {
//					    		mDayListView.getChildAt(0).setBackgroundColor(Color.DKGRAY);
								if (mCurrentSelectedDayList.equals("")) {
									mDayListAdapter.getItem(0).isSelected=true;
									mDayListAdapter.notifyDataSetChanged();
									mCurrentSelectedDayList=mDayListAdapter.getItem(0).folder_name;
								}
								createFileList(mCurrentSelectedDayList);
					    		for (int i=0;i<mDayListAdapter.getCount();i++) {
					    			if (mDayListAdapter.getItem(i).folder_name.equals(mCurrentSelectedDayList)) {
//					    				mDayListView.getChildAt(i).setBackgroundColor(Color.DKGRAY);
					    				break;
					    			}
					    		}
						        if (mFileListAdapter!=null && mFileListAdapter.isShowCheckBox()) setContextButtonSelectMode();
						        else setContextButtonNormalMode();
							}
			        	}, 100);
			        } else {
			        	if (mFileListAdapter!=null) mFileListAdapter.clear();
			        }
			        if (mFileListAdapter!=null && mFileListAdapter.isShowCheckBox()) setContextButtonSelectMode();
			        else setContextButtonNormalMode();
				}
			},500);
		};

    };

	private IRecorderClient mRecoderClient=null;
	private ServiceConnection mSvcConnection=null;
	
	private void openService(final NotifyEvent p_ntfy) {
		mLog.addDebugMsg(1,"I","openService entered");
        mSvcConnection = new ServiceConnection(){
    		public void onServiceConnected(ComponentName arg0, IBinder service) {
    			mLog.addDebugMsg(1,"I","onServiceConnected entered");
    	    	mRecoderClient=IRecorderClient.Stub.asInterface(service);
   	    		p_ntfy.notifyToListener(true, null);
    		}
    		public void onServiceDisconnected(ComponentName name) {
    			mSvcConnection = null;
    			mLog.addDebugMsg(1,"I","onServiceDisconnected entered");
    	    	mRecoderClient=null;
    		}
        };
    	
		Intent intmsg = new Intent(mContext, RecorderService.class);
		intmsg.setAction("Connection");
        bindService(intmsg, mSvcConnection, BIND_AUTO_CREATE);
	};

	private void closeService() {
		mLog.addDebugMsg(1,"I","closeService entered");
    	if (mSvcConnection!=null) {
        	setActivityStarted(false);
//    		if (!isRecording()) {
//        		try {
//    				mRecoderClient.aidlStopService();
//    			} catch (RemoteException e) {
//    				e.printStackTrace();
//    			}
//    		}
    		mRecoderClient=null;
    		unbindService(mSvcConnection);
	    	mSvcConnection=null;
    	}
//        Intent intent = new Intent(this, RecorderService.class);
//        stopService(intent);
	};
	
	final private void setCallbackListener() {
		mLog.addDebugMsg(1, "I", "setCallbackListener entered");
		try{
			mRecoderClient.setCallBack(mRecorderCallbackStub);
		} catch (RemoteException e){
			e.printStackTrace();
			mLog.addDebugMsg(0,"E", "setCallbackListener error :"+e.toString());
		}
	};

	final private void unsetCallbackListener() {
		if (mRecoderClient!=null) {
			try{
				mRecoderClient.removeCallBack(mRecorderCallbackStub);
			} catch (RemoteException e){
				e.printStackTrace();
				mLog.addDebugMsg(0,"E", "unsetCallbackListener error :"+e.toString());
			}
		}
	};
 
}