package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.AlarmManager;
import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.eveningoutpost.dexdrip.AddCalibration;
import com.eveningoutpost.dexdrip.DoubleCalibrationActivity;
import com.eveningoutpost.dexdrip.EditAlertActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.CalibrationRequest;
import com.eveningoutpost.dexdrip.Models.UserNotification;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Sensor;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by stephenblack on 11/28/14.
 */
public class Notifications extends IntentService {
    public static final long[] vibratePattern = {0,1000,300,1000,300,1000};
    public static boolean bg_notifications;
    public static boolean bg_ongoing;
    public static boolean bg_vibrate;
    public static boolean bg_lights;
    public static boolean bg_sound;
    public static boolean bg_sound_in_silent;
    public static String bg_notification_sound;

    public static boolean calibration_notifications;
    public static boolean calibration_override_silent;
    public static int calibration_snooze;
    public static String calibration_notification_sound;
    public static boolean doMgdl;
    public static boolean smart_snoozing;
    public static boolean smart_alerting;
    private final static String TAG = AlertPlayer.class.getSimpleName();

    Context mContext;
    private static Handler mHandler = new Handler(Looper.getMainLooper());

    int currentVolume;
    AudioManager manager;
    Bitmap iconBitmap;
    Bitmap notifiationBitmap;

    final int BgNotificationId = 001;
    final int calibrationNotificationId = 002;
    final int doubleCalibrationNotificationId = 003;
    final int extraCalibrationNotificationId = 004;
    public static final int exportCompleteNotificationId = 005;
    final int ongoingNotificationId = 8811;
    public static final int exportAlertNotificationId = 006;
    public static final int uncleanAlertNotificationId = 007;
    public static final int missedAlertNotificationId = 010;
    public static final int riseAlertNotificationId = 011;
    public static final int failAlertNotificationId = 012;

    final static int callbackPeriod = 60000 * 1;

    SharedPreferences prefs;

    public Notifications() {
        super("Notifications");
        Log.w("Notifications", "Running Notifications Intent Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("Notifications", "Running Notifications Intent Service");
        ReadPerfs(getApplicationContext());
        notificationSetter(getApplicationContext());
        ArmTimer();
    }

    public void ReadPerfs(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        bg_notifications = prefs.getBoolean("bg_notifications", true);
        bg_vibrate = prefs.getBoolean("bg_vibrate", true);
        bg_lights = prefs.getBoolean("bg_lights", true);
        bg_sound = prefs.getBoolean("bg_play_sound", true);
        bg_notification_sound = prefs.getString("bg_notification_sound", "content://settings/system/notification_sound");
        bg_sound_in_silent = prefs.getBoolean("bg_sound_in_silent", false);

        calibration_notifications = prefs.getBoolean("calibration_notifications", true);
        calibration_snooze = Integer.parseInt(prefs.getString("calibration_snooze", "20"));
        calibration_override_silent = prefs.getBoolean("calibration_alerts_override_silent", false);
        calibration_notification_sound = prefs.getString("calibration_notification_sound", "content://settings/system/notification_sound");
        doMgdl = (prefs.getString("units", "mgdl").compareTo("mgdl") == 0);
        smart_snoozing = prefs.getBoolean("smart_snoozing", true);
        smart_alerting = prefs.getBoolean("smart_alerting", true);
        bg_ongoing = prefs.getBoolean("run_service_in_foreground", false);
    }

/*
 * *************************************************************************************************************
 * Function for new notifications
 */


    public void FileBasedNotifications(Context context) {
        ReadPerfs(context);
        Sensor sensor = Sensor.currentSensor();

        BgReading bgReading = BgReading.last();
        if(bgReading == null) {
            // Sensor is stopped, or there is not enough data
            AlertPlayer.getPlayer().stopAlert(context, true, false);
            return;
        }

        Log.e(TAG, "FileBasedNotifications called bgReading.calculated_value = " + bgReading.calculated_value);

        // TODO: tzachi what is the time of this last bgReading
        // If the last reading does not have a sensor, or that sensor was stopped.
        // or the sensor was started, but the 2 hours did not still pass? or there is no calibrations.
        // In all this cases, bgReading.calculated_value should be 0.
        if (sensor != null && bgReading != null && bgReading.calculated_value != 0) {
            AlertType newAlert = AlertType.get_highest_active_alert(context, bgReading.calculated_value);

            if (newAlert == null) {
                Log.e(TAG, "FileBasedNotifications - No active notifcation exists, stopping all alerts");
                // No alert should work, Stop all alerts, but keep the snoozing...
                AlertPlayer.getPlayer().stopAlert(context, false, true);
                return;
            }

            AlertType activeBgAlert = ActiveBgAlert.alertTypegetOnly();
            if(activeBgAlert == null) {
                Log.e(TAG, "FileBasedNotifications we have a new alert, starting to play it... " + newAlert.name);
                // We need to create a new alert  and start playing
                boolean trendingToAlertEnd = trendingToAlertEnd(context, true, newAlert);
                AlertPlayer.getPlayer().startAlert(context, trendingToAlertEnd, newAlert, EditAlertActivity.unitsConvert2Disp(doMgdl, bgReading.calculated_value));
                return;
            }


            if (activeBgAlert.uuid.equals(newAlert.uuid)) {
                // This is the same alert. Might need to play again...
                Log.e(TAG, "FileBasedNotifications we have found an active alert, checking if we need to play it " + newAlert.name);
                boolean trendingToAlertEnd = trendingToAlertEnd(context, false, newAlert);
                AlertPlayer.getPlayer().ClockTick(context, trendingToAlertEnd, EditAlertActivity.unitsConvert2Disp(doMgdl, bgReading.calculated_value));
                return;
            }
           // Currently the ui blocks having two alerts with the same alert value.

            boolean alertSnoozeOver = ActiveBgAlert.alertSnoozeOver();
            if (alertSnoozeOver) {
                Log.e(TAG, "FileBasedNotifications we had two alerts, the snoozed one is over, we fall down to deleting the snoozed and staring the new");
                // in such case it is not important which is higher.

            } else {
                // we have a new alert. If it is more important than the previous one. we need to stop
                // the older one and start a new one (We need to play even if we were snoozed).
                // If it is a lower level alert, we should keep being snoozed.


                // Example, if we have two alerts one for 90 and the other for 80. and we were already alerting for the 80
                // and we were snoozed. Now bg is 85, the alert for 80 is cleared, but we are alerting for 90.
                // We should not do anything if we are snoozed for the 80...
                // If one allert was high and the second one is low however, we alarm in any case (snoozing ignored).
                boolean opositeDirection = AlertType.OpositeDirection(activeBgAlert, newAlert);
                AlertType  newHigherAlert = AlertType.HigherAlert(activeBgAlert, newAlert);
                if ((newHigherAlert == activeBgAlert) && (!opositeDirection)) {
                    // the existing alert is the higher, we should check if to play it
                    Log.e(TAG, "FileBasedNotifications The existing alert has the same direcotion, checking if to playit newHigherAlert = " + newHigherAlert.name +
                            "activeBgAlert = " + activeBgAlert.name);

                    boolean trendingToAlertEnd = trendingToAlertEnd(context, false, newHigherAlert);
                    AlertPlayer.getPlayer().ClockTick(context, trendingToAlertEnd, EditAlertActivity.unitsConvert2Disp(doMgdl, bgReading.calculated_value));
                    return;
                }
            }
            // For now, we are stopping the old alert and starting a new one.
            Log.e(TAG, "Found a new alert, that is higher than the previous one will play it. " + newAlert.name);
            AlertPlayer.getPlayer().stopAlert(context, true, false);
            boolean trendingToAlertEnd = trendingToAlertEnd(context, true, newAlert);
            AlertPlayer.getPlayer().startAlert(context, trendingToAlertEnd, newAlert, EditAlertActivity.unitsConvert2Disp(doMgdl, bgReading.calculated_value));
            return;

        } else {
            AlertPlayer.getPlayer().stopAlert(context, true, false);
        }
    }

    boolean trendingToAlertEnd(Context context, Boolean newAlert, AlertType Alert) {
        if(newAlert && !smart_alerting) {
        //  User does not want smart alerting at all.
            return false;
        }
        if((!newAlert) && (!smart_snoozing)) {
        //  User does not want smart snoozing at all.
            return false;
        }
        return BgReading.trendingToAlertEnd(context, Alert.above);
    }
/*
 * *****************************************************************************************************************
 */

    // only function that is really called from outside...
    public void notificationSetter(Context context) {
        ReadPerfs(context);
        BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(context);
        if (bg_ongoing && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)) {
            bgOngoingNotification(bgGraphBuilder);
        }
        if(prefs.getLong("alerts_disabled_until", 0) > new Date().getTime()){
            Log.w("NOTIFICATIONS", "Notifications are currently disabled!!");
            return;
        }
        FileBasedNotifications(context);
        BgReading.checkForDropAllert(context);
        BgReading.checkForRisingAllert(context);

        Sensor sensor = Sensor.currentSensor();

        List<BgReading> bgReadings = BgReading.latest(3);
        List<Calibration> calibrations = Calibration.allForSensorInLastFourDays();
        if(bgReadings == null || bgReadings.size() < 3) { return; }
        if(calibrations == null || calibrations.size() < 2) { return; }
        BgReading bgReading = bgReadings.get(0);

        if (calibration_notifications) {
            if (bgReadings.size() >= 3) {
                if (calibrations.size() == 0 && (new Date().getTime() - bgReadings.get(2).timestamp <= (60000 * 30)) && sensor != null) {
                    if ((sensor.started_at + (60000 * 60 * 2)) < new Date().getTime()) {
                        doubleCalibrationRequest();
                    } else { clearDoubleCalibrationRequest(); }
                } else { clearDoubleCalibrationRequest(); }
            } else { clearDoubleCalibrationRequest(); }
            if (CalibrationRequest.shouldRequestCalibration(bgReading) && (new Date().getTime() - bgReadings.get(2).timestamp <= (60000 * 24))) {
                extraCalibrationRequest();
            } else { clearExtraCalibrationRequest(); }
            if (calibrations.size() >= 1 && Math.abs((new Date().getTime() - calibrations.get(0).timestamp))/(1000*60*60) > 12) {
                Log.e("NOTIFICATIONS", "Calibration difference in hours: " + ((new Date().getTime() - calibrations.get(0).timestamp))/(1000*60*60));
                calibrationRequest();
            } else { clearCalibrationRequest(); }

        } else {
            clearAllCalibrationNotifications();
        }
    }

    private void  ArmTimer() {
        Log.e(TAG, "ArmTimer called");
        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
        if(activeBgAlert != null ) {
            AlertType alert = AlertType.get_alert(activeBgAlert.alert_uuid);
            if(alert != null) {
                int time = alert.minutes_between;
                if (time < 1) { time = 1; }
                Calendar calendar = Calendar.getInstance();
                AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    alarm.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + time * 60000, PendingIntent.getService(this, 0, new Intent(this, Notifications.class), 0));
                } else {
                    alarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + time * 60000, PendingIntent.getService(this, 0, new Intent(this, Notifications.class), 0));
                }
            }
        }
    }

    private Bitmap createWearBitmap(long start, long end) {
        return new BgSparklineBuilder(mContext)
                .setBgGraphBuilder(new BgGraphBuilder(mContext))
                .setStart(start)
                .setEnd(end)
                .showHighLine()
                .showLowLine()
                .showAxes()
                .setWidthPx(400)
                .setHeightPx(400)
                .setSmallDots()
                .build();
    }

    private Bitmap createWearBitmap(long hours) {
        return createWearBitmap(System.currentTimeMillis() - 60000 * 60 * hours, System.currentTimeMillis());
    }

    private Notification createExtensionPage(long hours) {
        return new NotificationCompat.Builder(mContext)
                .extend(new NotificationCompat.WearableExtender()
                                .setBackground(createWearBitmap(hours))
                                .setHintShowBackgroundOnly(true)
                                .setHintAvoidBackgroundClipping(true)
                )
                .build();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public Notification createOngoingNotification(BgGraphBuilder bgGraphBuilder, Context context) {
        mContext = context;
        ReadPerfs(mContext);
        Intent intent = new Intent(mContext, Home.class);
        List<BgReading> lastReadings = BgReading.latest(2);
        BgReading lastReading = null;
        if (lastReadings != null && lastReadings.size() >= 2) {
            lastReading = lastReadings.get(0);
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addParentStack(Home.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder b = new NotificationCompat.Builder(mContext);
        //b.setOngoing(true);
        b.setCategory(NotificationCompat.CATEGORY_STATUS);
        String titleString = lastReading == null ? "BG Reading Unavailable" : (lastReading.displayValue(mContext) + " " + BgReading.slopeArrow(lastReading.calculated_value_slope * 60000));
        b.setContentTitle(titleString)
                .setContentText("xDrip Data collection service is running.")
                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                .setUsesChronometer(false);
        if (lastReading != null) {
            b.setWhen(lastReading.timestamp);
            String deltaString = "Delta: " + bgGraphBuilder.unitizedDeltaString(lastReading.calculated_value - lastReadings.get(1).calculated_value);
            b.setContentText(deltaString);
            iconBitmap = new BgSparklineBuilder(mContext)
                    .setHeight(64)
                    .setWidth(64)
                    .setStart(System.currentTimeMillis() - 60000 * 60 * 3)
                    .setBgGraphBuilder(bgGraphBuilder)
                    .build();
            b.setLargeIcon(iconBitmap);
            NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
            notifiationBitmap = new BgSparklineBuilder(mContext)
                    .setBgGraphBuilder(bgGraphBuilder)
                    .showHighLine()
                    .showLowLine()
                    .build();
            bigPictureStyle.bigPicture(notifiationBitmap)
                    .setSummaryText(deltaString)
                    .setBigContentTitle(titleString);
            b.setStyle(bigPictureStyle);
        }
        b.setContentIntent(resultPendingIntent);
        return b.build();
    }

    private void bgOngoingNotification(final BgGraphBuilder bgGraphBuilder) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                NotificationManagerCompat
                        .from(mContext)
                        .notify(ongoingNotificationId, createOngoingNotification(bgGraphBuilder, mContext));
                iconBitmap.recycle();
                notifiationBitmap.recycle();
            }
        });
    }

    private void soundAlert(String soundUri) {
        manager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
        Uri notification = Uri.parse(soundUri);
        MediaPlayer player = MediaPlayer.create(mContext, notification);

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                manager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
            }
        });
        player.start();
    }

    private void clearAllCalibrationNotifications() {
        notificationDismiss(calibrationNotificationId);
        notificationDismiss(extraCalibrationNotificationId);
        notificationDismiss(doubleCalibrationNotificationId);
    }

    private void calibrationNotificationCreate(String title, String content, Intent intent, int notificationId) {
        NotificationCompat.Builder mBuilder = notificationBuilder(title, content, intent);
        mBuilder.setVibrate(vibratePattern);
        mBuilder.setLights(0xff00ff00, 300, 1000);
        if(calibration_override_silent) {
            mBuilder.setSound(Uri.parse(calibration_notification_sound), AudioAttributes.USAGE_ALARM);
        } else {
            mBuilder.setSound(Uri.parse(calibration_notification_sound));
        }

        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(notificationId);
        mNotifyMgr.notify(notificationId, mBuilder.build());
    }

    private NotificationCompat.Builder notificationBuilder(String title, String content, Intent intent) {
        return new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(notificationIntent(intent));
    }

    private PendingIntent notificationIntent(Intent intent){
        return PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void notificationDismiss(int notificationId) {
        NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(notificationId);
    }

    private void calibrationRequest() {
        UserNotification userNotification = UserNotification.lastCalibrationAlert();
        if ((userNotification == null) || (userNotification.timestamp <= ((new Date().getTime()) - (60000 * calibration_snooze)))) {
            if (userNotification != null) { userNotification.delete(); }
            UserNotification.create("12 hours since last Calibration", "calibration_alert");
            String title = "Calibration Needed";
            String content = "12 hours since last calibration";
            Intent intent = new Intent(mContext, AddCalibration.class);
            calibrationNotificationCreate(title, content, intent, calibrationNotificationId);
        }
    }

    private void doubleCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastDoubleCalibrationAlert();
        if ((userNotification == null) || (userNotification.timestamp <= ((new Date().getTime()) - (60000 * calibration_snooze)))) {
            if (userNotification != null) { userNotification.delete(); }
            UserNotification.create("Double Calibration", "double_calibration_alert");
            String title = "Sensor is ready";
            String content = "Sensor is ready, please enter a double calibration";
            Intent intent = new Intent(mContext, DoubleCalibrationActivity.class);
            calibrationNotificationCreate(title, content, intent, calibrationNotificationId);
        }
    }

    private void extraCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastExtraCalibrationAlert();
        if ((userNotification == null) || (userNotification.timestamp <= ((new Date().getTime()) - (60000 * calibration_snooze)))) {
            if (userNotification != null) { userNotification.delete(); }
            UserNotification.create("Extra Calibration Requested", "extra_calibration_alert");
            String title = "Calibration Needed";
            String content = "A calibration entered now will GREATLY increase performance";
            Intent intent = new Intent(mContext, AddCalibration.class);
            calibrationNotificationCreate(title, content, intent, extraCalibrationNotificationId);
        }
    }

    public static void bgUnclearAlert(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int otherAlertSnooze =  Integer.parseInt(prefs.getString("other_alerts_snooze", "20"));
        OtherAlert(context, "bg_unclear_readings_alert", "Unclear Sensor Readings", uncleanAlertNotificationId,  otherAlertSnooze);
    }

    public static void bgMissedAlert(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int otherAlertSnooze =  Integer.parseInt(prefs.getString("other_alerts_snooze", "20"));
        OtherAlert(context, "bg_missed_alerts", "BG Readings Missed", missedAlertNotificationId, otherAlertSnooze);
    }

    public static void RisingAlert(Context context, boolean on) {
        RiseDropAlert(context, on, "bg_rise_alert", "bg rising fast", riseAlertNotificationId);
    }
    public static void DropAlert(Context context, boolean on) {
        RiseDropAlert(context, on, "bg_fall_alert", "bg failing fast", failAlertNotificationId);
    }

    public static void RiseDropAlert(Context context, boolean on, String type, String message, int notificatioId) {
        if(on) {
         // This alerts will only happen once. Want to have maxint, but not create overflow.
            OtherAlert(context, type, message, notificatioId, Integer.MAX_VALUE / 100000);
        } else {
            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(notificatioId);
            UserNotification.DeleteNotificationByType(type);
        }
    }

    private static void OtherAlert(Context context, String type, String message, int notificatioId, int snooze) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String otherAlertsSound = prefs.getString("other_alerts_sound", "content://settings/system/notification_sound");
        Boolean otherAlertsOverrideSilent = prefs.getBoolean("other_alerts_override_silent", false);

        UserNotification userNotification = UserNotification.GetNotificationByType(type); //"bg_unclear_readings_alert"
        if ((userNotification == null) || (userNotification.timestamp <= ((new Date().getTime()) - (60000 * snooze)))) {
            if (userNotification != null) {
                userNotification.delete();
            }
            UserNotification.create(message, type);
            Intent intent = new Intent(context, Home.class);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_action_communication_invert_colors_on)
                            .setContentTitle(message)
                            .setContentText(message)
                            .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
            mBuilder.setVibrate(vibratePattern);
            mBuilder.setLights(0xff00ff00, 300, 1000);
            if(otherAlertsOverrideSilent) {
                mBuilder.setSound(Uri.parse(otherAlertsSound), AudioAttributes.USAGE_ALARM);
            } else {
                mBuilder.setSound(Uri.parse(otherAlertsSound));
            }
            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotifyMgr.cancel(notificatioId);
            mNotifyMgr.notify(notificatioId, mBuilder.build());
        }
    }

    private void clearCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastCalibrationAlert();
        if (userNotification != null) {
            userNotification.delete();
            notificationDismiss(calibrationNotificationId);
        }
    }

    private void clearDoubleCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastDoubleCalibrationAlert();
        if (userNotification != null) {
            userNotification.delete();
            notificationDismiss(doubleCalibrationNotificationId);
        }
    }

    private void clearExtraCalibrationRequest() {
        UserNotification userNotification = UserNotification.lastExtraCalibrationAlert();
        if (userNotification != null) {
            userNotification.delete();
            notificationDismiss(extraCalibrationNotificationId);
        }
    }
}
