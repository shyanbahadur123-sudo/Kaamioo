package com.kaamio.nepal.ui

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Market : Screen("market")
    object Learn : Screen("learn")
    object Community : Screen("community")
    object Chat : Screen("chat")
    object Negotiation : Screen("negotiation")
    object TrustLedger : Screen("trust_ledger")
    object TeacherDashboard : Screen("teacher_dashboard")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object Notifications : Screen("notifications")
    object IconShowcase : Screen("icon_showcase")
    object PostListing : Screen("post_listing")
    object CreateCourse : Screen("create_course")
    object EditProfile : Screen("edit_profile")
    object Payment : Screen("payment")
    object CourseDetail : Screen("course_detail")
    object MyActivities : Screen("my_activities")
    object Loading : Screen("loading")
    data class Error(val message: String) : Screen("error/{message}") {
        companion object {
            fun createRoute(message: String) = "error/$message"
        }
    }
}