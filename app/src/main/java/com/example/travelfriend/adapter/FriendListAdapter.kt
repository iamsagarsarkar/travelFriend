package com.example.travelfriend.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.travelfriend.R
import com.example.travelfriend.models.FriendListModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.item_friend_list.view.*
import java.net.URL
import java.util.concurrent.Executors

open class FriendListAdapter(private val context: Context, private val list: ArrayList<FriendListModel>, private val userFriend : MutableMap<String,Int>):RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
            holder.itemView.btn_friend_list_remove.setOnClickListener {
                deleteFriendToFirebase(holder,model.userId.toString(),position)
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

    @SuppressLint("NotifyDataSetChanged")
    private fun deleteFriendToFirebase(holder: RecyclerView.ViewHolder, userId:String,position: Int){
        userFriend.remove(userId)
        val friend = mapOf("friendId" to userFriend)

        dbFireStore.collection("friendList").document(currentUser!!.uid).update(friend)
            .addOnSuccessListener {
               deleteFriendListUser(holder, userId, position)
                Log.i("userData","friendList successfully update")
            }.addOnFailureListener { e -> Log.w("userData", "Error updating document", e) }

    }


    private fun deleteFriendListUser(holder: RecyclerView.ViewHolder,userId:String,position: Int){
        dbFireStore.collection("friendList").document(userId).get().addOnSuccessListener { document ->
            if (document.exists()){
                val friendListOfFriend = document.get("friendId") as MutableMap<String,Int>
                friendListOfFriend.remove(currentUser!!.uid)
                val friend = mapOf("friendId" to friendListOfFriend)
                dbFireStore.collection("friendList").document(userId).update(friend)
                    .addOnSuccessListener {
                        list.removeAt(position)
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
}