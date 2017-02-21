package com.eg.bletest;

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

    public ScanResult(String addressText, String nameText, String rssi, TableLayout table)
    {
        this.table = table;
        instance = this;
        rootRow = new Row (addressText, nameText, rssi);

    }

    public void addRow(String address, String name, String s)
    {
        Row r = exists(address);

        if (r == null)
        {
            r = new Row(address, name, s);
            getLast().setNext(r);
            table.addView(r.row);
        } else
        {
            r.update(address, name, s);
        }

    }

    public Row exists(String address)
    {
        Row index = rootRow;
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

        public Row(String addressText, String nameText, String rssiText) {
            row = new TableRow(MainActivity.getInstance());
            row.setWeightSum(4);

            address = new TextView(MainActivity.getInstance());
            address.setText(addressText);
            address.setLayoutParams(MainActivity.getInstance().getColParams());
            row.addView(address);

            name = new TextView(MainActivity.getInstance());
            name.setText(nameText == null ? "unknown" : nameText);
            name.setLayoutParams(MainActivity.getInstance().getColParams());
            row.addView(name);

            rssi = new TextView(MainActivity.getInstance());
            rssi.setText(rssiText);
            rssi.setLayoutParams(MainActivity.getInstance().getColParams());
            row.addView(rssi);

            Button button = new Button(MainActivity.getInstance());
            button.setText("Connect");
            button.setLayoutParams(MainActivity.getInstance().getColParams());
            row.addView(button);
        }


        public void setNext(Row row) {
            next = row;
        }

        public Row getNext() {
            return next;
        }

        public boolean equals(String text) {
            return address.getText().toString().equals(text);
        }

        public void update(String addressText, String nameText, String rssiText)
        {
            address.setText(addressText);
            name.setText(nameText == null ? "unknown" : nameText);
            rssi.setText(rssiText);
        }

    }
}
