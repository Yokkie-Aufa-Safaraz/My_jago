package com.example.myayamjago

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TransaksiAdapter(
    private val list: MutableList<Transaksi>,
    private val context: Context,
    private val onDataChanged: () -> Unit
) : RecyclerView.Adapter<TransaksiAdapter.ViewHolder>() {

    private val gson = Gson()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaksi, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = list[position]
        holder.tvTanggal.text = "${t.tanggal}/${t.bulan}/${t.tahun}"
        holder.tvJenis.text = t.jenis
        holder.tvNominal.text = "Rp ${t.nominal}"

        holder.btnHapus.setOnClickListener {
            hapusTransaksi(t)
        }
    }

    private fun hapusTransaksi(transaksi: Transaksi) {
        val sharedPref = context.getSharedPreferences("DataTabungan", Context.MODE_PRIVATE)

        val keyRiwayat = when (transaksi.jenis) {
            "TABUNGAN" -> "RIWAYAT_TABUNGAN"
            "PEMASUKAN" -> "RIWAYAT_PEMASUKAN"
            "PENGELUARAN" -> "RIWAYAT_PENGELUARAN"
            else -> ""
        }

        val keySaldo = when (transaksi.jenis) {
            "TABUNGAN" -> "TOTAL_TABUNGAN"
            "PEMASUKAN" -> "TOTAL_PEMASUKAN"
            "PENGELUARAN" -> "TOTAL_PEMASUKAN" // pengeluaran mengurangi saldo pemasukan
            else -> ""
        }

        val historyJson = sharedPref.getString(keyRiwayat, "[]")
        val type = object : TypeToken<MutableList<Transaksi>>() {}.type
        val listRiwayat: MutableList<Transaksi> = gson.fromJson(historyJson, type)

        // Hapus transaksi dari list
        listRiwayat.removeIf {
            it.tanggal == transaksi.tanggal &&
                    it.bulan == transaksi.bulan &&
                    it.tahun == transaksi.tahun &&
                    it.nominal == transaksi.nominal &&
                    it.jenis == transaksi.jenis
        }

        // Update saldo
        var saldo = sharedPref.getInt(keySaldo, 0)
        saldo = if (transaksi.jenis == "PENGELUARAN") saldo + transaksi.nominal else saldo - transaksi.nominal
        if (saldo < 0) saldo = 0

        sharedPref.edit()
            .putString(keyRiwayat, gson.toJson(listRiwayat))
            .putInt(keySaldo, saldo)
            .apply()

        // Update adapter
        list.remove(transaksi)
        notifyDataSetChanged()
        onDataChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
        val tvJenis: TextView = itemView.findViewById(R.id.tvJenis)
        val tvNominal: TextView = itemView.findViewById(R.id.tvNominal)
        val btnHapus: Button = itemView.findViewById(R.id.btnHapus)
    }
}
