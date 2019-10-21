package com.example.meshnet;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MainActivity extends AppCompatActivity {

    // Tag for logging endpoint connects + disconnects
    private static final String TAG = "MeshNet";
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };

    private ConnectionsClient connectionsClient;
    private TextView statusText;

    private TextView logs;

    private Button findNodesButton;
    private String otherNodeEndpointId;

    private ArrayList<String> connectedNodes = new ArrayList<String>();

    // Our randomly generated unique name for advertising
    private final String codeName = CodenameGenerator.generate();

    // Callback for receiving payloads
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    String message = new String(payload.asBytes(), UTF_8);
                    logs.append(endpointId + " sent: " + message + "\n");
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    if(update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        //display message to UI
                    }
                }
            };

    // Callback for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    logs.append("onEndpointFound: endpointId " + endpointId + " found, connecting\n");
                    connectionsClient.requestConnection("ENDPOINT", endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    logs.append("onEndpointLost: endpointId " + endpointId + " disconnected\n");
                }
            };

    // Callbacks for connections to other devices
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    logs.append("onConnectionInitiated: accepting connection with endpointId " + endpointId + "\n");
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    if (result.getStatus().isSuccess()) {
                        logs.append("onConnectionResult: connection successful with endpointId " + endpointId + "\n");

                        connectedNodes.add(endpointId);

                        setStatusText(getString(R.string.status_connected));
                    } else {
                        logs.append("onConnectionResult: connection failed with endpointId " + endpointId + "\n");
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    logs.append("onDisconnected: disconnected from endpointId " + endpointId + "\n");
                    connectedNodes.remove(endpointId);
                }
            };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onStart() { // Might want to bump up the minimum required API to 23
        super.onStart();

        if(!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
    }

    @Override
    protected void onStop() {
        connectionsClient.stopAllEndpoints();
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        connectionsClient = Nearby.getConnectionsClient(this);

        FloatingActionButton sendMessage = findViewById(R.id.sendMessage);

        statusText = findViewById(R.id.statusText);
        findNodesButton = findViewById(R.id.find_nodes);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // send message

                EditText messageBox = findViewById(R.id.editText);
                String messageBoxText = messageBox.getText().toString();

                Snackbar.make(view, "Message Sent!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                
                for(String id : connectedNodes) {
                    connectionsClient.sendPayload(id, Payload.fromBytes(messageBoxText.getBytes()));
                }

            }
        });


        this.logs = findViewById(R.id.textView);

    }

    public void findNodes(View view) {
        startAdvertising();
        startDiscovery();
        setStatusText(getString(R.string.status_searching));
        findNodesButton.setEnabled(false);
    }

    private void setStatusText(String string) {
        statusText.setText(string);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startAdvertising() {
        connectionsClient.startAdvertising(codeName,
                getPackageName(), connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
    }
    private void startDiscovery() {
        connectionsClient.startDiscovery(
                getPackageName(), endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());
    }
}
