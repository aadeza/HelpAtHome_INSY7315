package com.example.helpathome

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LoginValidatorTest {

    private lateinit var validator: LoginValidator

    @Before
    fun setup() {
        validator = LoginValidator()
    }

    @Test
    fun `should return error for invalid email`() {
        val result = validator.validate("invalidEmail", "123456")
        assertEquals("Enter a valid email", result)
    }

    @Test
    fun `should return error if password is empty`() {
        val result = validator.validate("test@test.com", "")
        assertEquals("Enter your password", result)
    }

    @Test
    fun `should return null when email and password are valid`() {
        val result = validator.validate("test@test.com", "123456")
        assertNull(result)
    }
}
