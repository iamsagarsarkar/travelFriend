package com.example.travelfriend.models

import java.io.Serializable

data class MyPostModel (
    val id:Int,
    val documentId: String,
    val userId: String,
    val userName: String,
    val userImage: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val postImage:String,
    val date: String
): Serializable