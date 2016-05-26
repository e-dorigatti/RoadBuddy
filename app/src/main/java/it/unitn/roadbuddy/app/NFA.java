package it.unitn.roadbuddy.app;

public class NFA {

    NFAState currentState;
    MapFragment fragment;
    boolean paused = true;

    public NFA( MapFragment fragment, NFAState initialState ) {
        this.fragment = fragment;
        Transition( initialState );
    }

    public void Transition( NFAState newState ) {
        Pause( );

        currentState = newState;

        Resume( );
    }

    public void Pause( ) {
        if ( !paused && currentState != null ) {
            currentState.onStateExit( this, fragment );
            paused = true;
        }
    }

    public void Resume( ) {
        if ( paused && currentState != null ) {
            currentState.onStateEnter( this, fragment );
            paused = false;
        }
    }
}