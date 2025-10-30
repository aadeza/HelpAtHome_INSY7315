package com.example.helpathome

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputLayout


class OfficerAccountDialog(
    private val currentName: String,
    private val currentSurname: String,
    private val currentEmail: String,
    private val currentPhone: String,
    private val dob: String
) : DialogFragment() {

    interface OnOfficerAccountUpdated {
        fun onOfficerAccountSaved(
            currentPassword: String,
            newName: String,
            newSurname: String,
            newPhone: String,
            newPassword: String?
        )
    }

    private lateinit var listener: OnOfficerAccountUpdated

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? OnOfficerAccountUpdated
            ?: throw ClassCastException("$context must implement OnOfficerAccountUpdated")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_officer_account, null)

        val edtName = view.findViewById<EditText>(R.id.edtName)
        val edtSurname = view.findViewById<EditText>(R.id.edtSurname)
        val edtPhone = view.findViewById<EditText>(R.id.edtPhone)
        val txtEmail = view.findViewById<TextView>(R.id.txtEmail)
        val txtDob = view.findViewById<TextView>(R.id.txtDob)
        val edtCurrentPassword = view.findViewById<EditText>(R.id.edtCurrentPassword)
        val edtNewPassword = view.findViewById<EditText>(R.id.edtNewPassword)

        // Pre-fill fields with current data
        edtName.setText(currentName)
        edtSurname.setText(currentSurname)
        edtPhone.setText(currentPhone)
        txtEmail.text = currentEmail
        txtDob.text = "DOB: $dob"

        return AlertDialog.Builder(requireContext())
            .setTitle("Edit Officer Account")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val currentPass = edtCurrentPassword.text.toString()
                val newName = edtName.text.toString()
                val newSurname = edtSurname.text.toString()
                val newPhone = edtPhone.text.toString()
                val newPassword = edtNewPassword.text.toString().takeIf { it.isNotEmpty() }

                listener.onOfficerAccountSaved(currentPass, newName, newSurname, newPhone, newPassword)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}