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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_PHOTO = 100;

    String port;
    String host;

    connectToServer connection = new connectToServer();

    TextView myMessages, lobbyMessages;
    EditText newMessage;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myMessages = (TextView) findViewById(R.id.myMessages);
        lobbyMessages = (TextView) findViewById(R.id.lobbyMessages);
        newMessage = (EditText) findViewById(R.id.newMessage);

        Intent intent = getIntent();
        host = intent.getStringExtra("host");
        port = intent.getStringExtra("port");

        handler = new Handler(Looper.getMainLooper());
        connectionToServer();
    }

    // Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.app_menu, menu);
        return true;
    }

    /**
     * Event Handling for Individual menu item selected
     * Identify single menu item by it's id
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.disconnect:
                Intent reconnect = new Intent(MainActivity.this, serverConnect.class);
                startActivity(reconnect);
                finish();
                return true;
            case R.id.sendImage:
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType("image/*");
                startActivityForResult(photoPickerIntent, SELECT_PHOTO);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connection.close();
    }

    public void connectionToServer() {
        connection.execute(host, port);
    }

    public void setTextMsg(int which, String msg) {
        if (which == 0) {
            myMessages.append("-" + newMessage.getText().toString() + "\n");
            newMessage.setText("");
        } else if (which == 1) {
            lobbyMessages.append("-" + msg + "\n");
        }
    }

    public class connectToServer extends AsyncTask<String, Void, Socket> {

        Socket connection = null;

        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        AlertDialog dialog = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            dialog = new AlertDialog.Builder(MainActivity.this).create();
        }

        @Override
        protected Socket doInBackground(String... params) {
            String host = (String) params[0];
            int port = Integer.valueOf((String) params[1]);

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
                Toast.makeText(MainActivity.this, "Successfully connected to " + soc.getInetAddress(), Toast.LENGTH_LONG).show();
                getMsg();
                sendMsg();
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


        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }

        public void getMsg() {
            Runnable get = new Runnable() {
                String msg = null;

                @Override
                public void run() {
                    while (true) {
                        if (connection.isClosed())
                            break;
                        String messageFromUser = null;
                        try {
                            messageFromUser = (String) in.readObject();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        msg = messageFromUser;
                        handler.post(new Runnable() {
                            public void run() {
                                setTextMsg(1, msg);
                            }
                        });
                    }
                }
            };
            Thread getM = new Thread(get);
            getM.start();
        }

        public void sendMsg() {
            Runnable send = new Runnable() {
                @Override
                public void run() {
                    newMessage.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            boolean handled = false;
                            if (actionId == EditorInfo.IME_ACTION_SEND) {
                                if (!(newMessage.getText().toString().equals(""))) {
                                    handler.post(new Runnable() {
                                        public void run() {
                                            setTextMsg(0, newMessage.getText().toString());
                                        }
                                    });
                                    String messageToSend = newMessage.getText().toString();
                                    try {
                                        out.writeObject(messageToSend);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    handled = true;
                                }
                            }
                            return handled;
                        }
                    });
                }
            };
            Thread sendM = new Thread(send);
            sendM.start();
        }

        public void close() {
            try {
                out.close();
                in.close();
                connection.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
