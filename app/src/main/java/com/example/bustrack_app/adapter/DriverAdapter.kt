package com.example.bustrack_app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.databinding.ItemDriverCardBinding
import com.example.bustrack_app.models.DriverModel

class DriverAdapter(
    private var drivers: List<DriverModel> = listOf(),
    private val onEditClick: (DriverModel) -> Unit
) : RecyclerView.Adapter<DriverAdapter.DriverViewHolder>() {

    fun setDrivers(newList: List<DriverModel>) {
        this.drivers = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DriverViewHolder {
        val itemBinding = ItemDriverCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DriverViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: DriverViewHolder, position: Int) = holder.bind(drivers[position])
    override fun getItemCount(): Int = drivers.size

    inner class DriverViewHolder(private val itemBinding: ItemDriverCardBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(driver: DriverModel) {
            itemBinding.txtDriverName.text = driver.name
            itemBinding.txtBusInfo.text = "Bus: ${driver.assignedBus}"
            itemBinding.txtRouteInfo.text = "Route: ${driver.route}"

            if (driver.profileImage == 0) {
                itemBinding.imgDriver.visibility = View.GONE
                itemBinding.txtAvatar.visibility = View.VISIBLE

                // Initials Logic
                val initials = driver.name.split(" ")
                    .filter { it.isNotEmpty() }
                    .map { it[0] }
                    .take(2)
                    .joinToString("")
                itemBinding.txtAvatar.text = initials.uppercase()
            } else {
                itemBinding.imgDriver.visibility = View.VISIBLE
                itemBinding.txtAvatar.visibility = View.GONE
                itemBinding.imgDriver.setImageResource(driver.profileImage)
            }

            itemBinding.btnViewProfile.setOnClickListener { onEditClick(driver) }
        }
    }
}