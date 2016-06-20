package it.unitn.roadbuddy.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import it.unitn.roadbuddy.app.backend.BackendException;
import it.unitn.roadbuddy.app.backend.DAOFactory;
import it.unitn.roadbuddy.app.backend.models.User;

class ChangeAppUserAsync extends CancellableAsyncTask<Object, Integer, Integer> {

    String errorMessage;
    Activity activity;
    String newUserName;
    boolean canSwitchToExistingUser;
    boolean userAlreadyExists;
    ProgressDialog dialog;

    public ChangeAppUserAsync( CancellableAsyncTaskManager taskManager,
                               Activity activity, String newUserName,
                               boolean canSwitchToExistingUser ) {
        super( taskManager );

        this.activity = activity;
        this.newUserName = newUserName;
        this.canSwitchToExistingUser = canSwitchToExistingUser;
    }

    @Override
    protected void onPreExecute( ) {
        super.onPreExecute( );

        dialog = new ProgressDialog( activity );
        dialog.setProgressStyle( ProgressDialog.STYLE_SPINNER );
        dialog.setMessage( "One moment please..." );
        dialog.setIndeterminate( true );
        dialog.setCanceledOnTouchOutside( false );
        dialog.show( );
    }

    @Override
    protected Integer doInBackground( Object... nothing ) {
        try {
            User existing;

            existing = DAOFactory.getUserDAO( ).getUser( newUserName );
            if ( existing == null ) {
                return DAOFactory.getUserDAO( ).createUser(
                        new User( -1, newUserName, null, null, null )
                ).getId( );
            }
            else if ( canSwitchToExistingUser ) {
                return existing.getId( );
            }
            else {
                errorMessage = "There is another user with that name!";
                userAlreadyExists = true;
                return null;
            }
        }
        catch ( BackendException exc ) {
            Log.e( getClass( ).getName( ), "while changing current user", exc );
            return null;
        }
    }

    @Override
    protected void onPostExecute( Integer userID ) {
        dialog.cancel( );

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( activity );
        SharedPreferences.Editor editor = pref.edit( );

        if ( userID != null ) {
            editor.putInt( SettingsFragment.KEY_PREF_USER_ID, userID );
            editor.apply( );

            activity.recreate( );
        }
        else {
            String message = errorMessage != null ? errorMessage : "Could not login";
            Toast.makeText( activity, message, Toast.LENGTH_LONG ).show( );

            if ( userAlreadyExists )
                activity.recreate( );
            else activity.finish( );
        }

        super.onPostExecute( userID );
    }
}