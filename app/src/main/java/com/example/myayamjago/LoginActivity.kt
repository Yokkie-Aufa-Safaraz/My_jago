package com.example.myayamjago

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myayamjago.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val user = binding.etUsername.text.toString()
            val pass = binding.etPassword.text.toString()

            // Contoh login sederhana (bisa diganti sesuai kebutuhan)
            if (user == "admin" && pass == "1234") {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Agar user tidak bisa kembali ke menu login dengan tombol back
            } else {
                Toast.makeText(this, "Username atau Password Salah", Toast.LENGTH_SHORT).show()
            }
        }
    }
}