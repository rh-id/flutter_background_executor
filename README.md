# flutter_background_executor

This plugin allow to execute function with arguments in background isolate and return the value.
Currently supporting Android only.

## Features
1. Some Plugin works within this isolate (not all see Caveat section)
2. Allow to configure pool size of the isolate (careful on this see Caveat section)
3. Able to pass arguments for the function
4. Able to return executing function value.
5. Components of this plugn is re-usable.

## Caveat
1. Not all plugin works, all pure dart plugin (example http package) should work.
some plugins that require access to UI (Activity in android) will not work properly.

2. Always monitor RAM usage if pool size > 1. Careful on setting the pool size of this executor as RAM usage can grow huge.

3. Return value and arguments of the function is limited to `StandardMessageCodec` support. see https://api.flutter.dev/javadoc/io/flutter/plugin/common/StandardMessageCodec.html
Any object or type return by the function other than specified on `StandardMessageCodec` will not work including its arguments.

4. Support only one argument `Function(args)`.

## Example Usage

For more detail usage see example directory.

```
void main() {
    WidgetsFlutterBinding.ensureInitialized();
    // initialize singleton with default poolSize (default 1)
    FlutterBackgroundExecutor flutterBackgroundExecutor = FlutterBackgroundExecutor.getInstance();
    flutterBackgroundExecutor.execute(_exampleReturnList,
            ["string1", "string2"]).catchError((error) => print(error));
    flutterBackgroundExecutor.execute(_exampleReturnMap,
            {"key1": "key1", "key2": 2}).catchError((error) => print(error));
    flutterBackgroundExecutor
            .execute(_exampleReturnString,
                "This is string message passed as args and return as String")
            .catchError((error) => print(error));
    flutterBackgroundExecutor
            .execute(_exampleReturnStringFuture,
                "This is string message passed as args and return as String")
            .catchError((error) => print(error));
}

List _exampleReturnList(List list) {
  return list;
}

Map _exampleReturnMap(Map map) {
  return map;
}

String _exampleReturnString(String message) {
  return message;
}

Future<String> _exampleReturnStringFuture(String message) async {
  return message;
}
```

You could also re-use this executor if you need to expand or re-use for background execution (in Android using Alarm Manager or Work Manager).
First, your own customized plugin must be able to pass dispatcher callback and background channel name:
```
void main() {
  WidgetsFlutterBinding.ensureInitialized();
  MethodChannel _methodChannel = MethodChannel("myOwnMethodChannelName");
  // you might need to implement initialize method to passed FlutterBackgroundExecutor callback and background channel name
  // use this plugin default dispatcher flutterBackgroundExecutorDispatcher and default background channel name backgroundChannelName
  // (make sure to import the package flutter_background_executor.dart )
  _methodChannel.invokeMethod("initialize", {
      "dispatcherCallback":
          PluginUtilities.getCallbackHandle(flutterBackgroundExecutorDispatcher)
              .toRawHandle(),
      "backgroundChannelName": backgroundChannelName,
    }).then((value) {
    // you could invoke any method once it is initialized
    });
}   
```
Next, implement FlutterPlugin (android example):
```
public class BackgroundExecutorPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler {
    private FlutterPluginBinding mBinding;
    private MethodChannel mMethodChannel;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        mBinding = binding;
        mMethodChannel =
                new MethodChannel(
                        binding.getBinaryMessenger(), "myOwnMethodChannelName");
        mMethodChannel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        mBinding = null;
        if (mMethodChannel != null) {
            mMethodChannel.setMethodCallHandler(null);
            mMethodChannel = null;
        }
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            case "initialize":
                method_initialize(call, result);
                break;
            case "submitTask":
                // Submit the function and function arguments to be queued in alarm manager or work manager
                break;
            default:
                result.notImplemented();
        }
    }

    private void method_initialize(MethodCall call, MethodChannel.Result result) {
        Number dispatcherCallback = call.argument("dispatcherCallback");
        String backgroundChannelName = call.argument("backgroundChannelName");
        long dispatcherCallbackLong = dispatcherCallback.longValue();
        // Initialize the pool here new FlutterExecutorPool(context, 1, dispatcherCallback, backgroundChannelName);
        // OR save these values to shared preferences to be used in alarm manager or work manager tasks
        result.success(null);
    }
}

```
