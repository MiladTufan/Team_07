package com.example.pangea

import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.text.Editable
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.posts_popup.*

class PostsPopup : AppCompatActivity(), TwitterHandler.ITwitterCallback, FacebookHandler.IFacebookCallback
{

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.posts_popup)

        if (Build.VERSION.SDK_INT > 9) {
            val policy: StrictMode.ThreadPolicy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }

        // facebook and twitter have booleans
        send.setOnClickListener {

            val message: Editable? = plain_text_input.text

            val facebook_check = facebookCheck.isChecked
            val twitter_check = twitterCheck.isChecked

            // save in database
            // TODO


            val register = DatabaseHandler()

            val userEmail = intent.getStringExtra("loggedInUserMail").toString()

            val curr_user: User = register.getRegisteredUser(userEmail, applicationContext)

            val fhandler = FacebookHandler(applicationContext, curr_user, this@PostsPopup)
            fhandler.initApi(this)
            val thandler = TwitterHandler(applicationContext, curr_user)
            thandler.initTwitterApi()

            // call facebook or twitter post message here
            if(facebook_check)
            {
                var postId = fhandler.postMessage(message.toString())
                register.addFBPost(userEmail, message.toString(), null, applicationContext, postId.toString())
            }
            else if(twitter_check) {
                var postId = thandler.postTweet(message.toString())
                register.addTwitterPost(userEmail, message.toString(), null, applicationContext, postId)
            }
            else if (facebook_check && twitter_check)
            {
                var postId = fhandler.postMessage(message.toString())
                register.addFBPost(userEmail, message.toString(), null, applicationContext, postId.toString())
                var twitterId = thandler.postTweet(message.toString())
                register.addTwitterPost(userEmail, message.toString(), null, applicationContext, twitterId)
            }
            finish()
        }
    }

    override fun oAuthResponse() {
        TODO("Not yet implemented")
    }

    override fun loggedOut() {
        TODO("Not yet implemented")
    }
}