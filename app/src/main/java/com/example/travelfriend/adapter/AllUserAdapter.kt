package com.example.travelfriend.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.travelfriend.R
import com.example.travelfriend.models.AllListModel
import com.example.travelfriend.models.LocationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_my_profile.*
import kotlinx.android.synthetic.main.activity_new_post.*
import kotlinx.android.synthetic.main.item_friend_list.view.*
import java.net.URL
import java.util.concurrent.Executors

class AllUserAdapter(private val context: Context,private val list: ArrayList<AllListModel>,private val friendList: MutableMap<String,Int>):RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val currentUser = Firebase.auth.currentUser
    private val dbFireStore = Firebase.firestore

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_friend_list,parent,false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        if (holder is MyViewHolder){
            holder.itemView.tv_friend_list.text = model.userName
            getBitmapFromURL(holder,model.userImage.toString())
            holder.itemView.btn_friend_list_remove.text = "ADD FRIEND"
            for (friend in friendList){
                if (friend.key == model.userId){
                    when (friend.value) {
                        0 -> {
                            buttonModeForRequest(holder)
                        }
                        1 -> {
                            buttonModeForAccept(holder)
                        }
                        else -> {
                            buttonModeForRemove(holder)
                        }
                    }
                }
            }
            holder.itemView.btn_friend_list_remove.setOnClickListener {
                when (holder.itemView.btn_friend_list_remove.text) {
                    "ADD FRIEND" -> {
                        updateFriendToFirebase(holder,model.userId.toString(),0)
                    }
                    "Accept" -> {
                        updateFriendToFirebase(holder,model.userId.toString(),2)
                    }
                    else -> {
                        deleteFriendToFirebase(holder,model.userId.toString())
                    }
                }


            }

            holder.itemView.btn_friend_list_cancel.setOnClickListener {
                deleteFriendToFirebase(holder,model.userId.toString())
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private class MyViewHolder(view: View): RecyclerView.ViewHolder(view)

    private fun getBitmapFromURL(holder: RecyclerView.ViewHolder,imageURL: String){
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        var image: Bitmap? = null
        executor.execute {
            try {
                val `in` = URL(imageURL).openStream()
                image = BitmapFactory.decodeStream(`in`)
                handler.post {
                   holder.itemView.iv_friend_list.setImageBitmap(image)
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateFriendToFirebase(holder: RecyclerView.ViewHolder,userId:String,current:Int){
        friendList[userId] = current
        val friend = mapOf("friendId" to friendList)

        dbFireStore.collection("friendList").document(currentUser!!.uid).update(friend)
            .addOnSuccessListener {
                  updateFriendListUser(holder,userId)

                Log.i("userData","friendList successfully update")
            }.addOnFailureListener { e -> Log.w("userData", "Error updating document", e) }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateFriendListUser(holder: RecyclerView.ViewHolder, userId:String){
        dbFireStore.collection("friendList").document(userId).get().addOnSuccessListener { document ->
            if (document.exists()){
                val friendListOfFriend = document.get("friendId") as MutableMap<String,Int>
                var current = 1
                if (holder.itemView.btn_friend_list_remove.text == "Accept"){
                    current = 2
                }
                friendListOfFriend[currentUser!!.uid] = current
                val friend = mapOf("friendId" to friendListOfFriend)
                dbFireStore.collection("friendList").document(userId).update(friend)
                    .addOnSuccessListener {
                        buttonModeForRemove(holder)
                        notifyDataSetChanged()
                        Log.i("userData","For new friend also friendList successfully update")
                    }.addOnFailureListener { e -> Log.w("userData", "Error updating document", e) }

            } else {
                Log.d("userData", "No such document")
            }
        }
            .addOnFailureListener { exception ->
                Log.d("userData", "get failed with ", exception)
            }
    }

    private fun deleteFriendToFirebase(holder: RecyclerView.ViewHolder,userId:String){
        friendList.remove(userId)
        val friend = mapOf("friendId" to friendList)

        dbFireStore.collection("friendList").document(currentUser!!.uid).update(friend)
            .addOnSuccessListener {
                deleteFriendListUser(holder,userId)
                Log.i("userData","friendList successfully update")
            }.addOnFailureListener { e -> Log.w("userData", "Error updating document", e) }

    }



    @SuppressLint("NotifyDataSetChanged")
    private fun deleteFriendListUser(holder: RecyclerView.ViewHolder, userId:String){
        dbFireStore.collection("friendList").document(userId).get().addOnSuccessListener { document ->
            if (document.exists()){
                val friendListOfFriend = document.get("friendId") as MutableMap<String,Int>
                friendListOfFriend.remove(currentUser!!.uid)
                val friend = mapOf("friendId" to friendListOfFriend)
                dbFireStore.collection("friendList").document(userId).update(friend)
                    .addOnSuccessListener {
                        buttonModeForAddFriend(holder)
                        notifyDataSetChanged()
                        Log.i("userData","For new friend also friendList successfully update")
                    }.addOnFailureListener { e -> Log.w("userData", "Error updating document", e) }

            } else {
                Log.d("userData", "No such document")
            }
        }
            .addOnFailureListener { exception ->
                Log.d("userData", "get failed with ", exception)
            }
    }


    private fun buttonModeForRequest(holder: RecyclerView.ViewHolder){
        holder.itemView.btn_friend_list_remove.isEnabled = false
        holder.itemView.btn_friend_list_cancel.visibility = View.VISIBLE
        holder.itemView.btn_friend_list_remove.setTextColor(Color.parseColor("#000000"))
        holder.itemView.btn_friend_list_remove.text = "Request Send"

    }
    private fun buttonModeForAccept(holder: RecyclerView.ViewHolder){
        holder.itemView.btn_friend_list_cancel.visibility = View.VISIBLE
        holder.itemView.btn_friend_list_remove.setTextColor(Color.parseColor("#FFFFFF"))
        holder.itemView.btn_friend_list_remove.text = "Accept"
    }

    private fun buttonModeForRemove(holder: RecyclerView.ViewHolder){
        holder.itemView.btn_friend_list_cancel.visibility = View.GONE
        holder.itemView.btn_friend_list_remove.setTextColor(Color.parseColor("#FFFFFF"))
        holder.itemView.btn_friend_list_remove.text = "REMOVE FRIEND"
    }

    private fun buttonModeForAddFriend(holder: RecyclerView.ViewHolder){
        holder.itemView.btn_friend_list_cancel.visibility = View.GONE
        holder.itemView.btn_friend_list_remove.isEnabled = true
        holder.itemView.btn_friend_list_remove.setTextColor(Color.parseColor("#FFFFFF"))
        holder.itemView.btn_friend_list_remove.text = "ADD FRIEND"
    }


}