package com.example.helpathome


class SignUpValidator {


    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

    fun validate(
        firstName: String,
        lastName: String,
        dob: String,
        email: String,
        password: String,
        confirmPassword: String
    ): String? {
        if (firstName.isEmpty() || lastName.isEmpty() || dob.isEmpty() ||
            email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()
        ) {
            return "Please fill in all fields."
        }

        if (!email.matches(emailRegex)) {
            return "Enter a valid email"
        }

        if (password.length < 6) {
            return "Password must be at least 6 characters"
        }

        if (password != confirmPassword) {
            return "Passwords do not match"
        }

        return null
    }
}
