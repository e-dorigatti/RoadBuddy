package it.unitn.roadbuddy.app;


public interface NFAState {
    void onStateEnter( NFA nfa, MainActivity activity );

    void onStateExit( NFA nfa, MainActivity activity );
}
