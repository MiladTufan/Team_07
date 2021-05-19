package com.example.pangea

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import org.json.JSONArray
import org.json.JSONObject


class FacebookHandler(private val context: Context, private val user: User, private var activity1: FragmentActivity?)
{
    lateinit var caller: IFacebookCallback
    lateinit var acces_token_string: String
    lateinit var id: String


    var callbackManager: CallbackManager = CallbackManager.Factory.create();
    var accessTokenTracker: AccessTokenTracker = object : AccessTokenTracker() {
        override fun onCurrentAccessTokenChanged(
                oldAccessToken: AccessToken?,
                currentAccessToken: AccessToken?
        ) {
            if (currentAccessToken == null) {
                caller.loggedOut()
            }
        }
    }

    fun  initApi(caller: IFacebookCallback){
        this.caller = caller
        accessTokenTracker.startTracking()
    }

    fun getCurrentAccesToken(): AccessToken
    {
        return AccessToken.getCurrentAccessToken()
    }

    fun isLoggedIn(): Boolean
    {
        val accessToken = AccessToken.getCurrentAccessToken()
        return accessToken != null && !accessToken.isExpired
    }

    fun isLoggedInWithWritePermissions(): Boolean
    {
        if(!isLoggedIn()) {
            loginFacebook()
        }
        val permission = AccessToken.getCurrentAccessToken().permissions
        return permission == arrayOf("publish_actions")
    }

    fun hasLinkedAccount(): Boolean
    {
        return !(user.facebookAuthToken == null)
    }

    fun loginFacebook()
    {
        Log.d("TAG", "LoginFacebook")

        //var permissions: Array<String> = arrayOf("user_posts")
        //LoginManager.getInstance().logInWithPublishPermissions(activity1, permissions.asList())

        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult>
        {
                override fun onSuccess(result: LoginResult)
                {
                    Log.d("TAG", "Success Login")
                    val access_token = result.accessToken;
                    val dbHandler = DatabaseHandler()
                    dbHandler.saveFacebookLink(user, access_token?.token, context)
                    AccessToken.setCurrentAccessToken(access_token); // set Current accessToken
                }

                override fun onCancel()
                {
                    // user closes login popup, safe to ignore
                    Log.d("TAG", "Cancel Login");
                }

                override fun onError(error: FacebookException?)
                {
                    Log.d("TAG", "Error with Facebook Login: " + error.toString());
                    //Toast.makeText(context, "error: "+error.toString(), Toast.LENGTH_LONG).show();
                    TODO("Not yet implemented")
                }
        });
    }

    fun logoutFacebook()
    {
        val dbHandler = DatabaseHandler()
        dbHandler.saveFacebookLink(user, null, context)
        if(AccessToken.getCurrentAccessToken() != null)
        {
            GraphRequest(AccessToken.getCurrentAccessToken(),
                "/me/permissions/", null, HttpMethod.DELETE,
                GraphRequest.Callback {
                    AccessToken.setCurrentAccessToken(null);
                    LoginManager.getInstance().logOut()
                })
        }
    }

    interface IFacebookCallback {

        fun loggedOut()
    }

    fun postMessage(msg: String) : Any
    {
        if(!isLoggedIn()) {
            loginFacebook()
        }


        GraphRequest(
            AccessToken.getCurrentAccessToken(),
            "/me/accounts",
            null,
            HttpMethod.GET,
            GraphRequest.Callback { response ->
                acces_token_string = (((response.jsonObject["data"] as JSONArray)[0]) as JSONObject).get("access_token").toString()

                id = (((response.jsonObject["data"] as JSONArray)[0]) as JSONObject).get("id").toString()
            }
            ).executeAndWait()

        var permissions: Collection<String>? = mutableListOf("pages_read_engagement", "pages_manage_engagement")
        var page_acces = AccessToken(acces_token_string, "1739804412857659", getCurrentAccesToken().userId,permissions, null, null, null, null, null, null, null)
        var page_id ="/"+id +"/feed"


        val jsonobj: JSONObject = JSONObject()
        jsonobj.put("message", msg)


        val request = GraphRequest.newPostRequest(
                page_acces,
                page_id,
            jsonobj)
        {
            Toast.makeText(context, "success!", Toast.LENGTH_LONG).show()

        }
        request.executeAsync()
        return 0



    }
}