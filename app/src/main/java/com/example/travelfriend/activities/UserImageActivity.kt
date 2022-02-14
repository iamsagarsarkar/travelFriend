package com.example.travelfriend.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.travelfriend.R
import com.google.android.libraries.places.widget.Autocomplete
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_my_profile.*
import kotlinx.android.synthetic.main.activity_my_profile.toolbar_my_profile
import kotlinx.android.synthetic.main.activity_new_post.*
import kotlinx.android.synthetic.main.activity_user_image.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import com.google.android.gms.tasks.OnSuccessListener




class UserImageActivity : AppCompatActivity() {

    private var saveImageUri : Uri? = null
    private var fireStorageImage: String? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var dbFireStore: FirebaseFirestore
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var storageReference : StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_image)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setSupportActionBar(toolbar_user_image)

        auth = Firebase.auth
        dbFireStore = Firebase.firestore
        firebaseStorage = Firebase.storage
        storageReference = firebaseStorage.reference


        btn_change_image.setOnClickListener {

            val imageDialog = AlertDialog.Builder(this)
            imageDialog.setTitle("Select Action")
            val imageDialogItem = arrayOf("Chose from Gallery","Take a Photo")
            imageDialog.setItems(imageDialogItem){ _, which ->
                when(which){
                    0 -> chooseFromGallery()
                    1 -> takeFromCamera()
                }
            }
            imageDialog.show()
        }
        btn_set_user_image.setOnClickListener {
            if (saveImageUri == null){
                Toast.makeText(this,"wait few moment for upload image into cloud",Toast.LENGTH_SHORT).show()
            }else{
                updateImageInFirestore(saveImageUri.toString())
            }

        }


    }


// image upload

    private fun chooseFromGallery(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                requestPermissions(permissions, PERMISSION_CODE_GALLERY)
            }else{
                galleryIntent()
            }
        }

    }

    private fun takeFromCamera(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ){
                val permissions = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,android.Manifest.permission.CAMERA)
                requestPermissions(permissions, PERMISSION_CODE_CAMERA)
            }else{
                cameraIntent()
            }
        }
    }
    private fun galleryIntent(){
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, ADD_IMAGE_REQUEST_CODE_GALLERY)
    }
    private fun cameraIntent(){
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, ADD_IMAGE_REQUEST_CODE_CAMERA)
    }

    private fun saveImageToStorage(imageBitmap: Bitmap) : Uri {
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")
        try {
            val stream : OutputStream = FileOutputStream(file)
            imageBitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e : IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }




    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
           PERMISSION_CODE_GALLERY ->{
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    galleryIntent()
                }else{
                    showDialogForPermission()
                }
            }
            PERMISSION_CODE_CAMERA ->{
                if (grantResults.isNotEmpty() && grantResults[2] == PackageManager.PERMISSION_GRANTED){
                    cameraIntent()
                }else{
                    showDialogForPermission()
                }
            }

        }


    }
    // permission dialog
    private fun showDialogForPermission(){
        AlertDialog.Builder(this).setMessage("Look like you turned off permission, To turned on permission").setPositiveButton("Go to Setting"){_,_ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package",packageName,null)
                intent.data = uri
                startActivity(intent)
            }catch (e : ActivityNotFoundException){
                e.printStackTrace()
            }
        }.setNegativeButton("Cancel"){dialog,_ ->
            dialog.dismiss()
        }.show()
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode ==ADD_IMAGE_REQUEST_CODE_GALLERY){
                if (data != null){
                    val uri = data.data
                    try {
                        val imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,uri)
                        saveImageUri =  saveImageToStorage(imageBitmap)
                        uploadImageToFireStorage(saveImageUri.toString())
                        iv_my_change_image.setImageBitmap(imageBitmap)
                    }catch (e : IOException){
                        e.printStackTrace()
                    }
                }
            }else if(requestCode == ADD_IMAGE_REQUEST_CODE_CAMERA){
                val imageBitmap = data!!.extras!!.get("data") as Bitmap
                saveImageUri = saveImageToStorage(imageBitmap)
                uploadImageToFireStorage(saveImageUri.toString())
                iv_my_change_image.setImageBitmap(imageBitmap)
            }
        }
    }



    private fun updateImageInFirestore(imagePath: String?){

        val file = Uri.fromFile(File(imagePath!!))
        val reference = storageReference.child("images/${file.lastPathSegment}")

        reference.downloadUrl.addOnSuccessListener {
            dbFireStore.collection("userInfo").document(auth.currentUser!!.uid).update("userImage",it.toString())
            .addOnSuccessListener {
                val intent = Intent(this,HomeActivity::class.java)
                startActivity(intent)
                finish()
            }.addOnFailureListener { e -> Log.w("userData", "Error updating document", e) }
        }.addOnFailureListener {
                e -> Log.i("path", "Error occurred during upload file", e)
        }
    }

    private fun uploadImageToFireStorage(imagePath: String){
        Toast.makeText(this,"Wait few moment",Toast.LENGTH_SHORT).show()
        val file = Uri.fromFile(File(imagePath))
        val reference = storageReference.child("images/${file.lastPathSegment}")
        val uploadTask = reference.putFile(file)
        uploadTask.addOnFailureListener { e -> Log.w("userData", "Error occurred during upload file", e) }.addOnSuccessListener { taskSnapshot ->
            cardView_user_image.visibility = View.VISIBLE
        }

    }

    companion object{
        private const val ADD_IMAGE_REQUEST_CODE_GALLERY = 1
        private const val ADD_IMAGE_REQUEST_CODE_CAMERA = 2
        private const val PERMISSION_CODE_GALLERY = 3
        private const val PERMISSION_CODE_CAMERA = 4
        private const val IMAGE_DIRECTORY = "TravelFriendImages"
    }



}