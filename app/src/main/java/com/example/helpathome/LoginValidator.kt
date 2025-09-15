package com.example.helpathome


class LoginValidator {

    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

    fun validate(email: String, password: String): String? {
        if (!email.matches(emailRegex)) {
            return "Enter a valid email"
        }
        if (password.isEmpty()) {
            return "Enter your password"
        }
        return null
    }
}
