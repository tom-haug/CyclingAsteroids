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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.wahoofitness.connector.conn.connections.params.ConnectionParams;
import com.wahoofitness.connector.listeners.discovery.DiscoveryListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import james.asteroid.R;
import james.asteroid.services.WahooService;

public class DevicePairingActivity extends AppCompatActivity implements DiscoveryListener {
    private static final String TAG = "DevicePairingActivity";

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

        layout.removeAllViews();
        Intent intent = new Intent(this, WahooService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        addContinueButton();
        addCadenceButton();
    }


    // Function to check and request permission
    public void checkPermission(String permission, int requestCode)
    {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(DevicePairingActivity.this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(DevicePairingActivity.this, new String[] { permission }, requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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
        addDeviceButton(connectionParams);
    }

    public void addCadenceButton(){
        String buttonText = "Cadence";


        Button button = new Button(this);
        button.setText(buttonText);
        button.setOnClickListener(v -> {
            try {
                double cadence = mWahooService.getCadence();
                Toast.makeText(DevicePairingActivity.this,"Cadence: " + String.valueOf(cadence), Toast.LENGTH_SHORT).show();
            }
            catch(Exception e) {
                Log.d(TAG, e.toString());
            }


        });
        layout.addView(button);

    }

    public void addContinueButton(){
        String buttonText = "Continue";


        Button button = new Button(this);
        button.setText(buttonText);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Create a task
                Runnable task = () -> {
                    scheduledTask.cancel(false);
                    Intent intent = new Intent(DevicePairingActivity.this, MainActivity.class);
                    startActivity(intent);
                };

                scheduledTask = executorService.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
            }
        });
        layout.addView(button);

    }

    public void addDeviceButton(@NonNull ConnectionParams connectionParams){
        String name = connectionParams.getName();
        String productType = connectionParams.getProductType().name();
        String sensorType = connectionParams.getSensorType().name();
        String id = connectionParams.getId();
        String buttonText = "Device - Name: " + name + ", Sensor Type: " + sensorType + " Product: " + productType + ", ID: " + id;


        Button button = new Button(this);
        button.setText(buttonText);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 mWahooService.connect(connectionParams);

//                // Create a task
//                Runnable task = () -> {
//                    if (mWahooService.isConnected()){
//                        scheduledTask.cancel(false);
//                        Intent intent = new Intent(DevicePairingActivity.this, MainActivity.class);
//                        startActivity(intent);
//                    }
//                };
//
//                scheduledTask = executorService.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
            }
        });
        layout.addView(button);
    }

    final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    static ScheduledFuture<?> scheduledTask;


    public void continueOnConnected(){

    }


    @Override
    public void onDiscoveredDeviceLost(@NonNull ConnectionParams connectionParams) {

    }

    @Override
    public void onDiscoveredDeviceRssiChanged(@NonNull ConnectionParams connectionParams, int i) {

    }
}