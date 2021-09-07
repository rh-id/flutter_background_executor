package m.co.rh.id.flutter_background_executor;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.view.FlutterCallbackInformation;
import io.flutter.view.FlutterMain;

public class FlutterExecutorPool {
    public static class FlutterResult {
        public Object result;
        public boolean isError;
        public String errorCode;
        public String errorMessage;
        public Object errorDetails;
        public boolean isNotImplemented;
        private boolean isDone;

        public boolean isDone() {
            return isDone;
        }

        void done() {
            isDone = true;
        }

        @Override
        public String toString() {
            return "FlutterResult{" +
                    "result=" + result +
                    ", isError=" + isError +
                    ", errorCode='" + errorCode + '\'' +
                    ", errorMessage='" + errorMessage + '\'' +
                    ", errorDetails=" + errorDetails +
                    ", isNotImplemented=" + isNotImplemented +
                    ", isDone=" + isDone +
                    '}';
        }
    }

    private static final String TAG = FlutterExecutorPool.class.getName();

    private Context mContext;
    private int mPoolSize;
    private Handler mHandler;
    private List<FlutterBackgroundExecutor> mFlutterBackgroundExecutorList;
    private long mDispatcherCallback;
    private String mBackgroundChannelName;
    private boolean mIsShutdown;

    public FlutterExecutorPool(Context context, int poolSize, long dispatcherCallback, String backgroundChannelName) {
        mContext = context;
        mPoolSize = poolSize;
        mFlutterBackgroundExecutorList = Collections.synchronizedList(new ArrayList(poolSize));
        mDispatcherCallback = dispatcherCallback;
        mBackgroundChannelName = backgroundChannelName;
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Initialize executors
     */
    @MainThread
    public void initExecutors() {
        checkShutdown();
        if (!mFlutterBackgroundExecutorList.isEmpty()) {
            return;
        }
        for (int i = 0; i < mPoolSize; i++) {
            mFlutterBackgroundExecutorList.add(initFlutterExecutor());
        }
    }

    private void checkShutdown() {
        if (mIsShutdown) {
            throw new IllegalStateException("This pool has shutdown, create new instance");
        }
    }

    /**
     * Execute function callback and wait.
     * this method is expected to be executed in background thread to avoid blocking main thread
     *
     * @param functionCallback
     * @param functionArgs
     * @return
     */
    @WorkerThread
    public FlutterResult execute(final long functionCallback, final Object functionArgs) {
        checkShutdown();
        FlutterBackgroundExecutor flutterBackgroundExecutor = null;
        int currentPoolSize = mFlutterBackgroundExecutorList.size();
        if (currentPoolSize < mPoolSize) {
            flutterBackgroundExecutor = addNewExecutor();
            flutterBackgroundExecutor.setBusy();
            while (!flutterBackgroundExecutor.isReady()) {
                Thread.yield();
            }
        } else {
            // use Thread.yield to avoid starvation, instead of keep looping or blocking
            for (; flutterBackgroundExecutor == null; Thread.yield()) {
                for (FlutterBackgroundExecutor flutterBackgroundExecutorInstance
                        : mFlutterBackgroundExecutorList) {
                    if (flutterBackgroundExecutorInstance.isReady() && !flutterBackgroundExecutorInstance.isBusy()) {
                        flutterBackgroundExecutor = flutterBackgroundExecutorInstance;
                        flutterBackgroundExecutor.setBusy();
                        break;
                    }
                }
            }
        }

        final FlutterBackgroundExecutor selectedExecutor = flutterBackgroundExecutor;
        final AtomicReference<FlutterResult> resultReference = new AtomicReference<>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "selectedExecutor :" + selectedExecutor.getName());
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                resultReference.set(selectedExecutor.execute(new FlutterBackgroundTask(functionCallback, functionArgs)));
                countDownLatch.countDown();
            }
        });
        while (countDownLatch.getCount() == 1) {
            Thread.yield();
        }
        FlutterResult flutterResult = resultReference.get();
        while (!flutterResult.isDone()) {
            Thread.yield();
        }
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "flutterResult :" + flutterResult);
        }
        return flutterResult;
    }

    private FlutterBackgroundExecutor addNewExecutor() {
        FlutterBackgroundExecutor flutterBackgroundExecutor;
        final AtomicReference<FlutterBackgroundExecutor> backgroundExecutorAtomicReference = new AtomicReference<>();
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                backgroundExecutorAtomicReference.set(initFlutterExecutor());
                countDownLatch.countDown();
            }
        });
        while (countDownLatch.getCount() == 1) {
            Thread.yield();
        }
        flutterBackgroundExecutor = backgroundExecutorAtomicReference.get();
        mFlutterBackgroundExecutorList.add(flutterBackgroundExecutor);
        return flutterBackgroundExecutor;
    }

    @MainThread
    private FlutterBackgroundExecutor initFlutterExecutor() {
        // Initialize the flutter engine and executor
        FlutterEngine flutterEngine = new FlutterEngine(mContext);
        DartExecutor dartExecutor = flutterEngine.getDartExecutor();
        FlutterBackgroundExecutor flutterBackgroundExecutor = new FlutterBackgroundExecutor(flutterEngine, mBackgroundChannelName);
        String appBundlePath = FlutterMain.findAppBundlePath();
        AssetManager assets = mContext.getAssets();
        FlutterCallbackInformation flutterCallback =
                FlutterCallbackInformation.lookupCallbackInformation(mDispatcherCallback);
        DartExecutor.DartCallback dartCallback = new DartExecutor.DartCallback(assets, appBundlePath, flutterCallback);
        dartExecutor.executeDartCallback(dartCallback);
        return flutterBackgroundExecutor;
    }

    /**
     * Shutdown this executor and its engines, this instance must not be used anymore once shutdown
     */
    public void shutdown() {
        if (mIsShutdown) {
            return;
        }
        for (FlutterBackgroundExecutor flutterBackgroundExecutor : mFlutterBackgroundExecutorList
        ) {
            flutterBackgroundExecutor.destroy();
        }
        mFlutterBackgroundExecutorList.clear();
        mFlutterBackgroundExecutorList = null;
        mContext = null;
        mHandler = null;
        mBackgroundChannelName = null;
        mIsShutdown = true;
    }
}


class FlutterBackgroundExecutor implements MethodChannel.MethodCallHandler {
    private static final String TAG = FlutterBackgroundExecutor.class.getName();

    private FlutterEngine mFlutterEngine;
    private MethodChannel mMethodChannel;
    private boolean mIsInit;
    private boolean mIsBusy;
    private String mName;

    FlutterBackgroundExecutor(FlutterEngine flutterEngine, String backgroundChannelName) {
        mFlutterEngine = flutterEngine;
        mMethodChannel = new MethodChannel(flutterEngine.getDartExecutor(), backgroundChannelName);
        mMethodChannel.setMethodCallHandler(this);
        mName = UUID.randomUUID().toString();
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            // callback from flutter engine to indicate this background executor is ready
            case "initialized":
                mIsInit = true;
                result.success(true);
                break;
            default:
                result.notImplemented();
        }
    }

    @MainThread
    public FlutterExecutorPool.FlutterResult execute(FlutterBackgroundTask flutterBackgroundTask) {
        if (!isReady()) {
            throw new IllegalStateException("Executor is not ready yet");
        }
        final FlutterExecutorPool.FlutterResult bgResult = new FlutterExecutorPool.FlutterResult();
        final MethodChannel.Result resultCallback = new MethodChannel.Result() {
            @Override
            public void success(@Nullable Object result) {
                bgResult.result = result;
                bgResult.done();
                mIsBusy = false;
            }

            @Override
            public void error(String errorCode, @Nullable String errorMessage, @Nullable Object errorDetails) {
                bgResult.isError = true;
                bgResult.errorCode = errorCode;
                bgResult.errorMessage = errorMessage;
                bgResult.errorDetails = errorDetails;
                bgResult.done();
                mIsBusy = false;
                Log.e(TAG, "error resultCallback: " + errorCode + " " + errorMessage);
            }

            @Override
            public void notImplemented() {
                bgResult.isNotImplemented = true;
                bgResult.done();
                mIsBusy = false;
                Log.e(TAG, "notImplemented resultCallback");
            }
        };
        mMethodChannel.invokeMethod("execute", flutterBackgroundTask.toArgs(), resultCallback);
        return bgResult;
    }

    public boolean isReady() {
        return mIsInit;
    }

    public boolean isBusy() {
        return mIsBusy;
    }

    public void setBusy() {
        mIsBusy = true;
    }

    public String getName() {
        return mName;
    }

    public void destroy() {
        mMethodChannel.setMethodCallHandler(null);
        mMethodChannel = null;
        mFlutterEngine.destroy();
        mFlutterEngine = null;
    }
}

class FlutterBackgroundTask {
    long functionCallback;
    Object functionArgs;

    FlutterBackgroundTask(long functionCallback, Object functionArgs) {
        this.functionArgs = functionArgs;
        this.functionCallback = functionCallback;
    }

    Map<String, Object> toArgs() {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("functionCallback", functionCallback);
        args.put("functionArgs", functionArgs);
        return args;
    }
}

