package com.aaronlife.remoteshot;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 顯示版本名稱
        TextView txtVersion = (TextView)findViewById(R.id.version);
        PackageInfo pInfo = null;
        try
        {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        }
        catch(PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        txtVersion.setText(pInfo.versionName);

        getSupportActionBar().hide();

        GlobalSettings.initBluetooth();
    }

    public void onRemoteMode(View v)
    {
        Intent it = new Intent();
        it.setClass(this, RemoteActivity.class);
        startActivity(it);
    }

    public void onCameraMode(View v)
    {
        Intent it = new Intent();
        it.setClass(this, CameraActivity.class);
        startActivity(it);
    }
}
