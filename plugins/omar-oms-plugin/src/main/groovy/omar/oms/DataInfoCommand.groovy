package omar.oms

import grails.validation.Validateable
import groovy.transform.ToString

import grails.databinding.BindUsing
import omar.core.BindUtil

@ToString(includeNames = true)
class DataInfoCommand implements Validateable
{
   String filename
   Integer entry
}
