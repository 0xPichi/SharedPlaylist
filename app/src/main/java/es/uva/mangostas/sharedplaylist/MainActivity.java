package es.uva.mangostas.sharedplaylist;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button serverButton = (Button) findViewById(R.id.buttonServer);
        serverButton.setOnClickListener(this);

        Button clientButton = (Button) findViewById(R.id.buttonClient);
        clientButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonServer :
                startActivity(new Intent(this, ServerActivity.class));
                break;

            case R.id.buttonClient :
                startActivity(new Intent(this, ClientActivity.class));
                break;
        }

    }

}
