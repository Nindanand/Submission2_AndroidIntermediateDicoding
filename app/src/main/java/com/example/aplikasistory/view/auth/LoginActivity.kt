package com.example.aplikasistory.view.auth

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.aplikasistory.data.pref.UserModel
import com.example.aplikasistory.databinding.ActivityLoginBinding
import com.example.aplikasistory.view.ViewModelFactory
import com.example.aplikasistory.view.main.MainActivity

class LoginActivity : AppCompatActivity() {
    private val viewModel by viewModels<AuthViewModel> {
        ViewModelFactory.getInstance(this)
    }
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showLoading(false)
        setupView()
        setupAction()
        playAnimation()
    }

    private fun playAnimation() {
        ObjectAnimator.ofFloat(binding.imageView, View.TRANSLATION_X, -30f, 30f).apply {
            duration = 6000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }.start()

        val title = ObjectAnimator.ofFloat(binding.titleTextView, View.ALPHA, 1f).setDuration(500)
        val messageText =
            ObjectAnimator.ofFloat(binding.messageTextView, View.ALPHA, 1f).setDuration(500)
        val emailText =
            ObjectAnimator.ofFloat(binding.emailTextView, View.ALPHA, 1f).setDuration(500)
        val emailEditText =
            ObjectAnimator.ofFloat(binding.emailEditTextLayout, View.ALPHA, 1f).setDuration(500)
        val passwordText =
            ObjectAnimator.ofFloat(binding.passwordTextView, View.ALPHA, 1f).setDuration(500)
        val passwordEditText =
            ObjectAnimator.ofFloat(binding.passwordEditTextLayout, View.ALPHA, 1f).setDuration(500)
        val loginButton =
            ObjectAnimator.ofFloat(binding.loginButton, View.ALPHA, 1f).setDuration(500)
        val donthaccountText =
            ObjectAnimator.ofFloat(binding.nothaveaccount, View.ALPHA, 1f).setDuration(500)
        val signupButton =
            ObjectAnimator.ofFloat(binding.signupButton, View.ALPHA, 1f).setDuration(500)

        val together = AnimatorSet().apply {
            playTogether(donthaccountText, signupButton)
        }

        AnimatorSet().apply {
            playSequentially(
                title,
                messageText,
                emailText,
                emailEditText,
                passwordText,
                passwordEditText,
                loginButton,
                together
            )
            start()
        }
    }

    private fun setupView() {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        supportActionBar?.hide()
    }

    private fun setupAction() {
        binding.loginButton.setOnClickListener {
            showLoading(true)
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(
                    this@LoginActivity,
                    "All sections are required to be filled in",
                    Toast.LENGTH_SHORT
                ).show()
                showLoading(false)
            } else {
                val email = binding.emailEditText.text.toString()
                val password = binding.passwordEditText.text.toString()

                viewModel.login(email, password)

            }
        }

        viewModel.loginUser.observe(this) { response ->
            response?.loginResult?.let { loginResult ->
                val name = loginResult.name ?: "DefaultName"
                val token = loginResult.token ?: "DefaultToken"
                viewModel.saveSession(UserModel(name, token))
            } ?: showErrorDialog("Login result is null")
            showLoading(false)
            showDialog()
        }

        binding.signupButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        viewModel.error.observe(this) { errorResponse ->
            errorResponse?.let {
                showLoading(false)
                showErrorDialog(errorResponse.message ?: "Unknown error occurred")
            }
        }
    }

    private fun showErrorDialog(errorMessage: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Login Failed")
            .setMessage(errorMessage)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Login Success")
            .setMessage("You have successfully logged in")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                val mainIntent = Intent(this, MainActivity::class.java)
                startActivity(mainIntent)
            }
            .create()
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}