import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_background_executor/flutter_background_executor.dart';
import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';

class SqfLitePage extends StatefulWidget {
  const SqfLitePage({Key key}) : super(key: key);

  @override
  _SqfLitePageState createState() => _SqfLitePageState();
}

class _SqfLitePageState extends State<SqfLitePage> {
  FlutterBackgroundExecutor _flutterBackgroundExecutor;

  @override
  void initState() {
    super.initState();
    _flutterBackgroundExecutor = FlutterBackgroundExecutor.getInstance();
  }

  @override
  void dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("SqfLite Example"),
      ),
      body: Center(
        child: FutureBuilder(
          future: _flutterBackgroundExecutor.execute(_getUserFromDb, null),
          builder: (context, snapshot) {
            if (!snapshot.hasData) {
              return CircularProgressIndicator();
            }
            User user = User.fromMap(Map.castFrom(snapshot.data));
            return Text("User data: $user");
          },
        ),
      ),
    );
  }
}

Future<Database> _getDatabase() async {
  String path = await getDatabasesPath();
  return openDatabase(
    join(path, 'sqflite_example.db'),
    version: 1,
    onCreate: (db, version) async {
      // Create and insert user table
      await db.execute(
          'CREATE TABLE USER_REC(id INTEGER PRIMARY KEY, name TEXT, age INTEGER)');
      await db.insert("USER_REC", User(name: "John", age: 35).toMap());
    },
  );
}

/// this will be called in background isolate/engine.
/// SQFLITE plugin can be used in isolate
Future<Map> _getUserFromDb() async {
  var database = await _getDatabase();
  List<Map> mapResult = await database.query("USER_REC");
  return mapResult.first;
}

class User {
  final int id;
  final String name;
  final int age;

  const User({
    this.id,
    this.name,
    this.age,
  });

  factory User.fromMap(Map<String, dynamic> map) {
    return User(
      id: map['id'] as int,
      name: map['name'] as String,
      age: map['age'] as int,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      "id": id,
      "name": name,
      "age": age,
    };
  }

  @override
  String toString() {
    return 'User{id: $id, name: $name, age: $age}';
  }
}
