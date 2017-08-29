package com.xangnun.facealarm;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.microsoft.projectoxford.emotion.EmotionServiceClient;
import com.microsoft.projectoxford.emotion.EmotionServiceRestClient;
import com.microsoft.projectoxford.emotion.contract.RecognizeResult;
import com.microsoft.projectoxford.emotion.rest.EmotionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


/**
 * Sources:
 *
 * Emotion Detection
 * https://github.com/Microsoft/Cognitive-emotion-android
 *
 * Camera2 API
 * https://github.com/googlesamples/android-Camera2Basic
 * https://inducesmile.com/android/android-camera2-api-example-tutorial/
 *
 * Immersive Mode
 * https://developer.android.com/training/system-ui/immersive.html
 *
 * Display Activity Over Lockscreen
 * http://stackoverflow.com/questions/3629179/android-activity-over-default-lock-screen
 *
 * Media Player
 * https://developer.android.com/reference/android/media/MediaPlayer.html
 */
public class FaceActivity extends AppCompatActivity implements View.OnClickListener {

    //
    // CONSTANTS
    //

    private final String TAG = "FaceActivity";

    private static final int REQUEST_CAMERA_PERMISSION = 1337;

    private static final int NUM_FIRST_EMOTIONS = 6;
    private static final int HAPPY = 0;
    private static final int SURPRISE = 1;
    private static final int FEAR = 2;
    private static final int ANGER = 3;
    private static final int CONTEMPT = 4;
    private static final int DISGUSTED = 5;
    private static final int SADNESS = 6;

    private static final double FEAR_SUPRISE_THRESHOLD = 0.4;
    private static final double ANGER_THRESHOLD = 0.4;
    private static final double SAD_THRESHOLD = 0.5;
    private static final double HAPPY_THRESHOLD = 0.5;

    //
    // INSTANCE VARIABLES
    //

    /* GUI elements. */
    private Button mCheckEmoButton;
    private TextView mUserEmoTextView;

    /* Audio players. */
    private MediaPlayer mYeahMediaPlayer;
    private MediaPlayer mGreatMediaPlayer;

    /* Preview elements. */
    private TextureView mPreviewTextureView;
    private Size mPreviewSize;
    private Bitmap mBitmapFrame;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private ImageReader mImageReader;

    /* Camera elements. */
    private CameraDevice mCameraDevice;
    private String mCameraId;
    private boolean mIsOpened;
    private CameraCaptureSession mCaptureSession;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    /* Emotion variables. */
    private EmotionServiceClient mEmoServClient;
    private int mCurrentEmo;

    /* Listens for lifecycle events of the TextureView. */
    private final TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "Surface texture available");
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "Surface texture size changed");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(TAG, "Surface texture destroyed");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            //Log.d(TAG, "Surface texture updated");
        }
    };

    /* Listens for changes in state of the CameraDevice. */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Camera opened");
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.d(TAG, "Camera disconnected");
            mCameraDevice.close();
            mCameraDevice = null;
            finish();
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            Log.e(TAG, "Camera error: " + error);
            mCameraDevice.close();
            mCameraDevice = null;
            finish();
        }
    };

    //
    // METHODS
    //


    /**
     * Immersive Mode
     * https://developer.android.com/training/system-ui/immersive.html
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /**
         * Display Activity Over Lockscreen
         * http://stackoverflow.com/questions/3629179/android-activity-over-default-lock-screen
         */
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_face);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Require screen orientation to be portrait.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Get references to GUI elements.
        mUserEmoTextView = (TextView) findViewById(R.id.textView_emotion);
        mCheckEmoButton = (Button) findViewById(R.id.button_shake_me);
        mPreviewTextureView = (TextureView) findViewById(R.id.textureView);

        // Set listeners
        mCheckEmoButton.setOnClickListener(this);
        mCheckEmoButton.setEnabled(false);

        // Get the emotion service client if not present.
        if (mEmoServClient == null) {
            mEmoServClient = new EmotionServiceRestClient(getString(R.string.subscription_key));
        }

        try {
            mYeahMediaPlayer = new MediaPlayer();
            mYeahMediaPlayer.setDataSource(FaceActivity.this, Uri.parse("android.resource://com.xangnun.facealarm/" + R.raw.yeahkesden));
            mYeahMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mYeahMediaPlayer.prepare();

            mGreatMediaPlayer = new MediaPlayer();
            mGreatMediaPlayer.setDataSource(FaceActivity.this, Uri.parse("android.resource://com.xangnun.facealarm/" + R.raw.great_day));
            mGreatMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            mGreatMediaPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Error", e);
        }

        mYeahMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mYeahMediaPlayer.stop();
            }
        });
        mGreatMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mGreatMediaPlayer.stop();
                releasePlayers();
            }
        });

        String intentId = getIntent().getStringExtra("intent");
        if (intentId != null) {
            Log.d(TAG, "FINDING: " + intentId);
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(FaceActivity.this);
            Set<String> set = pref.getStringSet("Intents", new HashSet<String>());
            for (String s : set) {
                try {
                    Log.d(TAG, "FOUND: " + Intent.getIntent(s).getExtras().getString("Uid"));
                    if (Intent.getIntent(s).getExtras().getString("Uid").equals(intentId)) {
                        set.remove(s);
                    }
                } catch (URISyntaxException e) {
                    Log.e(TAG, "Error", e);
                }
            }
            pref.edit().putStringSet("Intents", set).commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        setFirstEmotion();
    }

    @Override
    protected void onResume() {
        super.onResume();

        startBackgroundThread();

        if (mPreviewTextureView.isAvailable()) {
            // Open camera if preview Texture already available.
            openCamera();
        } else {
            // Add listener if preview Texture not available.
            mPreviewTextureView.setSurfaceTextureListener(mTextureListener);
        }
    }

    private void setFirstEmotion() {
        // RNG.
        Random rand = new Random();

        // Get the first emotion.
        mCurrentEmo = 1 + rand.nextInt(NUM_FIRST_EMOTIONS);
        switch (mCurrentEmo) {
            case SURPRISE:
                mUserEmoTextView.setText("MAKE A SURPRISED FACE!!!");
                break;
            case FEAR:
                mUserEmoTextView.setText("MAKE A SCARED FACE!!!");
                break;
            case ANGER:
                mUserEmoTextView.setText("MAKE AN ANGRY FACE!!!");
                break;
            case CONTEMPT:
                mUserEmoTextView.setText("MAKE A CONTEMPT FACE!!!");
                break;
            case DISGUSTED:
                mUserEmoTextView.setText("MAKE A DISGUSTED FACE!!!");
                break;
            case SADNESS:
                mUserEmoTextView.setText("MAKE A SAD FACE!!!");
        }
        mCheckEmoButton.setEnabled(true);
    }


    @Override
    protected void onPause() {
        super.onPause();

        // Clean up.
        closeCamera();
        stopBackgroundThread();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            // Make sure camera access is allowed.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(FaceActivity.this,
                        "Sorry, but this app requires camera permissions.",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // Get the front camera.
            mCameraId = manager.getCameraIdList()[1];

            // Get the size the preview should be based on the camera.
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            // Make sure you have camera permissions.
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(FaceActivity.this,
                        new String[]{android.Manifest.permission.CAMERA,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CAMERA_PERMISSION);
            } else {
                if(!mIsOpened) {
                    manager.openCamera(mCameraId, mStateCallback, null);
                    mIsOpened = true;
                }

            }
        } catch (CameraAccessException exception) {
            exception.printStackTrace();
        } catch (ArrayIndexOutOfBoundsException exception) {
            Log.e(TAG, "Front camera not found", exception);
            Toast.makeText(FaceActivity.this,
                    "Sorry, you need a front camera in order to use this app.",
                    Toast.LENGTH_LONG).show();
            finish();
        } catch (Exception exception) {
            Log.e(TAG, "Error opening camera", exception);
        }
    }

    private void closeCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        mIsOpened = false;
    }

    protected void createCameraPreviewSession() {
        try {
            // Initialize the preview TextureView.
            SurfaceTexture texture = mPreviewTextureView.getSurfaceTexture();
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);

            // Attach camera preview to the SurfaceTexture.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mCameraDevice.createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback(){
                        @Override
                        public void onConfigured(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.d(TAG, "Preview session created");
                            if (mCameraDevice != null) {
                                // Start displaying preview once session is ready.
                                mCaptureSession = cameraCaptureSession;
                                updatePreview();
                            }
                        }
                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "Preview session configuration failed");
                            finish();
                        }
                    }, null);
        } catch (CameraAccessException exception) {
            Log.e(TAG, "Error when creating camera preview session", exception);
        }
    }

    protected void updatePreview() {
        // Set preview Builder.
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            // Repeatedly capture camera in preview.
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException exception) {
            Log.e(TAG, "Error when updating preview", exception);
        }
    }

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException exception) {
            Log.e(TAG, "Error when stopping background thread", exception);
        }
    }

    private void processFrame() {
        // Get the Bitmap image from the TextureView.
        mBitmapFrame = mPreviewTextureView.getBitmap();

        mCheckEmoButton.setEnabled(false);

        // Start a UI thread that will compute emotion recognition and update the UI
        new FaceActivity.EmoRecTask().execute();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_shake_me) {
            processFrame();
        }
    }

    /**
     * AsyncTask let's you perform background operations, but also let's you update the UI.
     */
    private class EmoRecTask extends AsyncTask<String, String, List<RecognizeResult>> {

        @Override
        protected List<RecognizeResult> doInBackground(String... args) {
            try {
                return processWithAutoFaceDetection();
            } catch (Exception exception) {
                Log.e(TAG, "Error when processing emotion", exception);
                Toast.makeText(FaceActivity.this,
                        "An error occurred while detecting your emotions. Try again!",
                        Toast.LENGTH_LONG).show();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<RecognizeResult> result) {
            super.onPostExecute(result);
            if (result != null) {
                if (result.size() == 0) {
                    Toast.makeText(FaceActivity.this,
                            "No emotion detected. Try again!",
                            Toast.LENGTH_LONG).show();
                    mCheckEmoButton.setEnabled(true);
                } else {
                    checkUserEmo(result);
                }
            }
        }
    }

    private List<RecognizeResult> processWithAutoFaceDetection() throws EmotionServiceException, IOException {
        Log.i(TAG, "Starting emotion detection");

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mBitmapFrame.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        // Let Microsoft handle the emotion detection.
        List<RecognizeResult> result = this.mEmoServClient.recognizeImage(inputStream);

        // Gson Derulo. This is just used to log the result. Converts the result into a JSON array.
        Gson gson = new Gson();
        String json = gson.toJson(result);
        Log.d(TAG, "Result: " + json);

        return result;
    }

    private void checkUserEmo(List<RecognizeResult> result) {
        mCheckEmoButton.setEnabled(true);
        if (result.size() != 1) {
            Toast.makeText(FaceActivity.this, "Sorry, you should be the only person in the picture.", Toast.LENGTH_LONG).show();
            return;
        }

        RecognizeResult currentEmo = result.get(0);

        switch (mCurrentEmo) {
            case SURPRISE:
            case FEAR:
                if (currentEmo.scores.surprise + currentEmo.scores.fear > FEAR_SUPRISE_THRESHOLD) {
                    Toast.makeText(FaceActivity.this, "Yeah!!!", Toast.LENGTH_LONG).show();
                    mCurrentEmo = HAPPY;
                    mUserEmoTextView.setText("MAKE A HAPPY FACE!!!");
                    playYeah();
                } else {
                    Toast.makeText(FaceActivity.this, "Sorry, try again!", Toast.LENGTH_LONG).show();
                }

                break;
            case ANGER:
            case CONTEMPT:
            case DISGUSTED:
                if (currentEmo.scores.anger + currentEmo.scores.contempt + currentEmo.scores.disgust > ANGER_THRESHOLD) {
                    Toast.makeText(FaceActivity.this, "Yeah!!!", Toast.LENGTH_LONG).show();
                    mCurrentEmo = HAPPY;
                    mUserEmoTextView.setText("MAKE A HAPPY FACE!!!");
                    playYeah();
                } else {
                    Toast.makeText(FaceActivity.this, "Sorry, try again!", Toast.LENGTH_LONG).show();
                }
                break;

            case SADNESS:
                if (currentEmo.scores.sadness > SAD_THRESHOLD) {
                    Toast.makeText(FaceActivity.this, "Yeah!!!", Toast.LENGTH_LONG).show();
                    mCurrentEmo = HAPPY;
                    mUserEmoTextView.setText("MAKE A HAPPY FACE!!!");
                    playYeah();
                } else {
                    Toast.makeText(FaceActivity.this, "Sorry, try again!", Toast.LENGTH_LONG).show();
                }
                break;

            case HAPPY:
                if (currentEmo.scores.happiness > HAPPY_THRESHOLD) {
                    Toast.makeText(FaceActivity.this, "Have a great day!!!", Toast.LENGTH_LONG).show();
                    mCheckEmoButton.setVisibility(View.GONE);
                    playDay();

                    //need to set the text back to off
                    Intent my_intent = new Intent(FaceActivity.this, Alarm_Receiver.class);
                    //stop the alarm
                    my_intent.putExtra("extra", "Alarm OFF");
                    sendBroadcast(my_intent);
                    finish();
                } else {
                    Toast.makeText(FaceActivity.this, "Sorry, try again!", Toast.LENGTH_LONG).show();
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_face, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shut_off:
                emergencyShutOff();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void emergencyShutOff() {
        AlertDialog alert = new AlertDialog.Builder(FaceActivity.this).create();
        alert.setTitle("Emergency Shutoff Button");
        alert.setMessage("Are you sure you want to cheat and use the emergency shutoff button?");
        alert.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //need to set the text back to off
                Intent my_intent = new Intent(FaceActivity.this, Alarm_Receiver.class);
                //stop the alarm
                my_intent.putExtra("extra", "Alarm OFF");
                sendBroadcast(my_intent);
                if (FirebaseAuth.getInstance().getCurrentUser() != null && AccessToken.getCurrentAccessToken() != null) {
                    String uid = AccessToken.getCurrentAccessToken().getUserId();
                    final DatabaseReference userCountRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("count");
                    userCountRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Log.d(TAG, "HERE");
                            int count = 0;
                            if (dataSnapshot.exists()) {
                                count = dataSnapshot.getValue(Integer.class);
                            }
                            ++count;
                            userCountRef.setValue(count);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e(TAG, "NO", databaseError.toException());
                        }
                    });
                }
                releasePlayers();
                finish();
            }
        });
        alert.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    private void playYeah() {
        mYeahMediaPlayer.start();
    }

    private void playDay() {
        mYeahMediaPlayer.stop();
        mGreatMediaPlayer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void releasePlayers() {
        mYeahMediaPlayer.release();
        mGreatMediaPlayer.release();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
}
