package it.unitn.roadbuddy.app;

import android.os.Bundle;
import android.util.Log;

public class NFA {

    public static final String CURRENT_STATE_KEY = "current-state";

    NFAState currentState;
    MapFragment fragment;
    boolean paused = true;

    public NFA( MapFragment fragment, NFAState initialState, Bundle savedInstanceState ) {
        this.fragment = fragment;

        String savedState = savedInstanceState != null ?
                savedInstanceState.getString( CURRENT_STATE_KEY ) : null;

        if ( savedState != null ) {
            try {
                initialState = ( NFAState ) Class.forName( savedState ).newInstance( );
            }
            catch ( ClassNotFoundException | InstantiationException | IllegalAccessException exc ) {
                Log.e( getClass( ).getName( ), "while restoring state", exc );

                // it's okay to crash because exceptions can happen only for a dev's mistake
                throw new RuntimeException( exc );
            }

            initialState.onRestoreInstanceState( savedInstanceState );
        }

        Transition( initialState, savedInstanceState );
    }

    public void onSaveInstanceState( Bundle savedInstanceState ) {
        if ( currentState != null ) {
            currentState.onSaveInstanceState( savedInstanceState );
            savedInstanceState.putString( CURRENT_STATE_KEY, currentState.getClass( ).getName( ) );
        }
    }

    public void Transition( NFAState newState, Bundle savedInstanceState ) {
        Pause( );

        currentState = newState;

        Resume( savedInstanceState );
    }

    public void Pause( ) {
        if ( !paused && currentState != null ) {
            currentState.onStateExit( this, fragment );
            paused = true;
        }
    }

    public void Resume( Bundle savedInstanceState ) {
        if ( paused && currentState != null ) {
            currentState.onStateEnter( this, fragment, savedInstanceState );
            paused = false;
        }
    }
}