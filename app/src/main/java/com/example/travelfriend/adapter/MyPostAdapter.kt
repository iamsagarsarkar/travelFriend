package com.example.travelfriend.adapter

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.travelfriend.R
import com.example.travelfriend.models.MyPostModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.item_travel_friend.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

open class MyPostAdapter(private val context: Context, private val list: ArrayList<MyPostModel>): RecyclerView.Adapter<RecyclerView.ViewHolder>(),
     PopupMenu.OnMenuItemClickListener {

    private lateinit var bitmap: Bitmap
    private lateinit var imageURL: String
    private val dbFireStore = Firebase.firestore
    var positionOfList: Int? = null
    var documentId : String ? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
       return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_travel_friend,parent,false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]
        if (holder is MyViewHolder){
            holder.itemView.tv_user_item.text = model.userName
            holder.itemView.tv_location_item.text = model.location.substringBefore(",")
            getBitmapFromURL(holder.itemView.iv_user_item,model.userImage)
            getBitmapFromURL(holder.itemView.iv_post_item,model.postImage)

            holder.itemView.tv_options.setOnClickListener { view ->
                documentId = model.documentId
                positionOfList = position
                imageURL = model.postImage
                showPopupMenu(view)
            }
        }
    }

    override fun getItemCount(): Int {
       return list.size
    }

    private class MyViewHolder(view: View): RecyclerView.ViewHolder(view)




    private fun getBitmapFromURL(imageView: ImageView, imageURL: String){
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        var image: Bitmap? = null
        executor.execute {
            try {
                val `in` = URL(imageURL).openStream()
                image = BitmapFactory.decodeStream(`in`)
                handler.post {
                    bitmap = image!!
                    imageView.setImageBitmap(image)
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun showPopupMenu(view: View){
        val popupMenu = PopupMenu(view.context,view)
        popupMenu.inflate(R.menu.my_post_menu)
        popupMenu.setOnMenuItemClickListener(this)
        popupMenu.show()
    }

    override fun onMenuItemClick(menuItem: MenuItem?): Boolean {
        when(menuItem?.itemId){
            R.id.nav_my_post_save_image ->{
                getBitmapForSave(imageURL)
            }
            R.id.nav_my_post_delete ->{

                deletePost(positionOfList!!,documentId!!)

            }

        }
        return false
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deletePost(position: Int,documentId: String){
        dbFireStore.collection("posts").document(documentId).delete().addOnSuccessListener {
            list.removeAt(position)
            notifyDataSetChanged()
            Log.i("userData","$documentId delete successfully")
        }.addOnFailureListener {
            Log.i("userData","Fail to delete post")
        }


    }


    private fun getBitmapForSave(imageURL : String){
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        var image: Bitmap? = null
        executor.execute {
            try {
                val `in` = URL(imageURL).openStream()
                image = BitmapFactory.decodeStream(`in`)
                handler.post {
                    saveImageToStorage(image)
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }



    private fun saveImageToStorage(imageBitmap: Bitmap?) {


        var stream : OutputStream ? = null
        val fileName = "${UUID.randomUUID()}.jpg"

       try {
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            context.contentResolver.also { contentResolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM+"/TravelFriend/")


                }

                val imageUri: Uri? = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                stream = imageUri?.let { uri -> contentResolver.openOutputStream(uri) }
            }
        }else{
            val root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString()
            val file = File(root,fileName)
            stream = FileOutputStream(file)
        }

        imageBitmap?.compress(Bitmap.CompressFormat.JPEG,100,stream)
        stream?.flush()
        stream?.close()
        Toast.makeText(context,"Image Successfully saved",Toast.LENGTH_SHORT).show()
        }catch (e : IOException){
            Log.i("save",e.toString())
            e.printStackTrace()
        }
    }


}