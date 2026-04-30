# Meu Consultório — Flutter

Migração do app Android (Kotlin/Compose) para Flutter (iOS + Android).

## Status da migração

### ✅ Concluído
- `pubspec.yaml` — todas as dependências
- `lib/main.dart` — entry point com Firebase + Riverpod
- `lib/firebase_options.dart` — placeholder (precisa configurar, ver abaixo)
- `lib/core/theme.dart` — tema Material 3
- `lib/data/models/` — Patient, Appointment, Treatment, Payment, ProntuarioEntry
- `lib/data/database/app_database.dart` — banco sqflite com todas as tabelas
- `lib/data/repositories/` — 5 repositórios (Patient, Appointment, Treatment, Payment, Prontuario)
- `lib/data/services/firestore_sync.dart` — sync bidirecional com Firestore
- `lib/data/services/calendar_service.dart` — integração com calendário do dispositivo (iOS + Android)
- `lib/providers/` — auth, patient, appointment, payment, treatment, prontuario
- `lib/navigation/app_router.dart` — go_router com ShellRoute (bottom nav + nav rail no tablet)
- `lib/widgets/common_widgets.dart` — componentes compartilhados
- `lib/screens/auth/login_screen.dart`
- `lib/screens/home/home_screen.dart`
- `lib/screens/patients/` — lista, detalhe, formulário
- `lib/screens/appointments/` — lista, formulário
- `lib/screens/financial/` — tela principal, formulário de pagamento
- `lib/screens/treatments/treatment_form_screen.dart`
- `lib/screens/prontuario/prontuario_form_screen.dart`

### 🔧 Pendente para próxima sessão
- [ ] Configurar Firebase (ver seção abaixo)
- [ ] Configurar `google-services.json` (Android) e `GoogleService-Info.plist` (iOS)
- [ ] Adicionar `android/` e `ios/` configs de permissão (câmera, galeria, calendário)
- [ ] Testar e ajustar o fluxo de autenticação Google no iOS
- [ ] Adicionar internacionalização (pt_BR) no `MaterialApp` (`localizationsDelegates`)
- [ ] Corrigir pequenos bugs após primeiro `flutter run`

---

## Como configurar o Firebase

1. Instale o FlutterFire CLI:
   ```
   dart pub global activate flutterfire_cli
   ```

2. Na pasta `flutter_app/`, rode:
   ```
   flutterfire configure --project=meu-consultorio
   ```
   Escolha **Android** e **iOS**.

3. O arquivo `lib/firebase_options.dart` será regenerado automaticamente com os valores corretos.

4. Para o login com Google no iOS, você precisa adicionar o `CLIENT_ID` reverso do `GoogleService-Info.plist` no `ios/Runner/Info.plist`:
   ```xml
   <key>CFBundleURLTypes</key>
   <array>
     <dict>
       <key>CFBundleURLSchemes</key>
       <array>
         <string>com.googleusercontent.apps.SEU_CLIENT_ID_AQUI</string>
       </array>
     </dict>
   </array>
   ```

## Como rodar

```bash
cd flutter_app
flutter pub get
flutter run
```

## Arquitetura

```
lib/
  main.dart                    # Entry point
  firebase_options.dart        # Config Firebase (gerado pelo FlutterFire CLI)
  core/
    theme.dart                 # Tema Material 3
  data/
    models/                    # Entidades de dados (com fromMap/toMap)
    database/app_database.dart # Banco local sqflite
    repositories/              # Camada de acesso a dados
    services/
      firestore_sync.dart      # Sincronização com Firestore
      calendar_service.dart    # Integração com Google Calendar
  providers/                   # Estado global (Riverpod StateNotifier)
  screens/                     # Telas do app
  navigation/app_router.dart   # Rotas (go_router)
  widgets/common_widgets.dart  # Componentes reutilizáveis
```

## Diferenças em relação ao app Android

| Android | Flutter |
|---|---|
| Room | sqflite |
| Hilt | Riverpod |
| ViewModel + StateFlow | StateNotifier + StateNotifierProvider |
| Navigation Compose | go_router |
| CalendarContract | add_2_calendar (abre o app de calendário) |
| Jetpack Compose | Flutter widgets |
| Firebase Auth (Google via CredentialManager) | google_sign_in |
