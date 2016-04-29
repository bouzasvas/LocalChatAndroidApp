package test.android.vassilis.localchatting;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class selectClient extends AppCompatActivity {

    Button refreshButton, broadcastButton, setNick;
    EditText nickname;
    TextView label;

    String[] clients;
    ListView availableUsers;
    ArrayAdapter<String> adapter;

    connectAndDisplayAvailableClients conn = new connectAndDisplayAvailableClients();

    Handler handler;

    String host;
    String port;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_client);

        handler = new Handler(Looper.getMainLooper());

        nickname = (EditText) findViewById(R.id.nickname);
        setNick = (Button) findViewById(R.id.setNick);
        setNick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectionToServer();
                setNick.setVisibility(View.INVISIBLE);
                label.setVisibility(View.VISIBLE);
                availableUsers.setVisibility(View.VISIBLE);
                refreshButton.setVisibility(View.VISIBLE);
                broadcastButton.setVisibility(View.VISIBLE);
            }
        });

        refreshButton = (Button) findViewById(R.id.resfreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conn.initClients(false);
            }
        });

        broadcastButton = (Button) findViewById(R.id.broadcastButton);
        broadcastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conn.setBroadCastDest();
            }
        });

        label = (TextView) findViewById(R.id.chooseClientLabel);
        availableUsers = (ListView) findViewById(R.id.availableUsers);
        availableUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                int itemPosition = position;

                // ListView Clicked item value
                String itemValue = (String) availableUsers.getItemAtPosition(position);

                conn.communicateWithClient(itemPosition + 1, itemValue);
            }

        });

        Intent intent = getIntent();
        host = intent.getStringExtra("host");
        port = intent.getStringExtra("port");

        //connectionToServer();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    public void connectionToServer() {
        conn.execute(host, port);
    }

    public class connectAndDisplayAvailableClients extends AsyncTask<String, Void, Socket> {

        Socket connection = null;

        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        AlertDialog dialog = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            dialog = new AlertDialog.Builder(selectClient.this).create();
        }

        @Override
        protected Socket doInBackground(String... params) {
            String host = params[0];
            int port = Integer.valueOf(params[1]);

            if (isNetworkAvailable()) {
                try {
                    connection = new Socket(InetAddress.getByName(host), port);
                    out = new ObjectOutputStream(connection.getOutputStream());
                    in = new ObjectInputStream(connection.getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return connection;
        }

        @Override
        protected void onPostExecute(Socket soc) {
            super.onPostExecute(soc);

            if (soc != null) {
                SocketHandler.setSocket(soc);
                SocketHandler.setIn(in);
                SocketHandler.setOut(out);
                Toast.makeText(selectClient.this, "Successfully connected to " + soc.getInetAddress(), Toast.LENGTH_LONG).show();

                setNickName();
                initClients(true);
            } else {
                dialog.setTitle("Error!");
                dialog.setMessage("Connection failed");
                dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                System.exit(1);
                            }
                        });
                dialog.show();
            }
        }

        private void setNickName() {
            String nick = nickname.getText().toString();
            try {
                out.writeObject(nick);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        private void initClients(final boolean firstTime) {
            handler.post(new Runnable() {
                public void run() {
                    Runnable initCL = new Runnable() {
                        @Override
                        public void run() {
                            if (firstTime) {
                                try {
                                    clients = (String[]) in.readObject();
                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                showClients();
                            } else {
                                try {
                                    out.writeObject("0");
                                    clients = (String[]) in.readObject();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                }
                                showClients();
                            }
                        }
                    };

                    Thread thread = new Thread(initCL);
                    thread.start();
                }
            });
        }

        private void showClients() {
            handler.post(new Runnable() {
                public void run() {
                    adapter = new ArrayAdapter<>(selectClient.this, android.R.layout.simple_list_item_1, clients);
                    availableUsers.setAdapter(adapter);
                }
            });
        }

        private void communicateWithClient(int userID, String nick) {
            try {
                String userIDstr = String.valueOf(userID);
                out.writeObject(userIDstr);
                Intent mainActivity = new Intent(selectClient.this, MainActivity.class);
                mainActivity.putExtra("user", nick);
                startActivity(mainActivity);
            } catch (IOException clEx) {
                clEx.printStackTrace();
            }
        }

        private void setBroadCastDest() {
            //code for broadcast messages
            try {
                out.writeObject("def");
                Intent mainActivity = new Intent(selectClient.this, MainActivity.class);
                startActivity(mainActivity);
            } catch (IOException brEx) {
                brEx.printStackTrace();
            }
        }

        private Socket getSocket() {
            return this.connection;
        }

        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }
}
