package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

data class UsageStats(
    val requestsToday: Int,
    val dailyLimit: Int, // 0 means unlimited
    val remainingToday: Int,
    val isLimitReached: Boolean,
    val estimatedTokensToday: Int,
    val requestsThisMinute: Int,
    val providerCounts: Map<String, Int>
)

class UsageTracker(private val context: Context, private val preferencesManager: PreferencesManager) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("omnichat_usage", Context.MODE_PRIVATE)

    private val timestampsInLastMinute = CopyOnWriteArrayList<Long>()

    private val _usageStats = MutableStateFlow(loadStats())
    val usageStats: StateFlow<UsageStats> = _usageStats.asStateFlow()

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    @Synchronized
    private fun ensureCurrentDay() {
        val today = getTodayDateString()
        val savedDay = prefs.getString("last_usage_day", "")
        if (savedDay != today) {
            // New day: atomically reset daily counters and remove previous provider counters
            val editor = prefs.edit()
                .putString("last_usage_day", today)
                .putInt("requests_today", 0)
                .putInt("tokens_today", 0)
            for (key in prefs.all.keys) {
                if (key.startsWith("provider_req_")) {
                    editor.remove(key)
                }
            }
            editor.apply()
        }
    }

    @Synchronized
    private fun loadStats(): UsageStats {
        ensureCurrentDay()
        cleanOldTimestamps()

        val requestsToday = prefs.getInt("requests_today", 0)
        val tokensToday = prefs.getInt("tokens_today", 0)
        val dailyLimit = preferencesManager.dailyLimit

        val remaining = if (dailyLimit <= 0) {
            999999
        } else {
            maxOf(0, dailyLimit - requestsToday)
        }

        val isReached = dailyLimit > 0 && requestsToday >= dailyLimit

        val providerMap = mutableMapOf<String, Int>()
        for ((key, value) in prefs.all) {
            if (key.startsWith("provider_req_") && value is Int) {
                val provId = key.removePrefix("provider_req_")
                providerMap[provId] = value
            }
        }

        return UsageStats(
            requestsToday = requestsToday,
            dailyLimit = dailyLimit,
            remainingToday = remaining,
            isLimitReached = isReached,
            estimatedTokensToday = tokensToday,
            requestsThisMinute = timestampsInLastMinute.size,
            providerCounts = providerMap
        )
    }

    private fun cleanOldTimestamps() {
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 60_000
        timestampsInLastMinute.removeIf { it < oneMinuteAgo }
    }

    @Synchronized
    fun recordRequest(providerId: String, estimatedTokens: Int = 150) {
        ensureCurrentDay()
        val now = System.currentTimeMillis()
        timestampsInLastMinute.add(now)
        cleanOldTimestamps()

        val currentRequests = prefs.getInt("requests_today", 0) + 1
        val currentTokens = prefs.getInt("tokens_today", 0) + estimatedTokens
        val currentProvRequests = prefs.getInt("provider_req_$providerId", 0) + 1

        prefs.edit()
            .putInt("requests_today", currentRequests)
            .putInt("tokens_today", currentTokens)
            .putInt("provider_req_$providerId", currentProvRequests)
            .apply()

        _usageStats.value = loadStats()
    }

    @Synchronized
    fun isLimitReached(): Boolean {
        ensureCurrentDay()
        val limit = preferencesManager.dailyLimit
        if (limit <= 0) return false // unlimited
        val current = prefs.getInt("requests_today", 0)
        return current >= limit
    }

    @Synchronized
    fun setDailyLimit(newLimit: Int) {
        preferencesManager.dailyLimit = newLimit
        _usageStats.value = loadStats()
    }

    @Synchronized
    fun resetTodayStats() {
        val editor = prefs.edit()
            .putInt("requests_today", 0)
            .putInt("tokens_today", 0)
        for (key in prefs.all.keys) {
            if (key.startsWith("provider_req_")) {
                editor.remove(key)
            }
        }
        editor.apply()
        timestampsInLastMinute.clear()
        _usageStats.value = loadStats()
    }
}
