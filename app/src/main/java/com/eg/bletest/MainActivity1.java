package com.eg.bletest;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.util.HashMap;
import java.util.UUID;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

public class MainActivity1 extends AppCompatActivity
{
    private static final int REQUEST_ENABLE_BT = 12;
    private static final int REQUEST_COURSE_LOCATION = 0;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BLEPeripheral blePeri;
    private Handler mHandler;
    private boolean mScanning;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, "This device does not support Bluetooth Smart", Toast.LENGTH_LONG).show();
            finish();
        }

        // Initializes Bluetooth adapter.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mScanning = false;
        mHandler = new Handler();
        blePeri = new BLEPeripheral();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        scanLeDevice(false);
        blePeri.stopAdvertise();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        int sts;
        sts = blePeri.init(this);
//
        blePeri.setConnectionCallback( new BLEPeripheral.ConnectionCallback ()
        {
               @Override
               public void onConnectionStateChange(BluetoothDevice device, int newState)
               {
                   //Toast.makeText(MainActivity.this, device.getName() +" "+ device.getAddress(), Toast.LENGTH_LONG).show();
               }
           }

        );



        if(0  > sts)
        {
            if(-1 == sts)
                Toast.makeText(this, "this device is without bluetooth module",
                        Toast.LENGTH_LONG).show();

            if(-2 == sts)
                Toast.makeText(this, "this device do not support Bluetooth low energy",
                        Toast.LENGTH_LONG).show();

            if(-3 == sts)
                Toast.makeText(this, "this device do not support to be a BLE peripheral, " +
                                "please buy nexus 6 or 9 then try again",
                        Toast.LENGTH_LONG).show();

            finish();
        }

        System.out.println("BLE Address: " + BLEPeripheral.getAddress());
    }

    public void startScanButton(View view)
    {
        if (!mayUseLocation())
        {
            return;
        }

        scanLeDevice(true);
    }

    public void startServerButton(View view)
    {
        if (!mayUseLocation())
        {
            return;
        }

        blePeri.stopAdvertise();



        blePeri.setService("GAIGER", "AndroidBLE", mWrittenCallback);

        blePeri.startAdvertise();


    }

    byte[]  writtenByte;
    BLEPeripheral.WriteCallback mWrittenCallback = new BLEPeripheral.WriteCallback()
    {
        @Override
        public
        void onWrite(byte[] data)
        {
            writtenByte = data.clone();


            try {
                System.out.println(new String(writtenByte, "UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    };

    private void scanLeDevice(final boolean enable)
    {
        if (enable)
        {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, 1000);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else
        {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback()
            {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            System.out.println("Found BLE Server:");
                            if (device.getUuids() != null)
                                for (ParcelUuid uuid : device.getUuids())
                                    System.out.println(uuid.toString());

                        }
                    });
                }
            };

    private boolean mayUseLocation()
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
        {
            return true;
        }
        if (checkSelfPermission(ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        if (shouldShowRequestPermissionRationale(ACCESS_COARSE_LOCATION))
        {
            /*Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener()
                    {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v)
                        {
                            requestPermissions(new String[]{ACCESS_COARSE_LOCATION}, REQUEST_COURSE_LOCATION);
                        }
                    });*/
            requestPermissions(new String[]{ACCESS_COARSE_LOCATION}, REQUEST_COURSE_LOCATION);
            System.out.println("SHOULD SHOW RATIONALE");
        } else
        {
            requestPermissions(new String[]{ACCESS_COARSE_LOCATION}, REQUEST_COURSE_LOCATION);
        }
        return false;
    }
}
