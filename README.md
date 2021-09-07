# flutter_background_executor

This plugin allow to execute function with arguments in background isolate and return the value.
Currently supporting Android only.

## Features
1. Some Plugin works within this isolate (not all see Caveat section)
2. Allow to configure pool size of the isolate (careful on this see Caveat section)
3. Able to pass arguments for the function
4. Able to return executing function value.

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