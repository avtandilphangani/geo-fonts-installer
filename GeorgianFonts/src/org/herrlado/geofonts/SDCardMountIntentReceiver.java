package org.herrlado.geofonts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SDCardMountIntentReceiver extends BroadcastReceiver{
	
	private Installer installer;

	public SDCardMountIntentReceiver(Installer installer){
		this.installer = installer;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		installer.onReceiveExternalStorageEvent(intent);
	}

}
