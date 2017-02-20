package com.eg.bletest;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

    private BLEPeripheral blePeri;
    private CheckBox  adverstiseCheckBox;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adverstiseCheckBox = (CheckBox) findViewById(R.id.advertise_checkBox);

        blePeri = new BLEPeripheral();

        // Initializes Bluetooth adapter.
        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 112);
        }

        mHandler = new Handler();

        adverstiseCheckBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if(adverstiseCheckBox.isChecked())
                {

                    TextView textView;
                    textView = (TextView)findViewById(R.id.status_textView);
                    textView.setText("advertising");

                    new Thread()
                    {
                        @Override
                        public void run()
                        {
                            blePeri.setService("GAIGER",
                                    "AndroidBLE",
                                    mWrittenCallback
                            );

                            blePeri.startAdvertise();
                        }
                    };
                }
                else
                {
                    TextView textView;
                    textView = (TextView)findViewById(R.id.status_textView);
                    textView.setText("disable");
                    blePeri.stopAdvertise();
                }
            }
        });

        adverstiseCheckBox.setEnabled(false);

        if(false == BLEPeripheral.isEnableBluetooth())
        {

            Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // The REQUEST_ENABLE_BT constant passed to startActivityForResult() is a locally defined integer (which must be greater than 0), that the system passes back to you in your onActivityResult()
            // implementation as the requestCode parameter.
            int REQUEST_ENABLE_BT = 1;
            startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);

            Toast.makeText(this, "Please enable bluetooth and execute the application agagin.",
                    Toast.LENGTH_LONG).show();
        }

    }


    byte[]  writtenByte;
    BLEPeripheral.WriteCallback mWrittenCallback = new BLEPeripheral.WriteCallback()
    {
        @Override
        public
        void onWrite(byte[] data)
        {
            writtenByte = data.clone();

            Thread timer = new Thread(){
                public void run() {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView textView;
                            textView = (TextView)findViewById(R.id.written_textView);
                            try {
                                textView.setText(new String(writtenByte, "UTF-8"));
                            } catch (UnsupportedEncodingException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    });
                }

            };

            timer.start();


        }

    };




    Runnable mCleanTextRunnable = new Runnable() {
        public void run() {
            TextView textView;
            textView = (TextView)findViewById(R.id.connected_textView);
            textView.setText("no connection");
        }
    };

    Handler mConnectTextHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String data = (String)msg.obj;


            switch (msg.what) {

                case 0:
                    data += new String(" disconnected");
                    this.postDelayed(mCleanTextRunnable, 3000);
                    break;

                case 2:
                    data += new String(" connected");
                    break;
                default:
                    break;
            }

            TextView textView;
            textView = (TextView)findViewById(R.id.connected_textView);
            textView.setText(data);
        }

    };


    @Override
    public void onResume(){

        super.onResume();

        int sts;
        sts = blePeri.init(this);

//        blePeri.mConnectionCallback = new BLEPeripheral.ConnectionCallback (){
//         @Override
//      public void onConnectionStateChange(BluetoothDevice device, int newState){
//       Log.d("main","onConnectionStateChange");
//      }
//        };
//
        blePeri.setConnectionCallback( new BLEPeripheral.ConnectionCallback (){
                                           @Override
                                           public void onConnectionStateChange(BluetoothDevice device, int newState){

                                               Message msg = new Message();

                                               msg.what = newState;
                                               msg.obj = new String( device.getName() +" "+ device.getAddress() );

                                               mConnectTextHandler.sendMessage(msg);
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
                Toast.makeText(this, "this device do not support to be a BLE peripheral",
                        Toast.LENGTH_LONG).show();

            if (sts > -3)
                finish();
            else
                adverstiseCheckBox.setEnabled(false);
        }

        TextView textView;
        textView = (TextView)findViewById(R.id.mac_textView);

        textView.setText(BLEPeripheral.getAddress());


        adverstiseCheckBox.setEnabled(true);


    }

    @Override
    protected void onPause() {
        super.onPause();

        blePeri.stopAdvertise();
    }

    public void startScanButton(View view)
    {
        if (mayUseLocation())
            scanLeDevice(true);
    }

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
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, 3000);

            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else
        {
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
                            System.out.println("Found BLE Server:");
                            System.out.print(device.getName());
                }
            };

    public boolean mayUseLocation()
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
        {
            return true;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }

        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 10);

        return false;
    }
}
