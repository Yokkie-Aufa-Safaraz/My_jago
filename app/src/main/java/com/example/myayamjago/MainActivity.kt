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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Transaksi(
    val tanggal: Int,
    val bulan: Int,
    val tahun: Int,
    val nominal: Int,
    val jenis: String
)

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: DatabaseHelper
    private val gson = Gson()
    private val riwayatList = mutableListOf<Transaksi>()
    private lateinit var adapter: TransaksiAdapter
    private var modeHalaman = "TABUNGAN"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)
        adapter = TransaksiAdapter(riwayatList, this) { loadData() }
        binding.rvRiwayat.layoutManager = LinearLayoutManager(this)
        binding.rvRiwayat.adapter = adapter

        loadData()

        binding.fabTambah.setOnClickListener { showTambahDialog() }

        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_tabungan -> { modeHalaman = "TABUNGAN"; loadData(); true }
                R.id.nav_pemasukan -> { modeHalaman = "PEMASUKAN"; loadData(); true }
                R.id.nav_tambah -> { showTambahDialog(); true }
                R.id.nav_profil -> { modeHalaman = "PROFIL"; loadData(); true }
                else -> false
            }
        }
    }

    private fun loadData() {
        val sharedPref = getSharedPreferences("DataTabungan", Context.MODE_PRIVATE)

        // Reset semua view default
        binding.rvRiwayat.visibility = View.VISIBLE
        binding.barChart.visibility = View.VISIBLE
        binding.tvTotalSaldoHome.visibility = View.VISIBLE
        binding.fabTambah.visibility = View.VISIBLE

        when (modeHalaman) {
            "TABUNGAN" -> {
                riwayatList.clear()
                riwayatList.addAll(getRiwayat("RIWAYAT_TABUNGAN"))
                binding.tvTotalSaldoHome.text = "Saldo Tabungan: Rp ${sharedPref.getInt("TOTAL_TABUNGAN",0)}"
            }
            "PEMASUKAN" -> {
                riwayatList.clear()
                riwayatList.addAll(getRiwayat("RIWAYAT_PEMASUKAN"))
                riwayatList.addAll(getRiwayat("RIWAYAT_PENGELUARAN"))
                binding.tvTotalSaldoHome.text = "Saldo Pemasukan: Rp ${sharedPref.getInt("TOTAL_PEMASUKAN",0)}"
            }
            "PENGELUARAN" -> {
                riwayatList.clear()
                riwayatList.addAll(getRiwayat("RIWAYAT_PENGELUARAN"))
                binding.tvTotalSaldoHome.text = "Riwayat Pengeluaran"
            }
            "PROFIL" -> {
                // Hide semua view transaksi
                binding.rvRiwayat.visibility = View.GONE
                binding.barChart.visibility = View.GONE
                binding.tvTotalSaldoHome.visibility = View.GONE
                binding.fabTambah.visibility = View.GONE

                // Inflasi layout profil ke dalam root
                val profilLayout = layoutInflater.inflate(R.layout.layout_profil, null)
                profilLayout.layoutParams = binding.root.layoutParams

                binding.root.removeAllViews()
                binding.root.addView(profilLayout)

                // Ambil data akun dari SQLite
                val akun = db.getUser()

                profilLayout.findViewById<TextView>(R.id.tvNamaProfil).text = akun?.first ?: "Tidak ada akun"
                profilLayout.findViewById<TextView>(R.id.tvEmailProfil).text = akun?.second ?: "-"

                // Tombol reset data
                profilLayout.findViewById<Button>(R.id.btnResetData).setOnClickListener {
                    resetData()
                    modeHalaman = "TABUNGAN"
                    loadData()
                }

                // Tombol keluar
                profilLayout.findViewById<LinearLayout>(R.id.menuKeluar).setOnClickListener {
                    finishAffinity()
                }

                return
            }
        }

        adapter.notifyDataSetChanged()
        setupGrafik()
    }

    private fun getRiwayat(key: String): MutableList<Transaksi> {
        val sharedPref = getSharedPreferences("DataTabungan", Context.MODE_PRIVATE)
        val json = sharedPref.getString(key, "[]")
        val type = object : TypeToken<MutableList<Transaksi>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun setupGrafik() {
        if (modeHalaman == "PEMASUKAN") {
            val entriesPemasukan = ArrayList<BarEntry>()
            val entriesPengeluaran = ArrayList<BarEntry>()

            getRiwayat("RIWAYAT_PEMASUKAN").forEachIndexed { idx, t ->
                entriesPemasukan.add(BarEntry((idx + 1).toFloat(), t.nominal.toFloat()))
            }
            getRiwayat("RIWAYAT_PENGELUARAN").forEachIndexed { idx, t ->
                entriesPengeluaran.add(BarEntry((idx + 1).toFloat(), t.nominal.toFloat()))
            }

            val ds1 = BarDataSet(entriesPemasukan, "Pemasukan")
            ds1.color = Color.BLUE
            val ds2 = BarDataSet(entriesPengeluaran, "Pengeluaran")
            ds2.color = Color.RED

            binding.barChart.data = BarData(ds1, ds2)
        } else if (modeHalaman != "PROFIL") {
            val entries = ArrayList<BarEntry>()
            riwayatList.forEachIndexed { idx, t ->
                entries.add(BarEntry((idx + 1).toFloat(), t.nominal.toFloat()))
            }
            val ds = BarDataSet(entries, modeHalaman)
            ds.color = if (modeHalaman == "TABUNGAN") Color.GREEN else Color.GRAY
            binding.barChart.data = BarData(ds)
        }

        binding.barChart.description.isEnabled = false
        binding.barChart.invalidate()
    }

    private fun showTambahDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_input_tabungan, null)
        dialog.setContentView(view)

        val etTanggal = view.findViewById<EditText>(R.id.etTanggal)
        val etBulan = view.findViewById<EditText>(R.id.etBulan)
        val etTahun = view.findViewById<EditText>(R.id.etTahun)
        val etNominal = view.findViewById<EditText>(R.id.etNominal)
        val spJenis = view.findViewById<Spinner>(R.id.spinnerJenis)
        val btnSimpan = view.findViewById<Button>(R.id.btnSimpan)

        val jenisList = arrayOf("TABUNGAN", "PEMASUKAN", "PENGELUARAN")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_item, jenisList)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spJenis.adapter = adapterSpinner

        btnSimpan.setOnClickListener {
            if (etTanggal.text.isEmpty() || etBulan.text.isEmpty() || etTahun.text.isEmpty() || etNominal.text.isEmpty()) {
                Toast.makeText(this, "Lengkapi data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nominal = etNominal.text.toString().toInt()
            val jenis = spJenis.selectedItem.toString()
            val sharedPref = getSharedPreferences("DataTabungan", Context.MODE_PRIVATE)

            when (jenis) {
                "TABUNGAN" -> {
                    val total = sharedPref.getInt("TOTAL_TABUNGAN", 0) + nominal
                    val list = getRiwayat("RIWAYAT_TABUNGAN")
                    list.add(Transaksi(
                        etTanggal.text.toString().toInt(),
                        etBulan.text.toString().toInt(),
                        etTahun.text.toString().toInt(),
                        nominal,
                        "TABUNGAN"
                    ))
                    sharedPref.edit()
                        .putInt("TOTAL_TABUNGAN", total)
                        .putString("RIWAYAT_TABUNGAN", gson.toJson(list))
                        .apply()
                }
                "PEMASUKAN" -> {
                    val total = sharedPref.getInt("TOTAL_PEMASUKAN", 0) + nominal
                    val list = getRiwayat("RIWAYAT_PEMASUKAN")
                    list.add(Transaksi(
                        etTanggal.text.toString().toInt(),
                        etBulan.text.toString().toInt(),
                        etTahun.text.toString().toInt(),
                        nominal,
                        "PEMASUKAN"
                    ))
                    sharedPref.edit()
                        .putInt("TOTAL_PEMASUKAN", total)
                        .putString("RIWAYAT_PEMASUKAN", gson.toJson(list))
                        .apply()
                }
                "PENGELUARAN" -> {
                    val total = sharedPref.getInt("TOTAL_PEMASUKAN", 0) - nominal
                    if (total < 0) {
                        Toast.makeText(this, "Saldo pemasukan tidak cukup", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    val list = getRiwayat("RIWAYAT_PENGELUARAN")
                    list.add(Transaksi(
                        etTanggal.text.toString().toInt(),
                        etBulan.text.toString().toInt(),
                        etTahun.text.toString().toInt(),
                        nominal,
                        "PENGELUARAN"
                    ))
                    sharedPref.edit()
                        .putInt("TOTAL_PEMASUKAN", total)
                        .putString("RIWAYAT_PENGELUARAN", gson.toJson(list))
                        .apply()
                }
            }

            loadData()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun resetData() {
        val sharedPref = getSharedPreferences("DataTabungan", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        db.hapusSemuaAkun()
        Toast.makeText(this, "Semua data berhasil dihapus", Toast.LENGTH_SHORT).show()
        modeHalaman = "TABUNGAN"
        loadData()
    }
}
