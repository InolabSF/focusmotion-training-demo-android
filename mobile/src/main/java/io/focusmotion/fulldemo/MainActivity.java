package io.focusmotion.trainingdemo;

import io.focusmotion.sdk.*;
import io.focusmotion.sdk.pebble.*;
import io.focusmotion.sdk.band.*;
import io.focusmotion.sdk.wear.*;

import android.app.*;
import android.os.*;
import android.content.*;
import android.text.method.*;
import android.util.Log;
import android.view.*;
import android.widget.*;

//import java.util.UUID;
import java.util.*;



public class MainActivity extends Activity implements DeviceListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_startButton = (Button) findViewById(R.id.start_button);
        m_startButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View v)
                    {
                        onStartButtonPressed();
                    }
                } );
        m_trainButton = (Button) findViewById(R.id.train_button);
        m_trainButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View v)
                    {
                        onTrainButtonPressed();
                    }
                } );
        m_clearButton = (Button) findViewById(R.id.clear_button);
        m_clearButton.setOnClickListener(
                new View.OnClickListener()
                {
                    public void onClick(View v)
                    {
                        onClearButtonPressed();
                    }
                } );

        m_resultsLabel = (TextView) findViewById(R.id.results_label);
        m_statusLabel = (TextView) findViewById(R.id.status_label);
        m_dataSetsLabel = (TextView) findViewById(R.id.data_sets_label);


        // initialize FocusMotion SDK
        Config config = new Config();

        // This is your API key; keep it secret!
        if (!FocusMotion.initialize(config, "2lIC6lPcPAVNdpq4kvKhUpACobirYB4z", this))
        {
            throw new Error("Could not initialize FocusMotion SDK");
        }

        // initialize general device support
        Device.addListener(this);

        // initialize Pebble support
        // the UUID is for the "simple" Pebble app, defined in fm/src/samples/simple/pebble/appinfo.json
        UUID uuid = UUID.fromString("f3fef676-0c23-41b9-8d23-ba225575b9a0");
        PebbleDevice.initialize(this, uuid);

        // initialize Microsoft Band support
        BandDevice.initialize(this);

        // initialize Android Wear support
        WearDevice.initialize(this);

        // create trainer for movement called "demo"
        m_trainer = new AnalyzerTrainer("demo");

        m_resultsLabel.setText("");
        updateStatusLabel();
        updateStartButton();
        updateTrainButton();
        updateDataSetsLabel();
    }

    @Override
    protected void onDestroy()
    {
        FocusMotion.shutdown();

        super.onDestroy();
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        updateStatusLabel();
        updateStartButton();
        updateTrainButton();
        updateDataSetsLabel();
    }

    @Override
    protected void onStop()
    {
        Device.onStop();

        super.onStop();
    }

    ////////////////////////////////////////

    // UI
    private TextView m_statusLabel;
    private Button m_startButton;
    private Button m_trainButton;
    private TextView m_dataSetsLabel;
    private Button m_clearButton;
    private TextView m_resultsLabel;

    private Device m_device; // the current device
    private DeviceOutput m_output;
    private AnalyzerTrainer m_trainer;


    ////////////////////////////////////////
    // button handlers

    private void onStartButtonPressed()
    {
        if (m_device != null)
        {
            if (m_device.isRecording())
            {
                m_device.stopRecording();
            }
            else
            {
                m_device.startRecording();
            }
        }
    }

    private void onTrainButtonPressed()
    {
        // train the analyzer with the data we just recorded.
        // NOTE: to simplify the user interface, we are assuming the user always performed 10 reps; in practice,
        // you would probably want the user to enter how many reps he just did.
        m_trainer.addTrainingDataSet(m_output, 10);
        m_trainer.train();

        m_output = null;

        updateTrainButton();
        updateDataSetsLabel();
    }

    private void onClearButtonPressed()
    {
        m_trainer.reset();
        updateDataSetsLabel();
    }

    ////////////////////////////////////////

    private void updateStatusLabel()
    {
        if (m_device != null)
        {
            m_statusLabel.setText(String.format("%s: %s", m_device.getName(), (m_device.isConnected() ? "connected" : "disconnected")));
        }
        else
        {
            m_statusLabel.setText("no available devices");
        }
    }

    private void updateStartButton()
    {
        if (m_device != null && m_device.isConnected())
        {
            m_startButton.setEnabled(true);

            if (m_device.isRecording())
            {
                m_startButton.setText(R.string.stop_recording);
            }
            else
            {
                m_startButton.setText(R.string.start_recording);
            }
        }
        else
        {
            m_startButton.setEnabled(false);
        }
    }

    private void updateTrainButton()
    {
        m_trainButton.setEnabled(m_output != null);
    }

    private void updateDataSetsLabel()
    {
        m_dataSetsLabel.setText("Training data sets: " + m_trainer.getNumTrainingDataSets());
        m_clearButton.setEnabled(m_trainer.getNumTrainingDataSets() > 0);
    }

    ////////////////////////////////////////
    // analysis

    private void analyze()
    {
        logInfo("analyzing...");

        // create analyzer for our "demo" analyzer
        MovementAnalyzer analyzer = MovementAnalyzer.createTrainedSingleMovementAnalyzer("demo");

        // count reps
        analyzer.analyze(m_output);

        // update the UI
        if (analyzer.getNumResults() > 0)
        {
            AnalyzerResult result = analyzer.getResult(0);
            m_resultsLabel.setText(String.format(
                    "%d reps\n" +
                            "duration %.2fs\n" +
                            "rep time %.2f (%.2f-%.2f)\n" +
                            "variation %.2f\n",
                    result.repCount,
                    result.duration,
                    result.meanRepTime, result.minRepTime, result.maxRepTime,
                    result.internalVariation));
        }
        else
        {
            m_resultsLabel.setText("(no result)");
        }

        analyzer.destroy();

        logInfo("...done");
    }

    private static void logInfo(String msg)
    {
        Log.i(MainActivity.class.getName(), msg);
    }

    ////////////////////////////////////////
    // DeviceListener

    @Override
    public void onAvailableChanged(Device device, boolean available)
    {
        // We are only supporting Pebble, and at most one Pebble can be paired at a time;
        // so just connect automatically when we detect one.
        if (available)
        {
            m_device = device;
            m_device.connect();
        }

        updateStatusLabel();
    }

    @Override
    public void onConnectedChanged(Device device, boolean connected)
    {
        updateStartButton();
        updateStatusLabel();
    }

    @Override
    public void onRecordingChanged(Device device, boolean recording)
    {
        updateStartButton();

        if (!recording)
        {
            // just stopped recording
            m_output = m_device.getOutput();
            if (m_trainer.getNumTrainingDataSets() > 0)
            {
                // we have already trained our analyzer; try counting reps.
                analyze();
            }
        }
        else
        {
            m_resultsLabel.setText("");
        }

        updateTrainButton();
    }

    @Override
    public void onDataReceived(Device device)
    {
    }

    @Override
    public void onConnectionFailed(Device device, ConnectionError error, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setTitle("Connection failed!");
        builder.setPositiveButton("OK",
                new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                } );
        builder.create().show();
    }

}

/*
package io.focusmotion.fulldemo;

import io.focusmotion.sdk.*;
import io.focusmotion.sdk.pebble.*;
import io.focusmotion.sdk.band.*;
import io.focusmotion.sdk.wear.*;

import android.app.*;
import android.os.*;
import android.content.*;
import android.text.method.*;
import android.util.Log;
import android.view.*;
import android.widget.*;

import java.util.*;


public class MainActivity extends Activity implements DeviceListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_startButton = (Button) findViewById(R.id.start_button);
        m_startButton.setOnClickListener(
                new View.OnClickListener() 
                {
                    public void onClick(View v) 
                    {
                        onStartButtonPressed();
                    }
                } );
        m_connectButton = (Button) findViewById(R.id.connect_button);
        m_connectButton.setOnClickListener(
                new View.OnClickListener() 
                {
                    public void onClick(View v) 
                    {
                        onConnectButtonPressed();
                    }
                } );

        m_resultsLabel = (TextView) findViewById(R.id.results_label);
        m_resultsLabel.setMovementMethod(new ScrollingMovementMethod());
        m_statusLabel = (TextView) findViewById(R.id.status_label);


        // initialize FocusMotion SDK
        Config config = new Config();

        // This is your API key; keep it secret!
        if (!FocusMotion.initialize(config, "2lIC6lPcPAVNdpq4kvKhUpACobirYB4z", this))
        {
            throw new Error("Could not initialize FocusMotion SDK");
        }

        // initialize general device support
        Device.addListener(this);

        // initialize Pebble support
        // the UUID is for the "simple" Pebble app, defined in fm/src/samples/simple/pebble/appinfo.json
        UUID uuid = UUID.fromString("f3fef676-0c23-41b9-8d23-ba225575b9a0");
        PebbleDevice.initialize(this, uuid);

        // initialize Microsoft Band support
        BandDevice.initialize(this);

        // initialize Android Wear support
        WearDevice.initialize(this);

        m_resultsLabel.setText("");
        updateStatusLabel();
        updateConnectButton();
        updateStartButton();


        {
            // populate spinner with analysis types
            m_analyzerSpinner = (Spinner) findViewById(R.id.analysis_spinner);
            String[] labels = new String[] 
            {
                "Single movement",
                "Multiple movement"
            };
            ArrayAdapter analysisAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
            analysisAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            m_analyzerSpinner.setAdapter(analysisAdapter);

            m_analyzerSpinner.setOnItemSelectedListener(
                    new AdapterView.OnItemSelectedListener()
                    {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
                        {
                            m_movementSpinner.setEnabled(pos != 1);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
        }

        {
            // populate spinner with movement types
            m_movementSpinner = (Spinner) findViewById(R.id.movement_spinner);
            ArrayList<String> labels = new ArrayList<>();

            labels.add("(unknown)");

            int numMovements = FocusMotion.getNumMovements();
            for (int i = 0; i < numMovements; ++i)
            {
                labels.add(FocusMotion.getMovementDisplayName(FocusMotion.getMovementType(i)));
            }

            ArrayAdapter movementAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
            movementAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            m_movementSpinner.setAdapter(movementAdapter);
        }

    }

    @Override
    protected void onDestroy()
    {
        FocusMotion.shutdown();

        super.onDestroy();
    }

    @Override
    protected void onStart() 
    {
        super.onStart();

        Device.onStart();

        updateStatusLabel();
        updateConnectButton();
        updateStartButton();
    }

    @Override
    protected void onStop() 
    {
        Device.onStop();

        super.onStop();
    }


    ////////////////////////////////////////

    // UI
    private Button m_startButton;
    private Button m_connectButton;
    private Spinner m_analyzerSpinner;
    private Spinner m_movementSpinner;
    private TextView m_resultsLabel;
    private TextView m_statusLabel;

    private Device m_device; // the current device


    ////////////////////////////////////////
    // button handlers

    private void onConnectButtonPressed()
    {
        if (m_device != null)
        {
            if (!m_device.isConnected())
            {
                m_device.connect();
            }
            else
            {
                m_device.disconnect();
            }
        }

        updateConnectButton();
    }

    private void onStartButtonPressed()
    {
        if (m_device != null)
        {
            if (m_device.isRecording())
            {
                m_device.stopRecording();
            }
            else
            {
                m_device.startRecording();
            }
        }
    }

    ////////////////////////////////////////

    private void updateStatusLabel()
    {
        if (m_device != null)
        {
            m_statusLabel.setText(String.format("%s: %s", m_device.getName(), (m_device.isConnected() ? "connected" : "disconnected")));
        }
        else
        {
            m_statusLabel.setText("no available devices");
        }
    }

    private void updateConnectButton()
    {
        if (m_device != null)
        {
            if (m_device.isConnected())
            {
                m_connectButton.setText("Disconnect");
                m_connectButton.setEnabled(true);
            }
            else if (m_device.isConnecting())
            {
                m_connectButton.setText("Connecting...");
                m_connectButton.setEnabled(false);
            }
            else
            {
                m_connectButton.setEnabled(true);
                m_connectButton.setText("Connect");
            }
        }
        else
        {
            m_connectButton.setText("Connect");
            m_connectButton.setEnabled(false);
        }
    }

    private void updateStartButton()
    {
        if (m_device != null && m_device.isConnected())
        {
            m_startButton.setEnabled(true);

            if (m_device.isRecording())
            {
                m_startButton.setText(R.string.stop_recording);
            }
            else
            {
                m_startButton.setText(R.string.start_recording);
            }
        }
        else
        {
            m_startButton.setEnabled(false);
        }
    }

    ////////////////////////////////////////
    // analysis

    private void analyze()
    {
        Log.i(getLocalClassName(), "analyzing...");

        // get the data that has been sent from the device
        DeviceOutput data = Device.getLastConnectedDevice().getOutput();

        // which movement type?
        int movementPos = m_movementSpinner.getSelectedItemPosition();
        String movementType = null;
        if (movementPos > 0)
        {
            movementType = FocusMotion.getMovementType(movementPos-1);
        }

        // which analyzer?
        int analyzerPos = m_analyzerSpinner.getSelectedItemPosition();
        MovementAnalyzer analyzer = null;
        switch (analyzerPos)
        {
            case 0: 
                analyzer = MovementAnalyzer.createSingleMovementAnalyzer(movementType); 
                break;

            case 1: 
                analyzer = MovementAnalyzer.createMultipleMovementAnalyzer();
                break;
        }

        analyzer.analyze(data);
        showResults(analyzer);
        analyzer.destroy();
    }

    private void showResults(MovementAnalyzer analyzer)
    {
        if (analyzer.getNumResults() > 0)
        {
            String text = "";
            for (int i = 0; i < analyzer.getNumResults(); ++i)
            {
                AnalyzerResult result = analyzer.getResult(i);
                if (result.movementType.equals("resting"))
                {
                    text += String.format("Resting: %.2fs\n", result.duration);
                }
                else
                {
                    text += String.format(
                            "%s\n" +
                            "  %d reps\n" +
                            "  duration %.2fs\n" +
                            "  rep time %.2f (%.2f-%.2f)\n" +
                            "  variation %.2f\n" +
                            "  ref variation %.2f\n" +
                            "  ref rep time %.2f\n",
                            FocusMotion.getMovementDisplayName(result.movementType), 
                            result.repCount,
                            result.duration,
                            result.meanRepTime, result.minRepTime, result.maxRepTime,
                            result.internalVariation,
                            result.referenceVariation,
                            result.referenceRepTime);
                }
                text += "\n";
            }
            m_resultsLabel.setText(text);
        }
        else
        {
            m_resultsLabel.setText("(no result)");
        }
    }


    ////////////////////////////////////////
    // DeviceListener

    @Override
    public void onAvailableChanged(Device device, boolean available)
    {
        if (available && m_device == null)
        {
            // didn't have a device before; set this to the current one
            m_device = device;
        }

        if (!available && m_device == device)
        {
            // just lost our current device; get another one, if possible
            m_device = Device.getAvailableDevices().get(0);
        }

        updateConnectButton();
        updateStatusLabel();
    }

    @Override
    public void onConnectedChanged(Device device, boolean connected)
    {
        if (!connected)
        {
            List<Device> availableDevices = Device.getAvailableDevices();
            if (availableDevices.isEmpty())
            {
                m_device = null;
            }
            else
            {
                // choose the next device
                int index = availableDevices.indexOf(device);
                ++index;
                if (index >= availableDevices.size())
                {
                    index = 0;
                }
                m_device = availableDevices.get(index);
            }
        }

        updateConnectButton();
        updateStartButton();
        updateStatusLabel();
    }

    @Override
    public void onRecordingChanged(Device device, boolean recording)
    {
        updateStartButton();
        if (!recording)
        {
            analyze();
        }
        else
        {
            m_resultsLabel.setText("");
        }
    }

    @Override
    public void onDataReceived(Device device)
    {
    }

    @Override 
    public void onConnectionFailed(Device device, ConnectionError error, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setTitle("Connection failed!");
        builder.setPositiveButton("OK", 
                new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) 
                    {   
                        dialog.dismiss();           
                    }
                } );
        builder.create().show();

        updateConnectButton();
        updateStatusLabel();
    }

}
*/