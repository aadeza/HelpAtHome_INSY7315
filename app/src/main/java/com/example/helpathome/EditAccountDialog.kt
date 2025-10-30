package com.example.helpathome

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputLayout

class EditAccountDialog(
    private val currentName: String,
    private val currentSurname: String,
    private val currentEmail: String,
    private val currentPhone: String,
    private val dob: String // Date of birth for password validation
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_account, null)

        val currentPassword = view.findViewById<EditText>(R.id.etCurrentPassword)
        val newName = view.findViewById<EditText>(R.id.etNewName)
        val newSurname = view.findViewById<EditText>(R.id.etNewSurname)
        val newPhone = view.findViewById<EditText>(R.id.etNewPhone)
        val newEmail = view.findViewById<EditText>(R.id.etNewEmail)
        val newPassword = view.findViewById<EditText>(R.id.etNewPassword)

        val emailLayout = view.findViewById<TextInputLayout>(R.id.layoutNewEmail)
        val passwordLayout = view.findViewById<TextInputLayout>(R.id.layoutNewPassword)

        // Prepopulate existing info
        newName.setText(currentName)
        newSurname.setText(currentSurname)
        newPhone.setText(currentPhone)
        newEmail.setText(currentEmail)

        // Email validation
        newEmail.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString().trim()
                emailLayout.error = if (email.isNotEmpty() && !isValidEmail(email)) "Invalid email format" else null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Password validation
        newPassword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString().trim()
                passwordLayout.error = if (password.isNotEmpty() && !isValidPassword(password, newName.text.toString(), newSurname.text.toString(), dob, newEmail.text.toString()))
                    "Weak password or contains personal info" else null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        builder.setView(view)
            .setTitle("Edit Account")
            .setPositiveButton("Save") { _, _ ->
                val currentPass = currentPassword.text.toString().trim()
                val name = newName.text.toString().trim()
                val surname = newSurname.text.toString().trim()
                val phone = newPhone.text.toString().trim()
                val email = newEmail.text.toString().trim()
                val password = newPassword.text.toString().trim()

                if (currentPass.isEmpty()) {
                    Toast.makeText(context, "Enter current password", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (email.isNotEmpty() && !isValidEmail(email)) {
                    Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (password.isNotEmpty() && !isValidPassword(password, name, surname, dob, email)) {
                    Toast.makeText(context, "Password too weak or contains personal info", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                (activity as? OnEditAccountListener)?.onSaveClicked(
                    currentPass, name, surname, phone, email, if (password.isEmpty()) null else password
                )
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        return builder.create()
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String, firstName: String, lastName: String, dob: String, email: String): Boolean {
        if (password.length < 8) return false

        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        if (!(hasUpper && hasLower && hasDigit && hasSpecial)) return false

        val lowerPassword = password.lowercase()
        val emailPrefix = email.substringBefore("@").lowercase()
        if (lowerPassword.contains(firstName.lowercase()) || lowerPassword.contains(lastName.lowercase()) || lowerPassword.contains(emailPrefix)) return false

        val dobDigits = dob.replace(Regex("\\D+"), "")
        if (dobDigits.length >= 6 && lowerPassword.contains(dobDigits)) return false

        return true
    }

    interface OnEditAccountListener {
        fun onSaveClicked(currentPass: String, newName: String, newSurname: String, newPhone: String, newEmail: String, newPassword: String?)
    }
}
