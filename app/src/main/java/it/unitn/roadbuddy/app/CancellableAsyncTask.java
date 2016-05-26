package it.unitn.roadbuddy.app;

import android.os.AsyncTask;

public abstract class CancellableAsyncTask<Params, Progress, Result>
        extends AsyncTask<Params, Progress, Result> {


    CancellableAsyncTaskManager manager;

    public CancellableAsyncTask( CancellableAsyncTaskManager manager ) {
        this.manager = manager;
    }

    @Override
    protected void onCancelled( ) {
        manager.removeTask( this );
    }

    @Override
    protected void onPostExecute( Result res ) {
        manager.removeTask( this );
    }
}