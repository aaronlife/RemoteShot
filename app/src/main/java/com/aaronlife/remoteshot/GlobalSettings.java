package com.aaronlife.remoteshot;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import java.util.UUID;

/**
 * Created by aaron on 8/7/16.
 */
public class GlobalSettings
{
    public final static UUID uuid = UUID.fromString("a60f35f0-b93a-11de-8a39-08002009c666");

    public final static byte CONNECTION_TEST = 0x00;
    public final static byte TAKE_SHOT = 0x01;
    public final static byte FLASH_ON = 0x02;
    public final static byte FLASH_OFF = 0x03;
    public final static byte FLASH_AUTO = 0x04;
    public final static byte FLASH_FILL = 0x05;
    public final static byte FLASH_TORCH = 0x06;
    public final static byte AUTO_FOCUS = 0x07;

    public final static int UPDATE_MESSAGE = 0;

    public final static String REMOTE_DEVICE_NAME = "remoteshot.remote";
    public final static String CAMERA_DEVICE_NAME = "remoteshot.camera";
    public final static String DEFAULT_DEVICE_NAME = "remoteshot.default";


    public static int byteArrayToInt(byte[] b)
    {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }

    public static byte[] intToByteArray(int a)
    {
        byte[] ret = new byte[4];
        ret[3] = (byte) (a & 0xFF);
        ret[2] = (byte) ((a >> 8) & 0xFF);
        ret[1] = (byte) ((a >> 16) & 0xFF);
        ret[0] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }

    public static BluetoothAdapter initBluetooth()
    {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Log.e("aarontest", "無法取得藍芽裝置");
        }
        else
        {
            int count = 0;
            while(!mBluetoothAdapter.isEnabled())
            {
                mBluetoothAdapter.enable();

                try
                {
                    Thread.sleep(100);
                }
                catch(InterruptedException e)
                {
                    e.printStackTrace();
                }

                if(count++ > 5) break;
            }
        }

        return mBluetoothAdapter;
    }

    public static void restoreBluetoothName(String orignalName)
    {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter != null)
        {
            if(orignalName.equals(GlobalSettings.CAMERA_DEVICE_NAME) ||
                    orignalName.equals(GlobalSettings.REMOTE_DEVICE_NAME))
                bluetoothAdapter.setName(GlobalSettings.DEFAULT_DEVICE_NAME);
            else
                bluetoothAdapter.setName(orignalName);
        }
    }
}
