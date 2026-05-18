package com.example.bustrack_app.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrack_app.adapter.StudentAdapter
import com.example.bustrack_app.databinding.ActivityManageStudentBinding
import com.example.bustrack_app.models.StudentModel
import com.example.bustrack_app.viewmodels.StudentViewModel

class ManageStudentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageStudentBinding
    private lateinit var viewModel: StudentViewModel
    private lateinit var studentAdapter: StudentAdapter
    private var fullStudentList = listOf<StudentModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[StudentViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupSearch()
        setupFilters()

        binding.btnMenu.setOnClickListener { finish() } // Back
    }

    private fun setupRecyclerView() {
        studentAdapter = StudentAdapter(listOf()) { student ->
            // Logic for Assign Route
        }
        binding.rvStudents.apply {
            layoutManager = LinearLayoutManager(this@ManageStudentActivity)
            adapter = studentAdapter
        }
    }

    private fun setupObservers() {
        viewModel.studentList.observe(this) { list ->
            fullStudentList = list
            studentAdapter.setStudents(list)
            binding.txtTotalCount.text = "${list.size} Students Total"
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterByQuery(s.toString().lowercase().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterByQuery(query: String) {
        val filtered = if (query.isEmpty()) fullStudentList
        else fullStudentList.filter {
            it.name.lowercase().contains(query) || it.id.lowercase().contains(query)
        }
        studentAdapter.setStudents(filtered)
    }

    private fun setupFilters() {
        // Example click for "Unassigned" filter
        binding.btnUnassigned.setOnClickListener {
            // UI Update (Background switch logic humne discuss ki thi)
            val filtered = fullStudentList.filter { it.route.isEmpty() }
            studentAdapter.setStudents(filtered)
        }

        binding.btnAll.setOnClickListener {
            studentAdapter.setStudents(fullStudentList)
        }
    }
}