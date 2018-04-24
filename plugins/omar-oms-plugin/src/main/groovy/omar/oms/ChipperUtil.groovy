package omar.oms

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import org.ossim.oms.util.ImageGenerator
import org.ossim.oms.util.TransparentFilter

import javax.imageio.ImageTypeSpecifier
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
import java.awt.image.IndexColorModel

import joms.oms.Chipper
import joms.oms.ossimMemoryImageSource
import joms.oms.ossimImageDataRefPtr

import omar.core.DateUtil

import org.ossim.oms.image.omsRenderedImage
import org.ossim.oms.image.omsImageSource

import java.awt.image.SampleModel
import java.awt.image.WritableRaster

/**
 * Created by sbortman on 1/15/16.
 */
@Slf4j
class ChipperUtil
{
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

  static void runChipper(Map<String,String> opts, Map<String,Object> hints, byte[] buffer)
  {
    log.trace "runChipper: Entered.................."
    def chipper = new Chipper()

    log.trace "runChipper options: ${opts}"
    if ( chipper.initialize( opts ) )
    {
      log.debug "initialize: good"
      //println 'initialize: good'
      if ( chipper.getChip( buffer, buffer.length, hints.transparent ) > 1 )
      {
        //println 'getChip: good'
      }
      else
      {
        // println 'getChip: bad'
      }
    }
    else
    {
      // println 'initialize: bad'
    }

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
      // println 'initialize: bad'
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
          def renderedImage = new omsRenderedImage( new omsImageSource( cacheSource ) )
          result.sampleModel = renderedImage.sampleModel
          result.colorModel = renderedImage.colorModel
          result.raster = renderedImage.data
          renderedImage=null
        }
      }
      else
      {
         log.error 'chipper.initialize( opts ): ${opts} was unsuccessful'
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
    Boolean keepBands = hints?.keepBands

    if(hints.keepBands)
    {
      if(!hints.type.contains("tiff"))
      {
        // The only type we will support raw band output is TIFF.
        // this way we can send back the raw tiff without modification
        //
        keepBands = false;
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
        image = ChipperUtil.optimizeRaster( chipperResult.raster, chipperResult.colorModel, hints )
      }
      catch ( e )
      {
        e.printStackTrace()
      }
    }
    image 
  }
  static def convertToColorIndexModel( def dataBuffer, def width, def height, def transparentFlag )
  {
    ImageTypeSpecifier isp = ImageTypeSpecifier.createGrayscale( 8, DataBuffer.TYPE_BYTE, false );
    ColorModel colorModel
    SampleModel sampleModel = isp.getSampleModel( width, height )
    if ( !transparentFlag )
    {
      colorModel = isp.getColorModel();
    }
    else
    {
      int[] lut = new int[256]
      ( 0..<lut.length ).each {i ->
        lut[i] = ( ( 0xff << 24 ) | ( i << 16 ) | ( i << 8 ) | ( i ) );
      }
      lut[0] = 0xff000000
      colorModel = new IndexColorModel( 8, lut.length, lut, 0, true, 0, DataBuffer.TYPE_BYTE )
    }
    WritableRaster raster = WritableRaster.createWritableRaster( sampleModel, dataBuffer, null )
    return new BufferedImage( colorModel, raster, false, null );

  }

  static def optimizeRaster(Raster image, ColorModel colorModel, def hints)//String mimeType, Boolean transparentFlag)
  {
    def result
    String mimeTypeTest = hints.type?.toLowerCase()
    Boolean transparentFlag = hints.transparent
    if(transparentFlag == null) transparentFlag = false

    if(mimeTypeTest?.contains("jpeg"))
    {
      transparentFlag = false
    }
    if ( image.numBands == 1 )
    {
      result = convertToColorIndexModel( image.dataBuffer,
              image.width,
              image.height,
              transparentFlag )
    }
    else
    {
      Boolean isRasterPremultiplied = true
      Hashtable<?, ?> properties = null
      result = new BufferedImage(
              colorModel,
              image,
              isRasterPremultiplied,
              properties
      )
      if ( image.numBands == 3 )
      {
        if ( transparentFlag )
        {
          result = TransparentFilter.fixTransparency( new TransparentFilter(), result )
        }
        if ( mimeTypeTest?.contains( "gif" ) )
        {
          result = ImageGenerator.convertRGBAToIndexed( result )
        }
      }
    }
    result
  }
}
