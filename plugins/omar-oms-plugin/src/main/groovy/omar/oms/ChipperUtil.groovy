package omar.oms

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import org.ossim.oms.util.ImageGenerator

import java.awt.Point
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.ComponentColorModel
import java.awt.image.DataBuffer
import java.awt.image.Raster
import java.awt.image.RenderedImage
import groovy.util.logging.Slf4j

import joms.oms.Chipper
import joms.oms.ossimMemoryImageSource

import omar.core.DateUtil
import org.ossim.oms.image.omsRenderedImage
import org.ossim.oms.image.omsImageSource
import java.awt.image.SampleModel

/**
 * Created by sbortman on 1/15/16.
 */
@Slf4j
class ChipperUtil
{
  static final int DEFAULT_PNG_SIZE = 262144
  static final int DEFAULT_JPEG_SIZE = 16384

  static HashMap stylesToOpts(String styles, HashMap options=null)
  {
    HashMap opts = options?:[:]

    if(styles)
    {
      try
      {
        def stylesObj = new JsonSlurper().parseText(styles)

        if(stylesObj?.bands)
        {
          //opts.three_band_out = false
          opts.bands = stylesObj.bands.join(",")
        }
        if(stylesObj?.histOp)
        {
          opts.hist_op = stylesObj.histOp
        }
      }
      catch(e)
      {
        log.error(e.toString())
      }

    }
    opts

}
  static ColorModel createColorModel(int numBands, boolean transparent)
  {
    def cs = ColorSpace.getInstance( ColorSpace.CS_sRGB )
    def mask = ( ( 0..<numBands ).collect { 8 } ) as int[]

    def colorModel = new ComponentColorModel( cs, mask,
        transparent, false, ( transparent ) ? Transparency.TRANSLUCENT : Transparency.OPAQUE,
        DataBuffer.TYPE_BYTE )

    return colorModel
  }

  static RenderedImage createImage(Map<String,String> opts, Map<String,Object> hints)
  {
    log.trace "createImage: Entered................"
    def numBands = hints.transparent ? 4 : 3

    def raster = Raster.createInterleavedRaster(
        DataBuffer.TYPE_BYTE,
        hints.width, hints.height,
        hints.width * numBands, numBands, (0..<numBands) as int[],
        new Point( 0, 0 ) )

    runChipper(opts, hints, raster.dataBuffer.data)

    def colorModel = createColorModel(numBands, hints.transparent)
    def image = new BufferedImage(colorModel, raster, false, null)

    log.trace "createImage: Leaving.............."

    return image
  }

  static void runChipper(Map<String,String> opts , Map<String,Object> hints, byte[] buffer)
  {

    log.trace "runChipper: Entered.................."
    def chipper = new Chipper()

    log.trace "runChipper options: ${opts}"
    if ( chipper.initialize( opts ) )
    {
      log.debug "initialize: good"
    }
    else
    {
      log.error  "initialize: bad"
    }

    log.trace "runChipper hints: ${hints}"
    log.trace "runChipper buffer: ${buffer}"

    chipper.delete()

    log.trace "runChipper: Leaving.................."
  }
  static Boolean executeChipper(Map<String,String> opts)
  {
    Boolean result = false
    log.trace "runChipper: Entered.................."
    def chipper = new Chipper()

    log.trace "runChipper options: ${opts}"
    if ( chipper.initialize( opts ) )
    {
      result = chipper.execute()
    }
    else
    {
      log.error  "initialize: bad"
    }

    chipper.delete()

    log.trace "runChipper: Leaving.................."

    result
  }
  static HashMap runChipper(Map<String,String> opts)
  {
    log.trace "runChipper: Entered.................."
    HashMap result = [colorModel:null,
                     sampleModel:null,
                     raster:null
    ]
    def chipper = new Chipper()
    def imageData
    def cacheSource

    def requestType = "GET"
    def requestMethod = "runChipper"
    def responseTime
    def httpStatus = 200
    Date startTime = new Date()
    Date endTime
    JsonBuilder logOutput

    try {
      log.trace "runChipper options: ${opts}"
      if ( chipper.initialize( opts ) )
      {
        imageData = chipper.getChip(opts);

        if((imageData != null ) && (imageData.get() != null))
        {
          cacheSource = new ossimMemoryImageSource();
          cacheSource?.setImage( imageData );
          def renderedImage  = new omsRenderedImage( new omsImageSource( cacheSource ) )
          result.sampleModel = renderedImage.sampleModel
          result.colorModel  = renderedImage.colorModel
          result.raster      = renderedImage.data
          renderedImage=null
        }
      }
      else
      {
         log.error "chipper.initialize( opts ): ${opts} was unsuccessful"
      }

    }
    catch(e)
    {
      log.error e.toString()
      httpStatus = 400
    }
    finally {
      cacheSource?.delete();cacheSource = null
      imageData?.delete(); imageData = null
      chipper?.delete(); chipper = null

    }

    log.trace "runChipper: Leaving.................."

    endTime = new Date()
    responseTime = Math.abs(startTime.getTime() - endTime.getTime())
    logOutput = new JsonBuilder(timestamp: DateUtil.formatUTC(startTime), requestType: requestType,
            requestMethod: requestMethod, httpStatus: httpStatus, endTime: DateUtil.formatUTC(endTime),
            responseTime: responseTime, filename: opts?."image0.file")

    log.info logOutput.toString()

    result
  }
  /**
    *chipperResult will expect input as a HashMap with contents:
    *   [colorModel:some java color model, Be of type ColorModel
    *    sampleModel:some java sample model, Be of type SampleModel
    *    raster: java raster image
    *    ]
    *
    * hints can have values:
    *     [ keepBands: true|false if output is tiff you can ask not to modify the band count and will 
    *                             output 10 bands if the raster has 10 bands.
    *        type: should be the output type and should contain a string that has 'jpeg' | 'gif' | 'tiff' or jpeg
    *        transparent: Will enable a transparent output.  If not supported by the format it is ignored
    *     ]
  *  
  **/
  static def chipperResultToImage(HashMap chipperResult, HashMap hints = [:])
  {
    def image
//    Boolean keepBands = hints?.keepBands ->not being used

    if(hints.keepBands)
    {
      if(!hints.type.contains("tiff"))
      {
        // The only type we will support raw band output is TIFF.
        // this way we can send back the raw tiff without modification
        //
//        keepBands = false; ->not being used
      }
    }
    if ( chipperResult.raster )
    {
      if ( (!hints.keepBands) && (chipperResult.raster.numBands > 3 ))
      {
        def planarImage = JaiImage.bufferedToPlanar( new BufferedImage( chipperResult.colorModel, chipperResult.raster, true, null ) )
        planarImage.data
        def modifiedImage = JaiImage.selectBandsForRendering( planarImage )

        if ( modifiedImage )
        {
          chipperResult.raster = modifiedImage.data
          chipperResult.colorModel = modifiedImage.colorModel
        }
      }

      try
      {
        image = ImageGenerator.optimizeRaster( chipperResult.raster, chipperResult.colorModel, hints )
      }
      catch ( e )
      {
        log.error e.toString()
      }
    }
    image 
  }
}
