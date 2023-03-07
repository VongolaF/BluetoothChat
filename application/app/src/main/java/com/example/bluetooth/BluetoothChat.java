package com.example.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.projectapplication.R;

public class BluetoothChat extends AppCompatActivity {

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    private static final int REQUEST_CONNECT_DEVICE = 1;  //请求连接设备
    private static final int REQUEST_ENABLE_BT = 2;
    private TextView mTitle;
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private StringBuffer mOutStringBuffer;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothConnection mBluetoothConnection = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();                   //hide the title bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        }

//        Toolbar toolbar = findViewById(R.id.toolbar);
//        toolbar.inflateMenu(R.menu.option_menu);        //create menu
//        toolbar.setOnMenuItemClickListener(new MyMenuItemClickListener());
//        mTitle = findViewById(R.id.title_left_text);
//        mTitle.setText(R.string.app_name);
//        mTitle = findViewById(R.id.title_right_text);

        //get Bluetooth adapter
        checkBlePermission();
        checkBleDevice();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Blue", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) { //若当前设备蓝牙功能未开启
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT); //
        } else {
            if (mBluetoothConnection== null) {
                setupChat();  //创建会话
            }
        }
    }

    //check the permission of Bluetooth
    public void checkBlePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    1);
        } else {
            Log.i("tag", "Already get the permission");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                // if the permission is canceled, the array is null
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("tag", "agree to apply");
                } else {
                    Log.i("tag", "deny to apply");
                }
                return;
            }
        }
    }

    private void checkBleDevice() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        // get BluetoothAdapter
        if (bluetoothManager != null) {
            BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
            if (mBluetoothAdapter != null) {
                if (!mBluetoothAdapter.isEnabled()) {
                    //call the enable() method to turn on the bluetooth
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    if (!mBluetoothAdapter.enable()) {
                        Log.i("tag", "fail to turn on Bluetooth");
                    } else {
                        Log.i("tag", "Bluetooth has been turned on");
                    }
                    //Use this method to turn on the Bluetooth via pop-up window
//                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                    enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(enableBtIntent);
                }
            } else {
                Log.i("tag","同意申请");
            }
        }
    }

    @Override
    protected synchronized void onResume() {
        // use synchronized to complete queue call
        super.onResume();
        if(mBluetoothConnection != null){
            if(mBluetoothConnection.getState() == BluetoothConnection.STATE_NONE)
                mBluetoothConnection.start();
        }
    }

    private void setupChat() {
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);
        mOutEditText = findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);
        mSendButton = findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                TextView view = findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });
        //create service object
        mBluetoothConnection = new BluetoothConnection(this, mHandler);
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mBluetoothConnection != null)
            mBluetoothConnection.stop();
    }

    private void ensureDiscoverable() {
        //Modify the visibility of native bluetooth devices
        //After turning on the phone's Bluetooth, the time it can be scanned by other Bluetooth devices is not permanent
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            //Set to be visible within 300 seconds
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
            Toast.makeText(this, "The visibility of the local Bluetooth device has been set, and the other party can search.", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendMessage(String message) {
        if (mBluetoothConnection.getState() != BluetoothConnection.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        if (message.length() > 0) {
            byte[] send = message.getBytes();
            mBluetoothConnection.write(send);
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }


}
