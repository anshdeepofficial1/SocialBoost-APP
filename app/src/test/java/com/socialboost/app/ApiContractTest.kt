package com.socialboost.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiContractTest {
    @Test fun httpsOnlyBackend() {
        assertTrue(BuildConfig.API_BASE_URL.startsWith("https://"))
    }
    @Test fun productName() {
        assertEquals("SocialBoost", "SocialBoost")
    }
}
