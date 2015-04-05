package com.inoho.inohoconnect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

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

import android.util.Log;

public class InohoPingWorkerThread implements Runnable {

	public static String m_linkToHome = "";
	private String m_ipAddress;
	private String m_localLinkPref;
	
	InohoPingWorkerThread(String ipAddress, String localLinkPref) {
		m_ipAddress = ipAddress;
		m_localLinkPref = localLinkPref;
	}
	
	@Override
	public void run() {
		if(isLinkAccessible(m_ipAddress)) {
			if(isItHome(m_ipAddress)) {
				m_linkToHome = m_ipAddress;
			}
		}
	}
	
	private boolean isLinkAccessible (String link) {
		Log.e("Inoho", "pinging ip - " + link);
		boolean linkAccessible = false;
		Runtime runtime = Runtime.getRuntime();
        try {
            Process  mIpAddrProcess = runtime.exec("/system/bin/ping -c 1 " + link);
            int mExitValue = mIpAddrProcess.waitFor();
            if(mExitValue==0) {
            	Log.i("Inoho", "ip available - " + link);
                linkAccessible = true;
            } else {
            	Log.e("Inoho", "ip not available - " + link);
                linkAccessible = false;
            }
        } catch (InterruptedException ignore) {
        	
        } catch (IOException e) {
        }
		return linkAccessible;
	}
	
	private boolean isItHome(String fullIPAddress) {		
		boolean result = false;		
		try {
			String completeLocalLink = String.format(m_localLinkPref, fullIPAddress);
			Log.i("Inoho", "check this link for home - " + completeLocalLink);
	        
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
}
