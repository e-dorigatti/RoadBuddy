package it.unitn.roadbuddy.app;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.facebook.*;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import org.json.JSONObject;

import java.util.Arrays;

public class SplashActivity extends AppCompatActivity {

    AccessTokenTracker accessTokenTracker;
    AccessToken FaceAccessToken = null;
    CancellableAsyncTaskManager taskManager = new CancellableAsyncTaskManager( );
    CallbackManager callbackManager;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        FacebookSdk.sdkInitialize( getApplicationContext( ) );
        callbackManager = CallbackManager.Factory.create( );
        LoginManager.getInstance( ).registerCallback(
                callbackManager,
                new FacebookCallback<LoginResult>( ) {
                    @Override
                    public void onSuccess( LoginResult loginResult ) {
                        setInitialPreferences( loginResult );
                    }

                    @Override
                    public void onCancel( ) {
                        FireLogInDialogFragment dialog = new FireLogInDialogFragment( );
                        dialog.show( getSupportFragmentManager( ), "login" );
                    }

                    @Override
                    public void onError( FacebookException exception ) {
                        Toast.makeText( getApplicationContext( ), "Could not login", Toast.LENGTH_LONG ).show( );
                        finish( );
                    }
                }
        );

        accessTokenTracker = new AccessTokenTracker( ) {
            @Override
            protected void onCurrentAccessTokenChanged(
                    AccessToken oldAccessToken,
                    AccessToken currentAccessToken ) {
                FaceAccessToken = currentAccessToken;
                Log.v( "Login", "Vecchio token " + oldAccessToken );
                Log.v( "Login", "Nuovo token " + currentAccessToken );
            }
        };

        FaceAccessToken = AccessToken.getCurrentAccessToken( );
    }

    @Override
    protected void onStart( ) {
        super.onStart( );

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( this );

        int currentUserId = pref.getInt( SettingsFragment.KEY_PREF_USER_ID, -1 );
        String userName = pref.getString( SettingsFragment.KEY_PREF_USER_NAME, null );

        if ( FaceAccessToken == null && currentUserId == -1 ) {
            // first launch, login
            FireLogInDialogFragment dialog = new FireLogInDialogFragment( );
            dialog.show( getSupportFragmentManager( ), "login" );
        }
        else {
            if ( pref.getBoolean( SettingsFragment.KEY_PREF_DEV_ENABLED, false ) ) {
                Toast.makeText(
                        this,
                        String.format(
                                "You are currently running as user %s (id: %d)",
                                userName, currentUserId
                        ), Toast.LENGTH_SHORT
                ).show( );
            }

            launchMainActivity( );
        }
    }

    void launchMainActivity( ) {
        Intent intent = new Intent( this, MainActivity.class );
        startActivity( intent );
        finish( );
    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        super.onActivityResult( requestCode, resultCode, data );
        callbackManager.onActivityResult( requestCode, resultCode, data );
    }

    @Override
    protected void onPause( ) {
        super.onPause( );
        taskManager.stopAllRunningTasks( );
    }

    @Override
    protected void onDestroy( ) {
        accessTokenTracker.stopTracking( );
        Log.v( "MY_STATE_LOG", "main activity distrutto" );
        super.onDestroy( );
    }

    void setInitialPreferences( LoginResult loginResult ) {
        GraphRequest graphRequest = GraphRequest.newMeRequest(
                loginResult.getAccessToken( ),
                new GraphRequest.GraphJSONObjectCallback( ) {
                    @Override
                    public void onCompleted( JSONObject object, GraphResponse response ) {
                        String first_name = object.optString( "first_name" );
                        String last_name = object.optString( "last_name" );

                        setInitialPreferences( ( first_name + " " + last_name ).trim( ) );
                    }
                } );

        Bundle parameters = new Bundle( );
        parameters.putString( "fields", "id,name,first_name,last_name,email" );
        graphRequest.setParameters( parameters );
        graphRequest.executeAsync( );
    }

    void setInitialPreferences( String username ) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences( this );
        SharedPreferences.Editor editor = sharedPref.edit( );
        editor.putString( SettingsFragment.KEY_PREF_USER_NAME, username );
        editor.apply( );

        taskManager.startRunningTask( new ChangeAppUserAsync( taskManager, this, username, false ), true );
    }

    public class FireLogInDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog( Bundle savedInstanceState ) {
            final AlertDialog.Builder builder = new AlertDialog.Builder( getActivity( ) );
            final View dialog_preference = getActivity( ).getLayoutInflater( ).inflate( R.layout.login_dialog, null );
            builder.setView( dialog_preference );
            builder.setPositiveButton( "Custom username", new DialogInterface.OnClickListener( ) {
                public void onClick( DialogInterface dialog, int id ) {
                    EditText username = ( EditText ) dialog_preference.findViewById( R.id.username_pref );
                    if ( username != null && username.getText( ) != null ) {
                        setInitialPreferences( username.getText( ).toString( ) );
                    }
                }
            } ).setNegativeButton( "Facebook", new DialogInterface.OnClickListener( ) {
                public void onClick( DialogInterface dialog, int id ) {
                    LoginManager.getInstance( ).logInWithReadPermissions(
                            SplashActivity.this, Arrays.asList( "public_profile", "email" )
                    );
                }
            } ).setTitle( R.string.dialog_sign_in );

            return builder.create( );
        }

        @Override
        public void onDismiss( DialogInterface dialog ) {
            super.onDismiss( dialog );
            //Toast.makeText( MainActivity.this, "You wont be able to perform most of action", Toast.LENGTH_LONG).show( );
        }
    }
}
