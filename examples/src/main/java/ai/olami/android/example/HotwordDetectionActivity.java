/*
	Copyright 2017, VIA Technologies, Inc. & OLAMI Team.

	http://olami.ai

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package ai.olami.android.example;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import ai.olami.android.hotwordDetection.HotwordDetect;
import ai.olami.android.hotwordDetection.IHotwordDetectListener;

public class HotwordDetectionActivity extends AppCompatActivity {

    static final String TAG = "HotwordDetectActivity";

    private HotwordDetect mHotwordDetect = null;
    private AudioRecord mAudioRecord = null;

    private TextView mDisplayText = null;
    private Switch mHotwordDetectSwitch = null;
    private long mDetectedCount = 0;

    private static final int REQUEST_EXTERNAL_PERMISSION = 1;
    private static final int REQUEST_MICROPHONE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void onResume() {
        super.onResume();

        setContentView(R.layout.activity_main);

        mDisplayText = (TextView) findViewById(R.id.displayText);

        // Listen the status of the hotword detection manual switch.
        mHotwordDetectSwitch = (Switch) findViewById(R.id.hotwordDetectSwitch);
        mHotwordDetectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startHotwordDetection();
                } else {
                    stopHotwordDetection();
                }
            }
        });

        hotwordDetectSwitchEnableChangeHandler(false);

        Log.i(TAG, "checkPermission = "+ checkDeviceResourcePermissions());
        // Check hardware resource permissions
        if (checkDeviceResourcePermissions()) {
            try {
                mDetectedCount = 0;
                initializeHotwordDetection();
                startHotwordDetection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    protected void onPause() {
        super.onPause();

        if (mHotwordDetect != null) {
            mHotwordDetect.stopDetection();
            mHotwordDetect.release();
            mHotwordDetect = null;
        }

        stopAndReleaseAudioRecord();
    }

    /**
     * Initial hotword detection
     */
    private void initializeHotwordDetection() {
        try {
            if (mHotwordDetect == null) {

                createAudioRecord();
                startRecording();

                // * Create HotwordDetect instance by the specified AudioRecord object.
                //   ------------------------------------------------------------------------------
                //   You should implement the IHotwordDetectListener to get all callbacks
                //   and assign the instance of your listener class into HotwordDetect object.
                mHotwordDetect = HotwordDetect.create(
                        mAudioRecord,
                        this.getApplicationContext(),
                        new HotwordDetectListener()
                );

                // * In this example, we set the resource control mode as auto release.
                //   ------------------------------------------------------------------------------
                //   It means the AudioRecord will be released after each detection task completed
                //   or hotward has been detected.
                 mHotwordDetect.setResourceControlMode(HotwordDetect.RESOURCE_CONTORL_MODE_AUTO_RELEASE);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Start hotword detection
     */
    private void startHotwordDetection() {
        if (mHotwordDetect != null) {
            try {

                // * You can SKIP this step if you are using RESOURCE_CONTROL_MODE_ALWAYS_ON
                //   to handle microphone resource.
                //   -------------------------------------------------------------------------------
                //   In this example,
                //   we have set the resource control mode to RESOURCE_CONTORL_MODE_AUTO_RELEASE.
                //   -> see initializeHotwordDetection() method
                //   So we MUST to create/re-create an AudioRecord object and initial it, including:
                //   (1) Create AudioRecord object
                //   (2) Start recording
                //   (3) Re-assign to the HotwordDetect object.

                createAudioRecord();
                startRecording();
                mHotwordDetect.setAudioRecord(mAudioRecord);

                // * Now you can start hotword detection
                mHotwordDetect.startDetection();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Stop hotword detection
     */
    private void stopHotwordDetection() {
        if (mHotwordDetect != null) {
            try {

                // * Stop detection
                //   -----------------------------------------------------------------------------
                //   Warning!
                //   If you are using RESOURCE_CONTORL_MODE_AUTO_RELEASE mode,
                //   the AudioRecord resource will be also released after this method called.
                mHotwordDetect.stopDetection();

                displayTextChangeHandler(getString(R.string.hotwordDetectIsClose));

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * This is a callback listener example.
     *
     * You should implement the IHotwordDetectListener
     * to get all callbacks and assign the instance of your listener class
     * into the recognizer instance of HotwordDetect.
     */
    private class HotwordDetectListener implements IHotwordDetectListener {
        @Override
        public void onInitializing() {
            String str = getString(R.string.hotwordDetectOnInitializing);

            hotwordDetectSwitchEnableChangeHandler(false);
            displayTextChangeHandler(str);
            Log.i(TAG, str);
        }

        @Override
        public void onInitialized() {
            String str = getString(R.string.hotwordOnInitialized);

            displayTextChangeHandler(str);
            Log.i(TAG, str);
        }

        @Override
        public void onStartDetect() {
            String str = getString(R.string.hotwordDetectOnDetect);

            displayTextChangeHandler(str);
            Log.i(TAG, str);

            hotwordDetectSwitchEnableChangeHandler(true);
        }

        @Override
        public void onHotwordDetect(int hotwordID) {
            String str = getString(R.string.hotwordOnHotwordDetect);

            mDetectedCount++;

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
            String deviceCurrentTime = sdf.format(new Date(System.currentTimeMillis()));
            str += "\n >> [ " + mDetectedCount + " ] << " + deviceCurrentTime + "\n";

            displayTextChangeHandler(str);
            Log.i(TAG, str);
        }
    }

    private void displayTextChangeHandler(final String STTStr) {
        new Handler(this.getMainLooper()).post(new Runnable(){
            public void run(){
                mDisplayText.setText(STTStr);
            }
        });
    }

    private void hotwordDetectSwitchCheckChangeHandler(final boolean open) {
        new Handler(this.getMainLooper()).post(new Runnable(){
            public void run(){
                mHotwordDetectSwitch.setChecked(open);
            }
        });
    }

    private void hotwordDetectSwitchEnableChangeHandler(final boolean open) {
        new Handler(this.getMainLooper()).post(new Runnable(){
            public void run(){
                mHotwordDetectSwitch.setEnabled(open);
            }
        });
    }

    private void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check hardware resource permissions.
     */
    private boolean checkDeviceResourcePermissions() {
        // Check if the user agrees to access the microphone
        boolean hasMicrophonePermission = checkPermissions(
                Manifest.permission.RECORD_AUDIO,
                REQUEST_MICROPHONE);
        boolean hasWriteExternalStorage = checkPermissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                REQUEST_EXTERNAL_PERMISSION);

        if (hasMicrophonePermission && hasWriteExternalStorage) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check the specified hardware permission.
     */
    private boolean checkPermissions(String permissionStr, int requestCode) {
        // Check to see if we have permission to access something,
        // such like the microphone.
        int permission = ActivityCompat.checkSelfPermission(
                HotwordDetectionActivity.this,
                permissionStr);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We can not access it, request authorization from the user.
            ActivityCompat.requestPermissions(
                    HotwordDetectionActivity.this,
                    new String[] {permissionStr},
                    requestCode
            );
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_MICROPHONE:
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.RECORD_AUDIO)) {
                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(
                                    this,
                                    getString(R.string.GetMicrophonePermission),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this,
                                    getString(R.string.GetMicrophonePermissionDenied),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
            case REQUEST_EXTERNAL_PERMISSION:
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(
                                    this,
                                    getString(R.string.GetWriteStoragePermission),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this,
                                    getString(R.string.GetWriteStoragePermissionDenied),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                break;
        }
    }

    /**
     * Initial AudioRecord
     */
    private void createAudioRecord() {
        if (mAudioRecord != null) {
            if (mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                stopAndReleaseAudioRecord();
            }
        }

        if (mAudioRecord == null) {
            int minBufferSize = AudioRecord.getMinBufferSize(
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            mAudioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize * 4);

            // Waiting for AudioRecord initialized
            int retry = 0;
            while ((mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) && (retry < 4)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                retry++;
            }

            // Check AudioRecord is initialized or not
            if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                if (mAudioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                    throw new UnsupportedOperationException("Init AudioRecord failed.");
                } else {
                    throw new UnknownError("Failed to initialize AudioRecord.");
                }
            }
        }
    }

    /**
     * Start audio recording.
     */
    private void startRecording() {
        if (mAudioRecord == null) {
            createAudioRecord();
        }

        if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            mAudioRecord.startRecording();

            // Waiting for AudioRecord Recording
            int retry = 0;
            while ((mAudioRecord.getRecordingState()
                    != AudioRecord.RECORDSTATE_RECORDING) && (retry < 4)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                retry++;
            }
        }
    }

    /**
     * Stop audio recording and release resource.
     */
    public void stopAndReleaseAudioRecord() {
        if ((mAudioRecord != null) && (mAudioRecord.getState() != AudioRecord.STATE_UNINITIALIZED)) {
            try {
                mAudioRecord.stop();
                mAudioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "stopAndReleaseAudioRecord() Exception: " + e.getMessage());
            }
        }
        mAudioRecord = null;
    }
}
