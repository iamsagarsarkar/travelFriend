package com.example.travelfriend.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travelfriend.R
import com.example.travelfriend.adapter.MyPostAdapter
import com.example.travelfriend.adapter.TravelFriendAdapter
import com.example.travelfriend.models.MyPostModel
import com.example.travelfriend.models.TravelFriendModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_friend.*
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_my_post.*
import java.util.ArrayList

class MyPostActivity : AppCompatActivity() {

    private val user = Firebase.auth.currentUser
    private lateinit var dbFireStore : FirebaseFirestore

    private lateinit var adapter: MyPostAdapter

    private lateinit var travelFriendList : ArrayList<MyPostModel>




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_post)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setSupportActionBar(toolbar_my_post)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar_my_post.setNavigationOnClickListener {
            onBackPressed()
        }

        dbFireStore = Firebase.firestore


        setupTravelFriendRecyclerView()



        swipe_refresher_layout_my_post.setOnRefreshListener {
           setupTravelFriendRecyclerView()
            swipe_refresher_layout_my_post.isRefreshing = false
        }
    }


    private fun setupTravelFriendRecyclerView(){

        rv_my_post.layoutManager = LinearLayoutManager(this)
        rv_my_post.setHasFixedSize(true)

        travelFriendList = arrayListOf()
        adapter = MyPostAdapter(this,travelFriendList)
        rv_my_post.adapter = adapter

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

                    rv_my_post.visibility = View.VISIBLE
                    tv_empty_my_post.visibility = View.GONE

                    for (documentChange: DocumentChange in value?.documentChanges!!) {
                        if (documentChange.type == DocumentChange.Type.ADDED) {
                            if (documentChange.document.getString("userId") == user!!.uid ){

                                travelFriendList.add(
                                    MyPostModel(
                                        0,documentChange.document.id
                                        ,documentChange.document.getString("userId")!!,
                                        documentChange.document.getString("userName")!!,
                                        documentChange.document.getString("userImage")!!,
                                        documentChange.document.getString("location")!!,
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


}