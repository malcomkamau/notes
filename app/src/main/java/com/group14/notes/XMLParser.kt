package com.group14.notes

import android.content.Context
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser

object XMLParser {

    fun readNotes(context: Context) {

        try {
            context.resources.openRawResource(R.raw.sample_notes).use { input ->

                val parser = Xml.newPullParser()
                parser.setInput(input, "UTF-8")

                var eventType = parser.eventType

                while (eventType != XmlPullParser.END_DOCUMENT) {

                    if (eventType == XmlPullParser.START_TAG) {

                        when (parser.name) {

                            "title" -> {
                                val title = parser.nextText()
                                Log.d("XML_NOTE", title)
                            }

                            "description" -> {
                                val description = parser.nextText()
                                Log.d("XML_NOTE", description)
                            }
                        }
                    }

                    eventType = parser.next()
                }
            }

        } catch (e: Exception) {
            Log.e("XML_NOTE", "Error reading XML notes", e)
        }
    }
}
