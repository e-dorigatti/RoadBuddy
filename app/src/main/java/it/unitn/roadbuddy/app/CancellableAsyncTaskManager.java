package it.unitn.roadbuddy.app;


import android.os.AsyncTask;

import java.util.HashMap;
import java.util.Map;

public class CancellableAsyncTaskManager {

    protected Map<Class<?>, CancellableAsyncTask<?, ?, ?>> runningTasksByType = new HashMap<>( );
    protected Map<CancellableAsyncTask<?, ?, ?>, Class<?>> runningTaskByInstance = new HashMap<>( );

    public void stopAllRunningTasks( ) {
        for ( CancellableAsyncTask task : runningTasksByType.values( ) )
            task.cancel( true );

        runningTasksByType.clear( );
        runningTaskByInstance.clear( );
    }

    public boolean isTaskRunning(Class<?> taskType) {
        CancellableAsyncTask task = runningTasksByType.get( taskType );
        return task != null;
    }

    public void stopRunningTask( Class<?> taskType ) {
        CancellableAsyncTask task = runningTasksByType.get( taskType );
        if ( task != null ) {
            task.cancel( true );
        }
    }

    public void stopRunningTask( CancellableAsyncTask<?, ?, ?> task ) {
        if ( runningTaskByInstance.containsKey( task ) )
            task.cancel( true );
    }

    protected void removeTask( CancellableAsyncTask task ) {
        Class<?> taskType = runningTaskByInstance.get( task );
        Utils.Assert( taskType != null, true );

        runningTaskByInstance.remove( task );
        runningTasksByType.remove( taskType );
    }

    public <Params> void startRunningTask( CancellableAsyncTask<Params, ?, ?> task,
                                           boolean mayInterruptOldTask, Params... args
    ) {
        CancellableAsyncTask<?, ?, ?> oldTask = runningTasksByType.get( task.getClass( ) );
        if ( oldTask != null && mayInterruptOldTask )
            stopRunningTask( oldTask );

        runningTaskByInstance.put( task, task.getClass( ) );
        runningTasksByType.put( task.getClass( ), task );

        task.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, args );
    }
}

