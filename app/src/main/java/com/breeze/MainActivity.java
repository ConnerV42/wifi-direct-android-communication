package com.breeze;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.core.content.ContextCompat;

import com.breeze.database.DatabaseHandler;
import com.breeze.encryption.BrzEncryption;
import com.breeze.packets.BrzChat;
import com.breeze.packets.BrzMessage;
import com.breeze.packets.BrzPacket;
import com.breeze.packets.BrzPacketBuilder;
import com.breeze.router.BrzRouter;

import com.breeze.state.BrzStateStore;
import com.google.android.gms.nearby.Nearby;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.lang.*;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.ECField;
import java.util.Enumeration;
import java.util.List;

import static com.breeze.packets.BrzPacketBuilder.*;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private BrzRouter router;
    private DatabaseHandler dbHelper;

    public PublicKey getPublicKey() {
        return publicKey;
    }

    private PublicKey publicKey;
    private byte[] publicKeyEncoded;
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private static final String[] REQUIRED_PERMISSIONS =
            new String[] {
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            };

    @Override
    protected void onStart() {
        super.onStart();

        if(!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        router.start();
    }

    @Override
    protected void onStop() {
        router.stop();
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BrzEncryption brzEnc = new BrzEncryption();
        try {
            KeyPair kp = BrzEncryption.getKeyPair();
            this.publicKey = brzEnc.grabPublicKey(kp);
            this.publicKeyEncoded = this.publicKey.getEncoded();
        }catch(Exception f)
        {
            System.out.println("Unable to get key pair or retrieve either keys. Check.");
        }
        dbHelper = new DatabaseHandler(this);
        dbHelper.getReadableDatabase();
        this.toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //this.router = BrzRouter.getInstance(Nearby.getConnectionsClient(this), getPackageName());
        this.router = BrzRouter.getInstance(Nearby.getConnectionsClient(this), "BREEZE_MESSENGER");

        BrzPacket keepackit = Handshake(this.router.getGraph(), "yeet1", new String(this.publicKey.getEncoded()), "yeet1");

        this.router.send(keepackit);

        BrzStateStore store = BrzStateStore.getStore();
        store.setTitle("Breeze");
        store.getTitle(title -> this.toolbar.setTitle(title));

        store.addChat(new BrzChat("yeet1", "Zach"));
        store.addChat(new BrzChat("yeet2", "Paul"));
        store.addChat(new BrzChat("yeet3", "Conner"));
        store.addChat(new BrzChat("yeet4", "Jake"));

        store.addMessage("yeet1", new BrzMessage("hey", "yeet1"));
        store.addMessage("yeet1", new BrzMessage("What's up?", router.id));

        store.addMessage("yeet2", new BrzMessage("hey", "yeet2"));
        store.addMessage("yeet2", new BrzMessage("What's up?", router.id));

        store.addMessage("yeet3", new BrzMessage("hey", "yeet3"));
        store.addMessage("yeet3", new BrzMessage("What's up?", router.id));

        store.addMessage("yeet4", new BrzMessage("hey", "yeet4"));
        store.addMessage("yeet4", new BrzMessage("What's up?", router.id));
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

}
