package com.example.lanya2;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ReceiveFileActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_CONNECT_PERMISSION = 100;

    private static final String TAG = "ReceiveFileActivity";
    private static final String APP_NAME = "FileTransferApp";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothServerSocket serverSocket;
    private TextView receivedFileContentTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_file);

        receivedFileContentTextView = findViewById(R.id.received_file_content);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported");
            finish();
        }

        createServerSocket();
    }

    private void createServerSocket() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                try {
                    serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, MY_UUID);
                    Log.i(TAG, "Server socket created, waiting for connection...");
                    new Thread(() -> {
                        try {
                            BluetoothSocket socket = serverSocket.accept();
                            Log.i(TAG, "Connected to device: " + socket.getRemoteDevice().getName());
                            readDataFromSocket(socket);
                        } catch (IOException e) {
                            Log.e(TAG, "Error accepting connection", e);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    receivedFileContentTextView.append("连接失败："+e.getMessage());
                                }
                            });

                        }
                    }).start();
                } catch (IOException e) {
                    Log.e(TAG, "Error creating server socket", e);
                    finish();
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_CONNECT_PERMISSION);
                receivedFileContentTextView.append("申请连接权限");
            }
        } else {

            try {
                serverSocket = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, MY_UUID);
                Log.i(TAG, "Server socket created, waiting for connection...");
                new Thread(() -> {
                    try {
                        BluetoothSocket socket = serverSocket.accept();
                        Log.i(TAG, "Connected to device: " + socket.getRemoteDevice().getName());
                        readDataFromSocket(socket);
                    } catch (IOException e) {
                        Log.e(TAG, "Error accepting connection", e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                receivedFileContentTextView.append("连接失败："+e.getMessage());
                            }
                        });
                    }
                }).start();
            } catch (IOException e) {
                Log.e(TAG, "Error creating server socket", e);
                finish();
            }
        }

    }


    private void readDataFromSocket(BluetoothSocket socket) {
        try {
            InputStream inputStream = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            StringBuilder stringBuilder = new StringBuilder();

            while ((bytesRead = inputStream.read(buffer)) != -1) {

                String text = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                stringBuilder.append(text);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        receivedFileContentTextView.setText(stringBuilder.toString());
                    }
                });
            }

//            inputStream.close();
//            socket.close();
//            serverSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error reading data from socket", e);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    receivedFileContentTextView.append("接收失败："+e.getMessage());
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing server socket", e);
            }
        }
    }
}


