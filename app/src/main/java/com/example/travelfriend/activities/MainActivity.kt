package com.example.travelfriend.activities

import android.content.ContentValues.TAG
import android.content.Intent
import android.net.DnsResolver
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.travelfriend.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.twitter.sdk.android.core.*


open class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbFireStore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private  var userAlreadyExist : Boolean = false
    private var emailArrayList= ArrayList<String>()
    private var userNameArrayList = ArrayList<String>()
    var photoUrl : String ? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        auth = Firebase.auth
        dbFireStore = Firebase.firestore


        getAllEmail()
        btn_signUp.setOnClickListener {
            when {
                input_email.text.toString().isEmpty() -> {
                    Toast.makeText(this,"Enter Email Address",Toast.LENGTH_SHORT).show()
                }
                (input_username.text.toString().isEmpty() && btn_signUp.text.toString() == "SIGN UP") ->{
                    Toast.makeText(this,"Enter User Name",Toast.LENGTH_SHORT).show()
                }
                input_password.text.toString().length<6 -> {
                    Toast.makeText(this,"Password should be minimum 6 characters",Toast.LENGTH_SHORT).show()
                }
                input_confirmPassword.text.toString().length<6 -> {
                    Toast.makeText(this,"Confirm password should be same as password",Toast.LENGTH_SHORT).show()
                }
                (input_password.text.toString() != input_confirmPassword.text.toString() )-> {
                    Toast.makeText(this,"Confirm password should be same as password",Toast.LENGTH_SHORT).show()
                }
                btn_signUp.text.toString() == "SIGN UP" ->{
                    when {
                        isAccountExist() -> {
                            Toast.makeText(this,"This email address all ready have account",Toast.LENGTH_SHORT).show()
                        }
                        isUserNameExist() -> {
                            Toast.makeText(this,"This userName already taken",Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            createAccount(input_email.text.toString(),input_password.text.toString())
                        }
                    }
                }
                btn_signUp.text.toString() == "SIGN IN" -> {
                   signIn(input_email.text.toString(),input_password.text.toString())
                }
            }
        }

        btn_google.setOnClickListener {
           signInWithGoogle()
        }
        btn_ChangedSignButton.setOnClickListener {
            if (btn_ChangedSignButton.text.toString() == "SIGN IN"){
                tf_userName.visibility = View.GONE
                tv_or_sign.text = "OR, Create a New Account"
                btn_ChangedSignButton.text = "SIGN UP"
                btn_signUp.text = "SIGN IN"
            }else{
                tf_userName.visibility = View.VISIBLE
                tv_or_sign.text = "OR, Already Have Account"
                btn_ChangedSignButton.text = "SIGN IN"
                btn_signUp.text = "SIGN UP"
            }

        }
        btn_twitter.setOnClickListener {
            val intent = Intent(this,TwitterActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser != null){
            reload()
        }
    }

    private fun createAccount(email: String, password: String) {

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    updateUI(null)
                }
            }
    }

    private fun signIn(email: String, password: String) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    userAlreadyExist = true
                    updateUI(user)
                } else {
                    Toast.makeText(this,"May be you type wrong password",Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUI(user: FirebaseUser?) {
         addNewUserToFireStore(user)
    }

    private fun reload() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                e.printStackTrace()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    input_email.setText("")
                    updateUI(user)
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun addNewUserToFireStore(currentUser : FirebaseUser?){
        var email : String ? = null
        var userName : String ? = null
        when {
            input_email.text.toString().isEmpty() -> {
                email = currentUser?.email.toString()
                userName = email.substringBefore("@")
                photoUrl = currentUser?.photoUrl.toString()
            }
            else -> {
                email =  input_email.text.toString()
                userName = input_username.text.toString()
            }
        }

        when {
            userAlreadyExist -> {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
            emailArrayList.contains(email) -> {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
            else -> {

                Log.i("userInfo",email)
                Log.i("userInfo",userName)
                val user = hashMapOf("userEmail" to email,
                    "userName" to userName,
                    "userImage" to photoUrl)
                dbFireStore.collection("userInfo").document(currentUser!!.uid).set(user)
                    .addOnSuccessListener {
                        if (photoUrl == null){
                            val intent = Intent(this, UserImageActivity::class.java)
                            startActivity(intent)
                            finish()
                        }else{
                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                    .addOnFailureListener { e -> Log.w("userData", "Error writing document", e) }
            }
        }
    }


    private fun getAllEmail(){
        dbFireStore.collection("userInfo").get().addOnSuccessListener { result ->
                for (document in result) {
                   val email = document.data.getValue("userEmail").toString()
                    val userName = document.data.getValue("userName").toString()
                    emailArrayList.add(email)
                    userNameArrayList.add(userName)
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }

    private fun isAccountExist():Boolean{
        for (email in emailArrayList){
            if (email == input_email.text.toString()){
                return true
            }
        }
        return false
    }

    private fun isUserNameExist():Boolean{
        for (userName in userNameArrayList){
            if (userName == input_username.text.toString()){
                return true
            }
        }
        return false
    }



    companion object{
        private const val RC_SIGN_IN = 1
    }


}
