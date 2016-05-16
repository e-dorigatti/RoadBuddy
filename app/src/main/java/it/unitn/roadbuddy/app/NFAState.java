package it.unitn.roadbuddy.app;


public interface NFAState {
    void onStateEnter( NFA nfa, MapFragment fragment );

    void onStateExit( NFA nfa, MapFragment fragment );
}
