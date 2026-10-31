package com.jo.selfcontrol.ultimate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigManagerTest {

    private fun createAppLimit(
        pkg: String = "com.test.app",
        minutes: Int = 15,
        protectionDelaySec: Int? = null,
        session: ConfigManager.SessionConfig? = null
    ): ConfigManager.AppLimit {
        return ConfigManager.AppLimit(
            packageName = pkg,
            maxMinutesPerDay = minutes,
            maxSecondsPerDay = minutes * 60,
            allowedDays = (0..6).toList(),
            allowedHoursStart = 0,
            allowedHoursEnd = 1440,
            allDay = true,
            session = session,
            protectionDelaySec = protectionDelaySec,
            channelBlocks = emptyList()
        )
    }

    @Test
    fun testQuotaIncreaseWithGlobalDelayRequiresDeferral() {
        val oldLimit = createAppLimit(minutes = 15)
        val newLimit = createAppLimit(minutes = 30)
        val globalDelay = 3600 // 1 hour

        val requirement = ConfigManager.requiredDeferForAppLimit(oldLimit, newLimit, globalDelay)
        assertTrue("Increasing quota must defer", requirement.mustDefer)
        assertEquals(3600, requirement.seconds)
        assertFalse(requirement.fromExplicitTimer)
    }

    @Test
    fun testQuotaIncreaseWithPerRuleDelayUsesPerRuleTimer() {
        val oldLimit = createAppLimit(minutes = 15, protectionDelaySec = 259200) // 3 days
        val newLimit = createAppLimit(minutes = 30, protectionDelaySec = 259200)
        val globalDelay = 3600 // 1 hour

        val requirement = ConfigManager.requiredDeferForAppLimit(oldLimit, newLimit, globalDelay)
        assertTrue("Increasing quota with per-rule delay must defer", requirement.mustDefer)
        assertEquals(259200, requirement.seconds)
        assertTrue(requirement.fromExplicitTimer)
    }

    @Test
    fun testQuotaDecreaseIsImmediate() {
        val oldLimit = createAppLimit(minutes = 30, protectionDelaySec = 259200)
        val newLimit = createAppLimit(minutes = 15, protectionDelaySec = 259200)
        val globalDelay = 3600

        val requirement = ConfigManager.requiredDeferForAppLimit(oldLimit, newLimit, globalDelay)
        assertFalse("Decreasing quota must NOT defer", requirement.mustDefer)
        assertEquals(0, requirement.seconds)
    }

    @Test
    fun testDeleteLimitWithGlobalDelayRequiresDeferral() {
        val oldLimit = createAppLimit(minutes = 15)
        val globalDelay = 3600

        val requirement = ConfigManager.requiredDeferForAppLimit(oldLimit, null, globalDelay)
        assertTrue("Deleting limit must defer for global delay", requirement.mustDefer)
        assertEquals(3600, requirement.seconds)
        assertFalse(requirement.fromExplicitTimer)
    }

    @Test
    fun testDeleteLimitWithPerRuleDelayRequiresPerRuleDelay() {
        val oldLimit = createAppLimit(minutes = 15, protectionDelaySec = 259200) // 3 days
        val globalDelay = 3600

        val requirement = ConfigManager.requiredDeferForAppLimit(oldLimit, null, globalDelay)
        assertTrue("Deleting limit with 3-day protection must defer for 3 days", requirement.mustDefer)
        assertEquals(259200, requirement.seconds)
        assertTrue(requirement.fromExplicitTimer)
    }

    @Test
    fun testLoweringProtectionTimerRequiresDeferral() {
        val oldLimit = createAppLimit(minutes = 15, protectionDelaySec = 259200) // 3 days
        val newLimit = createAppLimit(minutes = 15, protectionDelaySec = null)   // removed
        val globalDelay = 3600

        val requirement = ConfigManager.requiredDeferForAppLimit(oldLimit, newLimit, globalDelay)
        assertTrue("Removing/lowering protection timer must defer", requirement.mustDefer)
        assertEquals(259200, requirement.seconds)
        assertTrue(requirement.fromExplicitTimer)
    }

    @Test
    fun testIncreasingProtectionTimerIsImmediate() {
        val oldLimit = createAppLimit(minutes = 15, protectionDelaySec = 3600)   // 1 hour
        val newLimit = createAppLimit(minutes = 15, protectionDelaySec = 259200) // 3 days
        val globalDelay = 3600

        val requirement = ConfigManager.requiredDeferForAppLimit(oldLimit, newLimit, globalDelay)
        assertFalse("Increasing protection timer must NOT defer", requirement.mustDefer)
        assertEquals(0, requirement.seconds)
    }

    @Test
    fun testAddingBrandNewLimitIsImmediate() {
        val newLimit = createAppLimit(minutes = 15)
        val globalDelay = 3600

        val requirement = ConfigManager.requiredDeferForAppLimit(null, newLimit, globalDelay)
        assertFalse("Adding a brand new limit is tightening and must NOT defer", requirement.mustDefer)
        assertEquals(0, requirement.seconds)
    }

    @Test
    fun testZeroQuotaAppLimitIncreaseRequiresDeferral() {
        // App blocked 24/7 with 0s quota
        val oldLimit = createAppLimit(minutes = 0, protectionDelaySec = 259200)
        val newLimit = createAppLimit(minutes = 10, protectionDelaySec = 259200)
        val globalDelay = 3600

        val requirement = ConfigManager.requiredDeferForAppLimit(oldLimit, newLimit, globalDelay)
        assertTrue("Increasing 0-quota app limit must defer", requirement.mustDefer)
        assertEquals(259200, requirement.seconds)
        assertTrue(requirement.fromExplicitTimer)
    }
}
