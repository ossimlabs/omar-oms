package omar.oms

import grails.validation.Validateable
import groovy.transform.ToString

/**
 * Created by sbortman on 12/17/15.
 */
@ToString( includeNames = true )
class GetThumbnailCommand implements Validateable
{
   String filename
   String hist
   String ovr
   String geom
   Integer entry=0
   Integer size=128
   String outputFormat="image/jpeg"
   String histOp = "auto-minmax"
   Boolean transparent=false
   Boolean nullPixelFlip=false
   void setOutputFormat(String value)
   {
      outputFormat = value
      if(value.split("/").size() == 1)
      {
         this.outputFormat = "image/${value}"
      }
      if(!value) outputFormat = "image/jpeg"
   }
}
