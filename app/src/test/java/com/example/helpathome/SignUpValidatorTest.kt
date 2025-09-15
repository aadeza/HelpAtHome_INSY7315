package com.example.helpathome

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SignUpValidatorTest {

    private lateinit var validator: SignUpValidator

    @Before
    fun setup() {
        validator = SignUpValidator()
    }

    @Test
    fun `should return error if any field is empty`() {
        val result = validator.validate("", "Doe", "01-01-2000", "test@test.com", "123456", "123456")
        assertEquals("Please fill in all fields.", result)
    }

    @Test
    fun `should return error for invalid email`() {
        val result = validator.validate("John", "Doe", "01-01-2000", "invalidEmail", "123456", "123456")
        assertEquals("Enter a valid email", result)
    }

    @Test
    fun `should return error for short password`() {
        val result = validator.validate("John", "Doe", "01-01-2000", "test@test.com", "123", "123")
        assertEquals("Password must be at least 6 characters", result)
    }

    @Test
    fun `should return error if passwords do not match`() {
        val result = validator.validate("John", "Doe", "01-01-2000", "test@test.com", "123456", "654321")
        assertEquals("Passwords do not match", result)
    }

    @Test
    fun `should return null when all inputs are valid`() {
        val result = validator.validate("John", "Doe", "01-01-2000", "test@test.com", "123456", "123456")
        assertNull(result)
    }
}
