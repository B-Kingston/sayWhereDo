package com.example.reminders.billing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillingManagerTest {

    private lateinit var billingManager: BillingManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        billingManager = BillingManager(context)
    }

    @Test
    fun `isPro starts as false`() {
        assertThat(billingManager.isPro.value).isFalse()
    }

    @Test
    fun `productId is pro_upgrade`() {
        assertThat(BillingManager.PRODUCT_ID).isEqualTo("pro_upgrade")
    }

    @Test
    fun `launchBillingFlow does nothing when no product details`() {
        val activity = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext as android.app.Activity
        billingManager.launchBillingFlow(activity)
    }

    @Test
    fun `restorePurchases does nothing when client not ready`() {
        billingManager.restorePurchases()
    }
}
