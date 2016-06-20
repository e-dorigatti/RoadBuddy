package it.unitn.roadbuddy.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.facebook.CallbackManager;
import com.facebook.login.widget.LoginButton;

import java.util.Arrays;

public class SettingsFragment
        extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_PREF_USER_NAME = "pref_dev_user_name",
            KEY_PREF_USER_ID = "pref_dev_user_id",
            KEY_PREF_DEV_ENABLED = "pref_dev_enabled";

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

        LoginButton loginButton = ( LoginButton ) mainLayout.findViewById( R.id.login_button );
        loginButton.setFragment( this );
        loginButton.setReadPermissions( Arrays.asList( "public_profile", "email" ) );

        mainLayout.findViewById( R.id.btnLogout ).setOnClickListener( new View.OnClickListener( ) {
            @Override
            public void onClick( View view ) {
                logout( );
            }
        } );

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
