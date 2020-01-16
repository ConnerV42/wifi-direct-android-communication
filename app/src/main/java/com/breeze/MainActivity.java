package com.breeze;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.breeze.application.BreezeAPI;
import com.breeze.views.MainSettingsActivity;
import com.breeze.views.Messages.PublicMessagesView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private static final int REQUEST_CODE_PROFILE = 2;
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };

    @Override
    protected void onStart() {
        super.onStart();

        this.startApplicationService();

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
    }

    private void startApplicationService() {
        Intent brzService = new Intent(this, BreezeAPI.class);
        startService(brzService);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.startApplicationService();
        BreezeAPI.getInstance().initialize(this);

        setContentView(R.layout.activity_main);

        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // We've got a profile!
        if (requestCode == REQUEST_CODE_PROFILE) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        BreezeAPI api = BreezeAPI.getInstance();

        if (id == R.id.action_toggle_discovery) {
            if (api.router.isDiscovering) {
                item.setChecked(false);
                api.router.stopDiscovery();
            } else {
                item.setChecked(true);
                api.router.startDiscovery();
            }
        } else if (id == R.id.action_discovery_scan) {
            api.router.startDiscovery();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    api.router.stopDiscovery();
                }
            }, 5 * 1000);
        }
        else if (id == R.id.action_public_thread)
        {
            Intent i = new Intent(MainActivity.this, PublicMessagesView.class);
            startActivity(i);
        }
        else if ( id == R.id.action_settings){
            Intent i = new Intent(MainActivity.this, MainSettingsActivity.class);
            startActivity(i);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            // Permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                BreezeAPI api = BreezeAPI.getInstance();
                if (api != null && api.router != null) api.router.start();
            }

            // Permission denied
            else {

            }
        }

    }

}
