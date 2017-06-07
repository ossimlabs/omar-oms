package omar.oms

import grails.validation.Validateable
import groovy.transform.ToString

import grails.databinding.BindUsing
import omar.core.BindUtil

@ToString(includeNames = true)
class ChipperCommand implements Validateable
{
   String      bands
   String      brightness
   String      clipPolyLatLon
   String      clipWmsBboxLl
   String      colorBlue
   String      colorGreen
   String      colorRed
   String      contrast
   String      cutBboxXywh
   String      cutCenterLat
   String      cutCenterLon
   String      cutHeight
   String      cutMaxLat
   String      cutMaxLon
   String      cutMinLat
   String      cutMinLon
   String      cutRadius
   String      cutWidth
   String      cutWmsBbox
   String      cutWmsBboxLl
   String      degreesX
   String      degreesY
   String      dem
   String      gain
   List<Map>   images
   Boolean     histAoi
   Boolean     histCenter
   String      histLlwh
   String      histOp
   String      imageSpaceScaleX
   String      imageSpaceScaleY
   String      lutFile
   String      meters
   Boolean     northUp
   String      operation
   String      outputRadiometry
   Boolean     padThumbnail
   String      resamplerFilter
   String      rotation
   String      rrds
   Boolean     scale_2_8_bit
   String      sharpenMode
   String      snapTieToOrigin
   String      srs
   Boolean     threeBandOut
   String      thumbnailResolution
   String      tileSize
   Boolean     upIsUp
   String      combinerType

   String outputFormat
   Boolean transparent

   HashMap toChipperOptions()
   {
      HashMap result = [:]
      ArrayList props = this.properties.collect{k,v->k}

      props = props - ["errors", "class", "constraintsMap", "transparent", "format" ]

      props.each{k->
         if(k != "images")
         {
            // if we have any camel case convert to snake case
            String resultKey = k.replaceAll( /([A-Z])/, /_$1/ ).toLowerCase().replaceAll( /^_/, '' )
            if(this."${k}" != null) result."${resultKey}" = this."${k}".toString()
         }
      }

      images.eachWithIndex{v,i ->
         result."image${i}.file" = v.file?:""
         if(v.entry == null) result."image${i}.entry" = "0"
         else v.entry = v.entry.toString()
      }

      result
   }

}
