package com.example.travelfriend.activities

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travelfriend.R
import com.example.travelfriend.adapter.FriendListAdapter
import com.example.travelfriend.models.FriendListModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_alluser.*
import kotlinx.android.synthetic.main.activity_friend.*
import kotlinx.android.synthetic.main.activity_home.*

class FriendActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var dbFireStore: FirebaseFirestore
    private lateinit var adapter: FriendListAdapter
    private lateinit var friendList : ArrayList<FriendListModel>

    private lateinit var userFriend : MutableMap<String,Int>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setSupportActionBar(activity_friend_list_toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity_friend_list_toolBar.setNavigationOnClickListener {
            onBackPressed()
        }

        auth = Firebase.auth
        dbFireStore = FirebaseFirestore.getInstance()

        getFriendFromFirebase()


        swipe_refresher_layout_friend_list.setOnRefreshListener {
            getFriendFromFirebase()
            swipe_refresher_layout_friend_list.isRefreshing = false
        }

        btn_add_friend.setOnClickListener {
            val intent = Intent(this,AlluserActivity::class.java)
            startActivityForResult(intent, NEW_FRIEND_ACTIVITY_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == NEW_FRIEND_ACTIVITY_REQUEST_CODE){
            Log.i("Activity","Successfully operation done ")
            getFriendFromFirebase()
        }
    }


    private fun getAllFriendListFromFireStore() {

        dbFireStore.collection("userInfo").orderBy("userName", Query.Direction.ASCENDING)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        Log.e("fireStoreError", error.message.toString())
                        return
                    }

                    rv_friend_list.visibility = View.VISIBLE
                    tv_empty_list.visibility = View.GONE

                    for (documentChange: DocumentChange in value?.documentChanges!!) {
                        if (documentChange.type == DocumentChange.Type.ADDED) {
                            if ( userFriend.containsKey(documentChange.document.id)){
                                  friendList.add(
                                    FriendListModel(
                                        0, documentChange.document.id,
                                        documentChange.document.getString("userName"),
                                        documentChange.document.getString("userImage"),
                                    documentChange.document.getString("userEmail"),
                                    0))
                            }
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

            })

    }


    // get friend list from firebase


    private fun getFriendFromFirebase(){
        val documentFirebase = dbFireStore.collection("friendList").document(auth.currentUser!!.uid)

        documentFirebase.get().addOnSuccessListener { document ->
            if (document.exists()) {
                Log.d("userData", "get Data ")
                userFriend = document.get("friendId") as MutableMap<String, Int>
                for (friend in userFriend){
                    if (friend.value != 2){
                        userFriend.remove(friend.key)
                    }
                }
                    setUpFriendListRecyclerView()

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

        dbFireStore.collection("friendList").document(auth.currentUser!!.uid).set(friend)
            .addOnSuccessListener {
                getFriendFromFirebase()
                Log.i("userData","list successfully added")
            }
            .addOnFailureListener { e -> Log.w("userData", "Error writing document", e) }

    }



    private fun setUpFriendListRecyclerView(){
        rv_friend_list.layoutManager = LinearLayoutManager(this)
        rv_friend_list.setHasFixedSize(true)
        friendList = arrayListOf()
        adapter = FriendListAdapter(this,friendList,userFriend)
        rv_friend_list.adapter = adapter
        getAllFriendListFromFireStore()
    }

    companion object {
        private const val NEW_FRIEND_ACTIVITY_REQUEST_CODE = 1
    }
}