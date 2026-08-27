package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.models.StudentModel

class StudentViewModel : ViewModel() {
    val studentList: LiveData<List<StudentModel>> = StudentRepository.studentList

    fun addStudent(student: StudentModel) {
        StudentRepository.addStudent(student)
    }

    fun deleteStudent(studentId: String) {
        StudentRepository.deleteStudent(studentId)
    }

    fun updateStudent(student: StudentModel) {
        StudentRepository.updateStudent(student)
    }
}
