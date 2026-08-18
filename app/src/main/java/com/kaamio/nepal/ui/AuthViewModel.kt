package com.kaamio.nepal.ui

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.kaamio.nepal.data.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: IUserRepository
) : BaseViewModel() {

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading = _isAuthLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    private val _authSuccess = MutableStateFlow<String?>(null)
    val authSuccess = _authSuccess.asStateFlow()

    private val _verificationId = MutableStateFlow<String?>(null)
    val verificationId = _verificationId.asStateFlow()

    private val _resendToken = MutableStateFlow<PhoneAuthProvider.ForceResendingToken?>(null)
    val resendToken = _resendToken.asStateFlow()

    private val _pendingEmailVerification = MutableStateFlow(false)
    val pendingEmailVerification = _pendingEmailVerification.asStateFlow()

    private val _isEmailVerified = MutableStateFlow(false)
    val isEmailVerified = _isEmailVerified.asStateFlow()

    fun loginWithGoogle(credential: AuthCredential) {
        viewModelScope.launch {
            _isAuthLoading.value = true; _authError.value = null
            try { userRepository.signInWithGoogle(credential) }
            catch (e: Exception) { _authError.value = e.localizedMessage ?: "Google Sign-In failed" }
            finally { _isAuthLoading.value = false }
        }
    }

    fun loginWithPhone(credential: AuthCredential) {
        viewModelScope.launch {
            _isAuthLoading.value = true; _authError.value = null
            try { userRepository.signInWithPhoneCredential(credential) }
            catch (e: Exception) { _authError.value = e.localizedMessage ?: "Phone sign-in failed" }
            finally { _isAuthLoading.value = false }
        }
    }

    fun loginWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true; _authError.value = null
            try { userRepository.signInWithEmail(email, pass) }
            catch (e: Exception) { _authError.value = e.localizedMessage ?: "Invalid email or password" }
            finally { _isAuthLoading.value = false }
        }
    }

    fun signUpWithEmail(email: String, pass: String, name: String, agreedToTerms: Boolean) {
        viewModelScope.launch {
            _isAuthLoading.value = true; _authError.value = null
            try { 
                userRepository.signUpWithEmail(email, pass, name, agreedToTerms) 
                _pendingEmailVerification.value = true
                userRepository.sendEmailVerification()
            }
            catch (e: Exception) { _authError.value = e.localizedMessage ?: "Sign up failed" }
            finally { _isAuthLoading.value = false }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            _isAuthLoading.value = true; _authError.value = null
            try {
                userRepository.sendEmailVerification()
                _authSuccess.value = "Verification email sent. Please check your inbox."
            } catch (e: Exception) { _authError.value = e.localizedMessage ?: "Could not resend verification email" }
            finally { _isAuthLoading.value = false }
        }
    }

    fun checkEmailVerified(onVerified: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authError.value = null
            try {
                val verified = userRepository.reloadAndCheckEmailVerified()
                _isEmailVerified.value = verified
                if (verified) _pendingEmailVerification.value = false
                onVerified(verified)
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: "Could not verify email"
                onVerified(false)
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _isAuthLoading.value = true; _authError.value = null; _authSuccess.value = null
            try {
                userRepository.sendPasswordReset(email)
                _authSuccess.value = "Password reset link sent to $email"
            } catch (e: Exception) { _authError.value = e.localizedMessage ?: "Failed to send reset email" }
            finally { _isAuthLoading.value = false }
        }
    }

    fun setPhoneVerificationInfo(vId: String, token: PhoneAuthProvider.ForceResendingToken?) {
        _verificationId.value = vId; _resendToken.value = token
    }

    fun verifyOtp(otp: String) {
        val vId = _verificationId.value ?: return
        val normalizedOtp = otp.trim()
        if (!normalizedOtp.matches(Regex("^[0-9]{6}$"))) { _authError.value = "Enter a valid 6-digit code"; return }
        val credential = PhoneAuthProvider.getCredential(vId, normalizedOtp)
        viewModelScope.launch {
            _isAuthLoading.value = true; _authError.value = null
            try { userRepository.signInWithPhoneCredential(credential) }
            catch (e: Exception) { _authError.value = e.localizedMessage ?: "Invalid verification code" }
            finally { _isAuthLoading.value = false }
        }
    }

    fun verifyPhoneNumber(phoneNumber: String, activity: android.app.Activity, callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks) {
        val formattedPhone = if (phoneNumber.startsWith("+")) phoneNumber else "+977$phoneNumber"
        userRepository.verifyPhoneNumber(formattedPhone, activity, callbacks)
    }

    fun resendOtp(phoneNumber: String, activity: android.app.Activity, callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks) {
        val token = _resendToken.value ?: return
        val formattedPhone = if (phoneNumber.startsWith("+")) phoneNumber else "+977$phoneNumber"
        userRepository.verifyPhoneNumber(formattedPhone, activity, callbacks, token)
    }

    fun clearAuthError() { _authError.value = null }
    fun clearAuthSuccess() { _authSuccess.value = null }
    fun setAuthError(error: String) { _authError.value = error }

    fun logout() {
        viewModelScope.launch {
            try {
                userRepository.logout()
            } catch (_: Exception) {}
        }
    }
}
