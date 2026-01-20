package com.example.myayamjago

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myayamjago.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        binding.btnDaftar.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lengkapi semua field", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val berhasil = db.buatAkun(username, email, password)
            if (berhasil) {
                Toast.makeText(this, "Berhasil daftar!", Toast.LENGTH_SHORT).show()
                finish() // kembali ke LoginActivity
            } else {
                Toast.makeText(this, "Username sudah dipakai", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
