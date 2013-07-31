package org.elwin.mediascanner;

import java.io.File;
import java.util.Arrays;

import android.app.Activity;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MediaScannerActivity extends Activity {

	private static final String TAG = "MediaScannerActivity";
	private TextView mStatus;
	private EditText mPathView;
	private TextView mFailedView;
	private Button mScanBtn;
	private int mTotalSize;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
//		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		
		mPathView = (EditText) findViewById(R.id.path_view);
		mPathView.setText(Environment.getExternalStorageDirectory() + "/DCIM/Camera");
		mStatus = (TextView) findViewById(R.id.status);
		mFailedView = (TextView) findViewById(R.id.failed);
		mFailedView.append("Below are failed items: \n");
		mScanBtn = (Button) findViewById(R.id.scan_btn);
		mScanBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startWork();
			}
		});
	}

//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.main, menu);
//		return true;
//	}
	
	private void startWork() {
		String filePath = mPathView.getText().toString().trim();
        File file=new File(filePath);
        final File[] files = file.listFiles();
        mTotalSize = files.length;
        Log.d(TAG, "in startWork, total size: " + mTotalSize);
        setTitle("total: " + mTotalSize);
        
        String[] filePaths = new String[files.length];
		for (int i = 0; i < files.length; ++i) {
			Log.d(TAG, "in startWork, file" + (i+1) + ": " + files[i].getPath());
			filePaths[i] = files[i].getPath();
		}
		
		mScanBtn.setEnabled(false);
		setProgressBarIndeterminate(true);
        scanByService(filePaths);
	}

	private void scanByService(final String[] paths) {
		if (paths == null || paths.length == 0) {
			Log.e(TAG, "in scanByService, paths have nothing");
			return;
		}
		
		String[] mimeTypes = new String[paths.length];
		Arrays.fill(mimeTypes, "image/jpeg");
		
		MediaScannerConnection.scanFile(this, paths, /*mimeTypes*/null, new MediaScannerConnection.OnScanCompletedListener() {
			@Override
			public void onScanCompleted(String path, Uri uri) {
				++scannedCount;
				Log.d(TAG, "in onScanCompleted: path=" + path + " uri=" + uri + " scannedCount=" + scannedCount);
				mHandler.obtainMessage(MSG_SCANNED_COMPLETED, path).sendToTarget();
				if (uri == null) {
					mHandler.obtainMessage(MSG_SCANNED_FAILED, path).sendToTarget();;
				}

				if (path.equals(paths[paths.length-1]) && scannedCount != mTotalSize) {	// finished: for the bug, some item won't call this method
					Log.w(TAG, "in onScanCompleted: some item have not been scanned");
					scannedCount = mTotalSize;
					mHandler.obtainMessage(MSG_SCANNED_COMPLETED, path).sendToTarget();
				}
			}
		});
	}
	
	
	private int scannedCount;
	private static final int MSG_SCANNED_COMPLETED = 1;
	private static final int MSG_SCANNED_FAILED = 2;
	private Handler mHandler = new Handler(new  Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SCANNED_COMPLETED:
				mStatus.setText("scanning(" + scannedCount + "): " + msg.obj);
				setProgressBarIndeterminate(false);
				setProgress( (int) (scannedCount/((float)mTotalSize) * 10000) );
				
				if (scannedCount == mTotalSize) {	//finished
					mScanBtn.setEnabled(true);
					setProgressBarVisibility(false);
					setTitle("total: " + mTotalSize + ". [scan finished]");
				}
				break;
			case MSG_SCANNED_FAILED:
				mFailedView.append(msg.obj + "\n");
				break;
			}
			
			return false;
		}
	});
}
