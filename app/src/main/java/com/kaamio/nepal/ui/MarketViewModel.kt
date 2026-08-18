package com.kaamio.nepal.ui

import androidx.lifecycle.viewModelScope
import com.kaamio.nepal.data.JobListing
import com.kaamio.nepal.data.repository.IListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class MarketViewModel @Inject constructor(
    private val listingRepository: IListingRepository
) : BaseViewModel() {

    private val _marketSearchQuery = MutableStateFlow("")
    val marketSearchQuery = _marketSearchQuery.asStateFlow()

    private val _activeMarketTab = MutableStateFlow(KaamioConstants.TAB_JOBS)
    val activeMarketTab = _activeMarketTab.asStateFlow()

    private val _selectedChipFilter = MutableStateFlow(KaamioConstants.CHIP_ALL_ROLES)
    val selectedChipFilter = _selectedChipFilter.asStateFlow()

    private val _isJobsLoading = MutableStateFlow(true)
    val isJobsLoading = _isJobsLoading.asStateFlow()

    private val _isPosting = MutableStateFlow(false)
    val isPosting = _isPosting.asStateFlow()

    private var manualTabSet = false

    init {
        viewModelScope.launch {
            listingRepository.syncApplications()
            listingRepository.syncBookmarks()
        }
    }

    fun initializeWithRole(role: String) {
        if (manualTabSet) return
        val initialTab = if (role == "hire" || role == "business") KaamioConstants.TAB_LOCAL else KaamioConstants.TAB_JOBS
        _activeMarketTab.value = initialTab
    }

    private val debouncedSearchQuery = _marketSearchQuery
        .debounce(300L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(1000), "")

    val jobsList: StateFlow<List<JobListing>> = combine(
        listingRepository.allJobs,
        debouncedSearchQuery,
        _selectedChipFilter
    ) { list, q, chip ->
        _isJobsLoading.value = false
        val normalizedQuery = q.trim()
        list.filter { job ->
            val matchesQuery = normalizedQuery.isEmpty() ||
                job.title.contains(normalizedQuery, ignoreCase = true) ||
                job.location.contains(normalizedQuery, ignoreCase = true) ||
                job.preferredSkills.contains(normalizedQuery, ignoreCase = true)
            val matchesChip = when (chip) {
                KaamioConstants.CHIP_REMOTE -> job.isRemote
                KaamioConstants.CHIP_FULL_TIME -> job.type == "Full-time"
                KaamioConstants.CHIP_VERIFIED -> job.trustScore >= 80
                KaamioConstants.CHIP_KATHMANDU, KaamioConstants.CHIP_KATHMANDU_VALLEY -> job.location.contains("Kathmandu", ignoreCase = true)
                        || job.location.contains("Lalitpur", ignoreCase = true)
                        || job.location.contains("Bhaktapur", ignoreCase = true)
                KaamioConstants.CHIP_AVAILABLE -> !job.isApplied
                KaamioConstants.CHIP_HIGH_BUDGET -> extractBudget(job.budget) >= 10000
                KaamioConstants.CHIP_UNDER_TWO_WEEKS -> job.deadlineDays <= 14
                KaamioConstants.CHIP_ESCROW -> job.milestonesCount > 0 && job.type != "Full-time"
                KaamioConstants.CHIP_RURAL -> !job.location.contains("Kathmandu", ignoreCase = true)
                        && !job.location.contains("Lalitpur", ignoreCase = true)
                        && !job.location.contains("Bhaktapur", ignoreCase = true)
                KaamioConstants.CHIP_BAGMATI -> job.location.contains("Kathmandu", ignoreCase = true)
                        || job.location.contains("Lalitpur", ignoreCase = true)
                        || job.location.contains("Bhaktapur", ignoreCase = true)
                        || job.location.contains("Hetauda", ignoreCase = true)
                KaamioConstants.CHIP_MADHESH -> job.location.contains("Janakpur", ignoreCase = true)
                        || job.location.contains("Birgunj", ignoreCase = true)
                else -> true
            }
            matchesQuery && matchesChip
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun filterJobsByTab(list: List<JobListing>, tab: String): List<JobListing> = when (tab) {
        KaamioConstants.TAB_JOBS -> list.filter { it.category == "Tech" }
        KaamioConstants.TAB_LOCAL -> list.filter { it.category == "Local" }
        KaamioConstants.TAB_FREELANCE -> list.filter { it.category == "Freelance" }
        else -> list
    }

    fun setMarketTab(tab: String) {
        _activeMarketTab.value = tab
        manualTabSet = true
    }

    fun resetManualTab() { manualTabSet = false }
    fun setChipFilter(filter: String) { _selectedChipFilter.value = filter }
    fun setMarketSearchQuery(query: String) { _marketSearchQuery.value = query }

    fun refreshJobs() {
        viewModelScope.launch {
            _isJobsLoading.value = true
            try {
                listingRepository.refreshJobs()
                listingRepository.syncApplications()
                listingRepository.syncBookmarks()
            } catch (_: Exception) {
                showSnackbar("Failed to refresh listings.")
            } finally {
                _isJobsLoading.value = false
            }
        }
    }

    fun getMarketChipsForTab(tab: String): List<String> = when (tab) {
        KaamioConstants.TAB_JOBS -> listOf(
            KaamioConstants.CHIP_ALL_ROLES, KaamioConstants.CHIP_REMOTE,
            KaamioConstants.CHIP_KATHMANDU, KaamioConstants.CHIP_FULL_TIME
        )
        KaamioConstants.TAB_LOCAL -> listOf(
            KaamioConstants.CHIP_ALL_ROLES, KaamioConstants.CHIP_VERIFIED,
            KaamioConstants.CHIP_RURAL, KaamioConstants.CHIP_AVAILABLE
        )
        KaamioConstants.TAB_FREELANCE -> listOf(
            KaamioConstants.CHIP_ALL_ROLES, KaamioConstants.CHIP_HIGH_BUDGET,
            KaamioConstants.CHIP_UNDER_TWO_WEEKS, KaamioConstants.CHIP_ESCROW,
            KaamioConstants.CHIP_VERIFIED
        )
        else -> listOf(KaamioConstants.CHIP_ALL_ROLES)
    }

    private fun extractBudget(raw: String): Int {
        if (raw.isBlank()) return 0
        val numbers = Regex("""\d+(?:\.\d+)?(?:[kKmM])?""")
            .findAll(raw.replace(",", ""))
            .mapNotNull { m ->
                val s = m.value
                val multiplier = when {
                    s.endsWith("k", ignoreCase = true) -> 1000
                    s.endsWith("m", ignoreCase = true) -> 1_000_000
                    else -> 1
                }
                val num = s.replace(Regex("[kKmM]"), "").toDoubleOrNull()
                if (num != null) (num * multiplier).toInt() else null
            }.toList()
        // For ranges like "120k-180k", use the upper bound so a range never
        // under-qualifies for a high-budget filter.
        return numbers.maxOrNull() ?: 0
    }

    fun postJob(job: JobListing, onComplete: (Boolean) -> Unit = {}) {
        if (_isPosting.value) return
        viewModelScope.launch {
            _isPosting.value = true
            try {
                listingRepository.postListing(job)
                showSnackbar("Listing published successfully.")
                onComplete(true)
            } catch (_: Exception) {
                showSnackbar("Failed to publish listing. Check your connection.")
                onComplete(false)
            } finally {
                _isPosting.value = false
            }
        }
    }

    fun applyToJob(jobId: String) {
        viewModelScope.launch {
            try {
                listingRepository.applyToJob(jobId)
                showSnackbar("Application sent successfully.")
            } catch (_: Exception) {
                showSnackbar("Failed to submit application. Try again.")
            }
        }
    }

    fun toggleBookmark(jobId: String, currentlyBookmarked: Boolean) {
        viewModelScope.launch {
            try {
                listingRepository.bookmarkJob(jobId, !currentlyBookmarked)
            } catch (_: Exception) {
                showSnackbar("Failed to update bookmark.")
            }
        }
    }
}
