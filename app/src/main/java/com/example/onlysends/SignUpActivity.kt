package com.example.onlysends
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.onlysends.databinding.ActivitySignUpBinding
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "SignUpFragment"

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize oneTapClient
        oneTapClient = Identity.getSignInClient(this)

        // Set OnClickListener to the google_sign_in_image
        binding.googleSignInImage.setOnClickListener {
            // Start the sign-in process
            signingInGoogle()
        }

        // object to initialize signIn
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    // Your server's client ID, not your Android client ID.
                    .setServerClientId(getString(R.string.web_client_id))
                    // Only show accounts previously used to sign in (overriding this for now)
                    .setFilterByAuthorizedAccounts(false)
                    .build())
            .build()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        Log.d(TAG, "current user is: $currentUser")
    }

    private fun signingInGoogle() {
        Log.d(TAG, "signing in")
        CoroutineScope(Dispatchers.Main).launch {
            signInGoogle()
        }
    }

    private suspend fun signInGoogle() {
        val result = oneTapClient.beginSignIn(signInRequest).await()
        Log.d(TAG, "signInGoogle()")
//        val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
//        activityResultLauncher.launch(intentSenderRequest)
    }

    private val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            Log.d(TAG, "result code: ${result.resultCode}")
            if (result.resultCode == RESULT_OK) {
                try {
                    Log.d(TAG, "result: ${result.data}")
                    val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                    Log.d(TAG, "credential: $credential")
                    val idToken = credential.googleIdToken
                    Log.d(TAG, "token: $idToken")
                    if(idToken != null) {
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential).addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(this, "Sign In Complete", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: ApiException) {
                    e.printStackTrace()
                }
            }
        }
}