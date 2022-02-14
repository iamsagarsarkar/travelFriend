package com.example.travelfriend.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.OAuthProvider
import androidx.annotation.NonNull

import com.google.android.gms.tasks.OnFailureListener

import com.google.firebase.auth.AuthResult

import com.google.android.gms.tasks.OnSuccessListener

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*


class TwitterActivity : MainActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var dbFireStore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = Firebase.auth
        dbFireStore = Firebase.firestore

        val provider = OAuthProvider.newBuilder("twitter.com")
        provider.addCustomParameter("lang", "en")

        val pendingResultTask: Task<AuthResult>? = firebaseAuth.pendingAuthResult

        if (pendingResultTask != null) {
            pendingResultTask
                .addOnSuccessListener { authResult ->
                    updateUI(authResult.user)
                      Toast.makeText(this@TwitterActivity,"Twitter Login successfully done",Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {e ->
                    Log.i("twitter Info","login fail",e)
                }
        } else {
            firebaseAuth.startActivityForSignInWithProvider( this, provider.build()).addOnSuccessListener { authResult ->
                    updateUI(authResult.user)
                }
                .addOnFailureListener {e ->
                    val message = e.toString().substringAfter(": ").substringBefore(" but")
                    Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
                    Log.i("twitter Firebase","login fail",e)
                }
        }
    }



    private fun updateUI(user: FirebaseUser?) {
        addNewUserToFireStore(user)
    }


    private fun addNewUserToFireStore(currentUser : FirebaseUser?){

               val email = currentUser?.email.toString()
               val userName = email.substringBefore("@")
               val photoUrl = currentUser?.photoUrl.toString()

            val user = hashMapOf("userEmail" to email,
                "userName" to userName,
                "userImage" to photoUrl)
            dbFireStore.collection("userInfo").document(currentUser!!.uid).set(user)
                .addOnSuccessListener {
                        val intent = Intent(this, HomeActivity::class.java)
                        startActivity(intent)
                        finish()
                }
                .addOnFailureListener { e -> Log.w("userData", "Error writing document", e) }

    }
}