package com.inoho.inohoconnect;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.inoho.inohoconnect.InohoConnectionManager.CONNECTION_TYPE;

public class InohoMainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
		
		//Initialize webview
		inititializeWebView();
		
		//get home link and load 
		openHomeLinkInQuickMode();
	}
	
	public void onRetryClickConnect(View v) {
		openHomeLinkInQuickMode();
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
			
			openHomeLinkInQuickMode();
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
		m_retry = 0;
		//initialize view
    	mWebView = (WebView) findViewById(R.id.web_view);    	
    	mWebView.getSettings().setJavaScriptEnabled(true);        
    	    	
    	mWebView.setWebViewClient(new WebViewClient() {
    		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				if(m_retry < 5) {
					m_retry++;
					addPerformaceData("error loading trying " + m_retry + " time");
					
					Handler h = new Handler();
					h.postDelayed(new Runnable() {
						
						@Override
						public void run() {
							if(!m_linkToLoad.isEmpty())
								mWebView.loadUrl(m_linkToLoad);
						}
					}, 2000);
				} else {					
					//mWebView.loadData(mOfflineInfo,"text/html", "");
					showOfflineView();
				}
			}
			 
			public void onPageFinished (WebView view, String url){
				addPerformaceData("done with loading " + url);
				
				RelativeLayout fmflash = (RelativeLayout) findViewById(R.id.flashScreen);
				fmflash.setVisibility(View.INVISIBLE);
				 
				FrameLayout fmWebView = (FrameLayout) findViewById(R.id.webViewScreen);
				fmWebView.setVisibility(View.VISIBLE);
				
				RelativeLayout fmOfflineWebView = (RelativeLayout) findViewById(R.id.offlineScreen_RL);
				fmOfflineWebView.setVisibility(View.INVISIBLE);	
				
				showPerformanceData();
			 }
		 });
	}
	
	//private functionalities
	
	private void showPerformanceData() {
		if(m_perfDlg == null) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(m_perfData).setTitle("Inoho Performance Data");
			m_perfDlg = builder.create();
			ScrollView sv = new ScrollView(this);
			m_perfDlg.setView(sv);
		}
		
		m_perfDlg.show();
		
		m_handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if(m_perfDlg != null)
					m_perfDlg.dismiss();				
			}
		}, 5000);
		
	}
	
	private void addPerformaceData(String msg) {
		m_perfData += "+ " + getTimeLapsedFromStart() + "s " + msg + "\n";
	}
	
	private long getTimeLapsedFromStart() {
		return (System.currentTimeMillis() - m_startTime)/1000;
	}
	
	private void loadHomePage() {
		//all set now load content
		 if(!m_linkToLoad.isEmpty()) {
			 addPerformaceData("loading " + m_linkToLoad);
			 mWebView.loadUrl(m_linkToLoad);
		 } else {
			 //mWebView.loadData(mOfflineInfo,"text/html", "");
			 showOfflineView();
		 }
	}
	
	private void openHomeLinkInFullMode() {
		m_handler = new Handler();
		new Thread(new Runnable() {			
			@Override
			public void run() {
				addPerformaceData("getting home (full)");
				
				String linkToConnect = m_conMgr.getHomeLink(false);
				
				addPerformaceData("got home @ " + linkToConnect + " (full)");
				
				if(!linkToConnect.equalsIgnoreCase(m_linkToLoad)) {
					m_linkToLoad = linkToConnect;
					m_handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							InohoMainActivity.this.loadHomePage();
						}
					}, 500);
				} else {
					addPerformaceData("same home as quick scan, no re-loading");
				}
			}
		}).start();
	}
		
	private void openHomeLinkInQuickMode() {
		//reset performance data
		m_startTime = System.currentTimeMillis();
		m_perfData = "";
		//let's try quick connect first and then full
		m_handler = new Handler();
		
		new Thread(new Runnable() {			
			@Override
			public void run() {
				addPerformaceData("getting home(quick)");
				m_linkToLoad = m_conMgr.getHomeLink(true);
				addPerformaceData("got home @ " + m_linkToLoad + " (quick)");  
				
				m_handler.postDelayed(new Runnable() {
					  @Override
					  public void run() {
						  InohoMainActivity.this.loadHomePage();
						  if(m_conMgr.getCurrentConnectionType() != CONNECTION_TYPE.MOBILE) {
							  addPerformaceData("starting full scan (quick)");
							  openHomeLinkInFullMode();
						  } else {
							  addPerformaceData("on 3G, no full scan (quick)");
						  }
					  }
					}, 500);
			}
		}).start();
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
	
	//performance data
	private long m_startTime = 0;
	private String m_perfData = "";
	private AlertDialog m_perfDlg = null;
}
