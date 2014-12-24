package com.eveningoutpost.dexdrip.Services;

import java.io.IOException;
import java.util.Date;

import android.content.Context;
import android.util.Log;

import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.TransmitterData;


public class WixelReader  extends Thread {
	// TODO: rename class...

    static public String mDebugString;
    private final String TAG = "tzachi";

    private final Context mContext;

    private volatile boolean mStop = false;

    public WixelReader(Context ctx) throws IOException
    {
        mContext = ctx.getApplicationContext();
    }

    public boolean IsConfigured() {
        return true;
    }
    public void run()
    {
        // let's start by faking numbers....
        int i = 0;
        int added = 5;
        while (!mStop) {
            try {
                for (int j = 0 ; j < 300; j++) {
                    Thread.sleep(1000);
                    if(mStop ) {
                    // we were asked to leave, so do it....
                        return;
                    }
                }
                i+=added;
                if (i==50) {
                    added = -5;
                }
                if (i==0) {
                    added = 5;
                }

                int fakedRaw = 150000 + i * 1000;
                Log.e(TAG, "calling setSerialDataToTransmitterRawData " + fakedRaw);
                setSerialDataToTransmitterRawData(fakedRaw, 100);

               } catch (InterruptedException e) {
                   // time to get out...
                   break;
               }
        }
    }

    public void Stop()
    {
        mStop = true;
    }
    public void setSerialDataToTransmitterRawData(int raw_data ,int sensor_battery_leve) {

        TransmitterData transmitterData = TransmitterData.create(raw_data, sensor_battery_leve, new Date().getTime());
        if (transmitterData != null) {
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                BgReading bgReading = BgReading.create(transmitterData.raw_data, mContext);
                sensor.latest_battery_level = transmitterData.sensor_battery_level;
                sensor.save();
            } else {
                Log.w(TAG, "No Active Sensor, Data only stored in Transmitter Data");
            }
        }
    }
}