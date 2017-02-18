package com.aaronlife.remoteshot;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

public class RemoteActivity extends AppCompatActivity
{
    public static final int REQUEST_DISCOVERY_PERMISSIONS = 0;

    protected TextView txtMsg;
    protected ImageView iv;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket socket;
    private InputStream in;
    private OutputStream out;

    private MyHandler handler = new MyHandler(this);

    //private String orignalName = GlobalSettings.DEFAULT_DEVICE_NAME;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.d("aarontest", "RemoveActivity create");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);
        getSupportActionBar().setTitle("遙控模式");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        iv = (ImageView)findViewById(R.id.previewPic);
        txtMsg = (TextView)findViewById(R.id.message);

        initBluetooth();

//        if(mBluetoothAdapter != null)
//            orignalName = mBluetoothAdapter.getName();

        // 0111
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
        {
            Log.d("aarontest", "Discovery permission granted.");

            startSearching();
        }
        else
        {
            Log.d("aarontest", "Discovery permission not granted.");

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_DISCOVERY_PERMISSIONS);

        }
    }

    @Override
    protected void onResume()
    {
        Log.d("aarontest", "RemoteActivity resume");
        super.onResume();


    }

    @Override
    protected void onPause()
    {
        Log.d("aarontest", "RemoveActivity pause");
        super.onPause();

        // 0111 stopDiscovery(); // 0111
        // 0111 disconnect();// 0111
    }

    @Override
    protected void onStop()
    {
        Log.d("aarontest", "RemoveActivity stop");
        super.onStop();

        stopDiscovery();
        disconnect();
        //GlobalSettings.restoreBluetoothName(orignalName);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch(requestCode)
        {
        case REQUEST_DISCOVERY_PERMISSIONS:
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Log.d("aarontest", "Got discovery permission");
                startSearching();
            }
            else
            {
                Toast.makeText(this, "無法取得藍芽掃描權限", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_remote, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if(out != null)
        {
            switch(item.getItemId())
            {
            case R.id.flash_light:
                AlertDialog.Builder myDlg = new AlertDialog.Builder(this); // MainActivity是情況改為你的Activity類別名稱

                final String[] modes =
                            new String[]{"自動", "開", "關", "補光", "照明"};

                myDlg.setItems(modes, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        switch(modes[which])
                        {
                        case "自動": setFlash(GlobalSettings.FLASH_AUTO); break;
                        case "開": setFlash(GlobalSettings.FLASH_ON); break;
                        case "關": setFlash(GlobalSettings.FLASH_OFF); break;
                        case "補光": setFlash(GlobalSettings.FLASH_FILL); break;
                        case "照明": setFlash(GlobalSettings.FLASH_TORCH); break;
                        }
                    }
                });

                myDlg.show();
                break;
            }
        }
        else
            Toast.makeText(this, "尚未連線", Toast.LENGTH_LONG).show();

        return super.onOptionsItemSelected(item);
    }


    protected void initBluetooth()
    {
        mBluetoothAdapter = GlobalSettings.initBluetooth();

        if(!mBluetoothAdapter.isEnabled())
        {
            Toast.makeText(this, "藍芽尚未開啟，請稍候再試", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    protected void startSearching()
    {
        if(mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); // 0111
        filter.addAction(BluetoothDevice.ACTION_FOUND); // 0111

        // 註冊要接收掃描結果的receiver
        registerReceiver(scanCallback, filter);

        mBluetoothAdapter.startDiscovery();

        // 搜尋藍芽裝置
        updateMessage("搜尋裝置中.....");
    }

    private BroadcastReceiver scanCallback = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
            {
                Log.d("aarontest", "Start discovering");
            }
            else if(BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice device =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.d("aarontest", "Found: " + device.getName());

                if(device.getName() != null &&
                   device.getName().equals(GlobalSettings.CAMERA_DEVICE_NAME))
                {
                    updateMessage("連線中.....");


                    //connect(device); // 0111

                    switch(device.getBondState())
                    {
                    case BluetoothDevice.BOND_NONE: // 未配對
                        try
                        {
                            Log.d("aarontest", "未配對，開始配對");
                            // 使用反射主動發起配對
                            Method createBondMethod =
                                 BluetoothDevice.class.getMethod("createBond");
                            createBondMethod.invoke(device);
                        }
                        catch(Exception e)
                        {
                            Log.d("aarontest", "Start discovering");
                        }
                        break;

                    case BluetoothDevice.BOND_BONDING:
                        Log.d("aarontest", "配對中");
                        break;
                    case BluetoothDevice.BOND_BONDED: // 已配對
                        stopDiscovery(); // 0111
                        Log.d("aarontest", "已配對，開始連接");
                        connect(device);
                        break;
                    }

                }
            }
            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                Log.d("aarontest", "Stop discoverying");
                mBluetoothAdapter.startDiscovery();
            }
            else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) // 0111
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (device.getName().equalsIgnoreCase(GlobalSettings.CAMERA_DEVICE_NAME))
                {
                    switch(device.getBondState())
                    {
                    case BluetoothDevice.BOND_NONE:
                        break;

                    case BluetoothDevice.BOND_BONDING:
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Log.d("aarontest", "已配對2，開始連接");
                        connect(device);
                        break;
                    }
                }
            }
        }
    };

    public void connect(BluetoothDevice device)
    {
        try
        {
            Log.d("aarontest", "建立連線");
            socket = device.createInsecureRfcommSocketToServiceRecord(
                                                    GlobalSettings.uuid);
            socket.connect();

            in = socket.getInputStream();
            out = socket.getOutputStream();

            // 連線成功再開始啟動接收資料執行緒
            //startRead();
            updateMessage("已連線");

            new ConnectThread(in).start();
        }
        catch(IOException e)
        {
            Log.d("aarontest", "error4: " + e.getMessage());
            disconnect();
            startSearching();
            //updateMessage("連線失敗，請重新操作！");
        }
    }

    public void disconnect()
    {
        try
        {
            if(socket != null)
            {
                socket.close();
                socket = null;
            }
        }
        catch (IOException e)
        {
            Log.d("aarontest", "error5: " + e.getMessage());
        }
    }

    protected class ConnectThread extends Thread
    {
        InputStream in;

        public ConnectThread(InputStream in)
        {
            this.in = in;
        }

        public void run()
        {
            byte[] sizeArray = new byte[4];

            try
            {
                while(true)
                {
                    int dueCount = 0;

                    while(in.available() < 4 && in.available() >= 0)
                    {
                        try
                        {
                            Thread.sleep(150);
                        }
                        catch(InterruptedException e)
                        {
                            e.printStackTrace();
                        }

                        // 每2秒測試連線是否正常
                        if(++dueCount == 10)
                        {
                            out.write(GlobalSettings.CONNECTION_TEST);
                            out.flush();
                            dueCount = 0;
                        }
                    }

                    in.read(sizeArray, 0, 4);
                    final int size = GlobalSettings.byteArrayToInt(sizeArray);

                    final byte[] dataArray = new byte[size];

                    while(in.available() < size && in.available() >= 0)
                    {
                        try
                        {
                            Thread.sleep(150);
                        }
                        catch(InterruptedException e)
                        {
                            e.printStackTrace();
                        }

                        // 每2秒測試連線是否正常
                        if(++dueCount == 10)
                        {
                            out.write(GlobalSettings.CONNECTION_TEST);
                            out.flush();
                            dueCount = 0;
                        }
                    }

                    in.read(dataArray, 0, size);
                    final Bitmap bm =
                            BitmapFactory.decodeByteArray(dataArray, 0, size);
                    Matrix m = new Matrix();
                    m.postRotate(90);
                    final Bitmap pic =
                            Bitmap.createBitmap(bm , 0, 0, bm .getWidth(),
                                                bm .getHeight(), m, true);

                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            iv.setImageBitmap(pic);
                        }
                    });
                }
            }
            catch(IOException e)
            {
                Log.d("aarontest", "error1: " + e.getMessage());
                updateMessage("已斷線");
            }
            catch(RuntimeException e)
            {
                Log.d("aarontest", "error2: " + e.getMessage());
                updateMessage("已斷線");
            }

            disconnect();
        }
    }

    public void onFocus(View v)
    {
        if(out == null)
        {
            Toast.makeText(this, "尚未連線", Toast.LENGTH_LONG).show();
            return;
        }

        try
        {
            out.write(GlobalSettings.AUTO_FOCUS);
            out.flush();
        }
        catch(IOException e)
        {
        }
    }

    public void onShot(View v)
    {
        if(out == null)
        {
            Toast.makeText(this, "尚未連線", Toast.LENGTH_LONG).show();
            return;
        }

        try
        {
            out.write(GlobalSettings.TAKE_SHOT);
            out.flush();
        }
        catch(IOException e)
        {
            Toast.makeText(this, "拍照失敗", Toast.LENGTH_LONG).show();
        }
    }

    public void setFlash(byte flashMode)
    {
        try
        {
            out.write(flashMode);
            out.flush();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public void stopDiscovery()
    {
        if(mBluetoothAdapter.isEnabled())
        {
            try
            {
                // 取消註冊的receiver
                unregisterReceiver(scanCallback);
            }
            catch(IllegalArgumentException e)
            {
                Log.d("aarontest", "error3: " + e.getMessage());
            }

            if (mBluetoothAdapter.isDiscovering())
                mBluetoothAdapter.cancelDiscovery();
        }
    }

    private static class MyHandler extends Handler
    {
        private final WeakReference<RemoteActivity> mActivity;

        public MyHandler(RemoteActivity activity)
        {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg)
        {
            RemoteActivity act = mActivity.get();

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
    }
}
