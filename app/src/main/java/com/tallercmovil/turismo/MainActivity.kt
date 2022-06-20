package com.tallercmovil.turismo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.facebook.*
import com.tallercmovil.turismo.databinding.ActivityMainBinding

import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.squareup.picasso.Picasso


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var callbackManager: CallbackManager
    private lateinit var firebaseAuth: FirebaseAuth

    //Para saber si el usuario ya estaba loggeado
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    //Para el access token de Facebook
    private lateinit var accessTokenTracker: AccessTokenTracker


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Estos ya no son necesarios
        //FacebookSdk.sdkInitialize(getApplicationContext());
        //AppEventsLogger.activateApp(this);

        callbackManager = CallbackManager.Factory.create()
        firebaseAuth = FirebaseAuth.getInstance()

        binding.loginButton.setReadPermissions("email", "public_profile", "user_friends")

        binding.loginButton.registerCallback(callbackManager, object: FacebookCallback<LoginResult?>{
            override fun onSuccess(result: LoginResult?) {
                manejaTokenAcceso(result?.accessToken)
            }

            override fun onCancel() {
                Toast.makeText(this@MainActivity, "Error al ingresar. Por favor instala Facebook primeramente e inicia sesión desde ahí", Toast.LENGTH_LONG).show()
            }

            override fun onError(error: FacebookException?) {
                Toast.makeText(this@MainActivity, "Error al ingresar. Por favor instala Facebook primeramente e inicia sesión desde ahí", Toast.LENGTH_LONG).show()
            }

        })

        authStateListener = AuthStateListener { firebaseAuth->
            val user = firebaseAuth.currentUser
            if(user!=null){
                actualizaUI(user)
            }else{
                actualizaUI(null)
            }
        }

        accessTokenTracker = object: AccessTokenTracker(){
            override fun onCurrentAccessTokenChanged(
                oldAccessToken: AccessToken?,
                currentAccessToken: AccessToken?
            ) {
                if(currentAccessToken == null){
                    firebaseAuth.signOut()
                }
            }

        }
    }



    private fun manejaTokenAcceso(accessToken: AccessToken?) {
        //Para el registro en Firebase

        val authCredential = accessToken?.let{
            FacebookAuthProvider.getCredential(it.token)
        }

        if(authCredential!=null){
            firebaseAuth.signInWithCredential(authCredential).addOnCompleteListener(object: OnCompleteListener<AuthResult?> {
                override fun onComplete(p0: Task<AuthResult?>) {
                    if(p0.isSuccessful){
                        val user = firebaseAuth.currentUser
                        actualizaUI(user)
                    }else{
                        //Maneja error
                        Log.d("INFORMACION", "Error: ${p0.exception.toString()}")
                        Toast.makeText(this@MainActivity, "Error al ingresar", Toast.LENGTH_SHORT).show()
                        actualizaUI(null)
                    }
                }
            })
        }

    }

    private fun actualizaUI(user: FirebaseUser?) {
        if(user!=null){
            binding.tvNombrePerfil.text = user.displayName
            if(user.photoUrl!=null){
                var photoUrl = user.photoUrl.toString()
                photoUrl = photoUrl + "?access_token=" + AccessToken.getCurrentAccessToken()?.token + "&type=large"

                //Con Picasso
                Picasso.get().load(photoUrl).into(binding.ivImagenPerfil)

                //Con Glide
                //Glide.with(this).load(photoUrl).into(binding.ivImagenPerfil)

                binding.ivTravel.visibility = View.INVISIBLE
            }
        }else{
            binding.tvNombrePerfil.text = ""
            binding.ivImagenPerfil.setImageResource(0)
            binding.ivTravel.visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        if(authStateListener!=null){
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

}