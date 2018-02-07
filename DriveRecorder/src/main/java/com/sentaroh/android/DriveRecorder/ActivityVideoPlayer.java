package com.sentaroh.android.DriveRecorder;

import java.io.BufferedOutputStream;
import java.io.File;	
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.sentaroh.android.DriveRecorder.Log.LogUtil;
import com.sentaroh.android.Utilities.StringUtil;
import com.sentaroh.android.Utilities.MiscUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaScannerConnection;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.media.MediaScannerConnection.OnScanCompletedListener;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityVideoPlayer extends FragmentActivity{

    private int mRestartStatus=0;
    private Context mContext=null;
    
    @SuppressWarnings("unused")
	private boolean mApplicationTerminated=false;

    private GlobalParameters mGp=null;
    
    private Handler mUiHandler=null;
    
    private LogUtil mLog=null;
//    private CustomContextMenu mCcMenu=null;
    private CommonDialog mCommonDlg=null;

	private ArrayList<FileListItem> mFileList=new ArrayList<FileListItem>();
	private int mCurrentSelectedPos=0;
	private SurfaceView mSurfaceView=null, mThumnailView=null;;
	private SurfaceHolder mSurfaceHolder=null;
//	private Activity mActivity=null;

	private final static int VIDEO_STATUS_STOPPED=0;
	private final static int VIDEO_STATUS_PLAYING=1;
	private final static int VIDEO_STATUS_PAUSING=2;
	private int mVideoPlayerStatus=VIDEO_STATUS_STOPPED;
	
	private boolean mIsVideoReadyToBePlayed=true;
	private boolean mIsPlayRequiredAfterMoveFrame=false;
	
	private SeekBar mSbPlayPosition=null;
	private TextView mTvPlayPosition=null;
	private TextView mTvEndPosition=null;
	private ImageButton mIbPrevFile=null;
	private ImageButton mIbPlay=null;
	private ImageButton mIbNextFile=null;
	private ImageButton mIbDeleteFile=null;
	private ImageButton mIbShare=null;
	private TextView mTvTitle=null;
	private ImageButton mIbArchive=null;
	private ImageButton mIbCapture=null;
	private ImageButton mIbForward=null, mIbBackward=null;
	private ImageButton mIbMediaPlayer=null;
//	private LinearLayout mLayoutTop=null;
//	private LinearLayout mLayoutBottom=null;

	private boolean mIsArchiveFolder=false;
	private String mVideoFolder="";
    
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
	    Point dsz=new Point();
	    getWindowManager().getDefaultDisplay().getSize(dsz);
//	    Log.v("","x="+dsz.x+", y="+dsz.y);
	    
	    if (!isVideoPlayerStatusStopped()) {
			if (mGp.settingsVideoPlaybackKeepAspectRatio) {
			    float video_Width = mMediaPlayer.getVideoWidth();
			    float video_Height = mMediaPlayer.getVideoHeight();
			    float ratio_width = dsz.x/video_Width;
			    float ratio_height = dsz.y/video_Height;
			    float aspectratio = video_Width/video_Height;
			    android.view.ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
			    if (ratio_width > ratio_height){
				    layoutParams.width = (int) (dsz.y * aspectratio);
				    layoutParams.height = dsz.y;
			    }else{
			    	layoutParams.width = dsz.x;
			    	layoutParams.height = (int) (dsz.x / aspectratio);
			    }
			    mSurfaceView.setLayoutParams(layoutParams);
			}
	    }
	    if (mThumnailView.isShown()) {
			if (mThumnailBitmap!=null) {
			    float video_Width = mThumnailBitmap.getWidth();
			    float video_Height = mThumnailBitmap.getHeight();
			    float ratio_width = dsz.x/video_Width;
			    float ratio_height = dsz.y/video_Height;
			    float aspectratio = video_Width/video_Height;
			    android.view.ViewGroup.LayoutParams layoutParams = mThumnailView.getLayoutParams();
			    if (ratio_width > ratio_height){
				    layoutParams.width = (int) (dsz.y * aspectratio);
				    layoutParams.height = dsz.y;
			    }else{
			    	layoutParams.width = dsz.x;
			    	layoutParams.height = (int) (dsz.x / aspectratio);
			    }
			    mThumnailView.setLayoutParams(layoutParams);
			}
	    }
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.video_player);
        mUiHandler=new Handler();
        mGp=(GlobalParameters) this.getApplication();
        mGp.initSettingParms(this);
        mGp.loadSettingParms(this);
        
        mContext=this.getApplicationContext();
        
        mLog=new LogUtil(mContext, "VideoPlayer", mGp);
        
        mLog.addDebugMsg(1, "I","onCreate entered");
        
//        mCcMenu = new CustomContextMenu(getResources(),getSupportFragmentManager());
        mCommonDlg=new CommonDialog(mContext, getSupportFragmentManager());

//        if (mGp.settingsDeviceOrientationPortrait) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mTcPlayer=new ThreadCtrl();
        mMediaPlayer = new MediaPlayer();
        
		mSbPlayPosition=(SeekBar)findViewById(R.id.video_player_dlg_played_pos);
		mTvPlayPosition=(TextView)findViewById(R.id.video_player_dlg_played_time);
		mTvEndPosition=(TextView)findViewById(R.id.video_player_dlg_played_endpos);
		mSurfaceView=(SurfaceView)findViewById(R.id.video_player_dlg_video);
		mThumnailView=(SurfaceView)findViewById(R.id.video_player_dlg_thumnail);
		mIbPrevFile=(ImageButton) findViewById(R.id.video_player_dlg_prev);
		mIbPlay=(ImageButton)findViewById(R.id.video_player_dlg_start_stop);
		mIbNextFile=(ImageButton)findViewById(R.id.video_player_dlg_next);
		mIbDeleteFile=(ImageButton)findViewById(R.id.video_player_dlg_delete);
		mIbShare=(ImageButton)findViewById(R.id.video_player_dlg_share);
		mIbArchive=(ImageButton)findViewById(R.id.video_player_dlg_archive);
		mIbCapture=(ImageButton)findViewById(R.id.video_player_dlg_capture);
		mIbForward=(ImageButton)findViewById(R.id.video_player_dlg_forward);
		mIbBackward=(ImageButton)findViewById(R.id.video_player_dlg_backward);
		mIbMediaPlayer=(ImageButton)findViewById(R.id.video_player_dlg_start_media_player);
		mTvTitle=(TextView)findViewById(R.id.video_player_dlg_title);
//		mLayoutTop=(LinearLayout)findViewById(R.id.video_player_dlg_top_panel);
//		mLayoutBottom=(LinearLayout)findViewById(R.id.video_player_dlg_bottom_panel);

    };

    @Override
    public void onResume() {
    	super.onResume();
    	mLog.addDebugMsg(1, "I","onResume entered, restartStatus="+mRestartStatus);
    	if (mRestartStatus==1) {
    	} else {
    		initFileList();
			if (mRestartStatus==0) {
				
			} else if (mRestartStatus==2) {
				
			}
	        mRestartStatus=1;
	        
	        setMainViewListener();
    	}
    };
    
    @Override
    public void onPause() {
    	super.onPause();
    	mLog.addDebugMsg(1,"I","onPause entered");
    	if (isVideoPlayerStatusPlaying()) {
    		pauseVideoPlaying();
    	}
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
    	mApplicationTerminated=true;
		if (!isVideoPlayerStatusStopped()) {
			mMediaPlayer.stop();
			mMediaPlayer.reset();
		}
    	mMediaPlayer.release();
    	mLog.flushLog();
    };



	public String getRealPathFromURI(Context context, Uri contentUri) {
	  Cursor cursor = null;
	  try { 
	    String[] proj = { MediaStore.Images.Media.DATA };
	    cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
	    cursor.moveToFirst();
	    return cursor.getString(column_index);
	  } finally {
	    if (cursor != null) {
	      cursor.close();
	    }
	  }
	};


    
	private void initFileList() {
		Intent intent=getIntent();
		String s_fn="";
//		Log.v("","ext="+intent.getExtras()+", data="+intent.getData().toString());
		if (intent.getData()!=null) {//Invoked by other application
			mIsArchiveFolder=true;
			if (intent.getData().toString().startsWith("content:")) {
				String fp=getRealPathFromURI(this,intent.getData());
				String nfn="";
				if (fp.lastIndexOf("/")>0) {
					nfn=fp.substring(fp.lastIndexOf("/")+1);
				} else {
					nfn=fp;
				}
				String nfd=fp.replace(nfn,"");
				mVideoFolder=nfd;
				s_fn=nfn;
//				Log.v("","fp="+fp+", s_fn="+s_fn);
			} else {
				String fp=intent.getData().getPath().replace("file://", "");
				String nfn="";
				if (fp.lastIndexOf("/")>0) {
					nfn=fp.substring(fp.lastIndexOf("/")+1);
				} else {
					nfn=fp;
				}
				String nfd=fp.replace(nfn,"");
				mVideoFolder=nfd;
				s_fn=nfn;
			}
		} else {//Invoked by DriveRecorder
			if (intent.getExtras()!=null && intent.getExtras().containsKey("archive")) {
				mIsArchiveFolder=getIntent().getBooleanExtra("archive",false);
				mVideoFolder=getIntent().getStringExtra("fd");
				s_fn=getIntent().getStringExtra("fn");
			} else {
				mIsArchiveFolder=true;
				String fp=intent.getDataString().replace("file://", "");
				String nfn="";
				if (fp.lastIndexOf("/")>0) {
					nfn=fp.substring(fp.lastIndexOf("/")+1);
				} else {
					nfn=fp;
				}
				String nfd=fp.replace(nfn,"");
				mVideoFolder=nfd;
			}
		}
    	
//    	Log.v("","archive="+mIsArchiveFolder+", fd="+mVideoFolder+", fn="+s_fn);
    	File lf=new File(mVideoFolder);
    	File[] tfl=lf.listFiles();
    	if (tfl!=null && tfl.length>0) {
    		for (int i=0;i<tfl.length;i++) {
    			FileListItem fli=new FileListItem();
    			if (tfl[i].isFile()) {
    				String ft="";
    				String mime_type="";
    				if (tfl[i].getName().lastIndexOf(".")>=0) {
    					ft=tfl[i].getName().substring(tfl[i].getName().lastIndexOf(".")+1);
        				mime_type=MimeTypeMap.getSingleton().getMimeTypeFromExtension(ft);
    				}
//    				Log.v("","mt="+mime_type);
    				if (mime_type!=null && mime_type.startsWith("video/")) {
            			fli.file_name=tfl[i].getName();
            			fli.file_size=MiscUtil.convertFileSize(tfl[i].length());
            			mFileList.add(fli);	
            			mLog.addDebugMsg(1,"I","File added name="+fli.file_name+", mime type="+mime_type);
           			}
//    				Log.v("","ft="+ft+", mime="+mime_type);
    			}
    		}
    		Collections.sort(mFileList, new Comparator<FileListItem>(){
				@Override
				public int compare(FileListItem lhs, FileListItem rhs) {
					return lhs.file_name.compareToIgnoreCase(rhs.file_name);
				}
    		});
    	}

//    	Log.v("","fp="+s_fp);
		for (int i=0;i<mFileList.size();i++) {
			if (s_fn.equals(mFileList.get(i).file_name)) {
				mCurrentSelectedPos=i;
				break;
			}
		}

	};
	
	public void setMainViewListener() {

        mSurfaceHolder=mSurfaceView.getHolder();
//        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
//        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback(){
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				mLog.addDebugMsg(1,"I","surfaceCreated entered, Player status="+getVideoPlayerStatus());
				if (mIsVideoReadyToBePlayed) {
					mIsVideoReadyToBePlayed=false;
					mIbPlay.performClick();
				} else {
					if (isVideoPlayerStatusPausing()) {
						mMediaPlayer.setDisplay(mSurfaceHolder);
						mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition());
					}
				}
			};

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				mLog.addDebugMsg(1,"I","surfaceChanged entered, width="+width+", height="+height+
						", Player status="+getVideoPlayerStatus());
			};

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				mLog.addDebugMsg(1,"I","surfaceDestroyed entered, Player status="+getVideoPlayerStatus());
				if (isVideoPlayerStatusPlaying()) {
					mMediaPlayer.pause();
					setVideoPlayerStatus(VIDEO_STATUS_PAUSING);
				}
			};
        });

		if ((mCurrentSelectedPos+1)<mFileList.size()) {
			mIbNextFile.setEnabled(true);
			mIbNextFile.setImageResource(R.drawable.next_file_enabled);
		} else {
			mIbNextFile.setEnabled(false);
			mIbNextFile.setImageResource(R.drawable.next_file_disabled);
		}
		if (mCurrentSelectedPos>0) {
			mIbPrevFile.setEnabled(true);
			mIbPrevFile.setImageResource(R.drawable.prev_file_enabled);
		} else {
			mIbPrevFile.setEnabled(false);
			mIbPrevFile.setImageResource(R.drawable.prev_file_disabled);
		}

		mIbDeleteFile.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						stopVideoPlayer();
						stopMediaPlayer();
						setVideoPlayerStatus(VIDEO_STATUS_STOPPED);
						setPlayBtnEnabled(true);
						mSbPlayPosition.setProgress(0);
						mSbPlayPosition.setEnabled(false);
						
						FileListItem fli=mFileList.get(mCurrentSelectedPos);
						mFileList.remove(mCurrentSelectedPos);
						File lf=new File(mVideoFolder+fli.file_name);
						lf.delete();
						deleteMediaStoreItem(mVideoFolder+fli.file_name);
//						Log.v("","size="+mFileList.size()+", pos="+mCurrentSelectedPos);
						if (mFileList.size()>0) {
							if ((mCurrentSelectedPos+1)>mFileList.size()) mCurrentSelectedPos--;
//							showVideoThumnail(mCurrentSelectedPos);
//							mTvEndPosition.setText("");
							
							mSbPlayPosition.setProgress(0);
							mSbPlayPosition.setEnabled(true);
							prepareVideo(true, mFileList.get(mCurrentSelectedPos).file_name);
							setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
							setPlayBtnPause(true);
							
//							setNextPrevBtnStatus();
							setNextPrevBtnDisabled();

						} else {
							finish();
						}
						if (mGp.housekeepThumnailCache()) mGp.saveThumnailCacheList();
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
				});
				mCommonDlg.showCommonDialog(true, "W", String.format(
						mContext.getString(R.string.msgs_player_delete_file_confirm),
						mFileList.get(mCurrentSelectedPos).file_name), "", ntfy);
			}
		});

		mIbShare.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
			    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
			    Uri uri = Uri.parse(mVideoFolder+mFileList.get(mCurrentSelectedPos).file_name);
			     
			    sharingIntent.setType("video/mp4");
			    sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
			    startActivity(Intent.createChooser(sharingIntent,
			    		mContext.getString(R.string.msgs_main_ccmenu_share_title)));
			}
		});

		setCaptureBtnEnabled(true);
		mIbCapture.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
//				if (mIsVideoCapturedEnabled) {
//					mIsVideoCapturedEnabled=false;
//				}
				setCaptureBtnEnabled(false);
				final int c_pos=mSbPlayPosition.getProgress();
				Thread th=new Thread() {
					@Override
					public void run() {
//						long b_time=System.currentTimeMillis();
						MediaMetadataRetriever mr=new MediaMetadataRetriever();
						mr.setDataSource(mVideoFolder+mFileList.get(mCurrentSelectedPos).file_name);
//						Log.v("","prepare ="+(System.currentTimeMillis()-b_time));
//						Log.v("","sb="+mSbPlayPosition.getProgress()+", mp="+mMediaPlayer.getCurrentPosition());
						Bitmap bm=mr.getFrameAtTime(c_pos*1000);
//						Log.v("","bm ="+(System.currentTimeMillis()-b_time));
						putPicture(bm);
						mUiHandler.post(new Runnable(){
							@Override
							public void run() {
								setCaptureBtnEnabled(true);
							}
						});
					}
				};
				th.start();
			}
		});

		setForwardBtnEnabled(true);
		mIbForward.setOnTouchListener(new OnTouchListener(){
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
//				Log.v("","action="+event.getAction());
//				Log.v("","forward Player status="+getVideoPlayerStatus()+
//						", mIsPlayRequiredAfterMoveFrame="+mIsPlayRequiredAfterMoveFrame);
				if (isVideoPlayerStatusPlaying()) {
					mIsPlayRequiredAfterMoveFrame=true;
					pauseVideoPlaying();
				} else {
					if (isVideoPlayerStatusStopped()) {
						mSbPlayPosition.setProgress(0);
						mSbPlayPosition.setEnabled(true);
						prepareVideo(false,mFileList.get(mCurrentSelectedPos).file_name);
						setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
					}
				}
				if (event.getAction()==MotionEvent.ACTION_DOWN) {
					if (mMoveFrameActive) {
						stopMoveFrame();
					}
					mTcMoveFrame.setEnabled();
					startMoveFrame(mTcMoveFrame,"F");
				} else if (event.getAction()==MotionEvent.ACTION_UP) {
					stopMoveFrame();
					if (mIsPlayRequiredAfterMoveFrame) {
						setPlayBtnPause(true);
						resumePlayVideo();
					}
					mIsPlayRequiredAfterMoveFrame=false;
				} else if (event.getAction()==MotionEvent.ACTION_CANCEL) {
					stopMoveFrame();
					if (mIsPlayRequiredAfterMoveFrame) {
						setPlayBtnPause(true);
						resumePlayVideo();
					}
					mIsPlayRequiredAfterMoveFrame=false;
				}
				return false;
			}
		});

		setBackwardBtnEnabled(true);
		mIbBackward.setOnTouchListener(new OnTouchListener(){
			@SuppressLint("ClickableViewAccessibility")
			@Override
			public boolean onTouch(View arg0, MotionEvent event) {
				if (isVideoPlayerStatusPlaying()) {
					mIsPlayRequiredAfterMoveFrame=true;
					pauseVideoPlaying();
				} else {
					if (isVideoPlayerStatusStopped()) {
						mSbPlayPosition.setProgress(0);
						mSbPlayPosition.setEnabled(true);
						prepareVideo(false,mFileList.get(mCurrentSelectedPos).file_name);
						setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
					}
				}
				if (event.getAction()==MotionEvent.ACTION_DOWN) {
					if (mMoveFrameActive) {
						stopMoveFrame();
					}
					mTcMoveFrame.setEnabled();
					startMoveFrame(mTcMoveFrame,"B");
				} else if (event.getAction()==MotionEvent.ACTION_UP) {
					stopMoveFrame();
					if (mIsPlayRequiredAfterMoveFrame) {
						setPlayBtnPause(true);
						resumePlayVideo();
					}
					mIsPlayRequiredAfterMoveFrame=false;
				} else if (event.getAction()==MotionEvent.ACTION_CANCEL) {
					stopMoveFrame();
					if (mIsPlayRequiredAfterMoveFrame) {
						setPlayBtnPause(true);
						resumePlayVideo();
					}
					mIsPlayRequiredAfterMoveFrame=false;
				}
				return false;
			}
		});

		mIbMediaPlayer.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
//				String dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()+"/";
//				File lf=new File(dir);
//				File[] list=lf.listFiles();
//				String fp="";
//				Uri uri=null;
//				if (list!=null && list.length>0) {
//					for (int i=0;i<list.length;i++) {
//						if (list[i].getName().endsWith(".jpg")) {
//							fp=list[i].getPath();
//							
//							uri=getMediaUri(fp);
//							Log.v("","uri="+uri);
//							break;
//						}
//					}
//				}
//				final String REVIEW_ACTION = "com.android.camera.action.REVIEW";
//				if (fp.equals("")) {
//					uri=Uri.parse("file://"+dir);
//				} else {
//					uri=Uri.parse("file://"+fp);
//				}
				Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
//				Intent intent = new Intent(REVIEW_ACTION, uri);
				intent.setType("image/jpeg");
				startActivity(intent);
			}
		});
		
		if (mIsArchiveFolder) {
			mIbArchive.setImageResource(R.drawable.archive_disabled);
			mIbArchive.setEnabled(false);
		}
		mIbArchive.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						stopVideoPlayer();
						stopMediaPlayer();
						setVideoPlayerStatus(VIDEO_STATUS_STOPPED);
						setPlayBtnEnabled(true);
						mSbPlayPosition.setProgress(0);
						mSbPlayPosition.setEnabled(false);
						
						String fp=mGp.videoRecordDir+mFileList.get(mCurrentSelectedPos).file_name;
						String afp=mGp.videoArchiveDir+mFileList.get(mCurrentSelectedPos).file_name;
						File tlf=new File(mGp.videoArchiveDir);
						if (!tlf.exists()) tlf.mkdirs();
						
				    	File lf=new File(fp);
				    	boolean result=lf.renameTo(new File(afp));
				    	if (result) {
		        			mLog.addLogMsg("I", "File was archived. name="+mFileList.get(mCurrentSelectedPos).file_name);
					        deleteMediaStoreItem(fp);
					    	mFileList.remove(mCurrentSelectedPos);
					    	scanMediaStoreFile(afp);
					    	
							if (mFileList.size()>0) {
//								Log.v("","c_pos="+mCurrentSelectedPos+", size="+mFileList.size());
								if ((mCurrentSelectedPos+1)>mFileList.size()) mCurrentSelectedPos--;
//								showVideoThumnail(mCurrentSelectedPos);
								mSbPlayPosition.setProgress(0);
								mSbPlayPosition.setEnabled(true);
								prepareVideo(true, mFileList.get(mCurrentSelectedPos).file_name);
								setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
								setPlayBtnPause(true);
								
//								setNextPrevBtnStatus();
								setNextPrevBtnDisabled();

							} else {
								finish();
							}
							if (mGp.housekeepThumnailCache()) mGp.saveThumnailCacheList();
				    	} else {
				    		mLog.addLogMsg("E", "File can not archived. name="+mFileList.get(mCurrentSelectedPos).file_name);
							mCommonDlg.showCommonDialog(false, "E", 
									  mContext.getString(R.string.msgs_main_ccmenu_file_archive_error), 
									  mFileList.get(mCurrentSelectedPos).file_name, null);
				    	}
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
				});
				mCommonDlg.showCommonDialog(true, "W", 
						  mContext.getString(R.string.msgs_main_ccmenu_file_archive_file_confirm), 
						  mFileList.get(mCurrentSelectedPos).file_name, ntfy);
			}
		});

		mIbPrevFile.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				stopVideoPlayer();
				stopMediaPlayer();
				mSbPlayPosition.setProgress(0);
				mSbPlayPosition.setEnabled(true);
				mCurrentSelectedPos--;
				prepareVideo(true, mFileList.get(mCurrentSelectedPos).file_name);
//				setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
				setPlayBtnPause(true);
				
//				setNextPrevBtnStatus();
				setNextPrevBtnDisabled();
			}
		});
		
		mIbNextFile.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				stopVideoPlayer();
				stopMediaPlayer();
				mSbPlayPosition.setProgress(0);
				mSbPlayPosition.setEnabled(true);
				mCurrentSelectedPos++;
				prepareVideo(true, mFileList.get(mCurrentSelectedPos).file_name);
//				setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
				setPlayBtnPause(true);
//				setNextPrevBtnStatus();
				setNextPrevBtnDisabled();
			}
		});
		
		mSbPlayPosition.setProgress(0);
		mSbPlayPosition.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar sb, int progress, boolean byUser) {
//				Log.v("","seekTo="+progress+", max="+mSbPlayPosition.getMax()+", arg2="+arg2);
				if (byUser) {
					mMediaPlayer.seekTo(progress);
					mTvPlayPosition.setText(getTimePosition(progress));
					setMoveFrameBtnEnabled(progress, sb.getMax());
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
//				Log.v("","onStartTrackingTouch");
				mMediaPlayer.setVolume(0f, 0f);
			}
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
//				Log.v("","onStopTrackingTouch");
				mMediaPlayer.setVolume(1.0f, 1.0f);
			}
		});

		mIbPlay.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
//				Log.v("","isVideoPlayerStatusPlaying()="+isVideoPlayerStatusPlaying()+", isVideoPlayerStatusPausing()="+isVideoPlayerStatusPausing());
				if (isVideoPlayerStatusPlaying()) {
					pauseVideoPlaying();
				} else {
					//Start
					setPlayBtnPause(true);
					if (isVideoPlayerStatusStopped()) {
						mSbPlayPosition.setProgress(0);
						mSbPlayPosition.setEnabled(true);
						prepareVideo(true, mFileList.get(mCurrentSelectedPos).file_name);
						setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
					} else {
						resumePlayVideo();
					}
				}
			}
		});
	};

	private void resumePlayVideo() {
//		Log.v("","resumePlayvideo");
		int c_pos=mMediaPlayer.getCurrentPosition();
		int c_max=mMediaPlayer.getDuration();
		if (c_pos>=c_max) {
			mMediaPlayer.seekTo(0);
			mSbPlayPosition.setProgress(0);
		}
		mSbPlayPosition.setEnabled(true);
//		mMediaPlayer.start();
		setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
		mTcPlayer.setEnabled();
		startVideoThread();
	};
	
	private Thread mThMoveFrame=null;
	private boolean mMoveFrameActive=false;
	private ThreadCtrl mTcMoveFrame=new ThreadCtrl();
	private void waitMoveFrameThread() {
		if (mThMoveFrame!=null) {
			try {
				mThMoveFrame.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};
	
	private void stopMoveFrame() {
		mTcMoveFrame.setDisabled();
		synchronized(mTcMoveFrame) {
			mTcMoveFrame.notify();
		}
		waitMoveFrameThread();
	};
		
	private final int mStepIntervalTime=3000;
	private void startMoveFrame(final ThreadCtrl tc, final String direction) {
		final Handler hndl=new Handler();
		mMoveFrameActive=true;
		mThMoveFrame=new Thread(){
			@Override
			public void run() {
				long b_wt=0, e_wt=0;
				long wait_time=150;
				while(tc.isEnabled()) {
					hndl.post(new Runnable(){
						@Override
						public void run() {
							moveFrame(direction);
						}
					});
					synchronized(tc) {
						try {
							b_wt=System.currentTimeMillis();
							tc.wait();
							e_wt=wait_time-(System.currentTimeMillis()-b_wt);
							if (e_wt>10) tc.wait(e_wt);
//							tc.wait(300);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				mMoveFrameActive=false;
				tc.setEnabled();
			}
		};
		SystemClock.sleep(100);
		mThMoveFrame.start();
	};
	
	private void setMoveFrameBtnEnabled(int pos, int max) {
//		Log.v("","setMoveFrameBtnEnabled entered, pos="+pos+", max="+max);
		if (pos>0) {
			setBackwardBtnEnabled(true);
		} else if (pos<=0) {
			setBackwardBtnEnabled(false);
			stopMoveFrame();
			setVideoPlayerStatus(VIDEO_STATUS_PAUSING);
			mIsPlayRequiredAfterMoveFrame=false;
		} 
		if (pos>=max) {
			setForwardBtnEnabled(false);
			stopMoveFrame();
			setVideoPlayerStatus(VIDEO_STATUS_PAUSING);
			mIsPlayRequiredAfterMoveFrame=false;
//			Log.v("","forward disabled playing="+isVideoPlayerStatusPlaying()+", mIsPlayRequiredAfterMoveFrame="+mIsPlayRequiredAfterMoveFrame);
		} else if (pos<max) {
			setForwardBtnEnabled(true);
		}
	};

	private void moveFrame(String direction) {
//		Log.v("","moveFrame entered");
		int c_pos=mMediaPlayer.getCurrentPosition();
		int c_max=mMediaPlayer.getDuration();
		if (direction.equals("F")) {
			int n_pos=c_pos+mStepIntervalTime;
			if ((c_max-c_pos)<mStepIntervalTime) n_pos=c_max;
			mMediaPlayer.seekTo(n_pos);
			mSbPlayPosition.setProgress(n_pos);
			mTvPlayPosition.setText(getTimePosition(n_pos));
			setMoveFrameBtnEnabled(n_pos,c_max);
//			Log.v("","c_pos="+c_pos+", n_pos="+n_pos+", c_max="+c_max);
		} else {
			int n_pos=0;
			if (c_pos>mStepIntervalTime) n_pos=c_pos-mStepIntervalTime;
			mMediaPlayer.seekTo(n_pos);
			mSbPlayPosition.setProgress(n_pos);
			mTvPlayPosition.setText(getTimePosition(n_pos));
			setMoveFrameBtnEnabled(n_pos,c_max);
		}

	}

//    private Uri getMediaUri(String fp) {
//    	Uri image_uri=null;
//		Uri base_uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//		Uri query = base_uri.buildUpon().appendQueryParameter("limit", "1").build();
//		String [] projection = new String[] {ImageColumns.DATA, ImageColumns._ID};
//		String selection = ImageColumns.MIME_TYPE + "='image/jpeg'";
//		String order = ImageColumns.DATA;
//		Cursor ci = null;
//		try {
//			ci = getContentResolver().query(base_uri, projection, selection, null, order);
//			if( ci != null) {
//		        while( ci.moveToNext() ){
//		        	String file_path=ci.getString(ci.getColumnIndex( MediaStore.Images.Media.DATA));
//		        	Log.v("","data="+file_path);
//		        	if (file_path.equals(fp)) {
//						long id = ci.getLong(ci.getColumnIndex( MediaStore.Images.Media._ID));
//						image_uri = ContentUris.withAppendedId(base_uri, id);
//						image_uri = base_uri;
//						Log.v("","fp="+fp+", id="+id);
//						break;
//		        	}
//		        }
//		        ci.close();
//			}
//		}
//		finally {
//			if( ci != null ) {
//				ci.close();
//			}
//		}
//		return image_uri;
//    };
	
	private void pauseVideoPlaying() {
		stopVideoPlayer();
		setPlayBtnEnabled(true);
//		mSbPlayPosition.setEnabled(false);
		setVideoPlayerStatus(VIDEO_STATUS_PAUSING);
//		setCaptureBtnEnabled(true);
//		setForwardBtnEnabled(true);
//		setBackwardBtnEnabled(true);
	};
	
	private void setCaptureBtnEnabled(boolean p) {
		mIbCapture.setEnabled(p);
		if (p) mIbCapture.setImageResource(R.drawable.capture_enabled);
		else mIbCapture.setImageResource(R.drawable.capture_disabled);
	};
	
	private void setForwardBtnEnabled(boolean p) {
		mIbForward.setEnabled(p);
		if (p) mIbForward.setImageResource(R.drawable.player_fast_forward_enabled);
		else mIbForward.setImageResource(R.drawable.player_fast_forward_disabled);
	};

	private void setBackwardBtnEnabled(boolean p) {
		mIbBackward.setEnabled(p);
		if (p) mIbBackward.setImageResource(R.drawable.player_fast_backward_enabled);
		else mIbBackward.setImageResource(R.drawable.player_fast_backward_disabled);
	};

	private void putPicture(Bitmap bm) {
		String dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()+"/";
		File l_dir=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		if (!l_dir.exists()) l_dir.mkdirs();
		String ftime=StringUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis())
				.replaceAll("/","-").replaceAll(":","").replaceAll(" ", "_");
		String fn="dr_pic_"+ftime+".jpg";
		final String pfp=dir+fn;
		try {
			FileOutputStream fos=new FileOutputStream(pfp);
			BufferedOutputStream bos=new BufferedOutputStream(fos,4096*256);
			bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
			bos.flush();
			bos.close();
			mUiHandler.post(new Runnable(){
				@Override
				public void run() {
					Toast toast = Toast.makeText(mContext, 
							mContext.getString(R.string.msgs_player_picture_captured)+pfp, Toast.LENGTH_SHORT);
					toast.show();
				}
			});
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		scanMediaStoreFile(dir+fn);
	};
	
	private Bitmap mThumnailBitmap=null;
	@SuppressWarnings("unused")
	private void showVideoThumnail(int pos) {
		mThumnailView.setVisibility(SurfaceView.VISIBLE);
		if (mIsArchiveFolder) mTvTitle.setText(mContext.getString(R.string.msgs_main_folder_type_archive)+" "+mFileList.get(pos).file_name);
		else mTvTitle.setText(mContext.getString(R.string.msgs_main_folder_type_record)+" "+mFileList.get(pos).file_name);
		mSbPlayPosition.setProgress(0);
		mTvPlayPosition.setText("00:00");
		setNextPrevBtnStatus();

		mThumnailBitmap=ThumbnailUtils.createVideoThumbnail(mVideoFolder+mFileList.get(pos).file_name, 
				MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);

		Canvas canvas=mThumnailView.getHolder().lockCanvas();
		if (mThumnailBitmap!=null) {
			int surfaceView_Width = mThumnailView.getWidth();
		    int surfaceView_Height = mThumnailView.getHeight();
		    float video_Width = mThumnailBitmap.getWidth();
		    float video_Height = mThumnailBitmap.getHeight();
		    float ratio_width = surfaceView_Width/video_Width;
		    float ratio_height = surfaceView_Height/video_Height;
		    float aspectratio = video_Width/video_Height;
		    android.view.ViewGroup.LayoutParams layoutParams = mThumnailView.getLayoutParams();
		    if (ratio_width > ratio_height){
			    layoutParams.width = (int) (surfaceView_Height * aspectratio);
			    layoutParams.height = surfaceView_Height;
		    }else{
		    	layoutParams.width = surfaceView_Width;
		    	layoutParams.height = (int) (surfaceView_Width / aspectratio);
		    }
		    mThumnailView.setLayoutParams(layoutParams);
		    
			Rect f_rect=new Rect(0,0,mThumnailBitmap.getWidth(),mThumnailBitmap.getHeight());
			Rect t_rect=new Rect(0,0,mThumnailView.getWidth()-1,mThumnailView.getHeight()-1);
//			Log.v("","To width="+mThumnailView.getWidth()+", height="+mThumnailView.getHeight());
//			Log.v("","From width="+f_rect.right+", height="+f_rect.bottom);
			Paint paint=new Paint();
			canvas.drawBitmap(mThumnailBitmap, f_rect, t_rect, paint);
		} else {
			canvas.drawColor(Color.BLACK);
		}
		mThumnailView.getHolder().unlockCanvasAndPost(canvas);

		mSurfaceView.setVisibility(SurfaceView.INVISIBLE);
	};
	
	private void stopVideoPlayer() {
		if (isVideoPlayerStatusPlaying()) {
			mMediaPlayer.pause();
			setVideoPlayerStatus(VIDEO_STATUS_PAUSING);
			stopVideoThread();
		} 
	};
	
	private void setNextPrevBtnDisabled() {
		mIbNextFile.setEnabled(false);
		mIbNextFile.setImageResource(R.drawable.next_file_disabled);

		mIbPrevFile.setEnabled(false);
		mIbPrevFile.setImageResource(R.drawable.prev_file_disabled);
	};
	
	private void setPlayBtnEnabled(boolean p) {
//		Log.v("","play="+p);
		if(p) {
			mIbPlay.setEnabled(true);
			mIbPlay.setImageResource(R.drawable.player_play_enabled);
		} else {
			mIbPlay.setEnabled(false);
			mIbPlay.setImageResource(R.drawable.player_play_disabled);
		}
	};

	private void setPlayBtnPause(boolean p) {
		if(p) {
			mIbPlay.setEnabled(true);
			mIbPlay.setImageResource(R.drawable.player_play_pause);
		} else {
			mIbPlay.setEnabled(false);
			mIbPlay.setImageResource(R.drawable.player_play_pause);
		}
	};

	private void setNextPrevBtnStatus() {
		if ((mCurrentSelectedPos+1)<mFileList.size()) {
			mIbNextFile.setEnabled(true);
			mIbNextFile.setImageResource(R.drawable.next_file_enabled);
		} else {
			mIbNextFile.setEnabled(false);
			mIbNextFile.setImageResource(R.drawable.next_file_disabled);
		}
		if (mCurrentSelectedPos>0) {
			mIbPrevFile.setEnabled(true);
			mIbPrevFile.setImageResource(R.drawable.prev_file_enabled);
		} else {
			mIbPrevFile.setEnabled(false);
			mIbPrevFile.setImageResource(R.drawable.prev_file_disabled);
		}
	};
	
	private MediaPlayer mMediaPlayer=null;
	private ThreadCtrl mTcPlayer=null;
	private void prepareVideo(final boolean start, final String fp) {
		mLog.addDebugMsg(1,"I","prepareVideo entered, fp="+fp+", Player status="+getVideoPlayerStatus());
		if (isVideoPlayerStatusPlaying()) return;
		mTcPlayer.setEnabled();
		mSurfaceView.setVisibility(SurfaceView.VISIBLE);
		mThumnailView.setVisibility(SurfaceView.INVISIBLE);
		setCaptureBtnEnabled(true);
		try {
			if (mIsArchiveFolder) mTvTitle.setText(mContext.getString(R.string.msgs_main_folder_type_archive)+" "+fp);
			else mTvTitle.setText(mContext.getString(R.string.msgs_main_folder_type_record)+" "+fp);
			  
			mMediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener(){
				@Override
				public void onBufferingUpdate(MediaPlayer mp, int percent) {
					mLog.addDebugMsg(1,"I", "onBufferingUpdate percent:" + percent);
				}
			});
			mMediaPlayer.setOnErrorListener(new OnErrorListener(){
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					mLog.addDebugMsg(1,"I","onErrorListener called");
					stopVideoThread();
					stopMediaPlayer();
					return true;
				}
			});
			mMediaPlayer.setOnCompletionListener(new OnCompletionListener(){
				@Override
				public void onCompletion(MediaPlayer mp) {
					mLog.addDebugMsg(1,"I","onCompletion called");
					setVideoPlayerStatus(VIDEO_STATUS_PAUSING);
					mMediaPlayer.pause();
					stopVideoThread();
//					if (!mApplicationTerminated) {
//						setPlayBtnEnabled(true);
//					}
				}
			});
			mMediaPlayer.setOnPreparedListener(new OnPreparedListener(){
				@Override
				public void onPrepared(MediaPlayer mp) {
					mLog.addDebugMsg(1,"I","onPrepared called");
					MediaMetadataRetriever mr=new MediaMetadataRetriever();
					mr.setDataSource(mVideoFolder+fp);
					String br_str=mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
					BigDecimal br=new BigDecimal("0.00");
					if (br_str!=null)  {
						BigDecimal br_a=new BigDecimal(br_str);
						BigDecimal br_b=new BigDecimal(1000*1000);
						br=br_a.divide(br_b,0,BigDecimal.ROUND_HALF_UP);
//						Log.v("","br_str="+br_str+", br_a="+br_a+", br_b="+br_b);
//						BigDecimal dfs1 = new BigDecimal(fs);
//					    BigDecimal dfs2 = new BigDecimal(1024*1024*1024);
//					    BigDecimal dfs3 = new BigDecimal("0.00");
//					    dfs3=dfs1.divide(dfs2,1, BigDecimal.ROUND_HALF_UP);
					}
					
					String wh=" "+mMediaPlayer.getVideoWidth()+" x "+mMediaPlayer.getVideoHeight()+" "+br+"MBPS";
					if (mIsArchiveFolder) mTvTitle.setText(mContext.getString(R.string.msgs_main_folder_type_archive)+" "+fp +" "+wh);
					else mTvTitle.setText(mContext.getString(R.string.msgs_main_folder_type_record)+" "+fp +" "+wh);
					
					mSbPlayPosition.setMax(mMediaPlayer.getDuration());
					mTvEndPosition.setText(getTimePosition(mMediaPlayer.getDuration()));
					if (mGp.settingsVideoPlaybackKeepAspectRatio) {
						int surfaceView_Width = mSurfaceView.getWidth();
					    int surfaceView_Height = mSurfaceView.getHeight();
					    float video_Width = mMediaPlayer.getVideoWidth();
					    float video_Height = mMediaPlayer.getVideoHeight();
					    float ratio_width = surfaceView_Width/video_Width;
					    float ratio_height = surfaceView_Height/video_Height;
					    float aspectratio = video_Width/video_Height;
					    android.view.ViewGroup.LayoutParams layoutParams = mSurfaceView.getLayoutParams();
					    if (ratio_width > ratio_height){
						    layoutParams.width = (int) (surfaceView_Height * aspectratio);
						    layoutParams.height = surfaceView_Height;
					    }else{
					    	layoutParams.width = surfaceView_Width;
					    	layoutParams.height = (int) (surfaceView_Width / aspectratio);
					    }
//					    Log.v("","lp_w="+layoutParams.width+", lp_h="+layoutParams.height+", ratio="+aspectratio);
					    mSurfaceView.setLayoutParams(layoutParams);
//					    mThumnailView.setLayoutParams(layoutParams);
					} else {
//					    mMediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING);
					    mMediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
					}

					setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
					startVideoThread();
					setNextPrevBtnStatus();
				}
			});
			mMediaPlayer.setOnVideoSizeChangedListener(new OnVideoSizeChangedListener(){
				@Override
				public void onVideoSizeChanged(MediaPlayer mp, int video_width, int video_height) {
					mLog.addDebugMsg(1,"I","onVideoSizeChanged called, width="+video_width+", height="+video_height);
				}
			});
			
			mMediaPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener(){
				@Override
				public void onSeekComplete(MediaPlayer mp) {
//					Log.v("","seek completed, pos="+mp.getCurrentPosition());
					synchronized(mTcMoveFrame) {
						mTcMoveFrame.notify();
					}
				}
				
			});

//			setVideoPlayerStatus(VIDEO_STATUS_PLAYING);
			mMediaPlayer.setDataSource(mVideoFolder+fp);
			mMediaPlayer.setDisplay(mSurfaceHolder);
//			mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
			mMediaPlayer.prepareAsync();
			
		} catch (IllegalArgumentException e) {
			mCommonDlg.showCommonDialog(false, "E", "IllegalArgumentException","File="+fp, null);
			mUiHandler.post(new Runnable(){
				@Override
				public void run() {
					mSurfaceView.setVisibility(SurfaceView.INVISIBLE);
					setNextPrevBtnStatus();
					setPlayBtnEnabled(false);
					setCaptureBtnEnabled(false);
					setForwardBtnEnabled(false);
					setBackwardBtnEnabled(false);
				}
			});
			e.printStackTrace();
		} catch (SecurityException e) {
			mCommonDlg.showCommonDialog(false, "E", "SecurityException", "File="+fp, null);
			mUiHandler.post(new Runnable(){
				@Override
				public void run() {
					mSurfaceView.setVisibility(SurfaceView.INVISIBLE);
					setNextPrevBtnStatus();
					setPlayBtnEnabled(false);
					setCaptureBtnEnabled(false);
					setForwardBtnEnabled(false);
					setBackwardBtnEnabled(false);
				}
			});
			e.printStackTrace();
		} catch (IllegalStateException e) {
			mCommonDlg.showCommonDialog(false, "E", "IllegalStateException","File="+fp, null);
			mUiHandler.post(new Runnable(){
				@Override
				public void run() {
					mSurfaceView.setVisibility(SurfaceView.INVISIBLE);
					setNextPrevBtnStatus();
					setPlayBtnEnabled(false);
					setCaptureBtnEnabled(false);
					setForwardBtnEnabled(false);
					setBackwardBtnEnabled(false);
				}
			});
			e.printStackTrace();
		} catch (IOException e) {
			mCommonDlg.showCommonDialog(false, "E", "IOException","File="+fp, null);
			mUiHandler.post(new Runnable(){
				@Override
				public void run() {
					mSurfaceView.setVisibility(SurfaceView.INVISIBLE);
					setNextPrevBtnStatus();
					setPlayBtnEnabled(false);
					setCaptureBtnEnabled(false);
					setForwardBtnEnabled(false);
					setBackwardBtnEnabled(false);
				}
			});
			e.printStackTrace();
		}
	};
	
	private String getTimePosition(int cp) {
		int mm=cp/1000/60;
		int ss=(cp-(mm*1000*60))/1000;
		return String.format("%02d",mm)+":"+String.format("%02d",ss);
	};

	private Thread mPlayerThread=null;
	
	private void stopVideoThread() {
		mTcPlayer.setDisabled();
		synchronized(mTcPlayer) {
			mTcPlayer.notify();
		}
		try {
			if (mPlayerThread!=null) mPlayerThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	};
	
	private void startVideoThread() {
//		Thread.dumpStack();
//		setMoveFrameBtnEnabled(mMediaPlayer.getCurrentPosition(), mMediaPlayer.getDuration());
		mPlayerThread=new Thread() {
			@Override
			public void run() {
				mMediaPlayer.start();
				final int interval=100;
				while (isVideoPlayerStatusPlaying()) {
					try {
						if (isVideoPlayerStatusPlaying()) {
							mUiHandler.post(new Runnable(){
								@Override
								public void run() {
									if (isVideoPlayerStatusPlaying()) {
										int cp=mMediaPlayer.getCurrentPosition();
//										int cp=mSbPlayPosition.getProgress();
										mSbPlayPosition.setProgress(cp+interval);
										mTvPlayPosition.setText(getTimePosition(cp+interval));
//										Log.v("","cp="+cp);
										setMoveFrameBtnEnabled(cp+interval, mMediaPlayer.getDuration());
									}
								}
							});
						}
						synchronized(mTcPlayer) {
							mTcPlayer.wait(interval);
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (!mTcPlayer.isEnabled()) {
						mLog.addDebugMsg(1,"I", "startVideoThread cancelled");
						break;
					} else {
//						mSbPlayPosition.setProgress(mSbPlayPosition.getMax());
					}
				}
				mLog.addDebugMsg(1,"I", "startVideoThread expired");
				mUiHandler.post(new Runnable(){
					@Override
					public void run() {
						setPlayBtnEnabled(true);
					}
				});
			}
		};
		mPlayerThread.setName("Player");
		mPlayerThread.start();		
	};

	private void stopMediaPlayer() {
		setVideoPlayerStatus(VIDEO_STATUS_STOPPED);
		try {
			mMediaPlayer.stop();
			mMediaPlayer.reset();
//			mMediaPlayer.release();
		} catch(IllegalStateException e) {
		}
	};

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

	private void setVideoPlayerStatus(int p) {
		mVideoPlayerStatus=p;
	};
	private int getVideoPlayerStatus() {
		return mVideoPlayerStatus;
	};
	private boolean isVideoPlayerStatusStopped() {
		return mVideoPlayerStatus == VIDEO_STATUS_STOPPED;
	};
	private boolean isVideoPlayerStatusPlaying() {
		return mVideoPlayerStatus == VIDEO_STATUS_PLAYING;
	};
	private boolean isVideoPlayerStatusPausing() {
		return mVideoPlayerStatus == VIDEO_STATUS_PAUSING;
	};

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
}

