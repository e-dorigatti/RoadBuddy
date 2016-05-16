package it.unitn.roadbuddy.app;

public class NFA {

    NFAState currentState;
    MapFragment fragment;

    public NFA(MapFragment fragment, NFAState initialState ) {
        this.fragment = fragment;
        Transition( initialState );
    }

    public void Transition( NFAState newState ) {
        if ( currentState != null )
            currentState.onStateExit( this, fragment);

        if ( newState != null )
            newState.onStateEnter( this, fragment);

        currentState = newState;
    }
}