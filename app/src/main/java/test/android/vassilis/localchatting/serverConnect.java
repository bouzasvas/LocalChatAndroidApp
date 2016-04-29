package test.android.vassilis.localchatting;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

public class serverConnect extends AppCompatActivity {

    EditText host;
    EditText port;
    Button connect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_connect);

        host = (EditText) findViewById(R.id.host);
        port = (EditText) findViewById(R.id.port);

        connect = (Button) findViewById(R.id.connectButton);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectClient = new Intent(v.getContext(), selectClient.class);
                selectClient.putExtra("host", host.getText().toString());
                selectClient.putExtra("port", port.getText().toString());
                startActivity(selectClient);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }
}
