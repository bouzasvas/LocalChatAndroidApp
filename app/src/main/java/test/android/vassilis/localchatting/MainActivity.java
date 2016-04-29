package test.android.vassilis.localchatting;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_PHOTO = 100;

    NotificationCompat.Builder mBuilder;

    exchangeMsgs communication = new exchangeMsgs();
    Socket connection = null;

    TextView myMessages, lobbyMessages, lobbyLabel;
    EditText newMessage;

    String user;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myMessages = (TextView) findViewById(R.id.myMessages);
        lobbyMessages = (TextView) findViewById(R.id.lobbyMessages);

        lobbyLabel = (TextView) findViewById(R.id.lobbyLabel);

        newMessage = (EditText) findViewById(R.id.newMessage);

        handler = new Handler(Looper.getMainLooper());
        connection = SocketHandler.getSocket();

        Intent intent = getIntent();
        user = intent.getStringExtra("user");

        lobbyLabel.setText(user);

        initNoticationBuilder();

        startCommunication();
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

    private void initNoticationBuilder() {
        mBuilder =
                new NotificationCompat.Builder(MainActivity.this)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle(getString(R.string.notificationTitle))
                        .setContentText(getString(R.string.notificationDes) + " " + user)
                        .setAutoCancel(true);

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(sound);

//        Intent resultIntent = new Intent(this, MainActivity.class);
//        resultIntent.setAction(Intent.ACTION_MAIN);
//        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//
//        PendingIntent resultPendingIntent =
//                PendingIntent.getActivity(
//                        this,
//                        0,
//                        resultIntent,
//                        0
//                );
//

//        mBuilder.setContentIntent(resultPendingIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        communication.close();
    }

    public void startCommunication() {
        communication.execute(connection);
    }

    public void setTextMsg(int which, String msg) {
        if (which == 0) {
            myMessages.append("-" + newMessage.getText().toString() + "\n");
            newMessage.setText("");
        } else if (which == 1) {
            lobbyMessages.append("-" + msg + "\n");
        }
    }

    public class exchangeMsgs extends AsyncTask<Socket, Void, Socket> {

        Socket connection = null;

        ObjectOutputStream out = null;
        ObjectInputStream in = null;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Socket doInBackground(Socket... params) {
            this.connection = params[0];
            out = SocketHandler.getOut();
            in = SocketHandler.getIn();

            return connection;
        }

        @Override
        protected void onPostExecute(Socket soc) {
            super.onPostExecute(soc);
            getMsg();
            sendMsg();

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
                            notifyForNewMsg();
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

        public void notifyForNewMsg() {
            int mNotificationId = 001;
            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
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
