package com.app.demochats.model

class ChatMessageModel {
    var text: String? = null
    var name: String? = null
    var photoUrl: String? = null
    var imageUrl: String? = null
    var audioUrl:String?=null

//Empty constructor needed for firestore serialization...
    constructor()
    constructor(text: String?, name: String?, photoUrl: String?, imageUrl: String?,audioUrl:String?) {
        this.text = text
        this.name = name
        this.photoUrl = photoUrl
        this.imageUrl = imageUrl
        this.audioUrl=audioUrl
    }

}