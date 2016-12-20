package es.uva.mangostas.sharedplaylist;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;


import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final int REQUEST_ENABLE_BT = 3;
    private BluetoothAdapter btAdapter;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        btAdapter = BluetoothAdapter.getDefaultAdapter();

        Button serverButton = (Button) findViewById(R.id.buttonServer);
        serverButton.setOnClickListener(this);

        Button clientButton = (Button) findViewById(R.id.buttonClient);
        clientButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {



            } else {


                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        1);
            }
        }

        if (!btAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonServer :
                final File appState = new File(getApplicationContext().getCacheDir(),"appState");
                final Intent intentServidor = new Intent(this,ServerActivity.class);

                if(appState.exists()){
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.createlist)
                            .setTitle(R.string.copyfound)
                            .setCancelable(false)
                            .setNegativeButton(R.string.neww,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            appState.delete();
                                            startActivity(intentServidor);
                                            //dialog.cancel
                                        }
                                    })
                            .setPositiveButton(R.string.restore,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            startActivity(intentServidor);
                                        }
                                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    startActivity(intentServidor);
                }



                break;

            case R.id.buttonClient :
                startActivity(new Intent(this, ClientActivity.class));
                break;
        }

    }



}
