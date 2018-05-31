package com.example.position2shp;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static com.esri.arcgisruntime.internal.jni.av.df;

/**
 * Created by Franziska on 28.05.2018.
 */
public class SettingsActivity extends Activity implements AdapterView.OnItemSelectedListener {

    EditText edtText;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);

        edtText = (EditText) findViewById(R.id.editText3);

        String filepath = Environment.getExternalStorageDirectory().getPath() +
                getResources().getString(R.string.BasicDir) +
                getResources().getString(R.string.UserShapefiles);

        List<String> fileList = new ArrayList<String>();
        File f = new File(filepath);
        File[] files = f.listFiles();
        for (File inFile : files) {
            if (inFile.isDirectory()) {
                File[] files2 = inFile.listFiles();
                for (File inFile2 : files2){
                    if (inFile2.isFile() && inFile2.getAbsolutePath().contains(".shp"))
                        fileList.add(inFile2.getName());
                }

            }
        }

        filepath = Environment.getExternalStorageDirectory().getPath() +
                getResources().getString(R.string.BasicDir) +
                getResources().getString(R.string.NewShapefiles);

        f = new File(filepath);
        files = f.listFiles();
        for (File inFile : files) {
            if (inFile.isDirectory()) {
                File[] files2 = inFile.listFiles();
                for (File inFile2 : files2){
                    if (inFile2.isFile() && inFile2.getAbsolutePath().contains(".shp"))
                        fileList.add(inFile2.getName());
                }

            }
        }

        String[] strings = new String[fileList.size()];
        strings = fileList.toArray(strings);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_item, strings);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout
                .simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerArrayAdapter);

        Spinner spinner2 = (Spinner) findViewById(R.id.spinner2);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
        R.array.shapefile_type, android.R.layout.simple_spinner_item);
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner2.setAdapter(adapter2);
        spinner2.setOnItemSelectedListener(this);
    }

    public void copy(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    public void onEditEmpty(View view)
    {
        String copyTofilepath = Environment.getExternalStorageDirectory().getPath() +
                getResources().getString(R.string.BasicDir) +
                getResources().getString(R.string.NewShapefiles) +
                edtText.getText().toString() + "/";

        String copiedShp = Environment.getExternalStorageDirectory().getPath() +
                getResources().getString(R.string.BasicDir) +
                getResources().getString(R.string.BasicShapefiles);

        if(MainActivity.actualShpType == MainActivity.ShapefileTypes.Point)
        {
            copiedShp += getResources().getString(R.string.BasicPointDir);
        }
        else if(MainActivity.actualShpType == MainActivity.ShapefileTypes.Polygon)
        {
            copiedShp += getResources().getString(R.string.BasicPolygonDir);
        }
        else
        {
            copiedShp += getResources().getString(R.string.BasicLineDir);
        }

        File dest = new File(copyTofilepath);
        dest.mkdirs();
        File source = new File(copiedShp);

        if (dest.exists() && dest.isDirectory() && source.exists()) {
            File[] files = source.listFiles();
            for (File f : files) {
                try {
                    String filePath = f.getPath();
                    String fileEnding = filePath.substring( filePath.lastIndexOf('.'));
                    File newFile = new File(copyTofilepath + edtText.getText().toString() + fileEnding);
                    copy(f, newFile);
                    if (fileEnding.equals(".shp"))
                        MainActivity.mShapefilePath2 = newFile.getAbsolutePath();
                } catch (Exception ex) {
                }
            }
        }
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        switch (parent.getId()) {
            case R.id.spinner2:
                String type = parent.getItemAtPosition(pos).toString();
                switch(type) {
                    case "Points":
                        MainActivity.actualShpType = MainActivity.ShapefileTypes.Point;
                        break;
                    case "Polyline":
                        MainActivity.actualShpType = MainActivity.ShapefileTypes.Line;
                        break;
                    case "Polygon":
                        MainActivity.actualShpType = MainActivity.ShapefileTypes.Polygon;
                        break;
                }
                break;
        }
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

}
