package wright.ryan.bluetooth_connectiontest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity {
    protected static final int DISCOVERY_REQUEST = 1;

    /** Called when the activity is first created. **/
    private BluetoothAdapter btAdapter;

    public TextView statusUpdate;
    public Button connect;
    public Button disconnect;
    public String toastText="";
    private BluetoothDevice remoteDevice;
    public ListView devices;
    public ArrayList<String> deviceNameList = new ArrayList<>();
    public ArrayAdapter<String> deviceNameAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private UUID MY_UUID = UUID.fromString("ef637e31-e515-46ff-92d0-d787e45cfdd5");


    BroadcastReceiver bluetoothState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String prevStateExtra = BluetoothAdapter.EXTRA_PREVIOUS_STATE;
            String stateExtra = BluetoothAdapter.EXTRA_STATE;
            int state = intent.getIntExtra(stateExtra, -1);
            //int previousState = intent.getIntExtra(prevStateExtra, -1);
            switch(state){
                case(BluetoothAdapter.STATE_TURNING_ON) :
                {
                    toastText="Bluetooth turning on";
                    Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    break;
                }
                case(BluetoothAdapter.STATE_ON) :
                {
                    toastText="Bluetooth on";
                    Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    setupUI();
                    break;
                }
                case(BluetoothAdapter.STATE_TURNING_OFF) :
                {
                    toastText = "Bluetooth turning off";
                    Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    break;
                }
                case(BluetoothAdapter.STATE_OFF) :
                {
                    toastText="Bluetooth off";
                    Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    setupUI();
                    break;
                }
            }

        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

                setupUI();

        ListeningThread t = new ListeningThread();
        t.start();

        devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), ((TextView) view).getText(), Toast.LENGTH_SHORT).show();
                String itemValue = (String) devices.getItemAtPosition(position);
                String MAC = itemValue.substring(itemValue.length() - 17);
                BluetoothDevice bluetoothDevice = btAdapter.getRemoteDevice(MAC);
                // Initiate  a connection request in a seperate thread
                ConnectThread t = new ConnectThread(bluetoothDevice);
                t.start();

            }
        });


    }/**end onCreate**/





    private void setupUI() {
        /**get references**/
        statusUpdate =(TextView) findViewById(R.id.result);
        connect =(Button) findViewById(R.id.btnCn);
        disconnect =(Button) findViewById(R.id.btnDsc);
        devices = (ListView) findViewById(R.id.devices);
        deviceNameAdapter = new ArrayAdapter<> ( this, R.layout.my_listview_item, R.id.text1, deviceNameList );

        devices.setAdapter(deviceNameAdapter);
        /** set display view **/
        disconnect.setVisibility(View.GONE);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = btAdapter.getBondedDevices();
        if(btAdapter.isEnabled()) {
            String address = btAdapter.getAddress();
            String name = btAdapter.getName();
            String statusText = name + " : " + address;
            statusUpdate.setText(statusText);
            disconnect.setVisibility(View.VISIBLE);
            connect.setVisibility(View.GONE);
        }
        else {
            statusUpdate.setText("Bluetooth is disabled");
            connect.setVisibility(View.VISIBLE);

        }


        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /** String actionStateChanged = BluetoothAdapter.ACTION_STATE_CHANGED;
                 String actionRequestEnable = BluetoothAdapter.ACTION_REQUEST_ENABLE;
                 IntentFilter filter = new IntentFilter(actionStateChanged);
                 registerReceiver(bluetoothState, filter);
                 startActivityForResult(new Intent(actionRequestEnable), 0); **/

                /** register for discovery events **/
                String scanModeChanged = BluetoothAdapter.ACTION_SCAN_MODE_CHANGED;
                String beDiscoverable = BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE;
                IntentFilter filter = new IntentFilter(scanModeChanged);
                registerReceiver(bluetoothState, filter);
                startActivityForResult(new Intent(beDiscoverable),DISCOVERY_REQUEST);



            }
        });/** End of connect onClickListener**/





        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btAdapter.disable();
                disconnect.setVisibility(View.GONE);
                connect.setVisibility(View.VISIBLE);
                statusUpdate.setText("Bluetooth Off");
                deviceNameList.clear();

            }
        });/** End of disconnect onClickListener **/
    }/** End setupUI**/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == DISCOVERY_REQUEST){
            Toast.makeText(MainActivity.this, "Discovery in progress", Toast.LENGTH_SHORT).show();
            setupUI();
            findDevices();
        }
    }

    private void findDevices() {
        String lastUsedRemoteDevice = getLastUsedRemoteBTDevice();
        if (lastUsedRemoteDevice != null) {
            toastText = "Checking for known paired devices, namely: " + lastUsedRemoteDevice;
            //Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
            /** see if this device is in a list of current visible (?), paired devices **/
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

            for (BluetoothDevice pairedDevice : pairedDevices) {
                if (pairedDevice.getAddress().equals(lastUsedRemoteDevice)) {
                    toastText = "Found device: " + pairedDevice.getName() + "@" + lastUsedRemoteDevice;
                    Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    remoteDevice = pairedDevice;
                }
            }
        } /**end if **/
        if (remoteDevice == null) {
            toastText = "Starting discovery for remote devices...";
            Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
            /** Start Discovery **/
            if (btAdapter.startDiscovery()) {
                toastText = "Discovery thread started...Scanning for Devices";
                Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                registerReceiver(discoveryResult, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
        }

        //deviceNameAdapter.notifyDataSetChanged();
    } /** End find devices **/

    /** Create a BroadcastReceiver to receive device discovery **/
    BroadcastReceiver discoveryResult = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            BluetoothDevice remoteDevice;




            remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String pairDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE);

            String remoteDeviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
            if (remoteDeviceName == null){
                remoteDeviceName = remoteDevice.getAddress();
            }

            //toastText = "Discovered: " + remoteDeviceName;
            //Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
            deviceNameList.add(remoteDeviceName);
            deviceNameAdapter.notifyDataSetChanged();
            /** statusUpdate.setText(statusText);**/
        }


    };

    private String getLastUsedRemoteBTDevice(){
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String result = prefs.getString("LAST_REMOTE_DEVICE_ADDRESS", null);
        return result;
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            btAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }

            // Do work to manage the connection (in a separate thread)
//            manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ListeningThread extends Thread {
        private final BluetoothServerSocket bluetoothServerSocket;

        public ListeningThread() {
            BluetoothServerSocket temp = null;
            try {
                temp = btAdapter.listenUsingRfcommWithServiceRecord(getString(R.string.app_name), MY_UUID);

            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothServerSocket = temp;
        }

        public void run() {
            BluetoothSocket bluetoothSocket;
            // This will block while listening until a BluetoothSocket is returned
            // or an exception occurs
            while (true) {
                try {
                    bluetoothSocket = bluetoothServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection is accepted
                if (bluetoothSocket != null) {

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "A connection has been accepted.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Manage the connection in a separate thread

                    try {
                        bluetoothServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Cancel the listening socket and terminate the thread
        public void cancel() {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



}

/** End MainActivity **/
