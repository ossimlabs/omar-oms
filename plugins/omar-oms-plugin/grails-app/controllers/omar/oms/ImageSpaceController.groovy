package omar.oms

import grails.converters.JSON
import omar.core.BindUtil
import omar.core.HttpStatus

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import io.swagger.annotations.*

@Api(value = "/imageSpace",
     description = "API operations in image space."
)
import grails.async.web.AsyncController

@Api(value = "/imageSpace",
        description = "API operations in image space."
)
class ImageSpaceController implements AsyncController
{
    final Logger logger = LoggerFactory.getLogger("myLogger")

  def imageSpaceService

  def index(/*GetTileCommand cmd*/)
  {
    def cmd = new GetTileCommand()

    BindUtil.fixParamNames( GetTileCommand, params )
    bindData( cmd, params )
    def filename = cmd.filename //?: '/data/uav/predator/vesdata/po_197675_pan_0000000.ntf'
    def entry = cmd.entry ?: 0
    def imageInfo = imageSpaceService.readImageInfo( filename as File )
    def upAngle = imageSpaceService.computeUpIsUp( filename, entry )
    def northAngle = imageSpaceService.computeNorthIsUp( filename, entry )


    def initParams = [
        filename: filename,
        entry: entry,
        imgWidth: imageInfo.images[entry].resLevels[0].width,
        imgHeight: imageInfo.images[entry].resLevels[0].height,
        upAngle: Math.toRadians( upAngle ),
        northAngle: Math.toRadians( northAngle )
    ]

    [initParams: initParams]
  }

  @ApiOperation(value = "Get a tile from the passed in image file",
                produces="image/jpeg,image/png,image/gif",
                httpMethod="GET")
  @ApiImplicitParams([
          @ApiImplicitParam(name = 'x', value = 'Tile in x direction', defaultValue = '0', paramType = 'query', dataType = 'integer', required=true),
          @ApiImplicitParam(name = 'y', value = 'Tile in y direction', defaultValue = '0', paramType = 'query', dataType = 'integer', required=true),
          @ApiImplicitParam(name = 'z', value = 'Resolution level (0 lowest resolution)', defaultValue = '0', paramType = 'query', dataType = 'integer', required=true),
          @ApiImplicitParam(name = 'tileSize', value = 'Tile size', allowableValues="512,256", defaultValue="256", paramType = 'query', dataType = 'integer', required=false),
          @ApiImplicitParam(name = 'nullPixelFlip', value = 'Flip interior null pixels to valid', allowableValues="true,false", defaultValue="false", paramType = 'query', dataType = 'boolean', required=false),
          @ApiImplicitParam(name = 'outputFormat', value = 'Output image format', allowableValues="png,jpeg,gif,tiff", defaultValue="png", paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'filename', value = 'Filename', paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'entry', value = 'Image entry id(typically 0)', defaultValue="0", paramType = 'query', dataType = 'integer', required=false),
          @ApiImplicitParam(name = 'hist', value = 'Histogram file', paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'ovr', value = 'Overview file', paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'geom', value = 'Geometry file', paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'bands', value = 'Bands', defaultValue="", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'histOp', value = 'Histogram Operation',defaultValue = '',allowableValues="none,auto-minmax,auto-percentile,std-stretch-1,std-stretch-2,std-stretch-3", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'histCenterClip', value = 'Adjust center for min max clip (defaults to 0.5)', defaultValue="0.5", paramType = 'query', dataType = 'number', required=false),
          @ApiImplicitParam(name = 'sharpenMode', value = 'Sharpen Operation',allowableValues="none,light,heavy", defaultValue="none", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'sharpenPercent', value = 'Sharpen Percentage (0..1)', defaultValue="0.0", paramType = 'query', dataType = 'number', required=false),
          @ApiImplicitParam(name = 'resamplerFilter', value = 'Which resampling engine to use', defaultValue = '',  allowableValues= "nearest-neighbor, bilinear, cubic, gaussian, blackman, bspline, hanning, hamming, hermite, mitchell, quadratic, sinc, magic", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'brightness', value = 'Brightness Operation',defaultValue="0.0", paramType = 'query', dataType = 'number', required=false),
          @ApiImplicitParam(name = 'contrast', value = 'Contrast Operation',defaultValue="1.0",  paramType = 'query', dataType = 'number', required=false),
          @ApiImplicitParam(name = 'histCenterTile', value = 'Use Center File for Histogram', defaultValue="false",  paramType = 'query', dataType = 'boolean', required=false),
          @ApiImplicitParam(name = 'transparent', value = 'Enable transparent if the outputFormat supports it', defaultValue="true",  paramType = 'query', dataType = 'boolean', required=false),
          @ApiImplicitParam(name = 'numResLevels', value = 'Number of Resolution Levels', defaultValue="1", paramType = 'query', dataType = 'integer', required=false),
          @ApiImplicitParam(name = 'gamma', value = 'Gamma correction', defaultValue="", paramType = 'query', dataType = 'number', required=false)
  ])
  def getTile(/*GetTileCommand cmd*/)
  {
    def cmd = new GetTileCommand()
    BindUtil.fixParamNames( GetTileCommand, params )
    bindData( cmd, params )
    def outputStream = null
    try
    {
       response.status = HttpStatus.OK
       def result = imageSpaceService.getTile( cmd )
       outputStream = response.outputStream
       if(result.status != null) response.status        = result.status
       if(result.contentType) response.contentType      = result.contentType
       if(result.buffer?.length) response.contentLength = result.buffer.length

        if ( result.status == 500) {
            throw new IllegalArgumentException( new String(result.buffer) )
        }

       if(outputStream)
       {
          outputStream << result.buffer
       }
    }
    catch ( e )
    {
        //logger.error("There was an illegal argument in ImageSpaceController line 109", e)

       response.status = HttpStatus.INTERNAL_SERVER_ERROR
       //logger.debug(e.message)
    }
    finally{
       if(outputStream!=null)
       {
          try{
             outputStream.close()
          }
          catch(e)
          {
             //log.debug(e.message)
          }
       }
    }
  }


  @ApiOperation(value = "Get the footprint of  tile and its name",
                produces="image/jpeg,image/png,image/gif",
                httpMethod="GET")
  @ApiImplicitParams([
          @ApiImplicitParam(name = 'x', value = 'Tile in x direciton', defaultValue = '0', paramType = 'query', dataType = 'integer', required=true),
          @ApiImplicitParam(name = 'y', value = 'Tile in y direction', defaultValue = '0', paramType = 'query', dataType = 'integer', required=true),
          @ApiImplicitParam(name = 'z', value = 'Resolution level (0 full resolution)', defaultValue = '0', paramType = 'query', dataType = 'integer', required=true),
          @ApiImplicitParam(name = 'tileSize', value = 'Tile size', allowableValues="512,256", defaultValue="256", paramType = 'query', dataType = 'integer', required=true),
          @ApiImplicitParam(name = 'outputFormat', value = 'Output image format', allowableValues="png,jpeg,gif,tiff", defaultValue="png", paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'transparent', value = 'Enable transparent if the outputFormat supports it', defaultValue="true",  paramType = 'query', dataType = 'boolean', required=false)
  ])
  def getTileOverlay(/*GetTileCommand cmd*/)
  {
    def cmd = new GetTileCommand()

    BindUtil.fixParamNames( GetTileCommand, params )
    bindData( cmd, params )

    def outputStream = null
    try
    {
        outputStream = response.outputStream

        if(cmd.validate()) {
            response.status = HttpStatus.OK
            def result = imageSpaceService.getTileOverlay( cmd )

            if(result.status != null) response.status        = result.status
            if(result.contentType) response.contentType      = result.contentType
            if(result.buffer?.length) response.contentLength = result.buffer.length
            if(outputStream)
            {
                outputStream << result.buffer
            }
        }
        else {
            response.status = HttpStatus.BAD_REQUEST
        }
    }
    catch ( e )
    {
       response.status = HttpStatus.INTERNAL_SERVER_ERROR
       //log.debug(e.message)
    }
    finally{
        outputStream?.close()
    }

      return outputStream
  }

  def getAngles()
  {

    String filename = params.filename
    Integer entry = params.int( 'entry' )

    def upAngle = Math.toRadians( imageSpaceService.computeUpIsUp( filename, entry ) )
    def northAngle = Math.toRadians( imageSpaceService.computeNorthIsUp( filename, entry ) )

    def results = [
        upAngle: upAngle?.isNaN() ? 0 : upAngle,
        northAngle: northAngle?.isNaN() ? 0 : northAngle
    ]

    render contentType: 'application/json', text: results as JSON
  }

  @ApiOperation(value = "Get the thumbnail of the passed in file name",
                produces="image/jpeg,image/png,image/gif",
                httpMethod="GET")
  @ApiImplicitParams([
          @ApiImplicitParam(name = 'size', value = 'Overview image size', allowableValues="64,128,256,512,1024", defaultValue="256", paramType = 'query', dataType = 'integer', required=true),
          @ApiImplicitParam(name = 'outputFormat', value = 'Output image format', allowableValues="png,jpeg,gif,tiff", defaultValue="png", paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'nullPixelFlip', value = 'Flip interior null pixels to valid', allowableValues="true,false", defaultValue="false", paramType = 'query', dataType = 'boolean', required=false),
          @ApiImplicitParam(name = 'padThumbnail', value = 'pad the thumbnail so it is square', allowableValues="true,false", defaultValue="false", paramType = 'query', dataType = 'boolean', required=false),
          @ApiImplicitParam(name = 'transparent', value = 'enable/disable transparency', allowableValues="true,false", defaultValue="false", paramType = 'query', dataType = 'boolean', required=false),
          @ApiImplicitParam(name = 'id', value = 'ID', paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'filename', value = 'Filename', paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'hist', value = 'Histogram File', paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'ovr', value = 'Overview File', paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'histOp', value = 'Histogram Operation',defaultValue = '',allowableValues="none,auto-minmax,auto-percentile,std-stretch-1,std-stretch-2,std-stretch-3", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'entry', value = 'Image entry id(typically 0)', defaultValue="0", paramType = 'query', dataType = 'integer', required=true),
  ])
  def getThumbnail(/*GetThumbnailCommand cmd*/)
  {
   def ctx = startAsync()
   ctx.start {
     def cmd = new GetThumbnailCommand()

     BindUtil.fixParamNames( GetThumbnailCommand, params )
     bindData( cmd, params )


     def outputStream = null
     try
     {
         def result = imageSpaceService.getThumbnail( cmd )
         outputStream = response.outputStream
         if(result.status != null) response.status        = result.status
         if(result.contentType) response.contentType      = result.contentType
         if(result.buffer?.length) response.contentLength = result.buffer.length
          response.setDateHeader('Expires', System.currentTimeMillis() + 60*60*1000)
         if(outputStream)
         {
          outputStream << result.buffer
         }

     }
     catch(e)
     {
       response.status = HttpStatus.INTERNAL_SERVER_ERROR
       //log.error(e.message)
     }
     finally
     {
       if(outputStream!=null)
       {
          try{
             outputStream.close()
          }
          catch(e)
          {
             //log.debug(e.message)
          }
       }
     }
      ctx.complete()
    }     
  }
}
