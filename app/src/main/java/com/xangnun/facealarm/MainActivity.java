package com.xangnun.facealarm;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Sources:
 * Facebook login
 * https://developers.facebook.com/docs/facebook-login/android
 *
 * Firebase Authentication
 * https://firebase.google.com/docs/auth/android/facebook-login
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";

    private Button mSetAlarmButton;
    private Button mLeaderboardButton;
    private LoginButton mLoginButton;
    private ProgressBar mProgressBar;
    private AccessTokenTracker mTracker;

    private CallbackManager mCallbackManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get GUI references.
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mSetAlarmButton = (Button) findViewById(R.id.button_set_alarm);
        mLeaderboardButton = (Button) findViewById(R.id.button_leaderboard);
        mLoginButton = (LoginButton) findViewById(R.id.login_button);

        // Initialize instance variables.
        mTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                Log.d(TAG, "Access token changed to : " + currentAccessToken);
                if (currentAccessToken == null) {
                    // Should the AccessToken become null, sign out of Firebase.
                    mLeaderboardButton.setEnabled(false);
                    FirebaseAuth.getInstance().signOut();
                }
            }
        };

        mCallbackManager = CallbackManager.Factory.create();

        // Register listeners.
        mSetAlarmButton.setOnClickListener(MainActivity.this);
        mLeaderboardButton.setOnClickListener(MainActivity.this);

        /**
         * Facebook login
         * https://developers.facebook.com/docs/facebook-login/android
         */
        mLoginButton.setReadPermissions("user_friends");
        mLoginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "Login succesful");
                mProgressBar.setVisibility(View.VISIBLE);
                mLoginButton.setEnabled(false);
                authFirebase(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "Login cancelled");
            }

            @Override
            public void onError(FacebookException error) {
                Log.e(TAG, "Login failed", error);
                Toast.makeText(MainActivity.this,
                        "An error occurred while logging in. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();

        // Make sure user is logged in in order to access leaderboard.
        if (AccessToken.getCurrentAccessToken() == null
                || FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d(TAG, "User not already logged in");
            LoginManager.getInstance().logOut();
            FirebaseAuth.getInstance().signOut();
            mLeaderboardButton.setEnabled(false);
        } else {
            Log.d(TAG, "User already logged in");
        }

        mTracker.startTracking();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mTracker.stopTracking();
    }

    /**
     * Firebase Authentication
     * https://firebase.google.com/docs/auth/android/facebook-login
     */
    private void authFirebase(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            mLeaderboardButton.setEnabled(true);
                            Toast.makeText(MainActivity.this,
                                    "Logged in as: " + Profile.getCurrentProfile().getName(),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            LoginManager.getInstance().logOut();
                            mLeaderboardButton.setEnabled(false);
                            Toast.makeText(MainActivity.this,
                                    "An error occurred while logging in. Please try again.",
                                    Toast.LENGTH_SHORT).show();
                        }
                        mProgressBar.setVisibility(View.GONE);
                        mLoginButton.setEnabled(true);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_set_alarm:
                Intent alarmControlIntent = new Intent(MainActivity.this, AlarmControl.class);
                startActivity(alarmControlIntent);
                break;
            case R.id.button_leaderboard:
                Intent leaderboardIntent = new Intent(MainActivity.this,
                        LeadershipBoardActivity.class);
                startActivity(leaderboardIntent);
                break;
            default:
                break;
        }
    }
}
