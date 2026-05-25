package ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrack_app.adapter.RouteAdapter
import com.example.bustrack_app.databinding.ActivityManageRouteBinding
import com.example.bustrack_app.viewmodels.RouteViewModel

class ManageRouteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageRouteBinding
    private val viewModel: RouteViewModel by viewModels()
    private lateinit var routeAdapter: RouteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        utils.NavigationUtils.setupBottomNavigation(this)

        setupRecyclerView()
        setupObservers()
        setupSearchEngine()
        setupActionListeners()
    }

    private fun setupRecyclerView() {
        routeAdapter = RouteAdapter(listOf()) { selectedRoute ->
            val intent = Intent(this, com.example.bustrack_app.ui.admin.RouteDetailActivity::class.java)
            intent.putExtra("ROUTE_ID", selectedRoute.id)
            startActivity(intent)
        }
        binding.rvRoutes.apply {
            layoutManager = LinearLayoutManager(this@ManageRouteActivity)
            adapter = routeAdapter
            isNestedScrollingEnabled = false // NestedScrollView ke andar scrolling crash block resolution
        }
    }

    private fun setupObservers() {
        viewModel.routeList.observe(this) { list ->
            routeAdapter.updateData(list)
        }
    }

    private fun setupSearchEngine() {
        binding.etSearchRoute.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.filterRoutes(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupActionListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.fabAddRoute.setOnClickListener {
            Toast.makeText(this, "Redirecting to Add New Route Form...", Toast.LENGTH_SHORT).show()
        }
    }
}