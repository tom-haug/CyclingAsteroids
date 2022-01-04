package james.asteroid.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wahoofitness.common.datatypes.AngularSpeed;
import com.wahoofitness.connector.HardwareConnector;
import com.wahoofitness.connector.HardwareConnectorEnums;
import com.wahoofitness.connector.HardwareConnectorTypes;
import com.wahoofitness.connector.capabilities.BikeTrainer;
import com.wahoofitness.connector.capabilities.Capability;
import com.wahoofitness.connector.capabilities.Connection;
import com.wahoofitness.connector.capabilities.CrankRevs;
import com.wahoofitness.connector.capabilities.DeviceInfo;
import com.wahoofitness.connector.capabilities.Kickr;
import com.wahoofitness.connector.capabilities.KickrAdvanced;
import com.wahoofitness.connector.conn.connections.SensorConnection;
import com.wahoofitness.connector.conn.connections.params.ConnectionParams;
import com.wahoofitness.connector.listeners.discovery.DiscoveryListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import james.asteroid.activities.DevicePairingActivity;
import james.asteroid.activities.MainActivity;

public class WahooService extends Service {
    private static final String TAG = "WahooService";

    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    private ConnectionParams connectionParams;
    private SensorConnection connection;
    private Kickr kickr;
    private CrankRevs crankRevs;
    private double cadence = 0.0;
    //public boolean connectionComplete = true;
    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public WahooService getService() {
            // Return this instance of LocalService so clients can call public methods
            return WahooService.this;
        }
    }

    private HardwareConnector mHardwareConnector;
    private final HardwareConnector.Listener mHardwareConnectorListener = new HardwareConnector.Listener() {
        @Override
        public void onHardwareConnectorStateChanged(@NonNull HardwareConnectorTypes.NetworkType networkType, @NonNull HardwareConnectorEnums.HardwareConnectorState hardwareConnectorState) {

        }

        @Override
        public void onFirmwareUpdateRequired(@NonNull SensorConnection sensorConnection, @NonNull String s, @NonNull String s1) {

        }
    };

    final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    static ScheduledFuture<?> scheduledTask;

    private void startCrankRevsTask(){
        // Create a task
        Runnable task = this::updateCadence;

        scheduledTask = executorService.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
    }

    private final SensorConnection.Listener mListner = new SensorConnection.Listener() {
        @Override
        public void onNewCapabilityDetected(@NonNull SensorConnection sensorConnection, @NonNull Capability.CapabilityType capabilityType) {
            Log.d(TAG, "Capability Found: " + capabilityType.toString());
            if (capabilityType == Capability.CapabilityType.Kickr){
                kickr = (Kickr)sensorConnection.getCurrentCapability(capabilityType);
                //connectionComplete = true;
//                kickr.sendSetStandardMode(1);
            }
            else if (capabilityType == Capability.CapabilityType.CrankRevs){
                crankRevs = (CrankRevs)sensorConnection.getCurrentCapability(capabilityType);
                startCrankRevsTask();
            }
        }

        @Override
        public void onSensorConnectionStateChanged(@NonNull SensorConnection sensorConnection, @NonNull HardwareConnectorEnums.SensorConnectionState sensorConnectionState) {

        }

        @Override
        public void onSensorConnectionError(@NonNull SensorConnection sensorConnection, @NonNull HardwareConnectorEnums.SensorConnectionError sensorConnectionError) {

        }
    };


    CrankRevs.Data getCrankRevsData()
    {
//        if(crankRevs != null) {
//            CrankRevs crankRevs = (CrankRevs)connection.getCurrentCapability(Capability.CapabilityType.CrankRevs);
            if (crankRevs != null) {
                Log.d(TAG, "getCrankRevsData() - crankRevs FOUND!");
                return crankRevs.getCrankRevsData();
            } else {
// The sensor connection does not currently support the crank revs capability
                Log.d(TAG, "getCrankRevsData() - crankRevs NOT found :(");
                return null;
            }
//        } else {
//// Sensor not connected
//            Log.d(TAG, "getCrankRevsData() - Sensor not connected");
//            return null;
//        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHardwareConnector = new HardwareConnector(this, mHardwareConnectorListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHardwareConnector.disconnectAllSensors();
        mHardwareConnector.shutdown();
    }

    public void startDiscovery(@NonNull DiscoveryListener discoveryListener){
        mHardwareConnector.startDiscovery(discoveryListener);
    }

    public void stopDiscovery(@NonNull DiscoveryListener discoveryListener){
        mHardwareConnector.stopDiscovery(discoveryListener);
    }

    public void connect(ConnectionParams params){
        Log.d(TAG, "connect: " + params.toString());

        connectionParams = params;
        //connectionComplete = false;
        connection = mHardwareConnector.requestSensorConnection(connectionParams, mListner);
    }

    public void disconnect(){
        connection.disconnect();
    }

    public boolean isConnected() {
        return connection != null && connection.isConnected();
    }

    public void setResistance(float resistance){
        if (kickr == null)
            return;
        if (connection == null || !connection.isConnected())
            return;
        if (resistance < 0.0 || resistance > 1.0)
            return;

        kickr.sendSetResistanceMode(resistance);
    }

    public float getResistance(){
        if (kickr == null)
            return 0.0f;
        if (connection == null || !connection.isConnected())
            return 0.0f;

        float resistance = kickr.getResistanceModeResistance();
        return resistance;
    }

    private void updateCadence(){
        CrankRevs.Data data = getCrankRevsData();
        assert data != null;
        AngularSpeed crankSpeed = getCrankRevsData().getCrankSpeed();
        cadence = crankSpeed.asRpm();
    }

    public double getCadence(){
        return cadence;
    }
}
