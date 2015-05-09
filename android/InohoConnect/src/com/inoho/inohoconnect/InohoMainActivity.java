package com.inoho.inohoconnect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

public class InohoMainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.e("inohoMain", "Starting Activity");
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_inoho_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}		
		
		//broadcast receiver 
			final IntentFilter filters = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
			//filters.addAction("android.net.conn.CONNECTIVITY_CHANGE");
			//filters.addAction("android.net.wifi.STATE_CHANGE");
			
			m_inohoCnctnListner = new InohoConnectionChangeListner(this.getApplicationContext());
			this.getApplicationContext().registerReceiver(m_inohoCnctnListner, filters);		
		//
		
		//create instance of connection Manager
		m_conMgr = new InohoConnectionManager(this.getApplicationContext());
		clearLogCatLogs();
		//intialize 
		getHomeLinkAndInitializeWebView();
	}

	
	public void onRetryClickConnect(View v) {
		getHomeLinkAndInitializeWebView();
	}
	
	private void clearLogCatLogs() {
		try {
			Process p1 = Runtime.getRuntime().exec("logcat -c");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void getHomeLinkIntializeWebViewFull() {
		m_handler = new Handler();
		Log.e("inohoMain", "inside getHomeLinkIntializeWebViewFull");
		new Thread(new Runnable() {			
			@Override
			public void run() {
				String linkToConnect = m_conMgr.getHomeLink(false);			  
				if(!linkToConnect.equalsIgnoreCase(m_linkToLoad)) {
					m_linkToLoad = linkToConnect;
					m_handler.postDelayed(new Runnable() {
						@Override
						public void run() {
						  InohoMainActivity.this.inititializeWebView();						  
						}
					}, 500);
				}
			}
		}).start();
	}
	
	private void getHomeLinkAndInitializeWebView() {
		//let's try quick connect first and then full
		m_handler = new Handler();
		Log.e("inohoMain", "inside getHomeLinkAndInitializeWebView");
		new Thread(new Runnable() {			
			@Override
			public void run() {
				m_linkToLoad = m_conMgr.getHomeLink(true);			  
				  
				m_handler.postDelayed(new Runnable() {
					  @Override
					  public void run() {
						  InohoMainActivity.this.inititializeWebView();
						  getHomeLinkIntializeWebViewFull();
					  }
					}, 500);
			}
		}).start();
	}
	
	@Override
	public void onBackPressed() {
	  //eat it up :P
	}
	
	@Override
    protected void onDestroy() {
        super.onDestroy();
        if(m_inohoCnctnListner != null) {
        	this.getApplicationContext().unregisterReceiver(m_inohoCnctnListner);
        	m_inohoCnctnListner = null;
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.inoho_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		InohoApplication.activityResumed();
		if(InohoApplication.doesUINeedsRefresh()) {
			InohoApplication.setUINeedsRefresh(false);
			
			RelativeLayout fmflash = (RelativeLayout) findViewById(R.id.flashScreen);
			fmflash.setVisibility(View.VISIBLE);
			 
			FrameLayout fmWebView = (FrameLayout) findViewById(R.id.webViewScreen);
			fmWebView.setVisibility(View.INVISIBLE);
			
			getHomeLinkAndInitializeWebView();
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		InohoApplication.activityPaused();
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_inoho_main,
					container, false);
			return rootView;
		}
	}
	
	public void inititializeWebView() {
		Log.e("inohoMain", "inside inititializeWebView");
		m_retry = 0;
		//initialize view
    	mWebView = (WebView) findViewById(R.id.web_view);    	
    	mWebView.getSettings().setJavaScriptEnabled(true);        
    	
    	mWebView.setOnLongClickListener(new OnLongClickListener() {
			
			@Override
			public boolean onLongClick(View v) {
				try {
					StringBuilder logToSave;
					Process p1 = Runtime.getRuntime().exec("logcat -d");
					BufferedReader bufferedReader = new BufferedReader(
						    new InputStreamReader(p1.getInputStream()));

						    logToSave=new StringBuilder();
						    String line;
						    while ((line = bufferedReader.readLine()) != null) {
						      logToSave.append(line + "\n");
						    }
						    
						    final String logString = new String(logToSave.toString());

						    //create text file in SDCard
						    File sdCard = Environment.getExternalStorageDirectory();
						    File dir = new File (sdCard.getAbsolutePath() + "/inohoLogs");
						    dir.mkdirs();
						    File file = new File(dir, "inohoLog.txt");

						    try {  
						     //to write logcat in text file
						     FileOutputStream fOut = new FileOutputStream(file);
						     OutputStreamWriter osw = new OutputStreamWriter(fOut); 

						           // Write the string to the file
						           osw.write(logString);            
						           osw.flush();
						           osw.close();
						    } catch (FileNotFoundException e) {
						     e.printStackTrace();
						    } catch (IOException e) {
						     e.printStackTrace();
						    }
						    
						    Intent emailIntent = new Intent(Intent.ACTION_SEND);
						    emailIntent.setType("text/plain");
						    emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] {"octocatind@gmail.com"});
						    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Inoho Logs");
						    emailIntent.putExtra(Intent.EXTRA_TEXT, "Mr. Developer! Please find the attached logs.");
						    
						    Uri uri = Uri.fromFile(file);
						    emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
						    startActivity(Intent.createChooser(emailIntent, "Choose an Email provider"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				return true;
			}
		});
    	mWebView.setWebViewClient(new WebViewClient() {
    		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
    			Log.e("inohoMain", "inside onReceivedError error is: " + description + " for URL: " + failingUrl);
				
    			if(m_retry < 5) {
					m_retry++;
					Handler h = new Handler();
					h.postDelayed(new Runnable() {
						
						@Override
						public void run() {
							//InohoMainActivity.this.inititializeWebView(m_linkToLoad);
							 if(!m_linkToLoad.isEmpty()) {
								 Log.e("inohoMain", "trying to reload URL: " + m_linkToLoad + " for " + Integer.toString(m_retry) + " times");
								 mWebView.loadUrl(m_linkToLoad);
							 }
						}
					}, 2000);
				} else {					
					//mWebView.loadData(mOfflineInfo,"text/html", "");
					showOfflineView();
				}
			}
			 
			public void onPageFinished (WebView view, String url){
				Log.e("inohoMain", "inside onPageFinished URL is " + url);
				RelativeLayout fmflash = (RelativeLayout) findViewById(R.id.flashScreen);
				fmflash.setVisibility(View.INVISIBLE);
				 
				FrameLayout fmWebView = (FrameLayout) findViewById(R.id.webViewScreen);
				fmWebView.setVisibility(View.VISIBLE);
				
				RelativeLayout fmOfflineWebView = (RelativeLayout) findViewById(R.id.offlineScreen_RL);
				fmOfflineWebView.setVisibility(View.INVISIBLE);
			 }
		 });
		 
		 //all set now load content
		 if(!m_linkToLoad.isEmpty()) {
			 Log.e("inohoMain", "** Found Home at: " + m_linkToLoad);
			 mWebView.loadUrl(m_linkToLoad);
		 } else {
			 //mWebView.loadData(mOfflineInfo,"text/html", "");
			 showOfflineView();
		 }
	}
		
	private void showOfflineView() {
		FrameLayout fmWebView = (FrameLayout) findViewById(R.id.webViewScreen);
		fmWebView.setVisibility(View.INVISIBLE);
		
		RelativeLayout fmflash = (RelativeLayout) findViewById(R.id.flashScreen);
		fmflash.setVisibility(View.INVISIBLE);
		
		RelativeLayout fmOfflineWebView = (RelativeLayout) findViewById(R.id.offlineScreen_RL);
		fmOfflineWebView.setVisibility(View.VISIBLE);
	}
	
	//private data region
	private int m_retry;
	private String m_linkToLoad;

	private WebView mWebView;
	private InohoConnectionChangeListner m_inohoCnctnListner = null;
	private InohoConnectionManager m_conMgr = null;
	private Handler m_handler = null;
	
}
