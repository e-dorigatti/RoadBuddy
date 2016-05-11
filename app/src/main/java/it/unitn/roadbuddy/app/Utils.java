package it.unitn.roadbuddy.app;


public class Utils {

    public static boolean Assert( boolean condition, boolean force_crash ) {
        if ( condition ) {
            return false;
        }
        else if ( BuildConfig.DEBUG || force_crash ) {
            throw new RuntimeException( );
        }
        else {
            return true;
        }
    }

}
