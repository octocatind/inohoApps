package com.inoho.inohoconnect;

import android.app.Application;

public class InohoApplication extends Application {
	
	public static boolean isActivityVisible() {
	    return isAppInForeground;	  
	}  

	public static boolean doesUINeedsRefresh() {
	    return uiNeedsRefresh;
	}
	
	public static void setUINeedsRefresh(boolean state) {
		uiNeedsRefresh = state;
	}
	
	public static void activityResumed() {
		isAppInForeground = true;
	}

	public static void activityPaused() {
		isAppInForeground = false;
	}

	private static boolean uiNeedsRefresh = false;
	private static boolean isAppInForeground = false;
}
