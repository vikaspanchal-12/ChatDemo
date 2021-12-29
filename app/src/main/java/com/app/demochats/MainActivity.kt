package com.app.demochats

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
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
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var manager: LinearLayoutManager
    //  var toneGen1: ToneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)


    var outputFile = ""
    var myAudioRecorder: MediaRecorder? = null
    var ivVoiceChat: AppCompatImageView? = null
    private var progressDialog: Dialog? = null


    /*Firebase Instance Variables */
    private lateinit var auth: FirebaseAuth

    //Create firebase database..
    private lateinit var firebasedatabase: FirebaseDatabase
    private lateinit var adapter: ChatMessageAdapter


    var storageRef: StorageReference? = null


    // used for getResult from gallery..(registerForActivityResult)
    private val openGalleryDocument = registerForActivityResult(OpenMyGalleryDocument()) { uri ->
        onImageSelected(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (BuildConfig.DEBUG) {
            Firebase.database.useEmulator("10.0.2.2", 9000)
            Firebase.auth.useEmulator("10.0.2.2", 9099)
            Firebase.storage.useEmulator("10.0.2.2", 9199)
        }

        record_button.setRecordView(record_view)

        // ivVoiceChat = findViewById(R.id.ivVoiceChat)

        //  getVoice()

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
        adapter.registerAdapterDataObserver(MyScrollToBottomObserver(binding.messageRecyclerView,
            adapter,
            manager))
        // Disable the send button when there's no text in the input field
        // See MyButtonObserver for details
        binding.messageEditText.addTextChangedListener(MyButtonObserver(binding.sendButton))
        // When the send button is clicked, send a text message
        binding.sendButton.setOnClickListener {
            val chatMessage = ChatMessageModel(
                binding.messageEditText.text.toString(),
                getUserName(),
                getPhotoUrl(),
                "",
            )
            firebasedatabase.reference.child(MESSAGES_CHILD).push().setValue(chatMessage)
            binding.messageEditText.setText("")

        }

        // When the image button is clicked, launch the image picker
        binding.addMessageImageView.setOnClickListener {
            openGalleryDocument.launch(arrayOf("image/*"))
        }
        //record_view.setCounterTimeColor(30000)//counter time color
        /*Handling Permission (Optional)*/





        record_view.setOnRecordListener(object : OnRecordListener {

            override fun onStart() {
                if (checkRecordAudioPermission()) {
                    if (checkRecordAudioPermission()) {
                        try {
                            recordAudio()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),
                            105)
                    }
                    progressDialog?.dismiss()
                }


                //Start Recording..
                if (myAudioRecorder != null) {
                    cancelDialog()
                    // toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)
                    try {
                        if (myAudioRecorder != null) {
                            myAudioRecorder!!.stop()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
                val file = File(outputFile)
                val uri = Uri.fromFile(file)
                storageRef?.let { getUrlData(uri, it, null) }

                Log.d("RecordView", "onStart")
            }

            override fun onCancel() {

                Toast.makeText(this@MainActivity, "cancelled", Toast.LENGTH_SHORT).show()

                //On Swipe To Cancel

                Log.d("RecordView", "onCancel")
                Toast.makeText(
                    this@MainActivity, "Please Hold button for one sec for record voice",
                    Toast.LENGTH_SHORT,
                ).show()
            }

            override fun onFinish(recordTime: Long) {
                val time: String = recordTime.toString()
                val audioMessage = ChatMessageModel(null, getUserName(), getPhotoUrl(), "")
                firebasedatabase.reference.child(MESSAGES_CHILD).push().setValue(audioMessage)
                Log.d("RecordView", "onFinish")
                Log.d("RecordTime", time)
            }

            override fun onLessThanSecond() {
                //When the record time is less than One Second
                Toast.makeText(
                    this@MainActivity,
                    "Please Hold button for one sec at-least",
                    Toast.LENGTH_SHORT,
                ).show()
                Log.d("RecordView", "onLessThanSecond")
            }
        })
        /*Handle Clicks for record  buttons*/
        record_button.setOnRecordClickListener {
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
        record_view.setLessThanSecondAllowed(false)

        //change slide To Cancel Text Color
        record_view.setSlideToCancelTextColor(Color.parseColor("#ff0000"))

        record_view.setCounterTimeColor(Color.parseColor("#ff0000"))

        adapter.notifyDataSetChanged()
    }

    //Upload the Image picker ...
    private fun onImageSelected(uri: Uri?) {
        Log.d(TAG, "Uri: $uri")
        val user = auth.currentUser
        val tempoMessage = ChatMessageModel(null, getUserName(), getPhotoUrl(), LOADING_IMAGE_URL)
        firebasedatabase.reference.child(MESSAGES_CHILD).push().setValue(tempoMessage,

            DatabaseReference.CompletionListener { databaseError, databaseReference ->
                if (databaseError != null) {
                    Log.w(TAG, "Unable to write message to database.", databaseError.toException())
                    return@CompletionListener
                }
                // Build a StorageReference and then upload the file
                val key = databaseReference.key
                val storageReference = Firebase.storage.getReference(user!!.uid).child(key!!)
                    .child(uri?.lastPathSegment!!)
                putImageInStorage(storageReference, uri, key)

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
                            ChatMessageModel(null, getUserName(), getPhotoUrl(), uri.toString())
                        firebasedatabase.reference.child(MESSAGES_CHILD).child(key!!)
                            .setValue(chatMessage)
                    }
            }

            .addOnFailureListener(this) { e ->
                Log.w(TAG, "Image upload task was unsuccessful.", e)
            }
    }


/*
    @SuppressLint("ClickableViewAccessibility")
    fun getVoice() {
        if (checkRecordAudioPermission()) {
            ivVoiceChat!!.setOnTouchListener { v, event ->
                when (event!!.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (checkRecordAudioPermission()) {
                            try {
                                recordAudio()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        } else {
                            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 105)
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if (myAudioRecorder != null) {

                            cancelDialog()
                           // toneGen1.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 200)

                            try {
                                if (myAudioRecorder != null) {
                                    myAudioRecorder!!.stop()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                        }
                        val file = File(outputFile)
                        val uri = Uri.fromFile(file)
                        storageRef?.let { getUrlData(uri, it,null) }

                    }
                }

                true
            }
        }
    }*/


    fun checkRecordAudioPermission(): Boolean {
        return recordPermissionCheck()
    }

    /*Permissions for media recorder*/
    fun recordPermissionCheck(): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            Log.e("checkPERMISSIONSFGFGH", "Record")

            val hasNetworkStatePermission =
                this.checkSelfPermission(Manifest.permission.RECORD_AUDIO)

            val permissionList = ArrayList<String>()

            if (hasNetworkStatePermission != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.RECORD_AUDIO)
            }

            Log.e("PermissionsSize", "${permissionList.size}")

            if (permissionList.isNotEmpty()) {
                this.requestPermissions(permissionList.toTypedArray(), 3)
            } else {
                return true
            }
        } else if (Build.VERSION.SDK_INT < 23) {

            Log.e("checkPERMISSIONS", "Record")

            return true
        }
        return false
    }


    @SuppressLint("WrongConstant")
    private fun recordAudio() {
        //outputFile = this.cacheDir.absolutePath + UUID.randomUUID().toString()
        outputFile = this.cacheDir.absolutePath + "/" + UUID.randomUUID().toString()
        myAudioRecorder = MediaRecorder()
        myAudioRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        myAudioRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        myAudioRecorder!!.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB)
        myAudioRecorder!!.setAudioSamplingRate(16000)
        myAudioRecorder!!.setOutputFile(outputFile)
        myAudioRecorder!!.prepare()

        //  toneGen1.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 50000)
        myAudioRecorder!!.start()

        showDialog()

    }

    private fun cancelDialog() {
        try {
            if (progressDialog != null) {
                progressDialog!!.cancel()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }


    private fun getUrlData(uri: Uri, storageReference: StorageReference, key: String?) {
        // val storageReference = storageRef?.child("audio/" + System.currentTimeMillis())
        val metadata = StorageMetadata.Builder().setContentType(".m4a").build()
        storageReference.putFile(uri, metadata).addOnSuccessListener { p0 ->
            storageReference.downloadUrl.addOnSuccessListener {
                Log.d("audio", it.toString())
                val audioMessage =
                    ChatMessageModel(null, getUserName(), getPhotoUrl(), LOADING_IMAGE_URL)
                firebasedatabase.reference.child("audio/" + System.currentTimeMillis())
                    .setValue(audioMessage)
            }
        }.addOnProgressListener {
        }
    }

    private fun showDialog() {
        try {
            progressDialog = Dialog(this)
            progressDialog!!.setCancelable(false)
            progressDialog!!.setCanceledOnTouchOutside(false)
            progressDialog!!.setContentView(R.layout.progress_dialog)
            progressDialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
            progressDialog!!.show()

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
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
            user.providerData
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
        const val ANONYMOUS = "anonymous user"
        private const val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
    }

}