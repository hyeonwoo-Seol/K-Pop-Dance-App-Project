package com.example.kpopdancepracticeai.data.repository

import android.content.Context
import android.util.Log
import com.example.kpopdancepracticeai.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepository(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // 현재 로그인된 유저 가져오기
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // 1. 이메일 로그인
    suspend fun signInWithEmail(email: String, pw: String): Result<FirebaseUser?> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pw).await()
            Result.success(result.user)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // 2. 이메일 회원가입 (필요시 사용)
    suspend fun signUpWithEmail(email: String, pw: String): Result<FirebaseUser?> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pw).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. 로그아웃
    fun signOut() {
        auth.signOut()
        // 구글 로그아웃도 같이 처리
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .build()
        GoogleSignIn.getClient(context, gso).signOut()
    }

    // 4. 구글 로그인용: ID Token을 이용해 Firebase에 인증
    suspend fun firebaseAuthWithGoogle(idToken: String): Result<FirebaseUser?> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 5. 구글 로그인 클라이언트 옵션 가져오기
    fun getGoogleSignInOptions(): GoogleSignInOptions {
        // default_web_client_id는 google-services.json이 있으면 자동 생성됨
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
}