import 'package:flutter/material.dart';
import 'package:flutter_background_executor/flutter_background_executor.dart';
import 'package:flutter_background_executor_example/other_examples/network_request.dart';
import 'package:flutter_background_executor_example/other_examples/sqflite.dart';

void main() {
  runApp(MaterialApp(
    home: MyApp(),
  ));
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  FlutterBackgroundExecutor _flutterBackgroundExecutor;
  Future exampleList;
  Future exampleMap;
  Future exampleString;
  Future exampleStringFuture;

  @override
  void initState() {
    super.initState();
    // Initialize and get the instance
    _flutterBackgroundExecutor = FlutterBackgroundExecutor.getInstance();
    exampleList = _flutterBackgroundExecutor.execute(_exampleReturnList,
        ["string1", "string2"]).catchError((error) => print(error));
    exampleMap = _flutterBackgroundExecutor.execute(_exampleReturnMap,
        {"key1": "key1", "key2": 2}).catchError((error) => print(error));
    exampleString = _flutterBackgroundExecutor
        .execute(_exampleReturnString,
            "This is string message passed as args and return as String")
        .catchError((error) => print(error));
    exampleStringFuture = _flutterBackgroundExecutor
        .execute(_exampleReturnStringFuture,
            "This is string message passed as args and return as String")
        .catchError((error) => print(error));
  }

  @override
  Widget build(BuildContext context) {
    const margin = EdgeInsets.all(15);
    return MaterialApp(
      home: Scaffold(
        drawer: Drawer(
          child: ListView(
            padding: EdgeInsets.zero,
            children: <Widget>[
              DrawerHeader(
                decoration: BoxDecoration(
                  color: Colors.blue,
                ),
                child: Text(
                  'Other Examples',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 24,
                  ),
                ),
              ),
              ListTile(
                leading: Icon(Icons.network_wifi),
                title: Text('Network Request Example (Photo JSON)'),
                onTap: () {
                  /*
                  example taken from cookbook https://flutter.dev/docs/cookbook/networking/background-parsing
                  the difference is that the fetching and parsing
                  uses FlutterBackgroundExecutor instead of compute isolates
                  */
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (context) => NetworkRequestPage()),
                  );
                },
              ),
              ListTile(
                leading: Icon(Icons.storage),
                title: Text('Database Example (SQFLITE)'),
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (context) => SqfLitePage()),
                  );
                },
              ),
            ],
          ),
        ),
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: ListView(
          children: [
            Container(
              margin: margin,
              child: FutureBuilder(
                future: exampleList,
                builder: (context, snapshot) {
                  if (!snapshot.hasData) {
                    return CircularProgressIndicator();
                  }
                  return Text(
                      "_exampleReturnList :" + snapshot.data.toString());
                },
              ),
            ),
            Container(
              margin: margin,
              child: FutureBuilder(
                future: exampleMap,
                builder: (context, snapshot) {
                  if (!snapshot.hasData) {
                    return Text("empty result or error");
                  }
                  return Text("_exampleReturnMap :" + snapshot.data.toString());
                },
              ),
            ),
            Container(
              margin: margin,
              child: FutureBuilder(
                future: exampleString,
                builder: (context, snapshot) {
                  if (!snapshot.hasData) {
                    return CircularProgressIndicator();
                  }
                  return Text("_exampleReturnString :" + snapshot.data);
                },
              ),
            ),
            Container(
              margin: margin,
              child: FutureBuilder(
                future: exampleStringFuture,
                builder: (context, snapshot) {
                  if (!snapshot.hasData) {
                    return CircularProgressIndicator();
                  }
                  return Text("_exampleReturnStringFuture :" + snapshot.data);
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
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
