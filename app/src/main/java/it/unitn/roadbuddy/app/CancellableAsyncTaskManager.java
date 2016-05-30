package it.unitn.roadbuddy.app;


import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CancellableAsyncTaskManager {

    protected Map<Class<?>, List<CancellableAsyncTask<?, ?, ?>>> runningTasksByType = new HashMap<>( );
    protected Map<CancellableAsyncTask<?, ?, ?>, Class<?>> runningTaskByInstance = new HashMap<>( );

    public synchronized boolean isTaskRunning( Class<?> taskType ) {
        List<CancellableAsyncTask<?, ?, ?>> tasks = runningTasksByType.get( taskType );
        return tasks != null && tasks.size( ) > 0;
    }

    public synchronized void stopRunningTasksOfType( Class<?> taskType ) {
        List<CancellableAsyncTask<?, ?, ?>> tasks = runningTasksByType.get( taskType );
        if ( tasks != null ) {
            for ( CancellableAsyncTask<?, ?, ?> task : tasks )
                task.cancel( true );
        }

        // no need to remove the tasks as they will take care of it, therefore
        //  - isTaskRunning might return true for cancelled tasks, too
        //  - we are preferring consistency at the expense of performances
        //    (this makes the operation O(n**2) instead of O(n) plus the
        //    synchronization overhead for calling removeTask)
        // FIXME do we really want this?
    }

    public synchronized void removeTask( CancellableAsyncTask task ) {
        Class<?> taskType = runningTaskByInstance.get( task );
        List<CancellableAsyncTask<?, ?, ?>> siblingTasks = runningTasksByType.get( taskType );
        Utils.Assert( taskType != null && siblingTasks != null && siblingTasks.size( ) > 0, true );

        runningTaskByInstance.remove( task );

        boolean found = false;
        for ( CancellableAsyncTask<?, ?, ?> t : siblingTasks ) {
            if ( t.equals( task ) ) {
                siblingTasks.remove( task );
                found = true;
                break;
            }
        }

        Utils.Assert( found, true );
    }

    public synchronized <Params> void startRunningTask( CancellableAsyncTask<Params, ?, ?> task,
                                                        boolean mayInterruptOldTasks, Params... args
    ) {
        if ( mayInterruptOldTasks )
            stopRunningTasksOfType( task.getClass( ) );

        runningTaskByInstance.put( task, task.getClass( ) );

        List<CancellableAsyncTask<?, ?, ?>> siblingTasks = runningTasksByType.get( task.getClass( ) );
        if ( siblingTasks == null ) {
            siblingTasks = new ArrayList<>( );
            siblingTasks.add( task );

            runningTasksByType.put( task.getClass( ), siblingTasks );
        }
        else siblingTasks.add( task );

        task.executeOnExecutor( AsyncTask.THREAD_POOL_EXECUTOR, args );
    }
}

