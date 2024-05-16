package com.example.android.musicapp

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.android.musicapp.adapter.CategoryAdapter
import com.example.android.musicapp.adapter.SectionSongListAdapter
import com.example.android.musicapp.databinding.ActivityMainBinding
import com.example.android.musicapp.models.CategoryModel
import com.example.android.musicapp.models.SongModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects


class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var categoryAdapter: CategoryAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        auth = Firebase.auth
        getCategories()
        setupSection("section_1",binding.section1MainLayout,binding.section1Title,binding.section1RecyclerView)
        setupSection("section_2",binding.section2MainLayout,binding.section2Title,binding.section2RecyclerView)
        setupSection("section_3",binding.section3MainLayout,binding.section3Title,binding.section3RecyclerView)
        setupMostlyPlayed("mostly_played",binding.mostlyPlayedMainLayout,binding.mostlyPlayedTitle,binding.mostlyPlayedRecyclerView)

        binding?.btnSignOut?.setOnClickListener{
            if (auth.currentUser!= null)
            {
                auth.signOut()
                startActivity(Intent(this, GetStartedActivity::class.java))
                finish()
            }
        }

        binding.optionBtn.setOnClickListener {
            showPopupMenu()
        }

        ActionViewFilper();

    }

    fun showPopupMenu(){
        val popupMenu = PopupMenu(this,binding.optionBtn)
        val inflator = popupMenu.menuInflater
        inflator.inflate(R.menu.option_menu,popupMenu.menu)
        popupMenu.show()
        popupMenu.setOnMenuItemClickListener {
            when(it.itemId){
               R.id.logout ->{
                  logout()
                  true
               }
            }
            false
        }
    }


    fun logout(){
        MyExoplayer.getInstance()?.release()
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()
        showPlayerView()
    }

    fun showPlayerView(){
        binding.playerView.setOnClickListener {
            startActivity(Intent(this, PlayerActivity::class.java))
        }
        MyExoplayer.getCurrentSong()?.let {
            binding.playerView.visibility = View.VISIBLE
            binding.songTitleTextView.text = "Now Playing : " + it.title
            Glide.with(binding.songCoverImageView).load(it.coverUrl)
                .apply(
                    RequestOptions().transform(RoundedCorners(32))
                ).into(binding.songCoverImageView)

        } ?: run {
            binding.playerView.visibility = View.GONE
        }
    }



    fun getCategories() {
        FirebaseFirestore.getInstance().collection("category")
            .get().addOnSuccessListener {
                val categoryList = it.toObjects(CategoryModel::class.java)
                setupCategoryRecyclerView(categoryList)
            }
    }

    fun setupCategoryRecyclerView(categoryList: List<CategoryModel>) {
        categoryAdapter = CategoryAdapter(categoryList)
        LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false).also { it.also { it.also { binding.categoriesRecyclerView.layoutManager = it } } }
        binding.categoriesRecyclerView.adapter = categoryAdapter
    }

    fun setupSection(id : String, mainLayout : RelativeLayout, titleView : TextView, recyclerView: RecyclerView) {
        FirebaseFirestore.getInstance().collection("sections")
            .document(id)
            .get().addOnSuccessListener {
                val section = it.toObject(CategoryModel::class.java)
                section?.apply {
                    mainLayout.visibility = View.VISIBLE
                    titleView.text = name
                    recyclerView.layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL, false)
                    recyclerView.adapter = SectionSongListAdapter(songs)
                    mainLayout.setOnClickListener {
                        SongsListActivity.category = section
                        startActivity(Intent(this@MainActivity,SongsListActivity::class.java))
                    }
                }
            }
    }

    fun setupMostlyPlayed(id : String, mainLayout : RelativeLayout, titleView : TextView, recyclerView: RecyclerView) {
        FirebaseFirestore.getInstance().collection("sections")
            .document(id)
            .get().addOnSuccessListener {
                FirebaseFirestore.getInstance().collection("songs")
                    .orderBy("count",Query.Direction.DESCENDING)
                    .limit(5)
                    .get().addOnSuccessListener { songListSnapshot ->
                        val songsModelList = songListSnapshot.toObjects<SongModel>()
                        val songsIdList = songsModelList.map {
                            it.id
                        }.toList()
                        val section = it.toObject(CategoryModel::class.java)
                        section?.apply {
                                section.songs = songsIdList
                                mainLayout.visibility = View.VISIBLE
                                titleView.text = name
                                recyclerView.layoutManager = LinearLayoutManager(
                                    this@MainActivity,
                                    LinearLayoutManager.HORIZONTAL,
                                    false
                                )
                                recyclerView.adapter = SectionSongListAdapter(songs)
                                mainLayout.setOnClickListener {
                                    SongsListActivity.category = section
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            SongsListActivity::class.java
                                        )
                                    )
                                }

                        }

                    }


            }
    }

    fun Favourite(id_songs:String, id_user:String){
        FirebaseFirestore.getInstance().collection("favourite")
            .whereEqualTo("id_songs", id_songs)
            .whereEqualTo("id_user", id_user)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {

                    FirebaseFirestore.getInstance().collection("favourite").document(document.id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this,"Bạn đã ko yêu thích thành công",Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            e ->
                            Toast.makeText(this,"Lỗi không yêu thích",Toast.LENGTH_SHORT).show()
                            Log.w(TAG, "Error deleting document", e)
                        }
                }
            }
            .addOnFailureListener { exception ->

                val data = hashMapOf(
                    "song_id" to id_songs,
                    "id_user" to id_user,
                    "likes" to true
                )

                FirebaseFirestore.getInstance().collection("favourite")
                    .add(data)
                    .addOnSuccessListener { documentReference ->

                        Toast.makeText(this,"Yêu thích thành công",Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this,"Yêu thích thâ bại",Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Yêu thích thất bại ",e)
                    }
            }







                }
            }

    }



    private fun ActionViewFilper() {
        Log.d("MyFragment", "ActionViewFilper() called")

        val slide_in: Animation = AnimationUtils.loadAnimation(this , R.anim.slider_home_in_right)
        val slide_out: Animation = AnimationUtils.loadAnimation(this, R.anim.slide_home_out_left)
        binding.viewLipper.inAnimation = slide_in
        binding.viewLipper.outAnimation = slide_out

        val quangcao: ArrayList<Int> = arrayListOf(
            R.drawable.slider1,
            R.drawable.slider2,
            R.drawable.slider3,
            R.drawable.slider4,
            R.drawable.slider5,
            R.drawable.slider6

        )

        for (drawableId in quangcao) {
            val imageView = ImageView(applicationContext)
            val drawable = ContextCompat.getDrawable(this, drawableId)
            imageView.setImageDrawable(drawable)

            imageView.layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
            imageView.scaleType = ImageView.ScaleType.FIT_XY

            binding.viewLipper.addView(imageView)
        }
        binding.viewLipper.flipInterval = 3000
        binding.viewLipper.isAutoStart = true
    }
}