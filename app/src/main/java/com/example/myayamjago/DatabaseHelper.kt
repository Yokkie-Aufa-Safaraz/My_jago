package com.example.myayamjago

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "AkunTabungan.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_AKUN = "akun"
        private const val COL_ID = "id"
        private const val COL_USERNAME = "username"
        private const val COL_EMAIL = "email"
        private const val COL_PASSWORD = "password"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createAkunTable = """
            CREATE TABLE $TABLE_AKUN (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USERNAME TEXT UNIQUE,
                $COL_EMAIL TEXT,
                $COL_PASSWORD TEXT
            )
        """.trimIndent()
        db?.execSQL(createAkunTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_AKUN")
        onCreate(db)
    }

    // ===== Fungsi register =====
    fun buatAkun(username: String, email: String, password: String): Boolean {
        val db = writableDatabase
        val values = ContentValues()
        values.put(COL_USERNAME, username)
        values.put(COL_EMAIL, email)
        values.put(COL_PASSWORD, password)

        return try {
            db.insertOrThrow(TABLE_AKUN, null, values)
            true
        } catch (e: Exception) {
            false // gagal (misal username sudah ada)
        }
    }

    // ===== Fungsi login =====
    fun cekLogin(username: String, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_AKUN WHERE $COL_USERNAME=? AND $COL_PASSWORD=?",
            arrayOf(username, password)
        )
        val sukses = cursor.moveToFirst()
        cursor.close()
        return sukses
    }

    // ===== Ambil data user (profil) =====
    fun getUser(): Pair<String, String>? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $COL_USERNAME, $COL_EMAIL FROM $TABLE_AKUN LIMIT 1", null)
        if (cursor.moveToFirst()) {
            val username = cursor.getString(0)
            val email = cursor.getString(1)
            cursor.close()
            return Pair(username, email)
        }
        cursor.close()
        return null
    }

    // ===== Hapus semua akun =====
    fun hapusSemuaAkun() {
        writableDatabase.execSQL("DELETE FROM $TABLE_AKUN")
    }
}
