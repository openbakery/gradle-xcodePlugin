package org.openbakery.util

import java.text.SimpleDateFormat

/**
 * Created by stefangugarel on 23/03/2017.
 */
class DateHelper {

    DateHelper() {

    }

    def parseOpenSSLDate(String date) {

        SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd HH:mm:ss yyyy z")
        return dateFormatter.parse(date)
    }
}
