package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Tables.BgReadingTable;
import com.eveningoutpost.dexdrip.Tables.CalibrationDataTable;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by stephenblack on 11/5/14.
 */
public class NavDrawerBuilder {
    public final List<Calibration> last_two_calibrations = Calibration.latest(2);
    public final List<BgReading> last_two_bgReadings = BgReading.latest(2);
    public final List<BgReading> bGreadings_in_last_30_mins = BgReading.last30Minutes();
    public final boolean is_active_sensor = Sensor.isActive();
    public final double time_now = new Date().getTime();

    public final List<String> nav_drawer_options(Context context) {
        List<String> options = new ArrayList<String>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean IUnderstand = prefs.getBoolean("I_understand", false);
        if(IUnderstand == false) {
            options.add("Settings");
            return options;
        }

        options.add("DexDrip");
        if(is_active_sensor) {
            options.add("Calibration Graph");
        }
        options.add("BG Data Table");
        options.add("Calibration Data Table");
//        options.add("Sensor Data Table");

        if(is_active_sensor) {
            if(last_two_bgReadings.size() > 1) {
                if(last_two_calibrations.size() > 1) {
                    if(bGreadings_in_last_30_mins.size() >= 2) {
                        if (time_now - last_two_calibrations.get(0).timestamp < 5*60000) { options.add("Override Calibration"); }
                        options.add("Add Calibration");
                    } else { options.add("Cannot Calibrate right now"); }
                    if (last_two_calibrations.get(0).slope >= 1.4 || last_two_calibrations.get(0).slope <= 0.5) { options.add("Add Double Calibration"); }
                } else { options.add("Add Double Calibration"); }
            }
            options.add("Stop Sensor");
        } else { options.add("Start Sensor"); }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(CollectionServiceStarter.isBTWixel(context)) {
                options.add("Scan for BT");
            }
        }
        options.add("Settings");
//        options.add("Fake Numbers");
//        options.add("Add Double Calibration");
        return options;
    }

    public final List<Intent> nav_drawer_intents(Context context) {
        List<Intent> options = new ArrayList<Intent>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean IUnderstand = prefs.getBoolean("I_understand", false);
        if(IUnderstand == false) {
            options.add(new Intent(context, SettingsActivity.class));
            return options;
        }

        options.add(new Intent(context, Home.class));
        if(is_active_sensor) {
            options.add(new Intent(context, CalibrationGraph.class));
        }
        options.add(new Intent(context, BgReadingTable.class));
        options.add(new Intent(context, CalibrationDataTable.class));
//        options.add(new Intent(context, SensorDataTable.class));


        if(is_active_sensor) {
            if(last_two_bgReadings.size() > 1) {
                if (last_two_calibrations.size() > 1) {
                    if(bGreadings_in_last_30_mins.size() >= 3) {
                         if (time_now - last_two_calibrations.get(0).timestamp < 5*60000) { options.add(new Intent(context, CalibrationOverride.class)); }
                        options.add(new Intent(context, AddCalibration.class));
                    } else { options.add(new Intent(context, Home.class)); }
                    if (last_two_calibrations.get(0).slope >= 1.4 || last_two_calibrations.get(0).slope <= 0.5) { options.add(new Intent(context, DoubleCalibrationActivity.class)); }
                } else { options.add(new Intent(context, DoubleCalibrationActivity.class)); }
            }
            options.add(new Intent(context, StopSensor.class));
        } else { options.add(new Intent(context, StartNewSensor.class)); }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if(CollectionServiceStarter.isBTWixel(context)) {
                options.add(new Intent(context, BluetoothScan.class));
            }
        }
        options.add(new Intent(context, SettingsActivity.class));
//        options.add(new Intent(context, FakeNumbers.class));
//        options.add(new Intent(context, DoubleCalibrationActivity.class));
        return options;
    }

}
