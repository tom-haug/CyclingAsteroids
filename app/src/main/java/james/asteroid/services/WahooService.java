package james.asteroid.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wahoofitness.connector.HardwareConnector;
import com.wahoofitness.connector.HardwareConnectorEnums;
import com.wahoofitness.connector.HardwareConnectorTypes;
import com.wahoofitness.connector.conn.connections.SensorConnection;
import com.wahoofitness.connector.conn.connections.params.ConnectionParams;
import com.wahoofitness.connector.listeners.discovery.DiscoveryListener;

public class WahooService extends Service {
    // Binder given to clients
    private final IBinder binder = new LocalBinder();


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
        mHardwareConnector.shutdown();
    }

    public void startDiscovery(@NonNull DiscoveryListener discoveryListener){
        mHardwareConnector.startDiscovery(discoveryListener);
    }

    public void stopDiscovery(@NonNull DiscoveryListener discoveryListener){
        mHardwareConnector.stopDiscovery(discoveryListener);
    }
}
