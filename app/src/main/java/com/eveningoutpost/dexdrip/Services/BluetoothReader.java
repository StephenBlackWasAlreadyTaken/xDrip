package com.eveningoutpost.dexdrip.Services;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.UtilityModels.HC05Attributes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * Created by Radu Iliescu on 1/24/2015.
 *
 * Bluetooth (nonBLE) reader service
 */
public class BluetoothReader extends Thread {
    private final static String TAG = BluetoothReader.class.getName();
    private final static int minReadSize = 8;

    private static BluetoothReader singleton = null;
    private Context mContext;

    public synchronized static BluetoothReader startBluetoothReader(BluetoothDevice device, Context context) {
        if (singleton == null) {
            singleton = new BluetoothReader();
        }

        singleton.mDevice = device;
        singleton.mContext = context;
        singleton.restart();

        return singleton;
    }

    private BluetoothDevice mDevice;
    private BluetoothSocket mSocket;

    private UUID uuid = UUID.fromString(HC05Attributes.UUIDString);
    private boolean isRunning = false;

    private void restart() {
        if (isRunning) {
            bluetooth_stop();
        } else {
            this.start();
        }
    }

    /* data format "len raw dex_battery wixelbattery"
     * where len in the length of string "raw dex_battery wixel_battery"
     */
    private void readDataFromStream(InputStream stream) throws  IOException{
        byte[] buffer = new byte[100];
        int totalRead = 0;
        int readSize;
        String str;
        Arrays.fill(buffer, (byte)0);
        /* read the len -> first number until " " */
        do {
            totalRead += stream.read(buffer, totalRead, 1);
            str = new String(buffer, 0, totalRead);
        } while (!str.contains(" "));

        readSize = Integer.parseInt(str.substring(0, totalRead - 1));
        totalRead = 0;
        buffer = new byte[readSize];

        while (totalRead < readSize) {
            totalRead += stream.read(buffer, totalRead, readSize - totalRead);
        }
        str = new String(buffer, "UTF-8");
        Log.d(TAG, "received data size: "  + readSize + " data: " + str);

        TransmitterData transmitterData = TransmitterData.create(buffer, totalRead);
        if (transmitterData != null) {
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                BgReading.create(transmitterData.raw_data, mContext);
                sensor.latest_battery_level = transmitterData.sensor_battery_level;
                sensor.save();
            } else {
                Log.w(TAG, "No Active Sensor, Data only stored in Transmitter Data");
            }
        }
    }

    public void run() {
        InputStream stream = null;
        boolean exception = true;
        isRunning = true;

        while (isRunning && !interrupted()) {
            try {
                if (exception) {
                    connect();
                    stream = mSocket.getInputStream();
                }

                exception = false;
                readDataFromStream(stream);
            } catch (IOException e) {
                Log.i(TAG, "bluetooth exception " + e);
                exception = true;
            }

            try {
                sleep(5000);
            } catch (InterruptedException e) {}
        }
        Log.i(TAG, "Bluetooth reader exited");
        isRunning = false;
    }

    private void bluetooth_stop() {
        try {
            mSocket.close();
        } catch ( IOException e) { }
    }

    private void connect() throws IOException {
        BluetoothSocket tmp = null;

        if (mDevice == null) {
            Log.e(TAG, "Can't connect no device selected");
            throw new IOException("No bluetooth device");
        }
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        mSocket = mDevice.createRfcommSocketToServiceRecord(uuid);
        mSocket.connect();

        Log.d(TAG, "connected to bluetooth device");
    }
}
