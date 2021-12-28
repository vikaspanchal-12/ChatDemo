package com.app.demochats

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.demochats.databinding.ActivityMainBinding
import com.app.demochats.model.ChatMessageModel
import com.devlomi.record_view.OnRecordListener
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.BuildConfig
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var manager: LinearLayoutManager

    /*Firebase Instance Variables */
    private lateinit var auth: FirebaseAuth

    //Create firebase database..
    private lateinit var firebasedatabase: FirebaseDatabase
    private lateinit var adapter: ChatMessageAdapter


    // used for getResult from gallery..(registerForActivityResult)
    private val openGalleryDocument = registerForActivityResult(OpenMyGalleryDocument()) { uri ->
        onImageSelected(uri)
        onAudioFileSelected(uri)

    /*    if(View_Type_Document_Gallery=="Gallery"){
            onImageSelected(uri)
            startActivity(Intent(this,MainActivity::class.java))
        }
        else
        {
            onAudioFileSelected(uri)
            startActivity(Intent(this,MainActivity::class.java))
        }*/


       /* onImageSelected(uri)
        onAudioFileSelected(uri)*/

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        record_button.setRecordView(record_view)
        if (BuildConfig.DEBUG) {
            Firebase.database.useEmulator("10.0.2.2", 9000)
            Firebase.auth.useEmulator("10.0.2.2", 9099)
            Firebase.storage.useEmulator("10.0.2.2", 9199)
        }

        //Initialize the firebase auth ..
        auth = Firebase.auth
        if (auth.currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            this.finish()
            return
        }
        //Initialize firebase realtime datastore..
        firebasedatabase = Firebase.database
        //create reference for firebase db..
        val messageReference = firebasedatabase.reference.child(MESSAGES_CHILD)
        //Firebase RecyclerAdapter class and options come from the firebase UI Library..
        val options = FirebaseRecyclerOptions.Builder<ChatMessageModel>()
            .setQuery(messageReference, ChatMessageModel::class.java).build()
        adapter = ChatMessageAdapter(options, getUserName())
        binding.progressBar.visibility = View.INVISIBLE
        manager = LinearLayoutManager(this)
        manager.stackFromEnd = true
        //Here set Message Adapter..
        binding.messageRecyclerView.layoutManager = manager
        binding.messageRecyclerView.adapter = adapter

        // Scroll down when a new message arrives
        // See MyScrollToBottomObserver for details
        adapter.registerAdapterDataObserver(MyScrollToBottomObserver(binding.messageRecyclerView, adapter, manager))
        // Disable the send button when there's no text in the input field
        // See MyButtonObserver for details
        binding.messageEditText.addTextChangedListener(MyButtonObserver(binding.sendButton))

        // When the send button is clicked, send a text message
        binding.sendButton.setOnClickListener {
            val chatMessage = ChatMessageModel(binding.messageEditText.text.toString(),
                null,
                getUserName(),
                getPhotoUrl(),
                "")
            firebasedatabase.reference.child(MESSAGES_CHILD).push().setValue(chatMessage)
            binding.messageEditText.setText("")

        }

        // When the image button is clicked, launch the image picker
        binding.addMessageImageView.setOnClickListener {
            openGalleryDocument.launch(arrayOf("image/*"))
        }



        record_view.setOnRecordListener(object : OnRecordListener {

            override fun onStart() {
                //Start Recording..

                Log.d("RecordView", "onStart")
            }

            override fun onCancel() {
                //On Swipe To Cancel
                Log.d("RecordView", "onCancel")
            }

            override fun onFinish(recordTime: Long) {
                val time: String = recordTime.toString()
                val audioMessage = ChatMessageModel(null, getUserName(), getPhotoUrl(), "", getAudioUrl())
                firebasedatabase.reference.child(MESSAGES_CHILD).push().setValue(audioMessage)

                Log.d("RecordView", "onFinish")
                Log.d("RecordTime", time)
            }

            override fun onLessThanSecond() {
                //When the record time is less than One Second
                Log.d("RecordView", "onLessThanSecond")
            }
        })
        /*Handle Clicks for record  buttons*/
        record_button.setOnRecordClickListener {
            ///openGalleryDocument.launch(arrayOf("audio/*"))
            Toast.makeText(this, "RECORD BUTTON CLICKED", Toast.LENGTH_SHORT).show()
        }

        /*Listener for basket animation end ...*/
        record_view.setOnBasketAnimationEndListener {
            Log.d("RecordView", "Basket Animation Finished")
            Toast.makeText(this, "Basket Animation Finished", Toast.LENGTH_SHORT).show()
        }
        /*Set Bounds f0r end animation in floats*/
        record_view.cancelBounds = 8f//dp
        record_view.setSoundEnabled(true)
        //record_view.setSlideToCancelText("TEXT");
        record_view.setLessThanSecondAllowed(false);

        //change slide To Cancel Text Color
        record_view.setSlideToCancelTextColor(Color.parseColor("#ff0000"));

        record_view.setCounterTimeColor(Color.parseColor("#ff0000"));

        //record_view.setCounterTimeColor(30000)//counter time color
        /*Handling Permission (Optional)*/
        adapter.notifyDataSetChanged()
    }

    //Upload the Image picker ...
    private fun onImageSelected(uri: Uri?) {
        Log.d(TAG, "Uri: $uri")
        val user = auth.currentUser
        val tempoMessage = ChatMessageModel(null, getUserName(), getPhotoUrl(), LOADING_IMAGE_URL, "")
        firebasedatabase.reference.child(MESSAGES_CHILD).push().setValue(tempoMessage,
            DatabaseReference.CompletionListener { databaseError, databaseReference ->
                if (databaseError != null) {
                    Log.w(TAG, "Unable to write message to database.", databaseError.toException())
                    return@CompletionListener
                }
                // Build a StorageReference and then upload the file
                val key = databaseReference.key
                val storageReference = Firebase.storage.getReference(user!!.uid).child(key!!).child(uri?.lastPathSegment!!)
                putImageInStorage(storageReference, uri, key)

            })
        adapter.notifyDataSetChanged()
    }



    //For upload Audio...
    private fun onAudioFileSelected(uri: Uri?){
        Log.d(TAG, "Uri: $uri")
        val user = auth.currentUser
        val audioMessage = ChatMessageModel(null, getUserName(), getPhotoUrl(), "LOADING_IMAGE_URL", getAudioUrl())

        firebasedatabase.reference.child(AUDIO_CHILD).push().setValue(audioMessage,
            DatabaseReference.CompletionListener { databaseError, databaseReference ->
                if (databaseError != null) {
                    Log.w(TAG, "Unable to write message to database.", databaseError.toException())
                    return@CompletionListener
                }
                // Build a StorageReference and then upload the file
                val key = databaseReference.key
                val storageReference = Firebase.storage.getReference(user!!.uid).child(key!!).child(uri?.lastPathSegment!!)
                 putAudioInStorage(storageReference, uri, key)
            })


        adapter.notifyDataSetChanged()

    }


    // First upload the image to Cloud Storage..
    private fun putImageInStorage(storageReference: StorageReference, uri: Uri, key: String?) {
        storageReference.putFile(uri)
            .addOnSuccessListener(this) { taskSnapshot -> // After the image loads, get a public downloadUrl for the image
                // and add it to the message.
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        Log.e("photo_gallery", uri.toString())
                      //  Log.e("audio file", uri.toString())
                        val chatMessage =
                            ChatMessageModel(null, getUserName(), getPhotoUrl(), uri.toString(), "")
                        firebasedatabase.reference.child(MESSAGES_CHILD).child(key!!)
                            .setValue(chatMessage)
                    }
            }

            .addOnFailureListener(this) { e ->
                Log.w(TAG, "Image upload task was unsuccessful.", e)
            }
    }
    //Upload Audio File to cloud Storage..
    private fun putAudioInStorage(storageReference: StorageReference, uri: Uri, key: String?) {
        storageReference.putFile(uri)
            .addOnSuccessListener(this) { taskSnapshot -> // After the image loads, get a public downloadUrl for the image
                // and add it to the message.
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                      // Log.e("photo_gallery", uri.toString())
                        Log.e("audio file", uri.toString())
                        val audioMessage = ChatMessageModel(null,"", getUserName(), getPhotoUrl(), getAudioUrl())
                        firebasedatabase.reference.child(MESSAGES_CHILD).child(key!!).setValue(audioMessage)
                    }
            }

            .addOnFailureListener(this) { e ->
                Log.w(TAG, "Image upload task was unsuccessful.", e)
            }
    }
    public override fun onStart() {
        super.onStart()
        // Check if user is signed in.
        if (auth.currentUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, SignInActivity::class.java))
            return
        }
    }

    public override fun onPause() {
        adapter.stopListening()
        super.onPause()
    }

    public override fun onResume() {
        if (adapter != null) {
            adapter.startListening()
            super.onResume()
        } else {
            onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun signOut() {
        AuthUI.getInstance().signOut(this)
        startActivity(Intent(this, SignInActivity::class.java))
        this.finish()
    }

    private fun getPhotoUrl(): String {
        val user = auth.currentUser
        return user?.photoUrl.toString()
    }


    /*ADDED BY VIKAS */
    private fun getAudioUrl(): String {
        val user = auth.currentUser
        return if (user != null) ({
            user.photoUrl
        }).toString()
        else
            ANONYMOUS
    }

    private fun getUserName(): String? {
        val user = auth.currentUser
        return if (user != null) {
            user.displayName
        } else ANONYMOUS
    }

    companion object {
        private const val TAG = "MainActivity"
        const val MESSAGES_CHILD = "messages"
        const val IMAGES_CHILD="images"
        const val AUDIO_CHILD = "audio"

        const val ANONYMOUS = "anonymous user"
         private val View_Type_Document_Gallery="Gallery"
         private val View_Type_Document_Record="2"
        private const val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
    }

}