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
  Double sharpenPercent = 0
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
  Double gamma

    static constraints = {
        x(nullable: false, blank: false)
        y(nullable: false, blank: false)
        z(nullable: false, blank: false)

        tileSize(nullable: false, blank: false, validator: { val, obj ->
            String result
            if (val != 256 && val != 512)
            {
                result = "tileSize must be either 256 or 512"
            }
            result
        })
        outputFormat(nullable: false, blank: false, validator: { val, obj ->
            String result
            if (val != "image/png" && val != "image/jpeg" && val != "image/gif" && val != "image/tiff")
            {
                result = "outputFormat must be png, jpeg, gif, or tiff"
            }
            result
        })
        transparent(nullable: true, blank: true)

        filename(nullable: true, blank: true)
        geom(nullable: true, blank: true)
        gamma(nullable: true, blank: true)
        hist(nullable: true, blank: true)
        bands(nullable: true, blank: true)
        ovr(nullable: true, blank: true)
    }

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
