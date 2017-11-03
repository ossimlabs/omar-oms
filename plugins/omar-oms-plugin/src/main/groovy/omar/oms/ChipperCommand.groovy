package omar.oms

import grails.validation.Validateable
import groovy.transform.ToString

import grails.databinding.BindUsing
import omar.core.BindUtil

@ToString(includeNames = true)
class ChipperCommand implements Validateable
{
   String      bands
   Double      brightness
   String      clipPolyLatLon
   String      clipWmsBboxLl
   String      colorBlue
   String      colorGreen
   String      colorRed
   Double      contrast
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
   Boolean     nullPixelFlip = true // HACK
   String      operation
   String      outputRadiometry
   Boolean     padThumbnail
   String      resamplerFilter
   Double      rotation
   Integer      rrds
   Boolean     scale_2_8_bit
   String      sharpenMode
   String      snapTieToOrigin
   String      srs
   Boolean     threeBandOut
   Integer      thumbnailResolution
   String      tileSize
   Boolean     upIsUp
   String      combinerType

   // parameters that are not part of chipper
   String outputFormat
   Boolean transparent
   Boolean keepBands

    static constraints = {
      bands(nullable:true, blank:true)
      brightness(nullable:true, blank:true)
      clipPolyLatLon(nullable:true, blank:true)
      clipWmsBboxLl(nullable:true, blank:true)
      colorBlue(nullable:true, blank:true)
      colorGreen(nullable:true, blank:true)
      colorRed(nullable:true, blank:true)
      contrast(nullable:true, blank:true)
      cutBboxXywh(nullable:true, blank:true)
      cutCenterLat(nullable:true, blank:true)
      cutCenterLon(nullable:true, blank:true)
      cutHeight(nullable:true, blank:true)
      cutMaxLat(nullable:true, blank:true)
      cutMaxLon(nullable:true, blank:true)
      cutMinLat(nullable:true, blank:true)
      cutMinLon(nullable:true, blank:true)
      cutRadius(nullable:true, blank:true)
      cutWidth(nullable:true, blank:true)
      cutWmsBbox(nullable:true, blank:true)
      cutWmsBboxLl(nullable:true, blank:true)
      degreesX(nullable:true, blank:true)
      degreesY(nullable:true, blank:true)
      dem(nullable:true, blank:true)
      gain(nullable:true, blank:true)
      images(nullable:false, blank:false)
      histAoi(nullable:true, blank:true)
      histCenter(nullable:true, blank:true)
      histLlwh(nullable:true, blank:true)
      histOp(nullable:true, blank:true)
      imageSpaceScaleX(nullable:true, blank:true)
      imageSpaceScaleY(nullable:true, blank:true)
      keepBands(nullable:true, blank:true)
      lutFile(nullable:true, blank:true)
      meters(nullable:true, blank:true)
      northUp(nullable:true, blank:true)
      nullPixelFlip(nullable:true, blank:true)
      operation(nullable:false, blank:false)
      outputRadiometry(nullable:true, blank:true)
      padThumbnail(nullable:true, blank:true)
      resamplerFilter(nullable:true, blank:true)
      rotation(nullable:true, blank:true)
      rrds(nullable:true, blank:true)
      scale_2_8_bit(nullable:true, blank:true)
      sharpenMode(nullable:true, blank:true)
      snapTieToOrigin(nullable:true, blank:true)
      srs(nullable:true, blank:true)
      threeBandOut(nullable:true, blank:true)
      thumbnailResolution(nullable:true, blank:true)
      tileSize(nullable:true, blank:true)
      upIsUp(nullable:true, blank:true)
      combinerType(nullable:true, blank:true)

      outputFormat(nullable:true, blank:true)
      transparent(nullable:true, blank:true)
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
   void setSharpenMode(String v)
   {
      if(v == "none")
      {
         this.sharpenMode = ""
      }
      else
      {
         this.sharpenMode = v
      }
   }
  HashMap toChipperOptions()
   {
      HashMap result = [:]
      ArrayList props = this.properties.collect{k,v->k}

      props = props - ["errors", "class","constraints", "constraintsMap", "transparent", "outputFormat", "keepBands" ]

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
         else result."image${i}.entry" = v.entry.toString()
      }

      result
   }


}
