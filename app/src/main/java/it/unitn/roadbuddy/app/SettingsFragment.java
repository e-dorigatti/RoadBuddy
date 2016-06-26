package it.unitn.roadbuddy.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.login.widget.LoginButton;
import com.facebook.login.widget.ProfilePictureView;

import java.util.Arrays;

public class SettingsFragment
        extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_PREF_USER_NAME = "pref_dev_user_name",
            KEY_PREF_USER_ID = "pref_dev_user_id";

    AccessTokenTracker accessTokenTracker;
    MainActivity mPActivity;
    AsyncTask runningAsyncTask;
    String currentUserName;
    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager( );

    @Override
    public void onCreatePreferences( Bundle savedInstanceState, String rootKey ) {
        addPreferencesFromResource( R.xml.preferences );
        this.mPActivity = ( MainActivity ) getActivity( );
    }

    @Override
    public View onCreateView( LayoutInflater inflater,
                              ViewGroup container,
                              Bundle savedInstanceState ) {
        View settings = super.onCreateView( inflater, container, savedInstanceState );
        if ( settings == null )
            return null;

        View mainLayout = inflater.inflate( R.layout.fragment_settings, container, false );
        FrameLayout settingsFrame = ( FrameLayout ) mainLayout.findViewById( R.id.settings );
        settingsFrame.addView( settings );
        LoginButton faceButton = ( LoginButton ) mainLayout.findViewById( R.id.login_button );
        Button nickButton = (Button) mainLayout.findViewById( R.id.btnLogout );
        nickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
        faceButton.setFragment( this );
        faceButton.setReadPermissions( Arrays.asList( "public_profile", "email" ) );
        TextView profileName = (TextView) mainLayout.findViewById(R.id.facebook_name);
        TextView profileDetail = (TextView) mainLayout.findViewById(R.id.facebook_details);
        if ( AccessToken.getCurrentAccessToken( ) == null ) {
            faceButton.setVisibility(View.GONE);
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( getContext( ) );
            currentUserName = pref.getString( SettingsFragment.KEY_PREF_USER_NAME, null );
            profileName.setText( pref.getString( SettingsFragment.KEY_PREF_USER_NAME, null ));
            profileDetail.setText("Male, Age 22");
        }else{
            nickButton.setVisibility(View.GONE);
            getPreferenceScreen().findPreference("pref_dev_user_name").setEnabled(false);
            ProfilePictureView profilePictureView;
            profilePictureView = (ProfilePictureView) mainLayout.findViewById(R.id.friendProfilePicture);
            com.facebook.Profile profile = com.facebook.Profile.getCurrentProfile();
            profilePictureView.setCropped(true);
            if(profile != null) {
                profilePictureView.setProfileId(profile.getId());
                profileName.setText(profile.getName());
                profileDetail.setText("Male, Age 22");
            }
        }

        accessTokenTracker = new AccessTokenTracker( ) {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken ) {
                Log.v( "Login", "Vecchio token " + oldAccessToken );
                Log.v( "Login", "Nuovo token " + currentAccessToken );
                if (currentAccessToken == null)
                    logout();
            }
        };
        return mainLayout;
    }

    void logout( ) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences( getActivity( ) );
        SharedPreferences.Editor editor = sharedPref.edit( );
        editor.remove( SettingsFragment.KEY_PREF_USER_NAME );
        editor.remove( SettingsFragment.KEY_PREF_USER_ID );
        editor.apply( );

        getActivity( ).recreate( );
    }


    @Override
    public void onResume( ) {
        super.onResume( );

        if ( getPreferenceScreen( ) != null ) {
            getPreferenceScreen( ).getSharedPreferences( )
                                  .registerOnSharedPreferenceChangeListener( this );
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( getContext( ) );
        currentUserName = pref.getString( SettingsFragment.KEY_PREF_USER_NAME, null );
    }

    @Override
    public void onPause( ) {
        super.onPause( );
        taskManager.stopAllRunningTasks( );
        getPreferenceScreen( ).getSharedPreferences( )
                              .unregisterOnSharedPreferenceChangeListener( this );

        cancelRunningAsyncTask( );
    }

    @Override
    public void onSharedPreferenceChanged( SharedPreferences sharedPreferences,
                                           String key ) {

        if ( key.equals( KEY_PREF_USER_NAME ) ) {
            if ( runningAsyncTask == null ) {
                String newUserName = sharedPreferences.getString( key, null );

                if ( newUserName != null ) {
                    taskManager.startRunningTask(
                            new ChangeAppUserAsync( taskManager, getActivity( ), newUserName, true ),
                            true
                    );
                }
            }
            else {
                Toast.makeText(
                        getContext( ), R.string.wait_for_async_op_completion, Toast.LENGTH_LONG
                ).show( );
            }
        }
    }

    void cancelRunningAsyncTask( ) {
        if ( runningAsyncTask != null ) {
            runningAsyncTask.cancel( true );
            runningAsyncTask = null;
        }
    }

    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data ) {
        CallbackManager manager = CallbackManager.Factory.create( );
        manager.onActivityResult( requestCode, resultCode, data );
    }
}
