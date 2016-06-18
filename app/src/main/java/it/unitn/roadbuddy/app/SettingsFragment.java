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
import android.widget.FrameLayout;
import android.widget.Toast;
import com.facebook.login.widget.LoginButton;

import java.util.Arrays;

import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.User;

public class SettingsFragment
        extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_PREF_USER_NAME = "pref_dev_user_name",
            KEY_PREF_USER_ID = "pref_dev_user_id",
            KEY_PREF_DEV_ENABLED = "pref_dev_enabled";

    MainActivity mPActivity;
    AsyncTask runningAsyncTask;
    String currentUserName;

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
        LoginButton loginButton = (LoginButton) mainLayout.findViewById(R.id.login_button);
        loginButton.setFragment(this);
        loginButton.setReadPermissions(Arrays.asList("public_profile","email"));
        return mainLayout;
    }

    @Override
    public void onResume( ) {
        super.onResume( );
        getPreferenceScreen( ).getSharedPreferences( )
                              .registerOnSharedPreferenceChangeListener( this );

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( getContext( ) );
        currentUserName = pref.getString( SettingsFragment.KEY_PREF_USER_NAME, null );
    }

    @Override
    public void onPause( ) {
        super.onPause( );
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
                Utils.Assert( newUserName != null, true );
                runningAsyncTask = new ChangeAppUserAsync( );
                runningAsyncTask.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, newUserName );
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPActivity.callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * **************************************************
     * THIS IS ONLY MEANT TO BE USED DURING DEVELOPMENT *
     * ***********************************************+**
     */
    class ChangeAppUserAsync extends AsyncTask<Object, Integer, Integer> {

        String exceptionMessage;

        @Override
        protected Integer doInBackground( Object... newUserName ) {
            try {
                String userName = ( String ) newUserName[ 0 ];
                User user;

                user = DAOFactory.getUserDAO( ).getUser( userName );
                if ( user == null ) {
                    user = DAOFactory.getUserDAO( ).createUser(
                            new User( -1, userName, null, null, null )
                    );
                }

                return user.getId( );
            }
            catch ( BackendException exc ) {
                Log.e( getClass( ).getName( ), "while changing current user", exc );
                exceptionMessage = exc.getMessage( );
                return null;
            }
        }

        @Override
        protected void onPostExecute( Integer userID ) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( getContext( ) );
            SharedPreferences.Editor editor = pref.edit( );

            if ( userID != null ) {
                editor.putInt( KEY_PREF_USER_ID, userID );
                editor.apply( );

                getActivity( ).recreate( );
            }
            else {

                editor.putString( KEY_PREF_USER_NAME, currentUserName );
                editor.apply( );

                if ( exceptionMessage != null ) {
                    Toast.makeText( getActivity( ), exceptionMessage, Toast.LENGTH_LONG ).show( );
                }
                else {
                    Toast.makeText( getActivity( ), R.string.generic_backend_error, Toast.LENGTH_LONG ).show( );
                }
            }
        }
    }
}
