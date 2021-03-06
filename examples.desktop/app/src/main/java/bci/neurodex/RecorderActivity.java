//Yo yo
package bci.neurodex;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGEegPower;
import com.neurosky.thinkgear.TGRawMulti;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class RecorderActivity extends Activity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_DISABLE_BT = 0;
    int pairedFlag = 0;
    double sum = 0;
    int count = 0;

    BluetoothAdapter mBluetoothAdapter;
    Switch btSelect;
    TextView pairedIndicator;
    Set<BluetoothDevice> pairedDevices;
    List<String> list;

    TGRawMulti rawData;
    TGDevice tgDevice;
    TGEegPower fbands;
    List<TGEegPower> points;
    //EEGPoint current;
    String thought;
    final boolean rawEnabled = false;

    TextView mindWaveIndicator;
    TextView attentionIndicator;
    TextView meditationIndicator;

    Button btnStop;
    /*
    TextView channel1;
    TextView channel2;
    TextView channel3;
    TextView channel4;
    TextView channel5;
    TextView channel6;
    TextView channel7;
    TextView channel8;
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recorder_layout);

        //points = new ArrayList<TGEegPower>();
        //db = new frequencyTable(this);
        //Bundle extras = getIntent().getExtras();
        //thought = extras.getString("thought");
        //current = new EEGPoint(thought);

        assignVariables();
        btSelect.setChecked(false);

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            btSelect.setClickable(false);
            pairedIndicator.setText("Bluetooth not supported.");

        } else {
            list = new ArrayList<>(); //The '<>' indicates String. No need of explicit declaration, it seems.

            pairedDevices = mBluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    list.add(device.getName() + "," + device.getAddress());
                    if (device.getName().startsWith("Mind")) {
                        pairedIndicator.setText("MindWave Mobile is paired.");
                        pairedFlag = 1;
                    }
                }

                if (pairedFlag == 0) {
                    pairedIndicator.setText("MindWave Mobile is not paired.");
                }
            }

            if (mBluetoothAdapter.isEnabled()) {
                btSelect.setChecked(true);
                tgDevice = new TGDevice(mBluetoothAdapter, handler);
                //Toast.makeText(getApplicationContext(), "The handler shit is done...", Toast.LENGTH_SHORT).show();
            }

            if (tgDevice.getState() != TGDevice.STATE_CONNECTING && tgDevice.getState() != TGDevice.STATE_CONNECTED)
                tgDevice.connect(rawEnabled);


            btSelect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                    if (isChecked) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } else {

                        Intent disableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(disableBtIntent, REQUEST_DISABLE_BT);
                    }

                }
            });
        }
        addListenerOnButton();
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


    @Override
    public void onDestroy() {
        super.onDestroy();
        tgDevice.close();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TGDevice.MSG_STATE_CHANGE:
                    switch (msg.arg1) {
                        case TGDevice.STATE_IDLE:
                            break;
                        case TGDevice.STATE_CONNECTING:
                            //Toast.makeText(getApplicationContext(), "Connecting ...",Toast.LENGTH_SHORT).show();
                            //myTextView = (TextView) findViewById(R.id.connectStatus);
                            mindWaveIndicator.setText("Connecting ...");
                            break;
                        case TGDevice.STATE_CONNECTED:
                            //Toast.makeText(getApplicationContext(), "Connected",Toast.LENGTH_SHORT).show();
                            //myTextView = (TextView) findViewById(R.id.connectStatus);
                            mindWaveIndicator.setText("Connected");
                            tgDevice.start();
                            break;
                        case TGDevice.STATE_NOT_FOUND:
                            //Toast.makeText(getApplicationContext(), "Connection Not Found, reset and try again.",Toast.LENGTH_SHORT).show();
                            //myTextView = (TextView) findViewById(R.id.connectStatus);
                            mindWaveIndicator.setText("Connection Not Found, reset and try again.");
                            break;
                        case TGDevice.STATE_NOT_PAIRED:
                            //Toast.makeText(getApplicationContext(), "There is not Mindset paired to this device.",Toast.LENGTH_SHORT).show();
                            //myTextView = (TextView) findViewById(R.id.connectStatus);
                            mindWaveIndicator.setText("There is not Mindset paired to this device.");
                            break;
                        case TGDevice.STATE_DISCONNECTED:
                            //Toast.makeText(getApplicationContext(), "Disconnected",Toast.LENGTH_SHORT).show();
                            //myTextView = (TextView) findViewById(R.id.connectStatus);
                            mindWaveIndicator.setText("Disconnected");
                    }
                    break;
                case TGDevice.MSG_POOR_SIGNAL:
                    //myTextView = (TextView) findViewById(R.id.signal);
                    if (msg.arg1 == 0) {
                        //Toast.makeText(getApplicationContext(), "Great Signal!",Toast.LENGTH_SHORT).show();
                        //myTextView.setText("Great");
                    } else if (msg.arg1 > 50) {
                        //Toast.makeText(getApplicationContext(), "Poor signal; please adjust headset.",Toast.LENGTH_SHORT).show();
                        //myTextView.setText("Poor, please readjust headset.");
                    }
                    break;
                case TGDevice.MSG_ATTENTION:
                    //mProgress = (ProgressBar) findViewById(R.id.attentionBar);
                    //mProgress.setProgress(msg.arg1);
                    //Toast.makeText(getApplicationContext(), "Attention:"+msg.arg1,Toast.LENGTH_SHORT).show();
                    //saveData(msg.arg1);
                    sum += msg.arg1;
                    count++;

                    attentionIndicator.setText("Attention: " + msg.arg1);
                    break;
                case TGDevice.MSG_MEDITATION:
                    meditationIndicator.setText("Meditation: " + msg.arg1);
                    break;
                case TGDevice.MSG_BLINK:
                    //tv.append("Blink: " + msg.arg1 + "\n");
                    //Toast.makeText(getApplicationContext(), "Ha! You blinked :P",Toast.LENGTH_SHORT).show();
                    break;
                case TGDevice.MSG_EEG_POWER:
                    //fbands = (TGEegPower)msg.obj;
                    //points.add(fbands);
                    //myTextView = (TextView) findViewById(R.id.ch1);
                    //myTextView.setText("Delta " + fbands.delta);
                    //channel1.setText("Delta: " + fbands.delta);
                    //myTextView = (TextView) findViewById(R.id.ch2);
                    //myTextView.setText("High Alpha " + fbands.highAlpha);
                    //channel2.setText("High Alpha: " + fbands.highAlpha);
                    //myTextView = (TextView) findViewById(R.id.ch3);
                    //myTextView.setText("High Beta " + fbands.highBeta);
                    //channel3.setText("High Beta: " + fbands.highBeta);
                    //myTextView = (TextView) findViewById(R.id.ch4);
                    //myTextView.setText("Low Alpha " + fbands.lowAlpha);
                    //channel4.setText("Low Alpha: " + fbands.lowAlpha);
                    //myTextView = (TextView) findViewById(R.id.ch5);
                    //myTextView.setText("Low Beta " + fbands.lowBeta);
                    //channel5.setText("Low Beta: " + fbands.lowBeta);
                    //myTextView = (TextView) findViewById(R.id.ch6);
                    //myTextView.setText("Low Gamma " + fbands.lowGamma);
                    //channel6.setText("Low Gamma: " + fbands.lowGamma);
                    //myTextView = (TextView) findViewById(R.id.ch7);
                    //myTextView.setText("Mid Gamma " + fbands.midGamma);
                    //channel7.setText("Mid Gamma: " + fbands.midGamma);
                    //myTextView = (TextView) findViewById(R.id.ch8);
                    //myTextView.setText("Theta " + fbands.theta);
                    //channel8.setText("Theta: " + fbands.theta);
                    break;
                case TGDevice.MSG_LOW_BATTERY:
                    Toast.makeText(getApplicationContext(), "Low battery!", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }

        }
    };


    void assignVariables() {
        btSelect = (Switch) findViewById(R.id.btSwitch);
        pairedIndicator = (TextView) findViewById(R.id.pairedStatus);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mindWaveIndicator = (TextView) findViewById(R.id.mindwaveStatus);
        attentionIndicator = (TextView) findViewById(R.id.attentionLabel);
        meditationIndicator = (TextView) findViewById(R.id.meditationLabel);

        btnStop = (Button) findViewById(R.id.stopButton);
    /*
        channel1 = (TextView) findViewById(R.id.ch1);
        channel2 = (TextView) findViewById(R.id.ch2);
        channel3 = (TextView) findViewById(R.id.ch3);
        channel4 = (TextView) findViewById(R.id.ch4);
        channel5 = (TextView) findViewById(R.id.ch5);
        channel6 = (TextView) findViewById(R.id.ch6);
        channel7 = (TextView) findViewById(R.id.ch7);
        channel8 = (TextView) findViewById(R.id.ch8);
        */
    }


    public void saveData(int passData) {
        String filename = "/sdcard/myfile.txt";
        //String string = "1,2,3,4\n";
        FileOutputStream outputStream;

        try {
            File sdCard = Environment.getExternalStorageDirectory();
            File dir = new File(sdCard.getAbsolutePath() + "/synapse/");
            dir.mkdirs();


            File file = new File(dir, "filename");

            System.out.println(passData);
            FileOutputStream f = new FileOutputStream(file, true);

            String string1 = new Integer(passData).toString();
            string1 = string1 + "\n";

            //f.write(string.getBytes());
            //bf.write(passData.getBytes());
            f.write(string1.getBytes());
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static class ReadCVS {

        public static void main(String[] args) {

            ReadCVS obj = new ReadCVS();
            obj.run();

        }

        public void run() {

            String csvFile = "/synapse/filename";
            BufferedReader br = null;
            String line;
            String cvsSplitBy = ",";

            try {

                br = new BufferedReader(new FileReader(csvFile));
                while ((line = br.readLine()) != null) {

                    // use comma as separator
                    String[] country = line.split(cvsSplitBy);

                    System.out.println("Country [code= " + country[0]
                            + " , name=" + country[1] + "]");

                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("Done");
        }

    }


    public void readFile(View v) {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/synapse/");


        File file = new File(dir, "filename");

        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }

//Find the view by its id
        //TextView tv = (TextView)findViewById(R.id.text_view);

//Set the text
        //tv.setText(text);

    }

    public void addListenerOnButton() {

        btnStop = (Button) findViewById(R.id.stopButton);
        btnStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                saveData((int) (sum / count));
                sum = 0;
                count = 0;

            }

        });

    }
}
