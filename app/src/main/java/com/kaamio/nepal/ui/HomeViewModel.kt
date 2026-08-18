package com.kaamio.nepal.ui

import androidx.lifecycle.viewModelScope
import com.kaamio.nepal.data.CourseDao
import com.kaamio.nepal.data.UserProfile
import com.kaamio.nepal.data.UserProfileDao
import com.kaamio.nepal.data.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val userProfileDao: UserProfileDao,
    private val courseDao: CourseDao,
    private val connectivityObserver: com.kaamio.nepal.data.ConnectivityObserver
) : BaseViewModel() {

    private val _currentGateway = MutableStateFlow("work")
    val currentGateway = _currentGateway.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Loading)
    val currentScreen = _currentScreen.asStateFlow()

    private val _pendingChatPartnerId = MutableStateFlow<String?>(null)
    val pendingChatPartnerId = _pendingChatPartnerId.asStateFlow()
    private val _pendingChatPartnerName = MutableStateFlow("")
    val pendingChatPartnerName = _pendingChatPartnerName.asStateFlow()
    private val _pendingChatPartnerAvatar = MutableStateFlow("")
    val pendingChatPartnerAvatar = _pendingChatPartnerAvatar.asStateFlow()

    private val _pendingPaymentJobId = MutableStateFlow<String?>(null)
    val pendingPaymentJobId = _pendingPaymentJobId.asStateFlow()
    private val _pendingPaymentWorkerId = MutableStateFlow("")
    val pendingPaymentWorkerId = _pendingPaymentWorkerId.asStateFlow()
    private val _pendingPaymentWorkerName = MutableStateFlow("")
    val pendingPaymentWorkerName = _pendingPaymentWorkerName.asStateFlow()
    private val _pendingPaymentAmount = MutableStateFlow(0.0)
    val pendingPaymentAmount = _pendingPaymentAmount.asStateFlow()

    val userProfile: StateFlow<UserProfile> = userRepository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    init {
        connectivityObserver.start()

        viewModelScope.launch {
            try { initialize() } catch (_: Exception) {
                _currentScreen.value = Screen.Error("Initialization failed")
                _isReady.value = true
            }
        }
        
        // Improved Reactive Auth Observer - Fixes Sign-In/Out Redirection
        viewModelScope.launch {
            userProfile.collectLatest { profile ->
                if (!_isReady.value) return@collectLatest
                
                if (!profile.isLoggedIn) {
                    if (_currentScreen.value != Screen.Onboarding) {
                        _currentScreen.value = Screen.Onboarding
                    }
                } else if (profile.profileCompleted) {
                    if (_currentScreen.value == Screen.Onboarding || _currentScreen.value == Screen.Loading) {
                        _currentScreen.value = Screen.Home
                    }
                }
            }
        }
    }

    private suspend fun initialize() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingCourses = courseDao.getAllCoursesSync()
                if (existingCourses.isEmpty() && userProfileDao.getUserProfileSync() == null) {
                    userProfileDao.insertProfile(UserProfile())
                }
            } catch (_: Exception) {}
        }

        // Sync profile from Firestore if user is already logged in
        viewModelScope.launch {
            try {
                userRepository.getCurrentUser()
                userRepository.syncFcmTokenIfNeeded()
            } catch (_: Exception) {}
        }

        val screen = withContext(Dispatchers.IO) {
            val cachedProfile = userProfileDao.getUserProfileSync()
            if (cachedProfile != null && cachedProfile.isLoggedIn) {
                if (cachedProfile.profileCompleted) Screen.Home else Screen.Onboarding
            } else {
                Screen.Onboarding
            }
        }
        _currentScreen.value = screen
        _isReady.value = true
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun toggleGateway() {
        val next = if (_currentGateway.value == "work") "education" else "work"
        _currentGateway.value = next
    }

    fun startNewChat(partnerId: String, partnerName: String, partnerAvatar: String = "") {
        _pendingChatPartnerId.value = partnerId
        _pendingChatPartnerName.value = partnerName
        _pendingChatPartnerAvatar.value = partnerAvatar
        _currentScreen.value = Screen.Negotiation
    }

    fun clearPendingChatPartner() {
        _pendingChatPartnerId.value = null
        _pendingChatPartnerName.value = ""
        _pendingChatPartnerAvatar.value = ""
    }

    fun startEscrow(jobId: String, workerId: String, workerName: String, amount: Double) {
        _pendingPaymentJobId.value = jobId
        _pendingPaymentWorkerId.value = workerId
        _pendingPaymentWorkerName.value = workerName
        _pendingPaymentAmount.value = amount
        _currentScreen.value = Screen.Payment
    }

    fun clearPendingPayment() {
        _pendingPaymentJobId.value = null
        _pendingPaymentWorkerId.value = ""
        _pendingPaymentWorkerName.value = ""
        _pendingPaymentAmount.value = 0.0
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }

    fun retryInit() {
        viewModelScope.launch {
            _currentScreen.value = Screen.Loading
            try { initialize() } catch (_: Exception) {
                _currentScreen.value = Screen.Error("Retry failed")
            }
        }
    }
}
