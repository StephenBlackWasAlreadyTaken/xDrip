package com.eveningoutpost.dexdrip.Models;

import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.UUID;


@Table(name = "TransmitterData", id = BaseColumns._ID)
public class TransmitterData extends Model {
    //private final static String TAG = BgReading.class.getSimpleName();
    private final static String TAG = TransmitterData.class.getSimpleName();

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
        if (len < 6) { return null; }
        if (buffer[0] == 0x10 && buffer[1] == 0x00) {
            //this is a dexbridge packet.  Process accordingly.
            Log.w(TAG, "create Processing a Dexbridge packet");
            ByteBuffer txData = ByteBuffer.allocate(len);
            txData.order(ByteOrder.LITTLE_ENDIAN);
            txData.put(buffer, 0, len);
            TransmitterData transmitterData = new TransmitterData();
            transmitterData.raw_data = txData.getInt(2);
            transmitterData.sensor_battery_level = txData.getShort(10);
            transmitterData.timestamp = new Date().getTime();
            transmitterData.uuid = UUID.randomUUID().toString();

            transmitterData.save();
            Log.w(TAG, "Created transmitterData record with Raw value of " + transmitterData.raw_data + " at " +transmitterData.timestamp);
            return transmitterData;
        } else {
            //this is NOT a dexbridge packet.  Process accordingly.
            Log.w(TAG, "create Processing a BTWixel or IPWixel packet");
            StringBuilder data_string = new StringBuilder();

            for (int i = 0; i < len; ++i) {
                data_string.append((char) buffer[i]);
            }
            String[] data = data_string.toString().split("\\s+");
            //randomDelay(100, 2000);
            TransmitterData lastTransmitterData = TransmitterData.last();
            if (lastTransmitterData != null && lastTransmitterData.raw_data == Integer.parseInt(data[0]) && Math.abs(lastTransmitterData.timestamp - new Date().getTime()) < (10000)) { //Stop allowing duplicate data, its bad!
                return null;
            }

            TransmitterData transmitterData = new TransmitterData();
            if (data.length > 1) {
                transmitterData.sensor_battery_level = Integer.parseInt(data[1]);
            }
            if (Integer.parseInt(data[0]) < 1000) {
                return null;
            } // Sometimes the HM10 sends the battery level and readings in separate transmissions, filter out these incomplete packets!
            /* note for above comment.  HM-1X BLE only send 80 characters at a time, and send data if there is any delay after one character.
            This may be what you are experiencing.  Depending on the DexDrip-wixel code, you may be inserting a delay on occasion with printf.
            Dexbridge resolves this by filling the bufer with the whole packet before sending it.  Also the message is at maximum 16 bytes long.
            @jstevensog
             */
            transmitterData.raw_data = Integer.parseInt(data[0]);
            transmitterData.timestamp = new Date().getTime();
            transmitterData.uuid = UUID.randomUUID().toString();

            transmitterData.save();
            Log.w(TAG, "Created transmitterData record with Raw value of " + transmitterData.raw_data + " at " +transmitterData.timestamp);
            return transmitterData;
        }
    }

    public static TransmitterData create(int raw_data ,int sensor_battery_level, long timestamp) {
        //randomDelay(100, 2000);
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
