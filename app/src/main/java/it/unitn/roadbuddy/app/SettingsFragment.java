package it.unitn.roadbuddy.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.github.clans.fab.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

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

    FloatingActionButton button_viaggi;
    FloatingActionButton button_map;

    private CallbackManager callbackManager;

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
        button_viaggi = ( FloatingActionButton ) mainLayout.findViewById( R.id.button_sett_viaggi );
        button_map = ( FloatingActionButton ) mainLayout.findViewById( R.id.button_sett_map );
        if ( button_viaggi == null )
            Log.v( "button", "è  null" );
        else
            Log.v( "button", "non è null" );
        button_map.setOnTouchListener( new View.OnTouchListener( ) {
            public boolean onTouch( View v, MotionEvent event ) {
                mPActivity.mPager.setCurrentItem( 0 );
                return false;
            }
        } );
        button_viaggi.setOnTouchListener( new View.OnTouchListener( ) {
            public boolean onTouch( View v, MotionEvent event ) {
                mPActivity.mPager.setCurrentItem( 1 );
                return false;
            }
        } );
        settingsFrame.addView( settings );
        LoginButton loginButton = (LoginButton) mainLayout.findViewById(R.id.login_button);
        loginButton.setFragment(this);
        callbackManager = CallbackManager.Factory.create();
        loginButton.setReadPermissions(Arrays.asList("public_profile","email"));
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                // App code
                GraphRequest graphRequest   =   GraphRequest.newMeRequest(loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback(){
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response){
                                Log.d("JSON", ""+response.getJSONObject().toString());
                                try{
                                    String email       = object.getString("email");
                                    String name        =   object.getString("name");
                                    String first_name  =   object.optString("first_name");
                                    String last_name   =   object.optString("last_name");

                                    //LoginManager.getInstance().logOut();
                                }
                                catch (JSONException e)
                                {
                                    e.printStackTrace();
                                }
                            }
                        });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,first_name,last_name,email");
                graphRequest.setParameters(parameters);
                graphRequest.executeAsync();
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });
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
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * **************************************************
     * THIS IS ONLY MEANT TO BE USED DURING DEVELOPMENT *
     * ***********************************************+**
     */
    // FIXME use strings
    class ChangeAppUserAsync extends AsyncTask<Object, Integer, Integer> {

        String exceptionMessage;

        @Override
        protected Integer doInBackground( Object... newUserName ) {
            try {
                User newUser = DAOFactory.getUserDAO( ).createUser(
                        new User( -1, ( String ) newUserName[ 0 ], null, null, null )
                );

                return newUser.getId( );
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
