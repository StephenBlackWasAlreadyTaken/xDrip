package com.eveningoutpost.dexdrip.Services;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.SnoozeActivity;
import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SnoozeOnNotificationDismissService extends IntentService {
    private final static String TAG = AlertPlayer.class.getSimpleName();

    public SnoozeOnNotificationDismissService() {
        super("SnoozeOnNotificationDismissService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        AlertType activeBgAlert = ActiveBgAlert.alertTypegetOnly();
        
        int snooze = 30;
        if(activeBgAlert != null) {
            if(activeBgAlert.default_snooze != 0) {
                snooze = activeBgAlert.default_snooze;
            } else {
                snooze = SnoozeActivity.getDefaultSnooze(activeBgAlert.above);
            }
        }
        
        AlertPlayer.getPlayer().Snooze(getApplicationContext(), snooze);
    }
}
