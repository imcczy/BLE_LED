package com.example.LED;


import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import android.view.View.OnTouchListener;

import java.util.*;

/**
 * Created by imcczy on 14-3-11.
 */
public class write extends Activity {
    private final static String TAG = write.class.getSimpleName();
    public BluetoothLeService mBluetoothLeService;
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String mDeviceAddress;
    private boolean mConnected = false;
    private ImageView imageView;
    private Bitmap bitmap;
    private boolean sss = true;
    private Timer mTimer;
    AlarmManager aManager;
    private byte[] ON = {(byte)0xff,(byte)0xff,(byte)0xff};
    private byte[] OFF = {(byte)0x00,(byte)0x00,(byte)0x00};
    private String RGB = "0000fff1-0000-1000-8000-00805f9b34fb";
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;

                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;

                invalidateOptionsMenu();

            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.write);

        mDeviceAddress ="D0:39:72:A1:C5:83";

        aManager = (AlarmManager) getSystemService(
                Service.ALARM_SERVICE);
        imageView = (ImageView) findViewById(R.id.rgb);
        BitmapDrawable db = (BitmapDrawable) getResources().getDrawable(R.drawable.rgb);
        bitmap = db.getBitmap();
        Button setTime = (Button) findViewById(R.id.setTime);
        Button ledOn = (Button) findViewById(R.id.ledOn);
        Button ledOff = (Button) findViewById(R.id.ledOff);
        setTime.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               final Calendar currentTime = Calendar.getInstance();
               // Create a TimePickerDialog instance，and display it
               new TimePickerDialog(write.this, 0, // Binding a listener
                       new TimePickerDialog.OnTimeSetListener()
                       {
                           @Override
                           public void onTimeSet(TimePicker tp,
                                                 int hourOfDay, int minute)
                           {

                               Intent intent = new Intent(write.this,
                                       write.class);
                               // Create PendingIntent object
                               PendingIntent pi = PendingIntent.getActivity(
                                       write.this, 0, intent, 0);
                               Calendar c = Calendar.getInstance();
                               //c.setTimeInMillis(System.currentTimeMillis());

                               c.set(Calendar.HOUR_OF_DAY,hourOfDay );
                               c.set(Calendar.MINUTE, minute);
                               if(hourOfDay < currentTime.get(Calendar.HOUR_OF_DAY)){
                                   c.add(Calendar.DAY_OF_MONTH,1);
                               }

                               aManager.set(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);

                               Toast.makeText(write.this, "Setup is successful!"
                                       , Toast.LENGTH_SHORT).show();
                           }
                       }, currentTime.get(Calendar.HOUR_OF_DAY), currentTime
                       .get(Calendar.MINUTE),true).show();
            }
       });
        ledOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothLeService.writebyte(UUID.fromString(RGB), ON);
                sss = true;
            }
        });
        ledOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothLeService.writebyte(UUID.fromString(RGB), OFF);
                sss = false;
            }
        });
        imageView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getX();
                int y = (int) event.getY();
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int color = bitmap.getPixel(x, y);
                    int r = Color.red(color);
                    int b = Color.blue(color);
                    int g = Color.green(color);
                    String rgb = Integer.toHexString((r & 0x000000FF) | 0xFFFFFF00).substring(6) +
                            Integer.toHexString((g & 0x000000FF) | 0xFFFFFF00).substring(6) +
                            Integer.toHexString((b & 0x000000FF) | 0xFFFFFF00).substring(6);
                    byte[] data = hex2byte(rgb.getBytes());
                    if (sss) {
                        mBluetoothLeService.writebyte(UUID.fromString(RGB), data);
                    }
                }
                return true;
            }
        });

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {

                mBluetoothLeService.writebyte(UUID.fromString(RGB), ON);
                sss = true;
            }
        }, 2000);
    }
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.connect, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;

    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
    public static byte[] hex2byte(byte[] b) {
        if ((b.length % 2) != 0) {
            throw new IllegalArgumentException("The length is not even");
        }
        byte[] b2 = new byte[b.length / 2];
        for (int n = 0; n < b.length; n += 2) {
            String item = new String(b, n, 2);
            b2[n / 2] = (byte) Integer.parseInt(item, 16);
        }
        b = null;
        return b2;
    }
}


