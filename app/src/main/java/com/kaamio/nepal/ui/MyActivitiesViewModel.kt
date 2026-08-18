package com.kaamio.nepal.ui

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kaamio.nepal.data.Course
import com.kaamio.nepal.data.CourseDao
import com.kaamio.nepal.data.JobListing
import com.kaamio.nepal.data.JobListingDao
import com.kaamio.nepal.data.Review
import com.kaamio.nepal.data.repository.IUserRepository
import com.kaamio.nepal.payment.EscrowService
import com.kaamio.nepal.payment.EscrowTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MyActivitiesViewModel @Inject constructor(
    private val jobListingDao: JobListingDao,
    private val courseDao: CourseDao,
    private val userRepository: IUserRepository,
    private val escrowService: EscrowService,
    private val firebaseAuth: FirebaseAuth
) : BaseViewModel() {

    private val currentUid: String get() = firebaseAuth.currentUser?.uid ?: ""

    val myApplications: Flow<List<JobListing>> = jobListingDao.getAppliedJobs()
    val myListings: Flow<List<JobListing>> = jobListingDao.getMyListings(currentUid)

    val myEnrollments: Flow<List<Course>> = courseDao.getAllCourses()

    private val _escrows = MutableStateFlow<List<EscrowTransaction>>(emptyList())
    val escrows = _escrows.asStateFlow()

    private val _reviews = MutableStateFlow<List<Review>>(emptyList())
    val reviews = _reviews.asStateFlow()

    private val _isLoadingReviews = MutableStateFlow(true)
    val isLoadingReviews = _isLoadingReviews.asStateFlow()

    init {
        viewModelScope.launch {
            escrowService.observeUserEscrows(currentUid).collect { _escrows.value = it }
        }
        viewModelScope.launch {
            _isLoadingReviews.value = true
            try {
                _reviews.value = withContext(Dispatchers.IO) { userRepository.getMyReviews() }
            } catch (_: Exception) {}
            _isLoadingReviews.value = false
        }
    }
}
