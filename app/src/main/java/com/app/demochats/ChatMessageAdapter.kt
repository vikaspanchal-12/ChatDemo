package com.app.demochats

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.app.demochats.MainActivity.Companion.ANONYMOUS
import com.app.demochats.databinding.ImageMessageBinding
import com.app.demochats.databinding.MessageBinding
import com.app.demochats.databinding.RecorderBinding
import com.app.demochats.model.ChatMessageModel
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

// The FirebaseRecyclerAdapter class and options come from the FirebaseUI library
// See: https://github.com/firebase/FirebaseUI-Android

class ChatMessageAdapter(
    private val options: FirebaseRecyclerOptions<ChatMessageModel>,
    private val currentUserName: String?,
) : FirebaseRecyclerAdapter<ChatMessageModel, ViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_TEXT) {
            val view = inflater.inflate(R.layout.message, parent, false)
            val binding = MessageBinding.bind(view)
            MessageViewHolder(binding)
        }
        else if (viewType == VIEW_TYPE_IMAGE) {
            val view = inflater.inflate(R.layout.image_message, parent, false)
            val binding = ImageMessageBinding.bind(view)
            ImageMessageViewHolder(binding)

        }
        else {
            val view = inflater.inflate(R.layout.recorder, parent, false)
            val binding = RecorderBinding.bind(view)
            RecordViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: ChatMessageModel) {
        if (options.snapshots[position].text != null) {
            (holder as MessageViewHolder).bind(model)
        }
        else if (options.snapshots[position].photoUrl!=null) {
            (holder as ImageMessageViewHolder).bind(model)
        }
        else
        {
            (holder as RecordViewHolder).bind(model)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (options.snapshots[position].text != null)
        {
            VIEW_TYPE_TEXT
        }
        else if (options.snapshots[position].photoUrl!=null)
        {
            VIEW_TYPE_IMAGE
        }

        else VIEW_TYPE_AUDIO
    }

    /*This is MessageViewHolder...*/
    inner class MessageViewHolder(private val binding: MessageBinding) : ViewHolder(binding.root) {
        fun bind(item: ChatMessageModel) {
            binding.messageTextView.text = item.text
            setTextColor(item.name, binding.messageTextView)
            binding.messengerTextView.text = if (item.name == null)
                ANONYMOUS
            else
                StringBuilder().append(item.name)
            //StringBuilder().append(item.name+" "+"komal :)")
            if (item.photoUrl != null) {
                loadImageIntoView(binding.messengerImageView, item.photoUrl!!)

            }
            else
            {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }
        }
        private fun setTextColor(userName: String?, textView: TextView) {
            if (userName != ANONYMOUS && currentUserName == userName && userName != null) {
                textView.setBackgroundResource(R.drawable.rounded_message_blue)
                textView.setTextColor(Color.WHITE)
            } else {
                textView.setBackgroundResource(R.drawable.rounded_message_gray)
                textView.setTextColor(Color.BLACK)
            }
        }
    }


    /*This is ImageViewHolder*/
    inner class ImageMessageViewHolder(private val binding: ImageMessageBinding) : ViewHolder(binding.root) {
        fun bind(item: ChatMessageModel) {
            loadImageIntoView(binding.messageImageView, item.imageUrl!!)
            loadImageIntoView(binding.messageImageView, item.imageUrl!!)
            binding.messengerTextView.text = if (item.name == null) ANONYMOUS
            else item.name
            if(item.photoUrl != null)
            {
                loadImageIntoView(binding.messengerImageView, item.photoUrl!!)
            }
            else
            {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }
        }
    }


    /*This is RecordViewHoler*/
    inner class RecordViewHolder(private val binding:RecorderBinding): ViewHolder(binding.root){
        fun bind(item: ChatMessageModel) {

            loadImageIntoView(binding.audioFileIV, item.audioUrl!!)
            loadImageIntoView(binding.audioFileIV, item.audioUrl!!)

            binding.audioUserName.text = if (item.name == null) ANONYMOUS

            else
                item.name

            if(item.audioUrl != null)
            {
                loadUrlIntoView(binding.audioFileIV, item.audioUrl!!)
            }
            else
            {
                binding.audioUserProfile.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }
        }
    }

    private fun loadImageIntoView(view: ImageView?, url: String) {
        if (url.startsWith("gs://")) {
            //To store image in firebase storage...
            val storageReference = Firebase.storage.getReferenceFromUrl(url)
            storageReference.downloadUrl
                .addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    Glide.with(view!!.context).load(downloadUrl).into(view)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Getting download url was not successful.", e)
                }
        }
        else
        {
            Glide.with(view!!.context).load(url).into(view)
        }
    }
    //LoadLink on imageView....
    private fun loadUrlIntoView(view: ImageView?, url: String) {
        if (url.startsWith("gs://")) {
            //To store audio in firebase storage...
            val storageReference = Firebase.storage.getReferenceFromUrl(url)
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                    val audioDownloadUrl = uri.toString()

                    Glide.with(view!!.context).load(audioDownloadUrl).into(view)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Getting download url was not successful.", e)
                }
        }
        else
        {
            Glide.with(view!!.context).load(url).into(view)
        }
    }

    companion object {
        const val TAG = "MessageAdapter"
        const val VIEW_TYPE_TEXT = 1
        const val VIEW_TYPE_IMAGE = 2
        const val VIEW_TYPE_AUDIO = 3

    }
}
