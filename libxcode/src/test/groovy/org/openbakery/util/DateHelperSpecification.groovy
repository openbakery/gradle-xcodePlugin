package org.openbakery.util

import spock.lang.Specification

import java.text.ParseException

/**
 * Created by stefangugarel on 23/03/2017.
 */
class DateHelperSpecification extends Specification {

    DateHelper dateHelper


    def setup() {
        dateHelper = new DateHelper()
    }

    def "dateHelper parse invalid date"() {
        when:
        dateHelper.parseOpenSSLDate("invalid Date")

        then:
        def exception = thrown(ParseException)
        exception.message == "Unparseable date: \"invalid Date\""
    }

    def "dateHelper parse valid date"() {
        when:
        def result = dateHelper.parseOpenSSLDate("Mar 20 10:16:40 2016 GMT")

        def calender = new GregorianCalendar()
        calender.setTime(result)


        then:

        calender.get(Calendar.DAY_OF_MONTH) == 20
        calender.get(Calendar.YEAR) == 2016
        calender.get(Calendar.MONTH) == 2
        calender.get(Calendar.HOUR) == 11
        calender.get(Calendar.MINUTE) == 16
        calender.get(Calendar.SECOND) == 40

    }
}
