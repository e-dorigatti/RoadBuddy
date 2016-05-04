package it.unitn.roadbuddy.app;

public class NFA {

    NFAState currentState;
    MainActivity activity;

    public NFA( MainActivity activity, NFAState initialState ) {
        this.activity = activity;
        Transition( initialState );
    }

    public void Transition( NFAState newState ) {
        if ( currentState != null )
            currentState.onStateExit( this, activity );

        if ( newState != null )
            newState.onStateEnter( this, activity );

        currentState = newState;
    }
}
