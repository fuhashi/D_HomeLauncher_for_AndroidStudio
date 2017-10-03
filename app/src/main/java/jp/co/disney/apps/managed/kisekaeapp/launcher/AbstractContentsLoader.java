package jp.co.disney.apps.managed.kisekaeapp.launcher;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

public abstract class AbstractContentsLoader {
    protected static final String TAG = "AbstractContentsLoader";

    protected static final HandlerThread sWorkerThread = new HandlerThread("launcher-loader");
    static {
        sWorkerThread.start();
    }
    protected static final Handler sWorker = new Handler(sWorkerThread.getLooper());

    protected final Object mLock = new Object();
    protected DeferredHandler mHandler = new DeferredHandler();
    protected AbstractLoaderTask mLoaderTask;
    protected volatile boolean mFlushingWorkerThread;
    protected boolean mIsLoaderTaskRunning;

    protected WeakReference<AbstractCallbacks> mCallbacks;

    public abstract interface AbstractCallbacks {
        public boolean setLoadOnResume();
    }

    /**
     * Set this as the current Launcher activity object for the loader.
     */
    public void initialize(AbstractCallbacks callbacks) {
        synchronized (mLock) {
            mCallbacks = new WeakReference<AbstractCallbacks>(callbacks);
        }
    }

    /** Runs the specified runnable immediately if called from the main thread, otherwise it is
     * posted on the main thread handler. */
    void runOnMainThread(Runnable r) {
        runOnMainThread(r, 0);
    }

    void runOnMainThread(Runnable r, int type) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            // If we are on the worker thread, post onto the main handler
            mHandler.post(r);
        } else {
            r.run();
        }
    }

    /** Runs the specified runnable immediately if called from the worker thread, otherwise it is
     * posted on the worker thread handler. */
    public static void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            // If we are not on the worker thread, then post to the worker handler
            sWorker.post(r);
        }
    }

    public void flushWorkerThread() {
        mFlushingWorkerThread = true;
        Runnable waiter = new Runnable() {
                public void run() {
                    synchronized (this) {
                        notifyAll();
                        mFlushingWorkerThread = false;
                    }
                }
            };

        synchronized(waiter) {
            runOnWorkerThread(waiter);
            if (mLoaderTask != null) {
                synchronized(mLoaderTask) {
                    mLoaderTask.notify();
                }
            }
            boolean success = false;
            while (!success) {
                try {
                    waiter.wait();
                    success = true;
                } catch (InterruptedException e) {
                }
            }
        }
    }

    protected abstract class AbstractLoaderTask implements Runnable {

        protected boolean mStopped;

        @Override
        public void run() {

            synchronized (mLock) {
                mIsLoaderTaskRunning = true;
            }

            runMain();

            synchronized (mLock) {
                // If we are still the last one to be scheduled, remove ourselves.
                if (mLoaderTask == this) {
                    mLoaderTask = null;
                }
                mIsLoaderTaskRunning = false;
            }
        }

        public void stopLocked() {
            synchronized (this) {
                mStopped = true;
                this.notify();
            }
        }

        /**
         * Gets the callbacks object.  If we've been stopped, or if the launcher object
         * has somehow been garbage collected, return null instead.  Pass in the Callbacks
         * object that was around when the deferred message was scheduled, and if there's
         * a new Callbacks object around then also return null.  This will save us from
         * calling onto it with data that will be ignored.
         */
        protected AbstractCallbacks tryGetCallbacks(AbstractCallbacks oldCallbacks) {

            synchronized (mLock) {

                if (mStopped) {
                    return null;
                }

                if (mCallbacks == null) {
                    return null;
                }

                final AbstractCallbacks  callbacks = mCallbacks.get();
                if (callbacks != oldCallbacks) {
                    // Callbacksを実装したActivityのインスタンスが、
                    // 再生成されている場合を想定。
                    return null;
                }
                if (callbacks == null) {
                    Log.w(TAG, "no mCallbacks");
                    return null;
                }

                return callbacks;
            }
        }

        protected abstract void runMain();
    }
}
