package com.example.bluetooth;

// Importing necessary Android and Bluetooth classes

import static java.lang.Double.parseDouble;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton;
import android.util.Log;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    // Declaration of Bluetooth related variables
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private int hrBuffers = 0;
    private BluetoothGattCharacteristic txCharacteristic, rxCharacteristic;
    private TextView bmeTempView, bmePresView, bmeHumView, bmeGasView, hrView, confidenceView, bloodOxView, bodyTempView, yellowButtonView, blueButtonView, alsView, uvsView, memsMicView;
    private SwitchCompat ledControlSwitch;
    private boolean isConnected = false;

    // UUIDs for the TX/RX characteristic
    private final UUID TX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private final UUID RX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    private final UUID Service_CHAR_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI elements initialization
        bmeTempView = findViewById(R.id.bmeTemp);
        bmeHumView = findViewById(R.id.bmeHumidity);
        bmePresView = findViewById(R.id.bmePressure);
        bmeGasView = findViewById(R.id.bmeGas);

        hrView = findViewById(R.id.heartRate);
        confidenceView = findViewById(R.id.hrConfidence);
        bloodOxView = findViewById(R.id.bloodOx);
        bodyTempView = findViewById(R.id.bodyTemp);

        alsView = findViewById(R.id.als);
        uvsView = findViewById(R.id.uvs);
        memsMicView = findViewById(R.id.memsMic);
        yellowButtonView = findViewById(R.id.yellowButton);
        blueButtonView = findViewById(R.id.blueButton);

        ledControlSwitch = findViewById(R.id.ledControl);

        Intent intent = getIntent();
        String originActivity = intent.getStringExtra("origin");

        if (!Objects.equals(originActivity, "MedicalDashboard")) {
            initializeBluetooth();
            setupUiInteractions();
        } else {
            if (bluetoothGatt != null) {
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                bluetoothGatt.discoverServices();
            }
        }

        Button ekgButton = findViewById(R.id.ekgButton);
        ekgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ecgActivity.class);
                startActivity(intent);
            }
        });

        Button sleepButton = findViewById(R.id.sleepButton);
        sleepButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), sleepTrackerActivity.class);
                startActivity(intent);
            }
        });
    }

    // Initializes Bluetooth
    private void initializeBluetooth() {
        Log.d("BLE", "Initializing Bluetooth");
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checking if Bluetooth is enabled, if not, request the user to enable it
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.d("BLE", "Bluetooth is not enabled, requesting user to enable it");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivityForResult(enableBtIntent, 1);
        } else {
            // Start scanning for BLE devices
            Log.d("BLE", "Bluetooth is enabled, starting scanning");
            startScanning();
        }
    }

    // Start scanning for BLE devices
    private void startScanning() {
        // Avoid scanning if already connected
        if (isConnected) {
            return;
        }
        Log.d("BLE", "Starting BLE scan");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
    }

    // Callback for BLE scan results
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // Handle each scan result
            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Log.d("BLE", "Device found: " + result.getDevice().getName());
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();

            // Connect to the device if it is the expected BLE device
            if (device.getName() != null && device.getName().equals("Nordic_UART_Service")) {
                Log.d("BLE", "nRF52 device found, stopping scan and connecting");

                // Close existing connection if any
                if (bluetoothGatt != null) {
                    Log.d("BLE", "Closing existing GATT connection");
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }

                // Start a new connection with the device
                Log.d("BLE", "Starting new GATT connection");
                bluetoothGatt = device.connectGatt(MainActivity.this, false, gattCallback);
                bluetoothAdapter.getBluetoothLeScanner().stopScan(this);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("BLE", "Scan failed with error code: " + errorCode);
            super.onScanFailed(errorCode);
        }
    };

    // Callbacks for GATT events
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        // Handle changes in connection state
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            // Connected to the GATT server
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BLE", "Connected to GATT server, discovering services");
                isConnected = true;
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.discoverServices();
            }
            // Disconnected from the GATT server
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("BLE", "Disconnected from GATT server");
                isConnected = false;
                initializeBluetooth();
            }
        }

        // Handle service discovery
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "Services discovered");
                // Get the service containing the characteristics
                BluetoothGattService service = gatt.getService(Service_CHAR_UUID);

                // Get the TX and RX characteristics
                if (service != null) {
                    txCharacteristic = service.getCharacteristic(TX_CHAR_UUID);
                    rxCharacteristic = service.getCharacteristic(RX_CHAR_UUID);

                    // Verify if characteristics are valid and set them up
                    if (txCharacteristic != null && rxCharacteristic != null) {
                        int charaProp = txCharacteristic.getProperties();
                        // Checking and setting characteristic properties
                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                            txCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                            Log.d("BLE", "TX characteristic is writeable");
                        } else if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                            txCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                            Log.d("BLE", "TX characteristic is writeable (No Response)");
                        } else {
                            Log.e("BLE", "TX characteristic is not writeable");
                        }

                        // Request permission to connect
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }

                        // Enable notifications for the RX characteristic
                        gatt.setCharacteristicNotification(rxCharacteristic, true);

                        // Set up descriptor to enable notification
                        BluetoothGattDescriptor descriptor = rxCharacteristic.getDescriptor(
                                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    } else {
                        Log.e("BLE", "TX or RX characteristic not found");
                    }
                } else {
                    Log.e("BLE", "Service not found");
                }
            } else {
                Log.e("BLE", "Service discovery failed with status: " + status);
            }
        }

        // Handle characteristic change (data received)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d("BLE", "Button Pressed");
            // Check if the changed characteristic is the one we're interested in
            if (characteristic.getUuid().equals(RX_CHAR_UUID)) {
                byte[] rawData = characteristic.getValue();

                // Convert the received bytes to a string and update the UI
                if (rawData != null) {
                    String dataString = new String(rawData);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String[] parts = dataString.split("\\s+");

                            for (String part : parts) {
                                String tag = part.substring(0, 4);
                                String val = part.substring(4);
                                if (tag.equals("BMT:")) {
                                    bmeTempView.setText("Temperature:   " + val + "\u00B0 F");
                                    DataSingleton.getInstance().setTempData(parseDouble(val));
                                } else if (tag.equals("BMH:")) {
                                    bmeHumView.setText("Relative Humidity:   " + val + " %");
                                    DataSingleton.getInstance().setHumidity(parseDouble(val));
                                } else if (tag.equals("BMP:")) {
                                    bmePresView.setText("Barometric Pressure:   " + val + " Pa");
                                    DataSingleton.getInstance().setPressData(parseDouble(val));
                                } else if (tag.equals("BMG:")) {
                                    bmeGasView.setText("Gas Resistance:   " + val + " \u2126");
                                    DataSingleton.getInstance().setGas(parseDouble(val));
                                } else if (tag.equals("BPM:")) {
                                    hrView.setText("Heart Rate:   " + val + " BPM");
                                    if (parseDouble(val) != 0.0) {
                                        DataSingleton.getInstance().setHeartRate(parseDouble(val));
                                        hrBuffers=0;
                                    } else if (hrBuffers >= 5) {
                                        DataSingleton.getInstance().setHeartRate(parseDouble(val));
                                        hrBuffers=0;
                                    } else {
                                        hrBuffers+=1;
                                    }
                                } else if (tag.equals("HRC:")) {
                                    confidenceView.setText("Heart Rate Confidence:   " + val + " %");
                                } else if (tag.equals("BOS:")) {
                                    bloodOxView.setText("Blood Oxygen Saturation:   " + val + " %");
                                    DataSingleton.getInstance().setBloodOx(parseDouble(val));
                                } else if (tag.equals("BTS:")) {
                                    bodyTempView.setText("Body Temperature:   " + val + "\u00B0 F");
                                    // Store data in singleton
                                    DataSingleton.getInstance().setBodyTemp(parseDouble(val));
                                } else if (tag.equals("ALS:")) {
                                    if (parseDouble(val) == 0) {
                                        alsView.setText("Light Level:   Dark");
                                    } else if (parseDouble(val) == 1) {
                                        alsView.setText("Light Level:   Dim");
                                    } else if (parseDouble(val) == 2) {
                                        alsView.setText("Light Level:   Moderate");
                                    } else if (parseDouble(val) == 3) {
                                        alsView.setText("Light Level:   Bright");
                                    } else if (parseDouble(val) == 4) {
                                        alsView.setText("Light Level:   Very Bright");
                                    } else if (parseDouble(val) == 5) {
                                        alsView.setText("Light Level:   Hazardous");
                                    }
                                } else if (tag.equals("UVS:")) {
                                    uvsView.setText("Ultraviolet Light Index:   " + val);
                                } else if (tag.equals("MIC:")) {
                                    memsMicView.setText("MEMS Microphone:   " + val + " mV");
                                } else if (tag.equals("BBV:")) {
                                    blueButtonView.setText("Blue Button Value:   " + val);
                                } else if (tag.equals("YBV:")) {
                                    yellowButtonView.setText("Yellow Button Value:   " + val);
                                }
                            }
                        }
                    });

                    Log.d("BLE", "Received string data: " + dataString);
                } else {
                    Log.d("BLE", "Received null or invalid data. Raw data: " + Arrays.toString(rawData));
                }
            }
        }
    };

    // Set up UI interactions, like handling LED control switch
    private void setupUiInteractions() {
        ledControlSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d("BLE", "LED Switch state changed: " + isChecked);
                // Write to the TX characteristic based on switch state
                if (txCharacteristic != null && bluetoothGatt != null) {
                    byte[] value = new byte[]{(byte) (isChecked ? 1 : 0)};
                    txCharacteristic.setValue(value);

                    // Set the appropriate write type
                    if ((txCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                        txCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    } else if ((txCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                        txCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                    }

                    txCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

                    if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    // Write the characteristic and check if the write operation was initiated successfully
                    boolean writeSuccessful = bluetoothGatt.writeCharacteristic(txCharacteristic);
                    if (!writeSuccessful) {
                        Log.e("BLE", "Failed to write characteristic");
                    } else {
                        Log.d("BLE", "Characteristic write initiated");
                    }
                } else {
                    Log.e("BLE", "TX characteristic" + txCharacteristic + "GATT characteristic" + bluetoothGatt);
                }
            }
        });
    }

    public void onMedicalDataClicked(View view) {
        Intent intent = new Intent(this, MedicalDashboardActivity.class);
        startActivity(intent);
    }

    public void onWeatherDataClicked(View view) {
        Intent intent = new Intent(this, WeatherDashboardActivity.class);
        startActivity(intent);
    }

    public void heartRateClicked(View view) {
        Intent intent = new Intent(this, ecgActivity.class);
        startActivity(intent);
    }

}
