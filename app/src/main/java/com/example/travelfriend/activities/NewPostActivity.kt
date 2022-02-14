package com.example.travelfriend.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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
import com.example.travelfriend.utils.GetAddressFromLatLng
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_new_post.*
import kotlinx.android.synthetic.main.activity_user_image.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class NewPostActivity : AppCompatActivity(), View.OnClickListener {


    private val user = Firebase.auth.currentUser
    private lateinit var dbFireStore : FirebaseFirestore
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var storageReference : StorageReference

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener

    private var userName :String ? = null
    private var userImage : String ? = null
    private var postImageUri: String ? = null

    private val calendar = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var locationAddress: String ? = ""
    private var date : String ? = null

    private var saveImageUri : Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_post)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setSupportActionBar(activity_new_post_toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity_new_post_toolBar.setNavigationOnClickListener {
          onBackPressed()
       }


        dbFireStore = Firebase.firestore
        firebaseStorage = Firebase.storage
        storageReference = firebaseStorage.reference

        getUserDataFromFireStore()

        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationListener = LocationListener {location ->
            if (et_add_location.text.isNullOrEmpty()){
                insertLocation(location)
            }
        }



        btn_add_image.setOnClickListener(this)
        et_add_location.setOnClickListener(this)
        et_add_date.setOnClickListener(this)
        btn_post.setOnClickListener(this)


        dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR,year)
            calendar.set(Calendar.MONTH,month)
            calendar.set(Calendar.DAY_OF_MONTH,dayOfMonth)
            setDate()
        }
        setDate()

        if (!Places.isInitialized()){
            Places.initialize(this@NewPostActivity,resources.getString(R.string.google_maps_api_key))
        }


        if (!isLocationEnable()){
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }else{
            currentLocationPermission()
        }
    }
// on click listener
    override fun onClick(view: View?) {
        when(view!!.id){
            R.id.btn_add_image ->{
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
            R.id.et_add_location ->{
             try {
                 val fields = listOf(Place.Field.ID,Place.Field.NAME,Place.Field.LAT_LNG,Place.Field.ADDRESS)
                 val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN,fields).build(this)
                 startActivityForResult(intent, LOCATION_REQUEST_CODE)
             }catch (e : Exception){
                 Log.i("location",e.toString())
             }
            }
            R.id.et_add_date ->{
                DatePickerDialog(this,dateSetListener,calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH)).show()
            }
            R.id.btn_post ->{
                when (saveImageUri) {
                    null -> {
                        Toast.makeText(this, "Please add image", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        addDataToFirebase()
                    }
                }

            }
        }
    }
// on activity result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == LOCATION_REQUEST_CODE){
                val place = Autocomplete.getPlaceFromIntent(data!!)
                et_add_location.setText(place.address)
                locationAddress = place.address
                latitude = place.latLng!!.latitude
                longitude = place.latLng!!.longitude

            }else if (requestCode == ADD_IMAGE_REQUEST_CODE_GALLERY){
                if (data != null){
                   val uri = data.data
                    try {
                        val imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,uri)
                        saveImageUri =  saveImageToStorage(imageBitmap)
                        buttonModeChangeToDisable()
                        uploadImageToFireStorage(saveImageUri.toString())
                        iv_add_image.setImageBitmap(imageBitmap)
                    }catch (e : IOException){
                        e.printStackTrace()
                    }
                }
            }else if(requestCode == ADD_IMAGE_REQUEST_CODE_CAMERA){
                val imageBitmap = data!!.extras!!.get("data") as Bitmap
                saveImageUri = saveImageToStorage(imageBitmap)
                buttonModeChangeToDisable()
                uploadImageToFireStorage(saveImageUri.toString())
                iv_add_image.setImageBitmap(imageBitmap)
            }
        }else{
              Log.i("post data","some error")
        }
    }

    private fun buttonModeChangeToDisable(){
        btn_post.isEnabled = false
        btn_post.setBackgroundResource(R.drawable.shape_button_disable_mode)
        btn_post.setTextColor(application.resources.getColor(R.color.black))
        btn_post.text = "Wait FEW MOMENT"
    }

    private fun buttonModeChangeToEnable(){
        btn_post.isEnabled = true
        btn_post.setBackgroundResource(R.drawable.shape_button_rounded)
        btn_post.setTextColor(application.resources.getColor(R.color.white))
        btn_post.text = "POST"
    }



// permission request result
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
            PERMISSION_CODE_CURRENT_LOCATION ->{
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    getLocation()
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
             }catch (e :ActivityNotFoundException){
                 e.printStackTrace()
             }
         }.setNegativeButton("Cancel"){dialog,_ ->
             dialog.dismiss()
         }.show()
    }
// to add date
    private fun setDate(){
        val format = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        et_add_date.setText(sdf.format(calendar.time).toString())
        date = et_add_date.text.toString()
    }
// To add image

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
        val galleryIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, ADD_IMAGE_REQUEST_CODE_GALLERY)
    }
    private fun cameraIntent(){
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, ADD_IMAGE_REQUEST_CODE_CAMERA)
    }

    private fun saveImageToStorage(imageBitmap: Bitmap) : Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY,Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")
        try {
            val stream : OutputStream = FileOutputStream(file)
            imageBitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e :IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

   //To add current location
    private fun currentLocationPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED){
                val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)
                requestPermissions(permissions, PERMISSION_CODE_CURRENT_LOCATION)
            }else{
                getLocation()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(){
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000, 0F,locationListener)
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        Log.i("location","location manager")
        if (location != null){
            insertLocation(location)
        }else{
            getLocationFromNetwork()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocationFromNetwork(){
        Log.i("location","network")
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,5000, 0F,locationListener)
        val location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (location != null){
            insertLocation(location)
        }
    }


    private fun insertLocation(location: Location){

        latitude = location.latitude
        longitude = location.longitude
        val address = GetAddressFromLatLng(this@NewPostActivity,latitude,longitude)
        address.setAddressListener(object : GetAddressFromLatLng.AddressListener{
            override fun onAddressFound(address: String?) {
                et_add_location.setText(address)
                locationAddress = address
            }
            override fun onError() {
                TODO("Not yet implemented")
            }

        })
        address.getAddress()
    }

    private fun isLocationEnable() : Boolean{
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }


    private fun getUserDataFromFireStore(){
        val document = dbFireStore.collection("userInfo").document(user!!.uid)
        document.addSnapshotListener { value, error ->
            if (error != null) {
                Log.i("userInfo", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (value != null && value.exists()){
                userName = value.getString("userName")
                userImage = value.getString("userImage")
            }

        }
    }


    private fun addDataToFirebase(){
         val timeStamp = Timestamp.now()
        val post = hashMapOf("id" to 0,"userId" to user!!.uid,"userName" to userName,
            "userImage" to userImage,"location" to locationAddress,
            "latitude" to latitude.toString(),"longitude" to longitude.toString(),
            "postImage" to postImageUri,"date" to date,"timeStamp" to timeStamp )


        dbFireStore.collection("posts").add(post).addOnSuccessListener { documentReference ->
            Log.d("newPost", "DocumentSnapshot written with ID: ${documentReference.id}")
                setResult(Activity.RESULT_OK)
                locationManager.removeUpdates(locationListener)
                finish()

        }
            .addOnFailureListener { e ->
                Log.w("newPost", "Error adding document", e)
            }
    }


    private fun uploadImageToFireStorage(imagePath: String){
        Toast.makeText(this,"Wait few moment",Toast.LENGTH_SHORT).show()
        val file = Uri.fromFile(File(imagePath))
        val reference = storageReference.child("postImages/${file.lastPathSegment}")
        val uploadTask = reference.putFile(file)
        uploadTask.addOnFailureListener { e -> Log.w("userData", "Error occurred during upload file", e) }.addOnSuccessListener {
            getImagePath(imagePath)
        }
    }

    private fun getImagePath(imagePath: String?){

        val file = Uri.fromFile(File(imagePath!!))
        val reference = storageReference.child("postImages/${file.lastPathSegment}")

        reference.downloadUrl.addOnSuccessListener {
             postImageUri =  it.toString()
             buttonModeChangeToEnable()
        }.addOnFailureListener { e -> Log.i("path", "Error occurred during upload file", e) }
    }
    companion object{
        private const val ADD_IMAGE_REQUEST_CODE_GALLERY = 1
        private const val ADD_IMAGE_REQUEST_CODE_CAMERA = 2
        private const val LOCATION_REQUEST_CODE = 3
        private const val PERMISSION_CODE_GALLERY = 4
        private const val PERMISSION_CODE_CAMERA = 5
        private const val PERMISSION_CODE_CURRENT_LOCATION = 6
        private const val IMAGE_DIRECTORY = "TravelFriendImages"
    }
}