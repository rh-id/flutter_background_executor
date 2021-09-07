package m.co.rh.id.flutter_background_executor;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;

/**
 * FlutterBackgroundExecutorPlugin
 */
public class FlutterBackgroundExecutorPlugin implements FlutterPlugin, MethodCallHandler {
    private static final String TAG = FlutterBackgroundExecutorPlugin.class.getName();

    private FlutterPluginBinding mBinding;
    private MethodChannel mMethodChannel;
    private ThreadPoolExecutor mThreadPoolExecutor;
    private Handler mHandler;
    private FlutterExecutorPool mFlutterExecutorPool;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        mBinding = binding;
        mMethodChannel =
                new MethodChannel(
                        binding.getBinaryMessenger(), "m.co.rh.id.flutter_background_executor");
        mMethodChannel.setMethodCallHandler(this);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        mBinding = null;
        if (mMethodChannel != null) {
            mMethodChannel.setMethodCallHandler(null);
            mMethodChannel = null;
        }
        if (mThreadPoolExecutor != null) {
            mThreadPoolExecutor.shutdown();
            mThreadPoolExecutor = null;
        }
        if (mFlutterExecutorPool != null) {
            mFlutterExecutorPool.shutdown();
            mFlutterExecutorPool = null;
        }
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            case "initialize":
                method_initialize(call, result);
                break;
            case "execute":
                method_execute(call, result);
                break;
            default:
                result.notImplemented();
        }
    }

    private void method_execute(final MethodCall call, final MethodChannel.Result result) {
        mThreadPoolExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Number functionCallback = call.argument("functionCallback");
                            Object functionArgs = call.argument("functionArgs");
                            final FlutterExecutorPool.FlutterResult flutterResult =
                                    mFlutterExecutorPool.execute(functionCallback.longValue(), functionArgs);
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (flutterResult.isError) {
                                        result.error(flutterResult.errorCode, flutterResult.errorMessage, flutterResult.errorDetails);
                                    } else if (flutterResult.isNotImplemented) {
                                        result.notImplemented();
                                    } else {
                                        result.success(flutterResult.result);
                                    }
                                }
                            });
                        } catch (final Exception e) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Log.e(TAG, "Error: " + e.getMessage(), e);
                                    result.error(e.getClass().getName(), "Unable to execute: " + e.getMessage(), e);
                                }
                            });
                        }
                    }
                }
        );
    }

    private void method_initialize(final MethodCall call, final MethodChannel.Result result) {
        Number dispatcherCallback = call.argument("dispatcherCallback");
        String backgroundChannelName = call.argument("backgroundChannelName");
        Number poolSize = call.argument("poolSize");
        int poolSizeInt = poolSize == null ? 1 : Math.max(1, poolSize.intValue());
        mFlutterExecutorPool = new FlutterExecutorPool(mBinding.getApplicationContext(), poolSizeInt, dispatcherCallback.longValue(), backgroundChannelName);
        mFlutterExecutorPool.initExecutors();
        int threadPoolSize = Math.max(2, poolSizeInt);
        mThreadPoolExecutor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize,
                5, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        mThreadPoolExecutor.allowCoreThreadTimeOut(true);
        mThreadPoolExecutor.prestartAllCoreThreads();
        result.success(null);
    }

}
