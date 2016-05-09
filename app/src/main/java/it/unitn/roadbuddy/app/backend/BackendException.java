package it.unitn.roadbuddy.app.backend;


public class BackendException extends Exception {
    public BackendException( String text ) {
        super( text );
    }

    public BackendException( String text, Throwable wrapped ) {
        super( text, wrapped );
    }
}
