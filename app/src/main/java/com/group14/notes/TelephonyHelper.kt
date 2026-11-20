package com.group14.notes

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

class TelephonyHelper(context: Context) {

    private val telephonyManager: TelephonyManager? =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    fun getNetworkOperator(): String {

        if (telephonyManager == null) {
            return "Unavailable"
        }

        val operator = telephonyManager.networkOperatorName

        return if (operator.isNullOrEmpty()) {
            "Unknown operator"
        } else {
            operator
        }
    }

    fun getNetworkCountry(): String {

        if (telephonyManager == null) {
            return "Unavailable"
        }

        val country = telephonyManager.networkCountryIso

        return if (country.isNullOrEmpty()) {
            "Unknown country"
        } else {
            country.uppercase(Locale.ROOT)
        }
    }
}
