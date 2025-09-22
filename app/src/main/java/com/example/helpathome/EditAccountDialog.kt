package com.example.helpathome

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class EditAccountDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_edit_account, null)

        val currentPassword = view.findViewById<EditText>(R.id.etCurrentPassword)
        val newName = view.findViewById<EditText>(R.id.etNewName)
        val newSurname = view.findViewById<EditText>(R.id.etNewSurname)
        val newEmail = view.findViewById<EditText>(R.id.etNewEmail)
        val newPassword = view.findViewById<EditText>(R.id.etNewPassword)

        builder.setView(view)
            .setTitle("Edit Account")
            .setPositiveButton("Save") { _, _ ->
                val currentPass = currentPassword.text.toString().trim()
                val name = newName.text.toString().trim()
                val surname = newSurname.text.toString().trim()
                val email = newEmail.text.toString().trim()
                val password = newPassword.text.toString().trim()

                if (currentPass.isNotEmpty()) {
                    (activity as? OnEditAccountListener)?.onSaveClicked(
                        currentPass, name, surname, email, password
                    )

                    ActivityLogger.log(
                        actorId = email,
                        actorType = "Civilian",
                        category = "Credentials Change",
                        message = "User $name $surname changed their account credentials",
                        color = "#FF5900"
                    )



                } else {
                    Toast.makeText(context, "Enter current password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        return builder.create()
    }

    interface OnEditAccountListener {
        fun onSaveClicked(currentPass: String, newName: String, newSurname: String, newEmail: String, newPassword: String)
    }
}
