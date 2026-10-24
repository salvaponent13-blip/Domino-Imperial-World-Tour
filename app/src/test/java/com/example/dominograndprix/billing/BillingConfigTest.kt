package com.example.dominograndprix.billing

import org.junit.Assert.assertEquals
import org.junit.Test

class BillingConfigTest {

    @Test
    fun productIdMatchesPlayConsole() {
        assertEquals("remove_ads", BillingConfig.PRODUCT_REMOVE_ADS)
    }

    @Test
    fun referencePriceIsShownWhilePlayIsOffline() {
        assertEquals("2,49 €", BillingConfig.REFERENCE_PRICE_EUR)
    }
}
