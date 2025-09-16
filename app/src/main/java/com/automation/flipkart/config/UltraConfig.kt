package com.automation.flipkart.config
 
import java.util.regex.Pattern
 
object UltraConfig {
    // Performance constants
    const val MIN_EVENT_INTERVAL = 50L
    const val CACHE_TTL = 2000L
    const val MAX_CONCURRENT_CLICKS = 8
    
    // Button patterns for ultra-fast recognition
    val ULTRA_FAST_PATTERNS = arrayOf(
        ButtonPattern(
            text = "Buy Now",
            resourceIds = listOf(
                "com.flipkart.android:id/buy_now_button",
                "com.flipkart.android:id/buyNowButton",
                "com.flipkart.android:id/btn_buy_now"
            ),
            contentDescriptions = listOf("Buy Now", "Buy now", "BUY NOW"),
            priority = 1,
            isCritical = true
        ),
        
        ButtonPattern(
            text = "बाय नाउ",
            resourceIds = listOf(
                "com.flipkart.android:id/buy_now_button"
            ),
            contentDescriptions = listOf("बाय नाउ", "Buy Now"),
            priority = 1,
            isCritical = true
        ),
        
        ButtonPattern(
            text = "Add to Cart",
            resourceIds = listOf(
                "com.flipkart.android:id/add_to_cart",
                "com.flipkart.android:id/addToCartButton"
            ),
            contentDescriptions = listOf("Add to Cart", "Add To Cart"),
            priority = 2,
            isCritical = false
        ),
        
        ButtonPattern(
            text = "Place Order",
            resourceIds = listOf(
                "com.flipkart.android:id/place_order",
                "com.flipkart.android:id/placeOrderButton"
            ),
            contentDescriptions = listOf("Place Order", "PLACE ORDER"),
            priority = 1,
            isCritical = true
        ),
        
        ButtonPattern(
            text = "Continue",
            resourceIds = listOf(
                "com.flipkart.android:id/continue_button",
                "com.flipkart.android:id/continueButton"
            ),
            contentDescriptions = listOf("Continue", "CONTINUE"),
            priority = 3,
            isCritical = false
        ),
        
        ButtonPattern(
            text = "Pay Now",
            resourceIds = listOf(
                "com.flipkart.android:id/pay_now",
                "com.flipkart.android:id/payNowButton"
            ),
            contentDescriptions = listOf("Pay Now", "PAY NOW"),
            priority = 1,
            isCritical = true
        )
    )
    
    data class ButtonPattern(
        val text: String,
        val resourceIds: List<String>,
        val contentDescriptions: List<String>,
        val priority: Int,
        val isCritical: Boolean
    ) {
        private var compiledRegex: Pattern? = null
        
        fun compileRegex() {
            compiledRegex = Pattern.compile(
                "(?i).*${Pattern.quote(text)}.*",
                Pattern.CASE_INSENSITIVE
            )
        }
        
        fun matches(text: String): Boolean {
            return compiledRegex?.matcher(text)?.matches() ?: false
        }
    }
}