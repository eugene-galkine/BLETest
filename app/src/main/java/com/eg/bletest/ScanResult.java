package com.eg.bletest;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Created by Eugene on 2/20/2017.
 */

public class ScanResult
{
    public static ScanResult getInstance()
    {
        return instance;
    }

    private static ScanResult instance;
    private Row rootRow;
    private TableLayout table;

    public ScanResult(BluetoothDevice device, String rssi, TableLayout table)
    {
        this.table = table;
        instance = this;
        rootRow = new Row (device, rssi);

    }

    public void addRow(BluetoothDevice device, String s)
    {
        Row r = exists(device.getAddress());

        if (r == null)
        {
            r = new Row(device, s);
            getLast().setNext(r);
            table.addView(r.row);
        } else
        {
            r.update(device.getAddress(), device.getName(), s);
        }

    }

    public Row exists(String address)
    {
        Row index = rootRow == null ? null : rootRow.getNext();




        while (index != null)
        {
            if (index.equals(address))
                return index;

            index = index.getNext();
        }

        return null;
    }

    public Row getLast()
    {
        Row index = rootRow;
        if (index == null)
            return null;

        while (index.getNext() != null)
            index = index.getNext();

        return index;
    }

    public void clear()
    {
        Row index = rootRow;
        index = index.getNext();
        rootRow.next = null;

        while (index != null)
        {
            table.removeView(index.row);

            index = index.getNext();
        }
    }

    class Row {
        private Row next = null;
        private TableRow row;
        private TextView address;
        private TextView name;
        private TextView rssi;

        private Row(final BluetoothDevice device, String rssiText)
        {
            if (device == null)
                return;

            row = new TableRow(MainActivity.getInstance());
            row.setWeightSum(4);

            address = new TextView(MainActivity.getInstance());
            address.setText(device.getAddress());
            address.setLayoutParams(MainActivity.getInstance().getColParams());
            row.addView(address);

            name = new TextView(MainActivity.getInstance());
            name.setText(device.getName() == null ? "null" : device.getName());
            name.setLayoutParams(MainActivity.getInstance().getColParams());
            row.addView(name);

            rssi = new TextView(MainActivity.getInstance());
            rssi.setText(rssiText);
            rssi.setLayoutParams(MainActivity.getInstance().getColParams());
            row.addView(rssi);

            Button button = new Button(MainActivity.getInstance());
            button.setText("Connect");
            button.setLayoutParams(MainActivity.getInstance().getColParams());
            button.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    device.connectGatt(MainActivity.getInstance(), false, MainActivity.getInstance().mGattCallback);
                }
            });
            row.addView(button);
        }


        public void setNext(Row row) {
            next = row;
        }

        public Row getNext() {
            return next;
        }

        public boolean equals(String text)
        {
            return address == null || address.getText().toString().equals(text);
        }

        public void update(String addressText, String nameText, String rssiText)
        {
            if (address == null)
                return;

            address.setText(addressText);
            name.setText(nameText == null ? "null" : nameText);
            rssi.setText(rssiText);
        }

    }
}
