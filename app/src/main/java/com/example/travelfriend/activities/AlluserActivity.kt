package com.example.travelfriend.activities

import android.content.ContentValues
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travelfriend.adapter.AllUserAdapter
import com.example.travelfriend.models.AllListModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_alluser.*
import kotlinx.android.synthetic.main.activity_home.*
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.travelfriend.R

import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import kotlinx.android.synthetic.main.item_friend_list.view.*
import android.R.attr.name

import android.R.attr.name
import android.annotation.SuppressLint
import android.app.Activity
import android.widget.Toast
import android.widget.TextView
import com.google.firebase.firestore.*

import de.hdodenhof.circleimageview.CircleImageView





class AlluserActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbFireStore: FirebaseFirestore
    private lateinit var adapter: AllUserAdapter
    private lateinit var userList : ArrayList<AllListModel>

    private lateinit var friendList : MutableMap<String,Int>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alluser)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setSupportActionBar(activity_all_user_toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity_all_user_toolBar.setNavigationOnClickListener {
            onBackPressed()
        }


        auth = Firebase.auth
        dbFireStore = FirebaseFirestore.getInstance()


        getFriendFromFirebase()

        swipe_refresher_layout_all_user.setOnRefreshListener {
            getFriendFromFirebase()
            swipe_refresher_layout_all_user.isRefreshing = false
        }


        //addFriendToFirebase()




    }



    private fun getAllUserListFromFireStore() {

        dbFireStore.collection("userInfo").orderBy("userName",Query.Direction.ASCENDING)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        Log.e("fireStoreError", error.message.toString())
                        return
                    }

                    rv_all_user.visibility = View.VISIBLE
                    tv_empty_all_user.visibility = View.GONE

                    for (documentChange: DocumentChange in value?.documentChanges!!) {
                        if (documentChange.type == DocumentChange.Type.ADDED) {
                           if ( documentChange.document.id != auth.uid){
                               userList.add(AllListModel(
                                   documentChange.document.id,
                                   documentChange.document.getString("userEmail"),
                                   documentChange.document.getString("userImage"),
                                   documentChange.document.getString("userName")))
                           }
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

            })

    }


    //AddFriend

    private fun getFriendFromFirebase(){
        val documentFirebase = dbFireStore.collection("friendList").document(auth.currentUser!!.uid)

        documentFirebase.get().addOnSuccessListener { document ->
            if (document.exists()) {
                Log.d("userData", "get Data ")
               friendList = document.get("friendId") as MutableMap<String, Int>
                setUpAllUserRecyclerView()

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
        friendList = mutableMapOf()
        val friend = hashMapOf("friendId" to friendList)

        dbFireStore.collection("friendList").document(auth.currentUser!!.uid).set(friend)
            .addOnSuccessListener {
                getFriendFromFirebase()
                Log.i("userData","list successfully added")
            }
            .addOnFailureListener { e -> Log.w("userData", "Error writing document", e) }

    }



    private fun setUpAllUserRecyclerView(){
        rv_all_user.layoutManager = LinearLayoutManager(this)
        rv_all_user.setHasFixedSize(true)
        userList = arrayListOf()
        adapter = AllUserAdapter(this,userList,friendList)
        rv_all_user.adapter = adapter
        getAllUserListFromFireStore()
    }

    companion object {
        private const val NEW_FRIEND_ACTIVITY_REQUEST_CODE = 1
    }





}