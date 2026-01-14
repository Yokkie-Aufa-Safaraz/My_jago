package com.example.myayamjago

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myayamjago.databinding.ActivityMainBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Model transaksi
data class Transaksi(
    val tanggal: Int,
    val bulan: Int,
    val tahun: Int,
    val nominal: Int,
    val jenis: String // "TABUNGAN", "PEMASUKAN", "PENGELUARAN"
)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val gson = Gson()
    private lateinit var adapter: TransaksiAdapter
    private var riwayatList = mutableListOf<Transaksi>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Simpan username default "admin" jika belum ada
        val sharedPref = getSharedPreferences("DataTabungan", Context.MODE_PRIVATE)
        if (!sharedPref.contains("USERNAME")) {
            sharedPref.edit().putString("USERNAME", "admin").apply()
        }

        // Setup RecyclerView
        adapter = TransaksiAdapter(riwayatList, this) { loadData() }
        binding.rvRiwayat.layoutManager = LinearLayoutManager(this)
        binding.rvRiwayat.adapter = adapter

        loadData()

        // FAB tambah catatan
        binding.fabTambah.setOnClickListener { showTambahDialog() }

        // BottomNavigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    binding.cardGrafik.visibility = View.VISIBLE
                    binding.tvRiwayatHarian.visibility =
                        if (riwayatList.isEmpty()) View.VISIBLE else View.GONE
                    true
                }
                R.id.nav_tambah -> {
                    showTambahDialog()
                    true
                }
                R.id.nav_profil -> {
                    showProfilDialog()
                    true
                }
                else -> false
            }
        }
    }

    // Dialog tambah catatan
    private fun showTambahDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_input_tabungan, null)
        dialog.setContentView(view)

        val rgJenis = view.findViewById<RadioGroup>(R.id.rgJenisCatatan)
        val etTanggal = view.findViewById<EditText>(R.id.etTanggal)
        val etBulan = view.findViewById<EditText>(R.id.etBulan)
        val etTahun = view.findViewById<EditText>(R.id.etTahun)
        val etNominal = view.findViewById<EditText>(R.id.etNominal)
        val btnSimpan = view.findViewById<Button>(R.id.btnSimpan)

        btnSimpan.setOnClickListener {
            val tanggalStr = etTanggal.text.toString()
            val bulanStr = etBulan.text.toString()
            val tahunStr = etTahun.text.toString()
            val nominalStr = etNominal.text.toString()
            val selectedId = rgJenis.checkedRadioButtonId

            if (tanggalStr.isEmpty() || bulanStr.isEmpty() || tahunStr.isEmpty() ||
                nominalStr.isEmpty() || selectedId == -1
            ) {
                Toast.makeText(this, "Lengkapi semua data!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tanggal = tanggalStr.toInt()
            val bulan = bulanStr.toInt()
            val tahun = tahunStr.toInt()
            val nominal = nominalStr.toInt()
            val jenis = when (selectedId) {
                R.id.rbTabungan -> "TABUNGAN"
                R.id.rbPemasukan -> "PEMASUKAN"
                R.id.rbPengeluaran -> "PENGELUARAN"
                else -> ""
            }

            val sharedPref = getSharedPreferences("DataTabungan", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()

            // Update total saldo
            var totalSaldo = sharedPref.getInt("TOTAL_SALDO", 0)
            totalSaldo = when (jenis) {
                "TABUNGAN", "PEMASUKAN" -> totalSaldo + nominal
                "PENGELUARAN" -> totalSaldo - nominal
                else -> totalSaldo
            }
            editor.putInt("TOTAL_SALDO", totalSaldo)

            // Simpan riwayat transaksi
            val historyJson = sharedPref.getString("RIWAYAT", "[]") ?: "[]"
            val type = object : TypeToken<MutableList<Transaksi>>() {}.type
            val riwayat: MutableList<Transaksi> = gson.fromJson(historyJson, type)
            riwayat.add(Transaksi(tanggal, bulan, tahun, nominal, jenis))
            editor.putString("RIWAYAT", gson.toJson(riwayat))
            editor.apply()

            loadData()
            dialog.dismiss()
            Toast.makeText(this, "Data berhasil disimpan!", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    // Load data riwayat dan saldo
    private fun loadData() {
        val sharedPref = getSharedPreferences("DataTabungan", Context.MODE_PRIVATE)

        // Load riwayat transaksi
        val historyJson = sharedPref.getString("RIWAYAT", "[]") ?: "[]"
        val type = object : TypeToken<MutableList<Transaksi>>() {}.type
        riwayatList.clear()
        riwayatList.addAll(gson.fromJson(historyJson, type))

        // Jika riwayat kosong, reset saldo ke 0
        if (riwayatList.isEmpty()) {
            val editor = sharedPref.edit()
            editor.putInt("TOTAL_SALDO", 0)
            editor.apply()
        }

        // Update tampilan saldo
        val total = sharedPref.getInt("TOTAL_SALDO", 0)
        binding.tvTotalSaldoHome.text = "Rp $total"

        adapter.notifyDataSetChanged()
        binding.tvRiwayatHarian.visibility =
            if (riwayatList.isEmpty()) View.VISIBLE else View.GONE

        setupGrafik()
    }

    // Setup grafik batang
    private fun setupGrafik() {
        val entries = ArrayList<BarEntry>()
        val colors = ArrayList<Int>()
        riwayatList.forEachIndexed { index, transaksi ->
            entries.add(BarEntry((index + 1).toFloat(), transaksi.nominal.toFloat()))
            val color = when (transaksi.jenis) {
                "TABUNGAN" -> Color.GREEN
                "PEMASUKAN" -> Color.BLUE
                "PENGELUARAN" -> Color.RED
                else -> Color.GRAY
            }
            colors.add(color)
        }
        val dataSet = BarDataSet(entries, "Transaksi")
        dataSet.colors = colors
        binding.barChart.data = BarData(dataSet)
        binding.barChart.description.isEnabled = false
        binding.barChart.animateY(1000)
        binding.barChart.invalidate()
    }

    // Dialog Profil dengan username dan tombol keluar
    private fun showProfilDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_profil, null)
        dialog.setContentView(view)

        // Ambil username dari SharedPreferences
        val sharedPref = getSharedPreferences("DataTabungan", Context.MODE_PRIVATE)
        val username = sharedPref.getString("USERNAME", "Pengguna")
        val tvUsername = view.findViewById<TextView>(R.id.tvUsername)
        tvUsername.text = username

        val btnKeluar = view.findViewById<Button>(R.id.btnKeluar)
        btnKeluar.setOnClickListener {
            finishAffinity() // keluar aplikasi
        }

        dialog.show()
    }
}
