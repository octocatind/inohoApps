package com.inoho.inohoconnect;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.StrictMode;
import android.text.format.Formatter;


public class InohoConnectionManager {
	//public APIs region
	public InohoConnectionManager(Context ctx) {
		m_AppContext = ctx;
	}
	
	public String getHomeLink() {
		String linkToHome = "";
		
		//is device online at all
		if(isDeviceOnline()) {
			//get connection type
			CONNECTION_TYPE ct = getConnectionInfo();
			
			if(ct == CONNECTION_TYPE.WIFI) {
				linkToHome = getLinkWhenOnWifi();
				if(linkToHome.isEmpty()) {
					Resources res = m_AppContext.getResources();
					linkToHome = res.getString(R.string.cloudLink);
				}
			} else if(ct == CONNECTION_TYPE.MOBILE){
				Resources res = m_AppContext.getResources();
				linkToHome = res.getString(R.string.cloudLink);
			}
		}
		
		return linkToHome;
	}
	
	//private APIs region	
	private String getLinkWhenOnWifi() {
		String addressToHome = "";
		
		WifiManager wifiManager = (WifiManager) m_AppContext.getSystemService(Context.WIFI_SERVICE);
	    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
	    int ipAddress = wifiInfo.getIpAddress();
	    String ip = Formatter.formatIpAddress(ipAddress);
	    
	    String ipAdrsPart = ip.substring(0, ip.lastIndexOf(".")+1);
	    addressToHome = getLinkByPingingPreferedIP(ipAdrsPart);
	    if(addressToHome.isEmpty()) {
	    	addressToHome = getLinkByPingingPrvslyCnctdIPs();
	    	if(addressToHome.isEmpty()) {
	    		addressToHome = getLinkByPingingBroadCastIP();
	    		if(addressToHome.isEmpty()){
	    			addressToHome = getLinkByPingingAll(ipAdrsPart);
	    		}
	    	}
	    }
		
	    if(!addressToHome.isEmpty()) {
	    	saveConnectedIps(addressToHome); //save only ip part
		    Resources res = m_AppContext.getResources();
			String localLinkPref = res.getString(R.string.localLink);
	        addressToHome = String.format(localLinkPref, addressToHome);	        
	    }
	    
	    return addressToHome;
	}
	
	private void saveConnectedIps(String localAddressToHome) {
		//get part ip from the current local address
		//String partIp = localAddressToHome.substring(localAddressToHome.lastIndexOf(".")+ 1, localAddressToHome.length());
		//int partIpInt = Integer.parseInt(partIp);
		
		SharedPreferences sp = m_AppContext.getSharedPreferences("connectedIps", Context.MODE_PRIVATE);
		String prefIpsCombStr = sp.getString("prefIpd", "");
		String [] prefIpsArray = prefIpsCombStr.split(";;");
		
		boolean alreadyPresent = false;
		for(int ii=0; ii<prefIpsArray.length; ii++) {
			if(prefIpsArray[ii] == localAddressToHome){
				alreadyPresent = true;
				break;
			}
		}
		
		if(!alreadyPresent) {
			//add to the list
			String [] newPrefIpsArray = new String[prefIpsArray.length+1];
			for(int jj=0; jj< prefIpsArray.length; jj++) {
				newPrefIpsArray[jj] = prefIpsArray[jj];
			}
			newPrefIpsArray[prefIpsArray.length] = localAddressToHome + ";;";
			
			SharedPreferences.Editor spEd = sp.edit();
			spEd.putString("connectedIps", newPrefIpsArray.toString());
			spEd.commit();
		}
	}
	
	private String getLinkByPingingPrvslyCnctdIPs(){
		String addressToHome = "";
		
		SharedPreferences sp = m_AppContext.getSharedPreferences("connectedIps", Context.MODE_PRIVATE);
		String prefIpsCombStr = sp.getString("prefIpd", "");
		String [] prefIpsArray = prefIpsCombStr.split(";;");
		
		for(int ii=0; ii<prefIpsArray.length; ii++) {
			//ping first
			String prvslCnctdLink = prefIpsArray[ii]; 
			if(isLinkAccessible(prvslCnctdLink)) {
				//ping success - now perform handshake
				if(isItHome(prvslCnctdLink)) {
					addressToHome = prvslCnctdLink;
					break;
				}
			}
		}
		return addressToHome;
	}
	
	private String getLinkByPingingPreferedIP(String ipAddressPart){
		String addressToHome = "";
		Resources res = m_AppContext.getResources();
		int[] prefIPs = res.getIntArray(R.array.inohoPrefIPs);
		for(int ii=0; ii<prefIPs.length; ii++) {
			String fullLinkToHome = ipAddressPart + prefIPs[ii];
			//ping first
			if(isLinkAccessible(fullLinkToHome)) {
				//ping success - now perform handshake
				if(isItHome(fullLinkToHome)) {
					addressToHome = fullLinkToHome;
					break;
				}
			}
		}
		return addressToHome;
	}
	
	private String getLinkByPingingBroadCastIP() {
		String linkToHome = "";
		
		final WifiManager wifiManager = (WifiManager) m_AppContext.getSystemService(Context.WIFI_SERVICE);
	    final DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
	    
	    int subnetMaskInt = dhcpInfo.netmask;
	    int ipAddressInt = dhcpInfo.ipAddress;
	    
	    int broadCastIPInt = ~subnetMaskInt | ipAddressInt;	    
	    String broadCastIPStr = Formatter.formatIpAddress(broadCastIPInt);
	    
	    Runtime runtime = Runtime.getRuntime();
        try {
            Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 -b " + broadCastIPStr);
            int mExitValue = mIpAddrProcess.waitFor();            
            if(mExitValue==0) {
	        	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(mIpAddrProcess.getInputStream()));
	            String line;
	            while ((line = bufferedReader.readLine()) != null) {
	            	if(line.contains("64 bytes from ")){
	            		String [] words = line.split(" "); 
	            		////192.168.1.7:
	            		linkToHome = words[3].substring(0, words[3].length()-1);
	            		if(isItHome(linkToHome)) {
	            			break;
	            		}
	            	}
	            }                
            } else {
            	linkToHome = "";
            }
        } catch (InterruptedException ignore) {
        	ignore.printStackTrace();
        } catch (IOException e) {
        	e.printStackTrace();
        }
	    
        return linkToHome;
	}
	
	private String getLinkByPingingAll(String ipAddressPart){
        Resources res = m_AppContext.getResources();
		String localLinkPref = res.getString(R.string.handShakeLink);
        
		InohoPingWorkerThread.m_linkToHome = "";
		
		ExecutorService exSer = Executors.newFixedThreadPool(10);
        
		//hard coded number of hosts - this would suffice for now
		for(int ii=2; ii< 255; ii++){
			String fullLinkToHome = ipAddressPart + ii;
			Runnable ru = new InohoPingWorkerThread(fullLinkToHome, localLinkPref);
			exSer.execute(ru);
		}
		
		exSer.shutdown();
		while(!exSer.isTerminated()){
			//wait for termination
			if(!InohoPingWorkerThread.m_linkToHome.isEmpty()){
				exSer.shutdownNow();
			}
		}
		
		return InohoPingWorkerThread.m_linkToHome;
	}
	
	//this API performs initial handshake to identify that device with this IP is actually home	
	private boolean isItHome(String fullIPAddress) {
		boolean result = false;		
		try {
			
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
	        StrictMode.setThreadPolicy(policy); 
	        
	        Resources res = m_AppContext.getResources();
			String localLinkPref = res.getString(R.string.handShakeLink);
	        String completeLocalLink = String.format(localLinkPref, fullIPAddress);
	        
	        URI homeURL = new URI(completeLocalLink);
			
	        HttpParams httpParameters = new BasicHttpParams();
	        int timeoutConnection = 2000;
	        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
	        HttpConnectionParams.setSoTimeout(httpParameters, 2000);
	        
	        
	        HttpClient httpclient = new DefaultHttpClient(httpParameters);
		    HttpResponse response = httpclient.execute(new HttpGet(homeURL));
		    
		    StatusLine statusLine = response.getStatusLine();
		    if(statusLine.getStatusCode() == HttpStatus.SC_OK){
		        ByteArrayOutputStream out = new ByteArrayOutputStream();
		        response.getEntity().writeTo(out);
		        out.close();
		        String responseString = out.toString();
		        
		        JSONObject jobj = new JSONObject(responseString);
		        if(jobj != null && jobj.getBoolean("success")){
		        	result = true;
		        }
		    } else {
		    	//Closes the connection.
		    	response.getEntity().getContent().close();
	        }
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}
	
	//this API performs ping to identify whether there is any device at the given IP
	
	private boolean isLinkAccessible (String link) {
		boolean linkAccessible = false;
		Runtime runtime = Runtime.getRuntime();
        try {
            Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 " + link);
            int mExitValue = mIpAddrProcess.waitFor();
            if(mExitValue==0) {
                linkAccessible = true;
            } else {
                linkAccessible = false;
            }
        } catch (InterruptedException ignore) {
        	
        } catch (IOException e) {
        }
		return linkAccessible;
	}
	
	private boolean isDeviceOnline() {
		boolean isOnline = false;
		final ConnectivityManager connMgr = (ConnectivityManager)  m_AppContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
	    if (networkInfo != null && networkInfo.isConnected()) {
	    	isOnline = true;
	    } else {
	    	isOnline = false;
	    }
	    return isOnline;
	}
	
	private CONNECTION_TYPE getConnectionInfo() {
		CONNECTION_TYPE ct = CONNECTION_TYPE.NONE;
		
		final ConnectivityManager connMgr = (ConnectivityManager)  m_AppContext.getSystemService(Context.CONNECTIVITY_SERVICE);   
			
		final NetworkInfo wifi =  connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		final NetworkInfo mobile =  connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		if(wifi.isConnected()){
			ct = CONNECTION_TYPE.WIFI;			
		} else if(mobile.isConnected()){
			ct = CONNECTION_TYPE.MOBILE;
		} else  {
			ct = CONNECTION_TYPE.NONE;
		}	
		return ct;
	}
	
	//Data region
	private Context m_AppContext = null;
	private enum CONNECTION_TYPE {
		NONE,
		WIFI,
		MOBILE
	};
	private static String LOG_TAG = "CONCTNMGR";
}
