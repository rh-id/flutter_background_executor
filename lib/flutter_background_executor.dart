import 'dart:ui';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

const String backgroundChannelName =
    "m.co.rh.id.flutter_background_executor.background";

class FlutterBackgroundExecutor {
  static FlutterBackgroundExecutor _instance;

  static FlutterBackgroundExecutor getInstance({int poolSize = 1}) {
    if (_instance == null) {
      _instance = FlutterBackgroundExecutor._(poolSize: poolSize);
    }
    return _instance;
  }

  MethodChannel _methodChannel;

  FlutterBackgroundExecutor._({int poolSize = 1}) {
    _methodChannel =
        const MethodChannel('m.co.rh.id.flutter_background_executor');
    _methodChannel.invokeMethod("initialize", {
      "dispatcherCallback": PluginUtilities.getCallbackHandle(
              flutterBackgroundExecutorDispatcher)
          .toRawHandle(),
      "backgroundChannelName": backgroundChannelName,
      "poolSize": poolSize,
    });
  }

  Future<dynamic> execute(Function function, dynamic functionArgs) {
    CallbackHandle callbackHandle = PluginUtilities.getCallbackHandle(function);
    if (callbackHandle == null) {
      return Future.error("Function must be top-level or static");
    }
    return _methodChannel.invokeMethod("execute", {
      "functionCallback": callbackHandle.toRawHandle(),
      "functionArgs": functionArgs,
    });
  }
}

void flutterBackgroundExecutorDispatcher() {
  WidgetsFlutterBinding.ensureInitialized();
  MethodChannel _methodChannel = MethodChannel(backgroundChannelName);
  _methodChannel.setMethodCallHandler((MethodCall call) async {
    final dynamic args = call.arguments;

    final CallbackHandle handle =
        CallbackHandle.fromRawHandle(args["functionCallback"]);

    final Function functionCallback =
        PluginUtilities.getCallbackFromHandle(handle);

    if (functionCallback == null) {
      print('Fatal: could not find callback');
      return Future.error("could not find callback");
    }

    dynamic functionArgs = args["functionArgs"];
    // See StandardMessageCodec for supported values
    if (functionCallback is Function()) {
      return functionCallback();
    } else {
      return functionCallback(functionArgs);
    }
  });

  _methodChannel.invokeMethod<void>('initialized');
}
