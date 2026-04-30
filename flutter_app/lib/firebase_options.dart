// ATENÇÃO: Este arquivo precisa ser gerado pelo FlutterFire CLI.
//
// Passos:
//   1. Instale o FlutterFire CLI:  dart pub global activate flutterfire_cli
//   2. Na raiz do projeto Flutter: flutterfire configure --project=meu-consultorio
//   3. Escolha Android e iOS quando perguntado.
//   4. O arquivo firebase_options.dart será gerado automaticamente.
//
// O projeto Firebase é: meu-consultorio (com.meuconsultorio)
// Certifique-se de adicionar o google-services.json (Android)
// e o GoogleService-Info.plist (iOS) nas pastas corretas.

import 'package:firebase_core/firebase_core.dart' show FirebaseOptions;
import 'package:flutter/foundation.dart'
    show defaultTargetPlatform, kIsWeb, TargetPlatform;

class DefaultFirebaseOptions {
  static FirebaseOptions get currentPlatform {
    if (kIsWeb) throw UnsupportedError('Web não suportado');
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return android;
      case TargetPlatform.iOS:
        return ios;
      default:
        throw UnsupportedError('Plataforma não suportada');
    }
  }

  // TODO: Substitua pelos valores reais após rodar `flutterfire configure`
  static const FirebaseOptions android = FirebaseOptions(
    apiKey: 'COLE_AQUI',
    appId: '1:380262589690:android:c8e7699d45ad05e459d543',
    messagingSenderId: '380262589690',
    projectId: 'meu-consultorio',
    storageBucket: 'meu-consultorio.appspot.com',
  );

  static const FirebaseOptions ios = FirebaseOptions(
    apiKey: 'COLE_AQUI',
    appId: 'COLE_AQUI',
    messagingSenderId: '380262589690',
    projectId: 'meu-consultorio',
    storageBucket: 'meu-consultorio.appspot.com',
    iosClientId: 'COLE_AQUI',
    iosBundleId: 'com.meuconsultorio',
  );
}
