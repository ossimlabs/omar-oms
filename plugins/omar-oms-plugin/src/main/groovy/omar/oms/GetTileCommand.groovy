package omar.oms

import grails.validation.Validateable
import groovy.transform.ToString

/**
 * Created by sbortman on 12/7/15.
 */
@ToString(includeNames = true)
class GetTileCommand implements Validateable
{
  int x
  int y
  int z
  int tileSize = 256
  String outputFormat
  String filename
  int entry = 0
  String bands
  String histOp = "auto-minmax"
  Double brightness = 0.0
  Double contrast  = 1.0
  String sharpenMode = "none"
  String resamplerFilter = "nearest"
  Boolean scale_2_8_bit = true
  Boolean histCenterTile = false
  Boolean nullPixelFlip = false;
  Boolean transparent=true
  Integer numResLevels = 1
  Integer width = 0
  Integer height = 0
  String hist
  String ovr
  String geom

   void setOutputFormat(String format)
   {
      if(format.split("/").size() == 1)
      {
         this.outputFormat = "image/${format.toLowerCase()}"
      }
      else
      {
         this.outputFormat = format
      }
   }

}
