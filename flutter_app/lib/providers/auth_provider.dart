import 'dart:async';

import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_sign_in/google_sign_in.dart';

import '../data/services/firestore_sync.dart';

// ─── State ────────────────────────────────────────────────────────────────────

enum AuthStatus { loading, authenticated, unauthenticated }

class AuthState {
  final AuthStatus status;
  final String? email;
  final String? errorMessage;

  const AuthState({
    required this.status,
    this.email,
    this.errorMessage,
  });

  const AuthState.loading()
      : status = AuthStatus.loading,
        email = null,
        errorMessage = null;

  const AuthState.authenticated(String email)
      : status = AuthStatus.authenticated,
        email = email,
        errorMessage = null;

  const AuthState.unauthenticated({String? error})
      : status = AuthStatus.unauthenticated,
        email = null,
        errorMessage = error;

  AuthState copyWith({
    AuthStatus? status,
    String? email,
    String? errorMessage,
  }) {
    return AuthState(
      status: status ?? this.status,
      email: email ?? this.email,
      errorMessage: errorMessage ?? this.errorMessage,
    );
  }
}

// ─── Notifier ─────────────────────────────────────────────────────────────────

class AuthNotifier extends StateNotifier<AuthState> {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  final GoogleSignIn _googleSignIn = GoogleSignIn();
  final FirestoreSync _sync = FirestoreSync();

  StreamSubscription<User?>? _authSubscription;

  AuthNotifier() : super(const AuthState.loading()) {
    _authSubscription = _auth.authStateChanges().listen(_onAuthChanged);
  }

  void _onAuthChanged(User? user) {
    if (user == null) {
      state = const AuthState.unauthenticated();
    } else {
      state = AuthState.authenticated(user.email ?? '');
      // Pull cloud data after sign-in
      _sync.pullAll();
    }
  }

  Future<void> signIn(String email, String password) async {
    state = const AuthState.loading();
    try {
      await _auth.signInWithEmailAndPassword(email: email, password: password);
      // state will be updated via authStateChanges stream
    } on FirebaseAuthException catch (e) {
      state = AuthState.unauthenticated(error: e.message);
    } catch (e) {
      state = AuthState.unauthenticated(error: e.toString());
    }
  }

  Future<void> signUp(String email, String password) async {
    state = const AuthState.loading();
    try {
      await _auth.createUserWithEmailAndPassword(
          email: email, password: password);
      // state will be updated via authStateChanges stream
    } on FirebaseAuthException catch (e) {
      state = AuthState.unauthenticated(error: e.message);
    } catch (e) {
      state = AuthState.unauthenticated(error: e.toString());
    }
  }

  Future<void> signInWithGoogle() async {
    state = const AuthState.loading();
    try {
      final googleUser = await _googleSignIn.signIn();
      if (googleUser == null) {
        // User cancelled the sign-in flow
        state = const AuthState.unauthenticated();
        return;
      }

      final googleAuth = await googleUser.authentication;
      final credential = GoogleAuthProvider.credential(
        accessToken: googleAuth.accessToken,
        idToken: googleAuth.idToken,
      );

      await _auth.signInWithCredential(credential);
      // state will be updated via authStateChanges stream
    } on FirebaseAuthException catch (e) {
      state = AuthState.unauthenticated(error: e.message);
    } catch (e) {
      state = AuthState.unauthenticated(error: e.toString());
    }
  }

  Future<void> signOut() async {
    try {
      await _googleSignIn.signOut();
    } catch (_) {}
    await _auth.signOut();
    // state will be updated via authStateChanges stream
  }

  @override
  void dispose() {
    _authSubscription?.cancel();
    super.dispose();
  }
}

// ─── Provider ─────────────────────────────────────────────────────────────────

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>(
  (ref) => AuthNotifier(),
);
