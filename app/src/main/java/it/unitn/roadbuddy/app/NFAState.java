package it.unitn.roadbuddy.app;


import android.os.Bundle;

/**
 * State the NFA is in, controls the behaviour of the interface.
 *
 * Special care should be taken when saving and restoring the instance state:
 *  - when stopping, onSaveInstanceState is called before onStateExit
 *  - when starting, onRestoreInstanceState is called before onStateEnter
 *    on a new instance created using the default parameterless constructor.
 *    If onRestoreInstanceState is called, onStateEnter will be called with the
 *    same bundle, otherwise the bundle should be null.
 *
 * Each state must have a default constructor with no parameters and allow the
 * whole initialization to happen through onRestoreInstanceState. What this means
 * is that onRestoreInstanceState should be able to initialize the object as
 * if one of the parametrized constructors was called.
 */
public interface NFAState {
    void onStateEnter( NFA nfa, MapFragment fragment, Bundle savedInstanceState );

    void onStateExit( NFA nfa, MapFragment fragment );

    void onSaveInstanceState( Bundle savedInstanceState );

    void onRestoreInstanceState( Bundle savedInstanceState );
}
