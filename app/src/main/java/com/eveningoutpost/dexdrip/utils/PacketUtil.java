package com.eveningoutpost.dexdrip.utils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Radu Iliescu on 26.02.2015.
 */
public class PacketUtil {
    public static int int32FromBuffer(byte data[], int offset) {
        ByteBuffer bb = ByteBuffer.wrap(data, offset, 4);
        IntBuffer sb = bb.asIntBuffer();
        return sb.get();
    }

    public static int uint32FromBuffer(byte data[], int offset) {
        int ret = int32FromBuffer(data, offset);

        if (ret < 0) {
            ret += 1 << 32;
        }

        return ret;
    }

    public static int int16FromBuffer(byte data[], int offset) {
        ByteBuffer bb = ByteBuffer.wrap(data, offset, 2);
        ShortBuffer sb = bb.asShortBuffer();
        return sb.get();
    }

    public static int uint16FromBuffer(byte data[], int offset) {
        int ret = int16FromBuffer(data, offset);

        if (ret < 0) {
            ret += 1 << 16;
        }

        return ret;
    }

    public static int uint8FromBuffer(byte data[], int offset) {
        int ret = data[offset];

        if (ret < 0) {
            ret += 1 << 8;
        }
        return ret;
    }
}
