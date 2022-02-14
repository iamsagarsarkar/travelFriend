package com.example.travelfriend.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travelfriend.R
import com.example.travelfriend.adapter.TravelFriendAdapter
import com.example.travelfriend.models.TravelFriendModel
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_friend.*
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_my_post.*
import kotlinx.android.synthetic.main.activity_my_profile.*
import kotlinx.android.synthetic.main.nav_header_layout.*
import java.net.URL
import java.util.ArrayList
import java.util.concurrent.Executors
import kotlin.concurrent.timer

class HomeActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val user = Firebase.auth.currentUser
    private lateinit var dbFireStore : FirebaseFirestore

    private lateinit var adapter: TravelFriendAdapter

    private lateinit var travelFriendList : ArrayList<TravelFriendModel>

    private lateinit var userFriend : MutableMap<String,Int>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setSupportActionBar(activity_home_toolBar)

        dbFireStore = Firebase.firestore

       val actionBarDrawerToggle = ActionBarDrawerToggle(this,drawer_layout,activity_home_toolBar,
           R.string.openNavDrawer,R.string.closeNavDrawer)
        drawer_layout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)


        getUserDataFromFireStore()
        getFriendFromFirebase()

        swipe_refresher_layout_home.setOnRefreshListener {
            getFriendFromFirebase()
            swipe_refresher_layout_home.isRefreshing = false
        }

         btn_new_post.setOnClickListener {
            val intent  = Intent(this,NewPostActivity::class.java)
            startActivityForResult(intent, NEW_POST_ACTIVITY_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == NEW_POST_ACTIVITY_REQUEST_CODE){
            if (resultCode == Activity.RESULT_OK){
                Log.i("firebase","data successfully added")
                getFriendFromFirebase()
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
       when(item.itemId){
           R.id.nav_my_post ->{
               val intent  = Intent(this,MyPostActivity::class.java)
               startActivity(intent)
           }
           R.id.nav_my_profile ->{
               val intent  = Intent(this,MyProfileActivity::class.java)
               startActivity(intent)
           }
           R.id.nav_friend_list ->{
               val intent  = Intent(this,FriendActivity::class.java)
               startActivity(intent)
           }
           R.id.nav_map ->{
               val intent  = Intent(this,FriendMapsActivity::class.java)
               startActivity(intent)
           }
       }
        return true
    }

    private fun getUserDataFromFireStore(){
       val document = dbFireStore.collection("userInfo").document(user!!.uid)
        document.addSnapshotListener { value, error ->
            if (error != null) {
                Log.i("userInfo", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (value != null && value.exists()){
               setUserNameAndPhoto(value)
            }

        }
    }

    private fun setUserNameAndPhoto(value: DocumentSnapshot){
        val imageURL = value.getString("userImage").toString()
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        var image: Bitmap? = null
        executor.execute {
            try {
                val `in` = URL(imageURL).openStream()
                image = BitmapFactory.decodeStream(`in`)
                handler.post {
                    iv_nav_user.setImageBitmap(image)
                    tv_nav_user_name.text = value.getString("userName")
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }



    // get friend list from firebase


    private fun getFriendFromFirebase(){
        val documentFirebase = dbFireStore.collection("friendList").document(user!!.uid)

        documentFirebase.get().addOnSuccessListener { document ->
            if (document.exists()) {
                Log.d("userData", "get Data ")
                userFriend = document.get("friendId") as MutableMap<String, Int>
                for (friend in userFriend){
                    if (friend.value != 2){
                        userFriend.remove(friend.key)
                    }
                }

               setupTravelFriendRecyclerView()

            } else {
                addFriendToFirebase()
                Log.d("userData", "No such document")
            }
        }
            .addOnFailureListener { exception ->
                Log.d("userData", "get failed with ", exception)
            }
    }

    private fun addFriendToFirebase(){
        userFriend = mutableMapOf()
        val friend = hashMapOf("friendId" to userFriend)

        dbFireStore.collection("friendList").document(user!!.uid).set(friend)
            .addOnSuccessListener {
                getFriendFromFirebase()
                Log.i("userData","list successfully added")
            }
            .addOnFailureListener { e -> Log.w("userData", "Error writing document", e) }

    }


    private fun setupTravelFriendRecyclerView(){

        rv_travel_friend_post.layoutManager = LinearLayoutManager(this)
        rv_travel_friend_post.setHasFixedSize(true)

        travelFriendList = arrayListOf()
        adapter = TravelFriendAdapter(this,travelFriendList)
        rv_travel_friend_post.adapter = adapter

        getPostFromFireStore()
    }


    private fun getPostFromFireStore() {

        dbFireStore.collection("posts").orderBy("timeStamp", Query.Direction.DESCENDING)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        Log.e("fireStoreError", error.message.toString())
                        return
                    }

                    rv_travel_friend_post.visibility = View.VISIBLE
                    tv_empty_post.visibility = View.GONE

                    for (documentChange: DocumentChange in value?.documentChanges!!) {
                        if (documentChange.type == DocumentChange.Type.ADDED) {
                            if ( userFriend.containsKey(documentChange.document.getString("userId")) || documentChange.document.getString("userId") == user!!.uid ){
                                        travelFriendList.add(
                                            TravelFriendModel(
                                                0,documentChange.document.getString("userId")!!,
                                                documentChange.document.getString("userName")!!,
                                                documentChange.document.getString("userImage")!!,
                                                documentChange.document.getString("location")!! ,
                                                documentChange.document.getString("latitude")!!.toDouble(),
                                                documentChange.document.getString("longitude")!!.toDouble(),
                                                documentChange.document.getString("postImage")!!,
                                                documentChange.document.getString("date")!!
                                            )
                                        )

                            }
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

            })

    }



    companion object {
        private const val NEW_POST_ACTIVITY_REQUEST_CODE = 1
    }

}