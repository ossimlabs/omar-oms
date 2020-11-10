package omar.oms

import grails.validation.Validateable
import groovy.transform.ToString


@ToString(includeNames = true)
class DataInfoCommand implements Validateable
{
   String filename
   Integer entry
}
