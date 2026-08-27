package utils

import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Patterns
import android.widget.EditText
import java.util.regex.Pattern

object FormUtils {

    /**
      Forces the input to be uppercase in real-time.
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
                if (isUpdating || s.isNullOrEmpty()) return
                isUpdating = true

                val str = s.toString().replace("-", "")
                if (str.length > 13) {
                    val limited = str.substring(0, 13)
                    val formattedLimited = StringBuilder()
                    for (i in limited.indices) {
                        formattedLimited.append(limited[i])
                        if ((i == 4 || i == 11) && i != limited.length - 1) {
                            formattedLimited.append("-")
                        }
                    }
                    editText.setText(formattedLimited.toString())
                    editText.setSelection(formattedLimited.length)
                    isUpdating = false
                    return
                }

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

    /**
     * Formats Student ID as GCW-XXX
     * First 3 letters are capitalized and a hyphen is inserted after them.
     */
    fun setupStudentIdFormatting(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating || s.isNullOrEmpty()) return
                isUpdating = true

                var str = s.toString().replace("-", "").uppercase()
                val formatted = StringBuilder()

                for (i in str.indices) {
                    if (i < 3) {
                        if (str[i].isLetter()) {
                            formatted.append(str[i])
                        }
                    } else {
                        formatted.append(str[i])
                    }
                    
                    if (i == 2 && str.length > 3) {
                        formatted.append("-")
                    }
                }

                if (s.toString() != formatted.toString()) {
                    val selection = editText.selectionStart
                    val hyphensBefore = s.toString().substring(0, selection.coerceAtMost(s.length)).count { it == '-' }
                    
                    editText.setText(formatted.toString())
                    
                    val hyphensAfter = formatted.toString().substring(0, formatted.length.coerceAtMost(selection)).count { it == '-' }
                    val newSelection = (selection + (hyphensAfter - hyphensBefore)).coerceIn(0, formatted.length)
                    
                    editText.setSelection(formatted.length) // Simplified for now, usually sufficient for auto-formatting
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

    /**
     * Formats a timestamp as a short relative string ("2m ago", "3h ago", "5d ago")
     * for notification feeds. Used across Admin/Driver/Parent/Principal notification
     * screens so they all read the same real Firestore timestamp consistently.
     */
    fun timeAgo(date: java.util.Date?): String {
        if (date == null) return "Just now"
        val diffMs = System.currentTimeMillis() - date.time
        if (diffMs < 0) return "Just now"
        val minutes = diffMs / 60000
        val hours = minutes / 60
        val days = hours / 24
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(date)
        }
    }
}
