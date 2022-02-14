package com.example.travelfriend.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.AsyncTask
import java.io.IOException
import java.util.*
import kotlin.text.StringBuilder

class GetAddressFromLatLng(context: Context, private val latitude : Double, private val longitude : Double) : AsyncTask<Void,String,String>() {

    private val geoCoder = Geocoder(context, Locale.getDefault())
    private lateinit var addressListener : AddressListener

    override fun doInBackground(vararg p0: Void?): String {
        try {
            val addressList : List<Address>? = geoCoder.getFromLocation(latitude,longitude,1)
            if (addressList != null && addressList.isNotEmpty()){
                val address = addressList[0]
                val sb = StringBuilder()
                for ( i in 0 .. address.maxAddressLineIndex){
                    sb.append(address.getAddressLine(i)).append(",")
                }
                sb.deleteCharAt(sb.length-1)
                return sb.toString()
             }
        }catch (e : IOException){
            e.printStackTrace()
        }
        return ""
    }

    override fun onPostExecute(result: String?) {
        if (result == null){
            addressListener.onError()
        }else{
            addressListener.onAddressFound(result)
        }
        super.onPostExecute(result)
    }

   fun setAddressListener(addressListener: AddressListener){
       this.addressListener = addressListener
   }

   fun getAddress(){
       execute()
   }

    interface AddressListener{
        fun onAddressFound(address : String?)
        fun onError()
    }
}