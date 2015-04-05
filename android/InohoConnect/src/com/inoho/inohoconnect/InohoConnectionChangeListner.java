package com.inoho.inohoconnect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Log;

public class InohoConnectionChangeListner extends BroadcastReceiver{

	public InohoConnectionChangeListner() {
		//Log.e("Inoho", "this is purely wrong");
	}	
	public InohoConnectionChangeListner(Context appContext) {
		m_appContext = appContext;
		inohoConnectionInfo conInfo = getCurrentConnectionInfo(); 
		m_prevConInfo.m_connectionType = conInfo.m_connectionType;
		m_prevConInfo.m_wifiNetworkID = conInfo.m_wifiNetworkID;
	}
	
	@Override
	public void onReceive(Context ctx, Intent intnt) {
		//something in connection has changed let's restart the activity
		
		if(!isInitialStickyBroadcast()) {	
			inohoConnectionInfo currentInfo = getCurrentConnectionInfo();
			
			String conectionType = currentInfo.m_connectionType;
			String networkId = currentInfo.m_wifiNetworkID;
			
			boolean hasConnectionStateChanged = false;
			if(conectionType.isEmpty() || conectionType.equalsIgnoreCase("NONE")) {
				hasConnectionStateChanged = false;
			} else if(!conectionType.equals(m_prevConInfo.m_connectionType)) {
				hasConnectionStateChanged = true;
			} else if(conectionType == "WIFI" && m_prevConInfo.m_connectionType == "WIFI") {
				if(!networkId.equals("<unknown ssid>") && !networkId.equals(m_prevConInfo.m_wifiNetworkID)){
					//wifi network changed
					hasConnectionStateChanged = true;
				}
			}
			
			m_prevConInfo.m_connectionType = currentInfo.m_connectionType;
			m_prevConInfo.m_wifiNetworkID = currentInfo.m_wifiNetworkID;
			
			if(hasConnectionStateChanged) {
				if(InohoApplication.isActivityVisible()){
					launchActivity();
				} else {//activity in background mark state to be refreshed on launch 
					InohoApplication.setUINeedsRefresh(true);
				}				
			}
		}
	}
	
	private void launchActivity() {
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
		  @Override
		  public void run() {
			Intent in = new Intent(m_appContext, com.inoho.inohoconnect.InohoMainActivity.class);
			in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			in.putExtra("onConctnChange", true);
			m_appContext.startActivity(in);			  
		  }
		}, 3000);
	}
	
	private inohoConnectionInfo getCurrentConnectionInfo () {
		//broadcast receiver 
		inohoConnectionInfo currentConInfo = new inohoConnectionInfo();
		final ConnectivityManager connMgr = (ConnectivityManager)  m_appContext.getSystemService(Context.CONNECTIVITY_SERVICE);   
		
		final NetworkInfo wifi =  connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		final NetworkInfo mobile =  connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		
		if(wifi.isConnected()){
			currentConInfo.m_connectionType = "WIFI";
			WifiManager wifiManager = (WifiManager) m_appContext.getSystemService(Context.WIFI_SERVICE);
		    WifiInfo wInfo = wifiManager.getConnectionInfo();
		    currentConInfo.m_wifiNetworkID = wInfo.getSSID();
		} else if(mobile.isConnected()){
			currentConInfo.m_connectionType = "MOBILE";
			currentConInfo.m_wifiNetworkID = "";
		} else {
			currentConInfo.m_connectionType = "NONE";
			currentConInfo.m_wifiNetworkID = "";
		}
		return currentConInfo;
	}
	
	private Context m_appContext = null;
	private static inohoConnectionInfo m_prevConInfo = new inohoConnectionInfo();
}
