package com.example.myayamjago

import android.content.Context
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

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvJenis: TextView = view.findViewById(R.id.tvJenis)
        val tvNominal: TextView = view.findViewById(R.id.tvNominal)
        val btnHapus: Button = view.findViewById(R.id.btnHapus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_transaksi, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transaksi = list[position]
        holder.tvTanggal.text = "${transaksi.tanggal}/${transaksi.bulan}/${transaksi.tahun}"
        holder.tvJenis.text = transaksi.jenis
        holder.tvNominal.text = "Rp ${transaksi.nominal}"

        holder.btnHapus.setOnClickListener {
            // Hapus dari SharedPreferences
            val sharedPref = context.getSharedPreferences("DataTabungan", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            val historyJson = sharedPref.getString("RIWAYAT", "[]") ?: "[]"
            val type = object : TypeToken<MutableList<Transaksi>>() {}.type
            val riwayat: MutableList<Transaksi> = gson.fromJson(historyJson, type)

            // Hapus item sesuai posisi
            val removed = riwayat.removeAt(holder.adapterPosition)

            // Update saldo
            var totalSaldo = sharedPref.getInt("TOTAL_SALDO", 0)
            totalSaldo = when (removed.jenis) {
                "TABUNGAN", "PEMASUKAN" -> totalSaldo - removed.nominal
                "PENGELUARAN" -> totalSaldo + removed.nominal
                else -> totalSaldo
            }

            editor.putInt("TOTAL_SALDO", totalSaldo)
            editor.putString("RIWAYAT", gson.toJson(riwayat))
            editor.apply()

            // Update list dan tampilan
            list.removeAt(holder.adapterPosition)
            notifyItemRemoved(holder.adapterPosition)
            onDataChanged()
        }
    }
}
