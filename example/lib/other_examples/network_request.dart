import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_background_executor/flutter_background_executor.dart';
import 'package:http/http.dart' as http;

class NetworkRequestPage extends StatefulWidget {
  const NetworkRequestPage({Key key}) : super(key: key);

  @override
  _NetworkRequestPageState createState() => _NetworkRequestPageState();
}

class _NetworkRequestPageState extends State<NetworkRequestPage> {
  FlutterBackgroundExecutor _flutterBackgroundExecutor;
  Future _futurePhotoList;

  @override
  void initState() {
    super.initState();
    _flutterBackgroundExecutor = FlutterBackgroundExecutor.getInstance();
    _futurePhotoList = _flutterBackgroundExecutor.execute(fetchPhotos, null);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Network Request Example (PHOTO JSON)"),
      ),
      body: FutureBuilder(
        future: _futurePhotoList,
        builder: (context, snapshot) {
          if (snapshot.hasError) {
            return const Center(
              child: Text('An error has occurred!'),
            );
          } else if (snapshot.hasData) {
            return PhotosList(photos: List.castFrom(snapshot.data));
          } else {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }
        },
      ),
    );
  }
}

class PhotosList extends StatelessWidget {
  const PhotosList({Key key, this.photos}) : super(key: key);

  final List<Map> photos;

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
      ),
      itemCount: photos.length,
      itemBuilder: (context, index) {
        Photo photoObj = Photo.fromJson(Map.castFrom(photos[index]));
        return Image.network(photoObj.thumbnailUrl);
      },
    );
  }
}

// This function will be executed in another isolate/engine
Future<List<Map>> fetchPhotos() async {
  var client = http.Client();
  final response = await client
      .get(Uri.parse('https://jsonplaceholder.typicode.com/photos'));

  List<Photo> photoList = parsePhotos(response.body);
  return photoList.map((e) => e.toMap()).toList();
}

// A function that converts a response body into a List<Photo>.
List<Photo> parsePhotos(String responseBody) {
  final parsed = jsonDecode(responseBody).cast<Map<String, dynamic>>();

  return parsed.map<Photo>((json) => Photo.fromJson(json)).toList();
}

class Photo {
  final int albumId;
  final int id;
  final String title;
  final String url;
  final String thumbnailUrl;

  const Photo({
    this.albumId,
    this.id,
    this.title,
    this.url,
    this.thumbnailUrl,
  });

  factory Photo.fromJson(Map<String, dynamic> json) {
    return Photo(
      albumId: json['albumId'] as int,
      id: json['id'] as int,
      title: json['title'] as String,
      url: json['url'] as String,
      thumbnailUrl: json['thumbnailUrl'] as String,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      "albumId": albumId,
      "id": id,
      "tittle": title,
      "url": url,
      "thumbnailUrl": thumbnailUrl,
    };
  }
}
