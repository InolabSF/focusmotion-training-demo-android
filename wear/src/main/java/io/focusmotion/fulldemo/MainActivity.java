package io.focusmotion.trainingdemo;

import io.focusmotion.sdk.wear.*;

import android.app.Activity;
import android.os.*;
import android.support.wearable.view.WatchViewStub;
import android.view.*;
import android.widget.*;


public class MainActivity extends Activity implements LocalDeviceListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(
                new WatchViewStub.OnLayoutInflatedListener() 
                {
                    @Override
                    public void onLayoutInflated(WatchViewStub stub) 
                    {
                        m_recordButton = (ToggleButton) stub.findViewById(R.id.recordButton);
                        m_recordButton.setOnClickListener(
                            new View.OnClickListener() 
                            {
                                public void onClick(View v) 
                                {
                                    onClickRecord();
                                }
                            } );

                        updateUI();
                    }
                });

        m_vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        m_device = new LocalDevice(this);
        m_device.addListener(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        m_device.activityStart();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        m_device.activityDestroy();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        m_device.activityPause();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        m_device.activityResume();
    }

    ////////////////////////////////////////
    // LocalDeviceListener

    @Override
    public void onAvailableChanged(LocalDevice device, boolean available)
    {
        m_device.connect();
    }

    @Override
    public void onConnectedChanged(LocalDevice device, boolean connected)
    {
        updateUI();
    }

    @Override
    public void onRecordingChanged(LocalDevice device, boolean recording)
    {
        if (m_vibrator != null)
        {
            m_vibrator.vibrate(recording ? 250 : 100);
        }
        updateUI();
    }

    @Override
    public void onDataSent(LocalDevice device)
    {
    }

    ////////////////////////////////////////

    private ToggleButton m_recordButton;
    private LocalDevice m_device;
    private Vibrator m_vibrator;

    private void updateUI()
    {
        m_recordButton.setChecked(m_device.isRecording());
        m_recordButton.setEnabled(m_device.isConnected());
    }

    private void onClickRecord()
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
