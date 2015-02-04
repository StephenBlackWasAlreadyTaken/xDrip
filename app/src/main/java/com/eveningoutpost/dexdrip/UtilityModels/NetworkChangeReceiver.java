package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * Created by Radu Iliescu on 2/4/2015.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = NetworkChangeReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        Log.d(TAG, "Network change event");
        if (connMgr.getActiveNetworkInfo() != null)
            NightscoutMongoClient.resetMongoConnection();
    }
}