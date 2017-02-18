package com.aaronlife.remoteshot;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;


public class CameraActivity extends AppCompatActivity
{
    public static final int REQUEST_CAMERA_PERMISSIONS = 0;
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSIONS = 1;

    private TextView txtMsg;
    private CameraView pw;

    private BluetoothAdapter mBluetoothAdapter;
    private InputStream in;
    private OutputStream out;

    private MyHandler handler = new MyHandler(this);

    private AcceptThread thread;

    private String orignalName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d("aarontest", "CameraActivity Create");
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_camera);
        getSupportActionBar().setTitle("相機模式");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initBluetooth();

        if(mBluetoothAdapter != null)
            orignalName = mBluetoothAdapter.getName();
    }

    @Override
    protected void onResume()
    {
        Log.d("aarontest", "CameraActivity resume");
        super.onResume();

        // Assume thisActivity is the current activity
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED)
        {
            Log.d("aarontest", "Camera permission granted.");
            startWaitting();
        }
        else
        {
            Log.d("aarontest", "Camera permission not granted.");

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSIONS);
        }
    }

    @Override
    protected void onPause()
    {
        Log.d("aarontest", "CameraActivity pause");
        super.onPause();

        if(thread != null)
        {
            thread.close();
            thread.interrupt();
            thread = null;
        }
    }

    @Override
    protected void onStop()
    {
        Log.d("aarontest", "CameraActivity stop");
        super.onStop();

        GlobalSettings.restoreBluetoothName(orignalName);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch(requestCode)
        {
        case REQUEST_CAMERA_PERMISSIONS:
            if (grantResults.length == 1 && grantResults[0] ==
                                        PackageManager.PERMISSION_GRANTED)
            {
                Log.d("aarontest", "Got camera permission");
                startWaitting();
            }
            else
            {
                Toast.makeText(this, "無法取得相機權限", Toast.LENGTH_LONG).show();
                finish();
            }
            break;

        case REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSIONS:
            if (grantResults.length == 1 && grantResults[0] ==
                                        PackageManager.PERMISSION_GRANTED)
            {
                Log.d("aarontest", "Got write permission");
            }
            else
            {
                Toast.makeText(this, "無法取得寫入權限", Toast.LENGTH_LONG).show();
            }
            break;
        }
    }

    protected void initBluetooth()
    {
        mBluetoothAdapter = GlobalSettings.initBluetooth();

        if(!mBluetoothAdapter.isEnabled())
        {
            Toast.makeText(this, "藍芽尚未開啟，請稍候再試", Toast.LENGTH_LONG).show();
            finish();
        }
        else
        {
            mBluetoothAdapter.setName(GlobalSettings.CAMERA_DEVICE_NAME);

            // 開始廣播（4.3或以上0代表無期限，4.1為1小時），最高為3600秒，如果超過3600或小於0會自動設為120
            Intent it = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            it.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(it);
        }
    }

    protected void startWaitting()
    {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
        {
            Log.d("aarontest", "Write permission granted.");
        }
        else
        {
            Log.d("aarontest", "Write permission not granted.");

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSIONS);
        }

        setContentView(R.layout.activity_camera);
        pw = (CameraView)findViewById(R.id.previewView);
        txtMsg = (TextView)findViewById(R.id.message);

        if(mBluetoothAdapter.isEnabled())
        {
            mBluetoothAdapter.setName(GlobalSettings.CAMERA_DEVICE_NAME);

            thread = new AcceptThread();
            thread.start();
        }
    }

    private class AcceptThread extends Thread
    {
        private BluetoothServerSocket mServerSocket = null;
        private BluetoothSocket socket = null;

        public AcceptThread()
        {
            try
            {
                mServerSocket = mBluetoothAdapter
                        .listenUsingRfcommWithServiceRecord(
                                "aaronlife", GlobalSettings.uuid);
            }
            catch (IOException e)
            {
                Log.d("aarontest", "listen() failed: " + e.getMessage());
            }
        }

        public void run()
        {
            try
            {
                updateMessage("等待連線.....");

                socket = mServerSocket.accept();

                in = socket.getInputStream();
                out = socket.getOutputStream();

                pw.setOutputStream(out);

                updateMessage("已連線（閃光燈：自動）");

                while(true)
                {
                    if(in.available() > 0)
                    {
                        int cmd = in.read();

                        switch(cmd)
                        {
                        case GlobalSettings.TAKE_SHOT:
                            if(pw != null) pw.takeShot();
                            break;
                        case GlobalSettings.FLASH_ON:
                            if(pw != null)
                            {
                                if(pw.turnOnFlash())
                                    updateMessage("已連線（閃光燈：開）");
                                else
                                    updateMessage("已連線（閃光燈：無）");
                            }
                            break;
                        case GlobalSettings.FLASH_OFF:
                            if(pw != null)
                            {
                                if(pw.trunOffFlash())
                                    updateMessage("已連線（閃光燈：關）");
                                else
                                    updateMessage("已連線（閃光燈：無）");
                            }
                            break;
                        case GlobalSettings.FLASH_AUTO:
                            if(pw != null)
                            {
                                if(pw.trunAutoFlash())
                                    updateMessage("已連線（閃光燈：自動）");
                                else
                                    updateMessage("已連線（閃光燈：無）");
                            }
                            break;
                        case GlobalSettings.FLASH_FILL:
                            if(pw != null)
                            {
                                if(pw.trunFillFlash())
                                    updateMessage("已連線（閃光燈：補光）");
                                else
                                    updateMessage("已連線（補光：不支援）");
                            }
                            break;
                        case GlobalSettings.FLASH_TORCH:
                            if(pw != null)
                            {
                                if(pw.trunTorchFlash())
                                    updateMessage("已連線（閃光燈：照明）");
                                else
                                    updateMessage("已連線（照明：不支援）");
                            }
                            break;
                        case GlobalSettings.AUTO_FOCUS:
                            if(pw != null) pw.autoFocus();
                            break;
                        }
                    }

                    try
                    {
                        Thread.sleep(500);
                    }
                    catch(InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            catch (IOException e)
            {
                Log.d("aarontest", "Socket error: " + e.getMessage());
                updateMessage("已斷線");
            }
            finally
            {
                close();
            }
        }

        public void close()
        {
            try
            {
                if(in != null) in.close();
                if(out != null) out.close();
                if(socket != null) socket.close();
                if(mServerSocket != null) mServerSocket.close();
                Log.d("aarontest", "Socket closed.");
            }
            catch (Exception e)
            {
                Log.d("aarontest", "Close error: " + e.getMessage());
            }
        }
    }

    private static class MyHandler extends Handler
    {
        private final WeakReference<CameraActivity> mActivity;

        public MyHandler(CameraActivity activity)
        {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            CameraActivity act = mActivity.get();

            if(act != null)
            {
                switch(msg.what)
                {
                case GlobalSettings.UPDATE_MESSAGE:
                    String message = msg.getData().getString("message");
                    act.txtMsg.setText(message);
                    break;
                }
            }
        }
    }

    public void updateMessage(String message)
    {
        Message msg = new Message();
        msg.what = GlobalSettings.UPDATE_MESSAGE;

        Bundle bundle = new Bundle();
        bundle.putString("message", message);
        msg.setData(bundle);

        handler.sendMessage(msg);
        Log.d("aarontest", message);
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();

        finish();
    }
}
