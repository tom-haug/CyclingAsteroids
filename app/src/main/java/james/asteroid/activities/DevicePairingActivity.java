package james.asteroid.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.wahoofitness.connector.conn.connections.params.ConnectionParams;
import com.wahoofitness.connector.listeners.discovery.DiscoveryListener;

import james.asteroid.R;
import james.asteroid.services.WahooService;

public class DevicePairingActivity extends AppCompatActivity implements DiscoveryListener {

    WahooService mWahooService;
    boolean mBound = false;

    LinearLayout layout;


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            WahooService.LocalBinder binder = (WahooService.LocalBinder) service;
            mWahooService = binder.getService();
            mBound = true;

             mWahooService.startDiscovery(DevicePairingActivity.this);
             Toast.makeText(DevicePairingActivity.this, "Discovery Started", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_pairing);

        layout = findViewById(R.id.devicePairingLayout);
    }

    @Override
    protected void onStart() {
        super.onStart();

        checkPermission(Manifest.permission.BLUETOOTH, 100);
        checkPermission(Manifest.permission.BLUETOOTH_ADMIN, 101);
        checkPermission(Manifest.permission.WAKE_LOCK, 102);
        checkPermission(Manifest.permission.INTERNET, 103);
        checkPermission(Manifest.permission.ACCESS_NETWORK_STATE, 104);
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, 105);


        Toast.makeText(DevicePairingActivity.this, "onStart", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, WahooService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }


    // Function to check and request permission
    public void checkPermission(String permission, int requestCode)
    {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(DevicePairingActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(DevicePairingActivity.this, new String[] { permission }, requestCode);
        }
        else {
            Toast.makeText(DevicePairingActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Toast.makeText(DevicePairingActivity.this, "onRequestPermissionsResult", Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onStop() {
        super.onStop();

        mWahooService.stopDiscovery(this);
        unbindService(connection);
        mBound = false;
    }

    @Override
    public void onDeviceDiscovered(@NonNull ConnectionParams connectionParams) {
        String name = connectionParams.getName();
        String productType = connectionParams.getProductType().name();
        String sensorType = connectionParams.getSensorType().name();
        String id = connectionParams.getId();

        Button button = new Button(this);
        button.setText("Device - Name: " + name + ", Sensor Type: " + sensorType + " Product: " + productType + ", ID: " + id);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWahooService.saveConnection(connectionParams);

                Intent intent = new Intent(DevicePairingActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        layout.addView(button);
    }



    @Override
    public void onDiscoveredDeviceLost(@NonNull ConnectionParams connectionParams) {

    }

    @Override
    public void onDiscoveredDeviceRssiChanged(@NonNull ConnectionParams connectionParams, int i) {

    }
}