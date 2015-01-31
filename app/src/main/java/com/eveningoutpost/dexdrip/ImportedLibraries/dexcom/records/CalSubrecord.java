package com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records;

import android.util.Log;

import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class CalSubrecord {
    private static final String TAG = CalSubrecord.class.getSimpleName();
    private Date dateEntered;
    private int calBGL;
    private int calRaw;
    private Date dateApplied;
    private byte unk;

    public CalSubrecord(byte[] packet, long displayTimeOffset) {
        int delta = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt();
        dateEntered = Utils.receiverTimeToDate(delta + displayTimeOffset);
        calBGL = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
        calRaw = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(8);
        delta = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(12);
        dateApplied = Utils.receiverTimeToDate(delta + displayTimeOffset);
        unk = packet[16];
    }

    public Date getDateEntered() {
        return dateEntered;
    }

    public int getCalBGL() {
        return calBGL;
    }

    public int getCalRaw() {
        return calRaw;
    }

    public Date getDateApplied() {
        return dateApplied;
    }

    public byte getUnk() {
        return unk;
    }
}
