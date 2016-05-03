package test.android.vassilis.localchatting;

/**
 * Created by Vassilis on 3/5/2016.
 */

import android.os.AsyncTask;
import android.os.Environment;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.*;

//Class for save the conversation with a user
public class ReadWriteConversation {

    String filename;

    File file = null;
    BufferedWriter writer = null;
    BufferedReader reader = null;

    public ReadWriteConversation(String user) {
        this.filename = "conv_" + user;
        initializeClassMembers();
    }

    private void initializeClassMembers() {
        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File path = new File(sdCard + "/localChatting");
            if (!path.exists()) {
                path.mkdir();
            }
            file = new File(path, filename);

            if (!file.exists())
                file.createNewFile();
        } catch (IOException initEx) {
            initEx.printStackTrace();
        }
    }


    public void writeFile(String msg, boolean me) {
        String sym = null;
        if (me)
            sym = "$";
        else
            sym = "%";

        if (!msg.contains("null")) {
            try {
                writer = new BufferedWriter(new FileWriter(file, true));
                writer.write(sym + msg + "\n");
                //writer.append(text.getText().toString() + "\n");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    writer.close();
                } catch (IOException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    public void readFile(boolean me, TextView text) {
        String line = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                if (me) {
                    if (line.startsWith("$")) {
                        text.append(line.substring(1) + "\n");
                    }
                } else {
                    if (line.startsWith("%")) {
                        text.append(line.substring(1) + "\n");
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException clEx) {
                clEx.printStackTrace();
            }
        }
    }
}
