package com.eveningoutpost.dexdrip.Models;

import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.utils.PacketUtil;

import java.util.Date;
import java.util.UUID;

/**
 * Created by stephenblack on 11/6/14.
 */

@Table(name = "TransmitterData", id = BaseColumns._ID)
public class TransmitterData extends Model {
    private final static String TAG = BgReading.class.getSimpleName();

    @Column(name = "timestamp", index = true)
    public long timestamp;

    @Column(name = "raw_data")
    public double raw_data;
//
//    @Column(name = "filtered_data")
//    public double filtered_data;

    @Column(name = "sensor_battery_level")
    public int sensor_battery_level;

    @Column(name = "uuid", index = true)
    public String uuid;

    public static TransmitterData create(byte[] buffer, int len) {
                StringBuilder data_string = new StringBuilder();
        if (len < 6) { return null; };

        for (int i = 0; i < len; ++i) {
            data_string.append((char) buffer[i]);
        }
        String[] data = data_string.toString().split("\\s+");

        randomDelay(100, 2000);
        TransmitterData lastTransmitterData = TransmitterData.last();
        if (lastTransmitterData != null && lastTransmitterData.raw_data == Integer.parseInt(data[0]) && Math.abs(lastTransmitterData.timestamp - new Date().getTime()) < (10000)) { //Stop allowing duplicate data, its bad!
            return null;
        }

        TransmitterData transmitterData = new TransmitterData();
        if(data.length > 1) {
            transmitterData.sensor_battery_level = Integer.parseInt(data[1]);
        }
        if (Integer.parseInt(data[0]) < 1000) { return null; } // Sometimes the HM10 sends the battery level and readings in separate transmissions, filter out these incomplete packets!
        transmitterData.raw_data = Integer.parseInt(data[0]);
        transmitterData.timestamp = new Date().getTime();
        transmitterData.uuid = UUID.randomUUID().toString();

        transmitterData.save();
        return transmitterData;
    }

    public static TransmitterData create(int raw_data ,int sensor_battery_level, long timestamp) {
        randomDelay(100, 2000);
        TransmitterData lastTransmitterData = TransmitterData.last();
        if (lastTransmitterData != null && lastTransmitterData.raw_data == raw_data && Math.abs(lastTransmitterData.timestamp - new Date().getTime()) < (10000)) { //Stop allowing duplicate data, its bad!
            return null;
        }

        TransmitterData transmitterData = new TransmitterData();
        transmitterData.sensor_battery_level = sensor_battery_level;
        transmitterData.raw_data = raw_data ;
        transmitterData.timestamp = timestamp;
        transmitterData.uuid = UUID.randomUUID().toString();
        transmitterData.save();
        return transmitterData;
    }

    public static TransmitterData createFromBinary(byte data[]) {
        /* this is the C structure packed in data
           - needs to be in sync with wixel-DexDrip/libraries/include/dexdrip_packet.h
        struct dexdrip_data_packet {
            uint32 raw;
            int16  dexdrip_battery;
            uint8  dexcom_battery;
            uint8  padding;
        };
        length 8
        */

        if (data.length != 8) {
            /* incompatible ABI with wixel */
            return null;
        }

        int wixel_battery;
        TransmitterData transmitterData = new TransmitterData();

        transmitterData.raw_data = PacketUtil.uint32FromBuffer(data, 0);
        wixel_battery = PacketUtil.int16FromBuffer(data, 4);
        transmitterData.sensor_battery_level = PacketUtil.uint8FromBuffer(data, 6);
        transmitterData.timestamp = new Date().getTime();
        transmitterData.uuid = UUID.randomUUID().toString();
        Log.d(TAG, "binary transmitter data raw " + transmitterData.raw_data +
                " sensor battery " + transmitterData.sensor_battery_level +
                " wixel battery " + wixel_battery);
        transmitterData.save();
        return transmitterData;
    }

    public static TransmitterData last() {
        return new Select()
                .from(TransmitterData.class)
                .orderBy("_ID desc")
                .executeSingle();
    }

    public static void randomDelay(float min, float max){
        int random = (int)(max * Math.random() + min);
        try {
            Log.d("Sleeping ", "for " + random + "ms");
            Thread.sleep(random);
        } catch (InterruptedException e) {
            Log.e("Random Delay ", "INTERUPTED");
        }
    }
}
