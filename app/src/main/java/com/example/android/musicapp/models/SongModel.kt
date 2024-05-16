package com.example.android.musicapp.models

data class SongModel(
    var id : String,
    var title : String,
    var subtitle : String,
    var url : String,
    var coverUrl : String,
) {
    constructor() : this("","","","","")
}
