package com.example.travelfriend.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.travelfriend.R
import com.example.travelfriend.models.LocationModel
import com.example.travelfriend.utils.GetAddressFromLatLng
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_alluser.*
import kotlinx.android.synthetic.main.activity_friend_maps.*
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_my_profile.*
import kotlinx.android.synthetic.main.activity_new_post.*
import android.R.attr.name
import android.location.Location
import android.location.LocationListener
import android.os.CountDownTimer
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.EventListener
import java.util.*
import kotlin.collections.ArrayList


class FriendMapsActivity : AppCompatActivity(), OnMapReadyCallback {


    private val user = Firebase.auth.currentUser
    private lateinit var dbFireStore : FirebaseFirestore
    private lateinit var userFriend : MutableMap<String,Int>

    private lateinit var locationList : ArrayList<LocationModel>

    private var userName: String?= null
    private var userEmail: String? = null
    private var userImage: String? = null


    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var locationAddress: String ? = null

    private lateinit var map: GoogleMap

    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    var myMarker : Marker? = null
    private var friendsMarker : ArrayList<Marker>   = ArrayList<Marker>()

    val image : Bitmap ? = null
    var addData: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_maps)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        setSupportActionBar(toolbar_map_friend)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar_map_friend.setNavigationOnClickListener {
            onBackPressed()
        }

        val supportMapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map_friend) as SupportMapFragment
        supportMapFragment.getMapAsync(this)



        locationList = arrayListOf()

        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationListener = LocationListener { location ->
            updateMap(location)
        }


        dbFireStore = Firebase.firestore
        getUserData()

        if (!isLocationEnable()){
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }



        btn_current_location.setOnClickListener {

            currentLocationPermission()

        }


    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
    }

    override fun onBackPressed() {
        super.onBackPressed()
         locationManager.removeUpdates(locationListener)
        dbFireStore.collection("location").document(user!!.uid)
            .delete()
            .addOnSuccessListener {
                Log.i("firebase", "location successfully deleted")
            finish()
            }
            .addOnFailureListener { e -> Log.i("firebase", "Error deleting document", e) }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_CODE_CURRENT_LOCATION ->{
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    getLocation()
                }else{
                    showDialogForPermission()
                }
            }
        }
    }





    private fun currentLocationPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED){
                val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                   Manifest.permission.ACCESS_COARSE_LOCATION)
                requestPermissions(permissions, PERMISSION_CODE_CURRENT_LOCATION)
            }else{
                   getLocation()
            }
        }
    }


    private fun isLocationEnable() : Boolean{
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }


    // permission dialog
    private fun showDialogForPermission(){
        AlertDialog.Builder(this).setMessage("Look like you turned off permission, To turned on permission").setPositiveButton("Go to Setting"){ _, _ ->
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


    private fun addDataToFirebase(){
        val post = hashMapOf("userId" to user!!.uid,"userName" to userName,
            "userEmail" to userEmail,"userImage" to userImage,"userLocation" to locationAddress,
            "userLatitude" to latitude.toString(),"userLongitude" to longitude.toString())

        dbFireStore.collection("location").document(user.uid).set(post)
            .addOnSuccessListener {
                getFriendFromFirebase()
                addData = 1
                Log.i("userData","location successfully added")
            }
            .addOnFailureListener { e -> Log.w("userData", "Error writing document", e) }
    }

    private fun updateDataToFirebase(){
        val post = mapOf("userId" to user!!.uid,"userName" to userName,
            "userEmail" to userEmail,"userImage" to userImage,"userLocation" to locationAddress,
            "userLatitude" to latitude.toString(),"userLongitude" to longitude.toString())

        dbFireStore.collection("location").document(user.uid).update(post)
            .addOnSuccessListener {
                getFriendFromFirebase()
                Log.i("userData","location successfully update")
            }.addOnFailureListener { e -> Log.w("userData", "Error updating document", e) }

    }


    private fun getUserData(){
        val document = dbFireStore.collection("userInfo").document(user!!.uid)
        document.addSnapshotListener { value, error ->
            if (error != null) {
                Log.i("userInfo", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (value != null && value.exists()){
                userName = value.getString("userName")
                userEmail = value.getString("userEmail")
                userImage = value.getString("userImage").toString()
            }

        }
    }


    private fun getLocationDataFromFireStore(){
        locationList.clear()
        dbFireStore.collection("location").orderBy("userName", Query.Direction.ASCENDING)
            .addSnapshotListener(object : EventListener<QuerySnapshot> {
                @SuppressLint("NotifyDataSetChanged")
                override fun onEvent(value: QuerySnapshot?, error: FirebaseFirestoreException?) {
                    if (error != null) {
                        Log.e("fireStoreError", error.message.toString())
                        return
                    }

                    for (documentChange: DocumentChange in value?.documentChanges!!) {
                        if (documentChange.type == DocumentChange.Type.ADDED) {
                            if (userFriend.containsKey(documentChange.document.id)){
                                locationList.add(documentChange.document.toObject(LocationModel::class.java))
                            }
                        }else if (documentChange.type == DocumentChange.Type.REMOVED){
                            if (userFriend.containsKey(documentChange.document.id)){
                                locationList.remove(documentChange.document.toObject(LocationModel::class.java))
                            }
                        }
                    }

                    setAllUserMap(locationList)
                }
            })
    }

    private fun setAllUserMap(locationList: ArrayList<LocationModel>){

        for (marker in friendsMarker){
            Log.i("location","remove location")
            marker.remove()
        }
        for (i in 0 until locationList.size) {
            if (locationList[i].userId != user!!.uid){
                friendsMarker.add(
                createMarker(
                    locationList[i].userLatitude,
                    locationList[i].userLongitude,
                    locationList[i].userName,
                    locationList[i].userImage,
                ))
            }

        }
    }

    private fun createMarker(userLatitude: String?, userLongitude: String?, userName: String?, userImage: String?): Marker {

         return map.addMarker(MarkerOptions().position(LatLng(userLatitude!!.toDouble(), userLongitude!!.toDouble())).title(userName).icon(getMarkerIcon('#' + (Math.random().toString() + "000000").substring(2,8))))!!
    }

    private fun getMarkerIcon(color: String?): BitmapDescriptor? {
        val hsv = FloatArray(3)
        Color.colorToHSV(Color.parseColor(color), hsv)
        return BitmapDescriptorFactory.defaultMarker(hsv[0])
    }




//get user friend list

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
                getLocationDataFromFireStore()

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

// current location


    private fun updateMap(location: Location){

         latitude = location.latitude
         longitude = location.longitude

        val address = GetAddressFromLatLng(this@FriendMapsActivity,latitude,longitude)
        address.setAddressListener(object : GetAddressFromLatLng.AddressListener{
            override fun onAddressFound(address: String?) {
                locationAddress = address
                Log.i("location",location.toString())
            }
            override fun onError() {
                TODO("Not yet implemented")
            }

        })
        address.getAddress()

        val position = LatLng(
            latitude,longitude
        )
        myMarker?.remove()
        myMarker = map.addMarker(MarkerOptions().position(position).title("you"))

        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position, 17f)
        map.animateCamera(newLatLngZoom)
        if (addData == 1){
            updateDataToFirebase()
        }else{
            addDataToFirebase()
        }


    }

    @SuppressLint("MissingPermission")
    private fun getLocation(){
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,5000, 0F,locationListener)
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location != null){
            Log.i("location","GPS")
            updateMap(location)
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
            updateMap(location)
        }
    }


    companion object{
        private const val PERMISSION_CODE_CURRENT_LOCATION = 1
    }
}

