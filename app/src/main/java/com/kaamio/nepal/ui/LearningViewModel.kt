package com.kaamio.nepal.ui

import androidx.lifecycle.viewModelScope
import com.kaamio.nepal.data.ConnectivityObserver
import com.kaamio.nepal.data.Course
import com.kaamio.nepal.data.repository.IEducationRepository
import com.kaamio.nepal.payment.KhaltiPaymentGateway
import com.kaamio.nepal.payment.PaymentRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearningViewModel @Inject constructor(
    private val educationRepository: IEducationRepository,
    private val khaltiGateway: KhaltiPaymentGateway
) : BaseViewModel() {

    private val _selectedCourseCategory = MutableStateFlow("All")
    val selectedCourseCategory = _selectedCourseCategory.asStateFlow()

    private val _courseSearchQuery = MutableStateFlow("")
    val courseSearchQuery = _courseSearchQuery.asStateFlow()

    private val _isCoursesLoading = MutableStateFlow(true)
    val isCoursesLoading = _isCoursesLoading.asStateFlow()

    private val _selectedCourse = MutableStateFlow<Course?>(null)
    val selectedCourse = _selectedCourse.asStateFlow()

    private val _enrolledCourseIds = MutableStateFlow<Set<String>>(emptySet())
    val enrolledCourseIds = _enrolledCourseIds.asStateFlow()

    private val _pendingCoursePidx = MutableStateFlow<String?>(null)
    val pendingCoursePidx = _pendingCoursePidx.asStateFlow()

    private val _pendingCourse = MutableStateFlow<Course?>(null)
    val pendingCourse = _pendingCourse.asStateFlow()

    private val _isUnlocking = MutableStateFlow(false)
    val isUnlocking = _isUnlocking.asStateFlow()

    val coursesList: StateFlow<List<Course>> = combine(
        educationRepository.allCourses,
        _selectedCourseCategory,
        _courseSearchQuery
    ) { list, cat, q ->
        _isCoursesLoading.value = false
        val normalizedQ = q.trim().lowercase()
        list.filter { course ->
            (cat == "All" || course.category == cat) &&
                (normalizedQ.isEmpty() ||
                    course.title.contains(normalizedQ, ignoreCase = true) ||
                    course.instructor.contains(normalizedQ, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            educationRepository.allCourses.first().let { list ->
                _enrolledCourseIds.value = list.filter { it.progress > 0 || it.isUnlocked }.map { it.id }.toSet()
            }
        }
    }

    fun setCourseCategory(category: String) { _selectedCourseCategory.value = category }

    fun setCourseSearchQuery(query: String) { _courseSearchQuery.value = query }

    fun openCourse(course: Course) {
        _selectedCourse.value = course
    }

    fun closeCourse() {
        _selectedCourse.value = null
    }

    fun isEnrolled(course: Course): Boolean =
        _enrolledCourseIds.value.contains(course.id) || course.progress > 0 || course.isUnlocked

    fun initiateCourseUnlock(course: Course) {
        val price = priceAsNumber(course.price) ?: run {
            showSnackbar("Could not determine course price.")
            return
        }
        viewModelScope.launch {
            _isUnlocking.value = true
            _pendingCoursePidx.value = null
            _pendingCourse.value = course
            try {
                val result = khaltiGateway.initiatePayment(
                    PaymentRequest(
                        amount = price,
                        orderId = "course_${course.id}_${System.currentTimeMillis()}",
                        productName = course.title
                    )
                )
                result.onSuccess { paymentResult ->
                    _pendingCoursePidx.value = paymentResult.transactionId
                }.onFailure {
                    showSnackbar(it.message ?: "Could not start payment")
                    _pendingCourse.value = null
                }
            } catch (e: Exception) {
                showSnackbar(e.message ?: "Could not start payment")
                _pendingCourse.value = null
            } finally {
                _isUnlocking.value = false
            }
        }
    }

    fun confirmCourseUnlock(transactionId: String?) {
        val course = _pendingCourse.value ?: return
        viewModelScope.launch {
            // 1. Verify the gateway payment server-side BEFORE unlocking.
            // course-unlock only accepts payments whose status is "completed",
            // which is set by khalti-verifyPayment / the webhook.
            val verified = transactionId != null &&
                khaltiGateway.verifyPayment(transactionId)
                    .fold(onSuccess = { it.success }, onFailure = { false })
            if (!verified) {
                showSnackbar("Payment could not be verified. Please try again.")
                _pendingCourse.value = null
                _pendingCoursePidx.value = null
                return@launch
            }
            val ok = educationRepository.unlockCourse(course.id, transactionId)
            if (ok) {
                val unlocked = _enrolledCourseIds.value + course.id
                _enrolledCourseIds.value = unlocked
                _selectedCourse.value = _selectedCourse.value?.copy(isUnlocked = true)
                showSnackbar("Premium course unlocked.")
            } else {
                showSnackbar("Unlock could not be verified. Please try again.")
            }
            _pendingCourse.value = null
            _pendingCoursePidx.value = null
        }
    }

    fun clearPendingCoursePidx() { _pendingCoursePidx.value = null }

    private fun priceAsNumber(price: String): Double? =
        price.replace(Regex("[^0-9.]"), "").toDoubleOrNull()?.takeIf { it > 0 }

    fun refreshCourses(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                educationRepository.refreshCourses()
            } catch (_: Exception) {
                showSnackbar("Failed to refresh courses.")
            } finally {
                onComplete()
            }
        }
    }

    fun enrollInCourse(courseId: String) {
        viewModelScope.launch {
            try {
                val courses = educationRepository.allCourses.first()
                val course = courses.find { it.id == courseId } ?: return@launch
                if (course.progress == 0) educationRepository.updateLocalCourse(course.copy(progress = 1))
            } catch (_: Exception) {
                showSnackbar("Could not enroll in course.")
            }
        }
    }

    fun createCourse(course: Course) {
        viewModelScope.launch {
            try {
                educationRepository.createCourse(course)
            } catch (_: Exception) {
                showSnackbar("Failed to publish course.")
            }
        }
    }
}
