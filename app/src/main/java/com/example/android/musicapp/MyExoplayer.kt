package com.example.android.musicapp

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.android.musicapp.models.SongModel
import com.google.firebase.firestore.FirebaseFirestore

object MyExoplayer {
    private var exoPlayer : ExoPlayer? = null
    private var currentSong : SongModel? = null

    fun getCurrentSong() :SongModel?{
        return currentSong
    }

    fun getInstance() : ExoPlayer? {
        return exoPlayer
    }

    fun startPlaying(context: Context, song : SongModel) {
        if (exoPlayer==null)
            exoPlayer = ExoPlayer.Builder(context).build()

        if (currentSong!=song){
            currentSong = song
            updateCount()
            currentSong?.url?.apply {
                val mediaItem = MediaItem.fromUri(this)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                exoPlayer?.play()
            }
        }


    }

    fun updateCount() {
        currentSong?.id?.let { id->
            FirebaseFirestore.getInstance().collection("songs")
                .document(id)
                .get().addOnSuccessListener {
                    var lastestCount = it.getLong("count")
                    if(lastestCount==null){
                        lastestCount = 1L
                    } else {
                        lastestCount = lastestCount+1
                    }

                    FirebaseFirestore.getInstance().collection("songs")
                        .document(id)
                        .update(mapOf("count" to lastestCount))
                }
        }
    }
}