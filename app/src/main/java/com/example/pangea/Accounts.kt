package com.example.pangea

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.isNotEmpty
import androidx.fragment.app.DialogFragment
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.facebook.FacebookSdk
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.android.synthetic.main.account_view.*
import com.facebook.login.widget.LoginButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

/* This class controls the logic in the "Accounts"-Tab
   New Methods can be implemented as needed.
   Layout-File: account_view.xml */
class Accounts() : DialogFragment(), TwitterHandler.ITwitterCallback, FacebookHandler.IFacebookCallback
{
    //creates the view (account_view.xml)
    lateinit var twitterDialog: Dialog
    lateinit var tHandler: TwitterHandler
    lateinit var twitter_login_btn: Button
    lateinit var fHandler: FacebookHandler
    lateinit var login_button_facebook: Button
    lateinit var add_account_button: FloatingActionButton
    lateinit var twitter_image: ImageView
    lateinit var account_view: View
    lateinit var linear_connected_accounts : LinearLayout


    /* delete if we don't need facebook - START */
    lateinit var facebook_image: ImageView
    /* delete if we don't need facebook - END */
    lateinit var hidden_facebook_button: LoginButton

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {

        FacebookSdk.setApplicationId(getString(R.string.facebook_app_id));
        FacebookSdk.sdkInitialize(context);
        account_view = inflater.inflate(R.layout.account_view, container, false)
        val sharedPref = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE)
        val userMail = sharedPref.getString("current_user", "").toString()

        val user = DatabaseHandler().getRegisteredUser(userMail, requireContext())

        val context = activity as AppCompatActivity
        tHandler = TwitterHandler(context, user)

        twitter_login_btn = account_view.findViewById(R.id.twitter_login_btn)
        login_button_facebook = account_view.findViewById(R.id.login_button_facebook)
        hidden_facebook_button = account_view.findViewById(R.id.hidden_facebook_button)

        add_account_button = account_view.findViewById(R.id.addaccount)

        facebook_image = account_view.findViewById(R.id.facebook_img2)
        twitter_image = account_view.findViewById(R.id.twitter_img)

        /* If you open the app and didn't Logout your twitter account before
        *  closing the app, the connected Account is shown after going to  accounts tab */
        // TODO delete this comment in yellow when database for connected social media accounts are implemented
        if(tHandler.hasLinkedAccount() && tHandler.checkIfTwitterObjectValid())
        {
            refreshConnectedAccounts(account_view, false)
        }


        add_account_button.setOnClickListener {

            twitter_image.visibility = View.VISIBLE
            twitter_image.isClickable = true

            /* delete if we don't need facebook - START */

            facebook_image.visibility = View.VISIBLE
            facebook_image.isClickable = true
            /* delete if we don't need facebook - END */


        }

        twitter_image.setOnClickListener {
            twitter_login_btn.isClickable = true
            twitter_login_btn.performClick()
            twitter_login_btn.isClickable = false

            twitter_image.visibility = View.INVISIBLE
            facebook_image.visibility = View.INVISIBLE

        }

        facebook_image.setOnClickListener {
            login_button_facebook.isClickable = true
            login_button_facebook.performClick()
            login_button_facebook.isClickable = false

            twitter_image.visibility = View.INVISIBLE
            facebook_image.visibility = View.INVISIBLE
        }



        if(tHandler.hasLinkedAccount()) {
            twitter_login_btn.text = getString(R.string.twitter_unlink_text)
        }
        else{
            twitter_login_btn.text = getString(R.string.twitter_link_text)
        }

        twitter_login_btn.setOnClickListener {
            if(tHandler.hasLinkedAccount()) {
                //clear before connected account from view
                cleanUPCardViewAfterLogout(account_view)
                add_account_button.isClickable = true

                //now unlink account
                tHandler.unlinkAccount()
                twitter_login_btn.text =getString(R.string.twitter_link_text)
            }
            else {
                setupDialogAndRequestAuthToken()
            }

        }

        fHandler = FacebookHandler(context, user, activity)
        if(fHandler.hasLinkedAccount() && fHandler.isLoggedIn())
        {
            refreshConnectedAccounts(account_view, true)
        }
        fHandler.initApi(this)
        login_button_facebook = account_view.findViewById(R.id.login_button_facebook)
        hidden_facebook_button = account_view.findViewById(R.id.hidden_facebook_button)
        hidden_facebook_button.setPermissions("pages_show_list");

        if(fHandler.hasLinkedAccount())
        {
            login_button_facebook.text = getString(R.string.facebook_unlink_text)
        }
        else
        {
            login_button_facebook.text = getString(R.string.facebook_link_text)
        }
        login_button_facebook.setOnClickListener {
            hidden_facebook_button.performClick()
        }

        hidden_facebook_button.setOnClickListener {
            fHandler.loginFacebook();
        }

        return account_view
    }

    private fun setupDialogAndRequestAuthToken() {
        GlobalScope.launch(Dispatchers.Default) {

            try {
                val requestToken = tHandler.getTwitterConnector()?.oAuthRequestToken
                withContext(Dispatchers.Main) {
                    if (requestToken != null) {
                        setupTwitterWebviewDialog(requestToken.authorizationURL + "&force_login=true")
                        twitterDialog.show()
                    }
                }
            } catch (e: IllegalStateException) {
                Log.e("ERROR: ", e.toString())
            }
        }
    }

    private fun setupTwitterWebviewDialog(url: String): Dialog {
        twitterDialog = Dialog(requireContext())
        val webView = WebView(activity?.applicationContext!!)
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        webView.webViewClient = tHandler.TwitterWebViewClient(this)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl(url)
        twitterDialog.setContentView(webView)
        return twitterDialog
    }

    override fun oAuthResponse() {
        GlobalScope.launch(Dispatchers.Main) {
            twitterDialog.dismiss()
            twitter_login_btn.text =  "Unlink twitter account"
            Log.d("TWITTER-Handler", "Logged in user and linked account")

            //get username here
            add_account_button.isClickable = false
            refreshConnectedAccounts(account_view, false)
        }
    }

    // needed for Facebook
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        fHandler.callbackManager.onActivityResult(requestCode, resultCode, data)
        if(fHandler.hasLinkedAccount())
        {
            login_button_facebook.text = getString(R.string.facebook_unlink_text)
        }
        else
        {
            login_button_facebook.text = getString(R.string.facebook_link_text)
        }
        refreshConnectedAccounts(account_view, true)
        super.onActivityResult(requestCode, resultCode, data)


    }

    override fun loggedOut() {
        login_button_facebook.text = getString(R.string.facebook_link_text)
    }

    /* this method shows the connected accounts as soon as you log in with a social media account
    *  TODO works for now just for twitter, facebook untouched because unclear if facebooks gets removed from app
    *  TODO also works now just for one connected account. */
    private fun refreshConnectedAccounts(view: View, bool_facebook: Boolean)
    {
        val sharedPref = requireContext().getSharedPreferences("user", Context.MODE_PRIVATE)
        val email = sharedPref.getString("current_user", "").toString()
        val register = DatabaseHandler()
        var connected_accounts = emptyList<SocialMediaAccounts>()

        /* setup button for logout the connected twitter account */


        /* TODO
        *  add database for registered accounts, right now it just adds
        *  the current connected twitter account */
        if(!bool_facebook) {
            val social_media = SocialMediaAccounts(user_name = tHandler.getTwitterUsername(), facebook = false, twitter = true)
            context?.let { register.addSocialMediaAccount(social_media, it) }
        }
        else
        {
            val social_media = SocialMediaAccounts(user_name = fHandler.getUser(), facebook = true, twitter = false)
            context?.let { register.addSocialMediaAccount(social_media, it) }

        }

        /*  get list of all connected social media accounts of this user from Database */
        //val refresh_button_account = account_view.findViewById(R.id.button_refresh_account)
        button_refresh_account.setOnClickListener {
            if (!email.isNullOrEmpty()) {
                connected_accounts = context?.let { register.getAllSocialMediaAccounts(it) }!!
            }

            linear_connected_accounts = view.findViewById(R.id.linearLayoutAccounts)
            linear_connected_accounts.removeAllViews();

            val logout_account: TextView = Button(context)
            logout_account.text = "Logout"
            logout_account.setBackgroundColor(Color.parseColor("#B86566"));
            val draw: Drawable? = Drawable.createFromPath(R.drawable.round_style.toString())
            logout_account.background = draw


            logout_account.setOnClickListener {
                if (!bool_facebook)
                    twitter_login_btn.performClick()
                else
                    hidden_facebook_button.performClick()
            }

            for (account in connected_accounts) {
                var imageParams: RelativeLayout.LayoutParams
                imageParams = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT
                )

                imageParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                val img = ImageView(context)
                val img_logout = ImageView(context)
                img.layoutParams = imageParams
                img_logout.layoutParams = imageParams
                img_logout.si


                if (account.facebook) {
                    img.setImageResource(R.drawable.facebookiconpreview)
                } else {
                    img.setImageResource(R.drawable.twitter_bird_logo_2012_svg)
                }
                img_logout.setImageResource(R.drawable.com_facebook_tooltip_black_xout)
                img_logout.setOnClickListener {
                    if (account.facebook) {
                        fHandler.logoutFacebook(account)
                        button_refresh_account.performClick()
                    } else
                        tHandler.unlinkAccount()
                }
                linear_connected_accounts.addView(img)


                val cardView = activity?.applicationContext?.let { CardView(it) }

                if (cardView != null) {
                    cardView.minimumWidth = 300
                    cardView.minimumHeight = 100
                    cardView.setContentPadding(15, 0, 15, 15)
                    cardView.setCardBackgroundColor(Color.LTGRAY)
                    cardView.radius = 20f
                }

                val textView = TextView(activity?.applicationContext)
                textView.text = account.user_name
                textView.textSize = 18F
                textView.setTextColor(Color.DKGRAY)
                cardView?.addView(textView)
                linear_connected_accounts.addView(cardView)
                linear_connected_accounts.addView(img_logout)

            }
        }

        button_refresh_account.performClick()
    }

    /* This Method deletes the information about the now logged out account */
    private fun cleanUPCardViewAfterLogout (view: View)
    {
        if(tHandler.hasLinkedAccount() && this::linear_connected_accounts.isInitialized)
        {
            linear_connected_accounts.removeAllViews()
        }
    }

}