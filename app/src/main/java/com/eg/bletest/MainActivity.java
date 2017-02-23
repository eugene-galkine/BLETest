package com.eg.bletest;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import static android.content.ContentValues.TAG;


public class MainActivity extends Activity {
    public static MainActivity getInstance() {
        return instance;
    }

    private static MainActivity instance;

    private BLEPeripheral blePeri;
    private CheckBox adverstiseCheckBox;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;

        adverstiseCheckBox = (CheckBox) findViewById(R.id.advertise_checkBox);

        blePeri = new BLEPeripheral();

        // Initializes Bluetooth adapter.
        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 112);
        }

        mHandler = new Handler();

        adverstiseCheckBox.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (adverstiseCheckBox.isChecked()) {

                    TextView textView;
                    textView = (TextView) findViewById(R.id.status_textView);
                    textView.setText("advertising");

                    final Handler handler = new Handler();

                    new Thread() {
                        @Override
                        public void run() {
                            blePeri.setService("GAIGER",
                                    "AndroidBLE",
                                    mWrittenCallback
                            );

                            blePeri.startAdvertise();
                        }
                    }.start();
                } else {
                    TextView textView;
                    textView = (TextView) findViewById(R.id.status_textView);
                    textView.setText("disable");
                    blePeri.stopAdvertise();
                }
            }
        });

        adverstiseCheckBox.setEnabled(false);

        if (!BLEPeripheral.isEnableBluetooth()) {

            Intent intentBtEnabled = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // The REQUEST_ENABLE_BT constant passed to startActivityForResult() is a locally defined integer (which must be greater than 0), that the system passes back to you in your onActivityResult()
            // implementation as the requestCode parameter.
            int REQUEST_ENABLE_BT = 1;
            startActivityForResult(intentBtEnabled, REQUEST_ENABLE_BT);

            Toast.makeText(this, "Please enable bluetooth and execute the application agagin.",
                    Toast.LENGTH_LONG).show();
        }

        new ScanResult(null, "rssi", (TableLayout) findViewById(R.id.table_layout));
    }

    byte[] writtenByte;
    BLEPeripheral.WriteCallback mWrittenCallback = new BLEPeripheral.WriteCallback() {
        @Override
        public void onWrite(byte[] data) {
            writtenByte = data.clone();

            Thread timer = new Thread() {
                public void run() {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView textView;
                            textView = (TextView) findViewById(R.id.written_textView);
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
            textView = (TextView) findViewById(R.id.connected_textView);
            textView.setText("no connection");
        }
    };

    Handler mConnectTextHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String data = (String) msg.obj;


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
            textView = (TextView) findViewById(R.id.connected_textView);
            textView.setText(data);
        }

    };


    @Override
    public void onResume() {

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
        blePeri.setConnectionCallback(new BLEPeripheral.ConnectionCallback() {
                                          @Override
                                          public void onConnectionStateChange(BluetoothDevice device, int newState) {

                                              Message msg = new Message();

                                              msg.what = newState;
                                              msg.obj = new String(device.getName() + " " + device.getAddress());

                                              mConnectTextHandler.sendMessage(msg);
                                          }
                                      }

        );


        if (0 > sts) {
            if (-1 == sts)
                Toast.makeText(this, "this device is without bluetooth module",
                        Toast.LENGTH_LONG).show();

            if (-2 == sts)
                Toast.makeText(this, "this device do not support Bluetooth low energy",
                        Toast.LENGTH_LONG).show();

            if (-3 == sts)
                Toast.makeText(this, "this device do not support to be a BLE peripheral",
                        Toast.LENGTH_LONG).show();

            if (sts > -3)
                finish();
            else
                adverstiseCheckBox.setEnabled(false);
        }

        TextView textView;
        textView = (TextView) findViewById(R.id.mac_textView);

        textView.setText(BLEPeripheral.getAddress());


        adverstiseCheckBox.setEnabled(true);


    }

    @Override
    protected void onPause() {
        super.onPause();

        blePeri.stopAdvertise();
    }

    public void startScanButton(View view) {
        if (mayUseLocation()) {
            ScanResult.getInstance().clear();
            scanLeDevice(true);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, 3000);

            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    System.out.println("Found BLE Server: " + device.getName());
                    System.out.println(device.getAddress());
                    System.out.println(rssi);
                    //for (byte)
                    System.out.println(new String(scanRecord));

                    ScanResult.getInstance().addRow(device, rssi + "");
                }
            };

    public boolean mayUseLocation() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 10);

        return false;
    }

    public ViewGroup.LayoutParams getColParams() {
        return findViewById(R.id.textView).getLayoutParams();
    }

    // Various callback methods defined by the BLE API.
    public final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //intentAction = ACTION_GATT_CONNECTED;
                //mConnectionState = STATE_CONNECTED;
                //broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");

                displayGattServices(gatt.getServices());

                //Log.i(TAG, "Attempting to start service discovery:" +
                //      mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected to GATT server.");
                //intentAction = ACTION_GATT_DISCONNECTED;
                //mConnectionState = STATE_DISCONNECTED;//Log.i(TAG, "Disconnected from GATT server.");
                //broadcastUpdate(intentAction);
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            Log.w(TAG, "onServicesDiscovered received: " + status);

            displayGattServices(gatt.getServices());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                // Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Read Characteristic: " + characteristic.getUuid().toString());
            }
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices)
    {
        Log.d(TAG, "GATT Services: " + gattServices);

        if (gattServices == null) return;
        String uuid = null;
        //String unknownServiceString = "unknown";
        //String unknownCharaString = "unknown characteristic";
        //ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        //ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        //mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices)
        {
            Log.d(TAG, "Service UUID: " + gattService.getUuid().toString());
            //Log.d(TAG, "Service: " + gattService.);
            //HashMap<String, String> currentServiceData = new HashMap<String, String>();
            //uuid = gattService.getUuid().toString();
            //currentServiceData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            //currentServiceData.put(LIST_UUID, uuid);
            //gattServiceData.add(currentServiceData);

            //ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            //ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)
            {
                Log.d(TAG, "String Value: " + gattCharacteristic.getStringValue(0));
                //charas.add(gattCharacteristic);
               // HashMap<String, String> currentCharaData = new HashMap<String, String>();
               // uuid = gattCharacteristic.getUuid().toString();
               // currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
               // currentCharaData.put(LIST_UUID, uuid);
                //gattCharacteristicGroupData.add(currentCharaData);
            }
            //mGattCharacteristics.add(charas);
            //gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }
}
