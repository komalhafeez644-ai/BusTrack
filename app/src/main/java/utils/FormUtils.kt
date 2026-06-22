package utils

import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Patterns
import android.widget.EditText
import java.util.regex.Pattern

object FormUtils {

    /**
     * Forces the input to be uppercase in real-time.
     */
    fun setupUppercaseInput(editText: EditText) {
        editText.filters = arrayOf(InputFilter.AllCaps())
    }

    /**
     * Capitalizes the first letter of each word in real-time.
     */
    fun setupTitleCaseInput(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                isUpdating = true

                val originalText = s.toString()
                val words = originalText.split(" ")
                val capitalizedText = words.joinToString(" ") { word ->
                    if (word.isNotEmpty()) {
                        word[0].uppercaseChar() + word.substring(1).lowercase()
                    } else {
                        ""
                    }
                }

                if (originalText != capitalizedText) {
                    val selectionStart = editText.selectionStart
                    val selectionEnd = editText.selectionEnd
                    editText.setText(capitalizedText)
                    editText.setSelection(selectionStart.coerceAtMost(capitalizedText.length), selectionEnd.coerceAtMost(capitalizedText.length))
                }

                isUpdating = false
            }
        })
    }

    /**
     * Formats CNIC as 00000-0000000-0
     */
    fun setupCnicFormatting(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return
                isUpdating = true

                val str = s.toString().replace("-", "")
                val formatted = StringBuilder()

                for (i in str.indices) {
                    formatted.append(str[i])
                    if ((i == 4 || i == 11) && i != str.length - 1) {
                        formatted.append("-")
                    }
                }

                if (s.toString() != formatted.toString()) {
                    editText.setText(formatted.toString())
                    editText.setSelection(formatted.length)
                }

                isUpdating = false
            }
        })
    }

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPhone(phone: String): Boolean {
        return phone.length == 11 && phone.startsWith("03")
    }

    /**
     * Password must be at least 8 characters and contain both letters and numbers.
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }
}
