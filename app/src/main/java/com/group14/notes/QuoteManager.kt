package com.group14.notes

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object QuoteManager {

    interface QuoteCallback {
        fun onSuccess(quote: String, author: String)
        fun onError(error: String)
    }

    fun fetchQuote(callback: QuoteCallback) {

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {

            var connection: HttpURLConnection? = null

            try {
                val url = URL("https://dummyjson.com/quotes/random")

                connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode

                if (responseCode != 200) {
                    throw Exception("Server error: $responseCode")
                }

                val response = connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

                val json = JSONObject(response)

                val quote = json.getString("quote")
                val author = json.getString("author")

                handler.post {
                    callback.onSuccess(quote, author)
                }

            } catch (e: Exception) {

                handler.post {
                    callback.onError("Unable to load quote.")
                }

            } finally {
                connection?.disconnect()
                executor.shutdown()
            }
        }
    }
}
