package omar.oms

import omar.core.BindUtil
import omar.core.HttpStatus
import com.github.rahulsom.swaggydoc.*
import com.wordnik.swagger.annotations.*
@Api(value = "Chipper",
        description = "API operations for Chipper"
)
class ChipperController {
   def chipperService
   def index()
   {
      def wmsParams = params - params.subMap( [ 'controller', 'format' ] )
      def json = request.JSON
      def operation = wmsParams.find { it.key.equalsIgnoreCase( 'operation' ) }

      if(!operation && json)
      {
         operation = json.operation
      }
      //println wmsParams

      switch ( operation?.toLowerCase() )
      {
      case "chip":
         forward action: 'chip'
         break
      case "ortho":
         forward action: 'ortho'
         break
      default:
         render ""
         break
      }
   }

  @ApiOperation(value = "Get image space chip from the passed in image file name", 
                produces="image/png,image/jpeg,image/gif,image/tiff,text/plain",
                httpMethod="GET",
                notes = """
    <ul>
        <li>
            <b>images</b><p/>
            Is an array of objects of the form images[0].file = <b>filename</b> and an optional images[0].entry = <b>entry</b>
        </li>
        <br>
        <li>
            <b>operation</b><p/>
            Is fixed to the value 'chip'
        </li>
        <br>
        <li>
        <b>brightness</b><p/>
             Allows one to control the brightness of the image. This is expressed as a normalized value between -1 and 1. 
        </li>
        <br>
        <li>
        <b>contrast</b><p/>
             Allows one to control the contrast of an image.  This is a multiplier.
        </li>
        <br>
        <li>
        <b>sharpenMode</b><p/>
             Sharpen mode can take on the values none, light, or heavy.
        </li>
        <br>
        <li>
        <b>thumbnailResolution</b><p/>
             Specify the resolution of the thumbnail.  The valie is assumed square so only one
             dimension is needed.  Setting the value to 512 will output to a 512x512 image.
        </li>
        <br>
        <li>
        <b>cutBboxXywh</b><p/>
        This is used to cut in image space.  It takes values <b>x</b>,<b>y</b>,<b>width</b>,<b>height</b> where x and y
        are defined as the upper left corner and the width extends positive x to the right and height extends positive y down. 
        </li>
        <br>
        <li>
        <b>histOp</b><p/>
        Histogram operations used can be none, auto-minmax, auto-percentile, std-stretch-1, std-stretch-2, std-stretch-3 
        </li>
        <br>
        <li>
        <b>outputRadiometry</b><p/>
        Defines the output radiometry.
        </li>
        <br>
        <li>
        <b>bands</b><p/>
        This defines an output band list that is a comma separated value of integers with band index 1's based.  So a value of 
        1,1,1 will output three bands with all being the first band of the input image.  If you have a 3 band input image you can reverse them by doing 3,2,1.  
        Commas are required.
        </li>
        <br>
        <li>
        <b>resamplerFilter</b><p/>
        We have exposed different filtering capabilities.  Values can be any of the following
        nearest-neighbor, bilinear, cubic, gaussian, blackman, 
        bspline, hanning, hamming, hermite, mitchell, quadratic, sinc, magic
        </li>
        <br>
        <li>
        <b>outputFormat</b><p/>
        Output format can be 'image/jpeg', 'image/png', 'image/gif', 'image/tiff'
        </li>
        <br>
        <li>
        <b>keepBands</b><p/>
        This is a flag that will allow the bands to be kept as specified.  So if you wnat a 10 band output then you must void setUp() 
        the <b>keepBands</b> to true and then specify the <b>outputFormat</b> to be image/tiff
        </li>
        <br>
       <li>
        <b>padThumbnail</b><p/>
        Allows one to enable padding of thumbnail products so it matches the size.
        </li>
        <br>
        <li>
        <b>transparent</b><p/>
        Enables transparent output if the <b>outputFormat</b> supports it
        </li>
        <br>
        </ul>
    """)
   @ApiImplicitParams([
          @ApiImplicitParam(name = 'images[0].file', value = 'filename', defaultValue = '', paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'images[0].entry', value = 'Image entry in the file', defaultValue = '', paramType = 'query', dataType = 'int', required=false),
          @ApiImplicitParam(name = 'operation', value = '', defaultValue = 'none', allowableValues= "[ortho]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'brightness', value = 'Brightness Operation',defaultValue="0.0", paramType = 'query', dataType = 'float', required=false),
          @ApiImplicitParam(name = 'contrast', value = 'Contrast Operation',defaultValue="1.0",  paramType = 'query', dataType = 'float', required=false),
          @ApiImplicitParam(name = 'sharpenMode', value = '', defaultValue = '', allowableValues="[light,heavy]",paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'thumbnailResolution', value = '', defaultValue = '',  paramType = 'query', dataType = 'int', required=false),
          @ApiImplicitParam(name = 'cutBboxXywh', value = 'Cut image box separated by commas: <x>,<y>,<width>,<height>', defaultValue = '',  paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'histOp', value = 'Histogram Operation',defaultValue = '',allowableValues="[none,auto-minmax,auto-percentile,std-stretch-1,std-stretch-2,std-stretch-3]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'outputRadiometry', value = 'Output radiometry', defaultValue = '', allowableValues="[ossim_uint8,ossim_uint11,ossim_uint16,ossim_sint16,ossim_float32,ossim_float64]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'bands', value = 'Bands', defaultValue = '',  paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'resamplerFilter', value = 'Which resampling engine to use', defaultValue = '',  allowableValues= "[nearest-neighbor, bilinear, cubic, gaussian, blackman, bspline, hanning, hamming, hermite, mitchell, quadratic, sinc, magic]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'outputFormat', value = 'Output format', defaultValue = '',  allowableValues= "[image/jpeg,image/png,image/gif,image/tiff]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'keepBands', value = 'Determine if we auto adjust bands or not', defaultValue = "false", paramType = 'query', dataType = 'boolean', required=false),
          @ApiImplicitParam(name = 'padThumbnail', value = 'Add padding to the output to make it square', defaultValue = "false", paramType = 'query', dataType = 'boolean', required=false),
          @ApiImplicitParam(name = 'transparent', value = 'Enable transparent if the outputFormat supports it', defaultValue = "true", paramType = 'query', dataType = 'boolean', required=false),
   ])
   def chip(){
      ChipperCommand command = new ChipperCommand()
      def json = request.JSON

      if(json)
      {
         json.operation="chip"
         bindData(command, BindUtil.fixParamNames(ChipperCommand, json))
      }
      else
      {
         params.operation="chip"
         bindData(command, BindUtil.fixParamNames(ChipperCommand, params))
      }
      def result = chipperService.getTile(command)
      response.contentType = result.contentType
      if(result.status != null) response.status        = result.status
      if(result.contentType) response.contentType      = result.contentType
      if(result.buffer?.length) response.contentLength = result.buffer.length

       def outputStream = null
       try{
          outputStream = response.getOutputStream()
          outputStream << result.buffer
       }
       catch(e)
       {
          log.error(e.toString())
       }
       finally{
          if(outputStream!=null)
          {
             try{
                outputStream.close()
             }
             catch(e)
             {
                log.debug(e.toString())
             }
          }
       }
   }

  @ApiOperation(value = "Get ortho chip from the passed in image file name", 
                produces="image/png,image/jpeg,image/gif,image/tiff,text/plain",
                httpMethod="GET",
                notes = """
    <ul>
        <li>
            <b>images</b><p/>
            Is an array of objects of the form images[0].file = <b>filename</b> and an optional images[0].entry = <b>entry</b>
        </li>
        <br>
        <li>
            <b>operation</b><p/>
            Is fixed to the value ortho
        </li>
        <br>
        <li>
        <b>brightness</b><p/>
             Allows one to control the brightness of the image. This is expressed as a normalized value between -1 and 1. 
        </li>
        <br>
        <li>
        <b>contrast</b><p/>
             Allows one to control the contrast of an image.  This is a multiplier.
        </li>
        <br>
        <li>
        <b>sharpenMode</b><p/>
             Sharpen mode can take on the values none, light, or heavy.
        </li>
        <br>
        <li>
        <b>thumbnailResolution</b><p/>
             Specify the resolution of the thumbnail.  The valie is assumed square so only one
             dimension is needed.  Setting the value to 512 will output to a 512x512 image.
        </li>
        <br>
        <li>
        <b>cutWidth</b><p/>
        This is usually specified in conjuction with a geo spatial cut bos such as <b>cutWmsBbox</b> 
        where the cutWidth is specified in pixels
        </li>
        <br>
        <li>
        <b>cutHeight</b><p/>
        This is usually specified in conjuction with a geo spatial cut bos such as <b>cutWmsBbox</b> 
        where the cutHeight is specified in pixels
        </li>
        <br>
        <li>
        <b>cutWmsBbox</b><p/>
        Is a comma separated list of floating point values in the form of
        <b>minLon</b>,<b>minLat</b>,<b>maxLon</b>,<b>maxLat</b> and the values depend on the SRS projection used
        If its 3857 then it's in meters <b>minX</b>,<b>minY</b>,<b>maxX</b>,<b>maxY</b>
        </li>
        <br>
        <li>
        <b>histOp</b><p/>
        Histogram operations used can be none, auto-minmax, auto-percentile, std-stretch-1, std-stretch-2, or std-stretch-3 
        </li>
        <br>
        <li>
        <b>srs</b><p/>
        This is the SRS code.  This can be EPSG:4326 (Geographic) EPSG:3857 (Global Mercator/Google mercator)
        </li>
        <br>
        <li>
        <b>outputRadiometry</b><p/>
        Defines the output radiometry.
        </li>
        <br>
        <li>
        <b>bands</b><p/>
        This defines an output band list that is a comma separated value of integers with band index 1's based.  So a value of 
        1,1,1 will output three bands with all being the first band of the input image.  If you have a 3 band input image you can reverse them by doing 3,2,1.  
        Commas are required.
        </li>
        <br>
        <li>
        <b>resamplerFilter</b><p/>
        We have exposed different filtering capabilities.  Values can be any of the following
        nearest-neighbor, bilinear, cubic, gaussian, blackman, 
        bspline, hanning, hamming, hermite, mitchell, quadratic, sinc, magic
        </li>
        <br>
        <li>
        <b>outputFormat</b><p/>
        Output format can be image/jpeg, image/png, image/gif, or image/tiff
        </li>
        <br>
        <li>
        <b>keepBands</b><p/>
        This is a flag that will allow the bands to be kept as specified.  So if you wnat a 10 band output then you must void setUp() 
        the <b>keepBands</b> to true and then specify the <b>outputFormat</b> to be image/tiff
        </li>
        <br>
        <li>
        <b>padThumbnail</b><p/>
        Allows one to enable padding of thumbnail products so it matches the size.
        </li>
        <br>
        <li>
        <b>transparent</b><p/>
        Enables transparent output if the <b>outputFormat</b> supports it
        </li>
        <br>
        </ul>
    """)
   @ApiImplicitParams([
          @ApiImplicitParam(name = 'images[0].file', value = 'filename', defaultValue = '', paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'images[0].entry', value = 'Image entry in the file', defaultValue = '', paramType = 'query', dataType = 'int', required=false),
          @ApiImplicitParam(name = 'operation', value = '', defaultValue = 'none', allowableValues= "[ortho]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'brightness', value = 'Brightness Operation',defaultValue="0.0", paramType = 'query', dataType = 'float', required=false),
          @ApiImplicitParam(name = 'contrast', value = 'Contrast Operation',defaultValue="1.0",  paramType = 'query', dataType = 'float', required=false),
          @ApiImplicitParam(name = 'sharpenMode', value = '', defaultValue = '', allowableValues="[light,heavy]",paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'thumbnailResolution', value = '', defaultValue = '',  paramType = 'query', dataType = 'int', required=false),
          @ApiImplicitParam(name = 'cutWidth', value = 'Cut width in pixels', defaultValue = '',  paramType = 'query', dataType = 'int', required=false),
          @ApiImplicitParam(name = 'cutHeight', value = 'Cut height in pixels', defaultValue = '',  paramType = 'query', dataType = 'int', required=false),
          @ApiImplicitParam(name = 'cutWmsBbox', value = 'Cut wms bbox format', defaultValue = '',  paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'histOp', value = 'Histogram Operation',defaultValue = '',allowableValues="[none,auto-minmax,auto-percentile,std-stretch-1,std-stretch-2,std-stretch-3]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'srs', value = 'srs', defaultValue = '',  allowableValues="[EPSG:4326, EPSG:3857]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'outputRadiometry', value = 'Output radiometry', defaultValue = '', allowableValues="[ossim_uint8,ossim_uint11,ossim_uint16,ossim_sint16,ossim_float32,ossim_float64]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'bands', value = 'Bands', defaultValue = '',  paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'resamplerFilter', value = 'Which resampling engine to use', defaultValue = '',  allowableValues= "[nearest-neighbor, bilinear, cubic, gaussian, blackman, bspline, hanning, hamming, hermite, mitchell, quadratic, sinc, magic]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'outputFormat', value = 'Output format', defaultValue = '',  allowableValues= "[image/jpeg,image/png,image/gif,image/tiff]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'keepBands', value = 'Determine if we auto adjust bands or not', defaultValue = "false", paramType = 'query', dataType = 'boolean', required=false),
          @ApiImplicitParam(name = 'padThumbnail', value = 'Add padding to the output to make it square', defaultValue = "false", paramType = 'query', dataType = 'boolean', required=false),
          @ApiImplicitParam(name = 'transparent', value = 'Enable transparent if the outputFormat supports it', defaultValue = "true", paramType = 'query', dataType = 'boolean', required=false),
   ])
   def ortho(){
      ChipperCommand command = new ChipperCommand()
      def json = request.JSON

      if(json)
      {
         json.operation="ortho"
         bindData(command, BindUtil.fixParamNames(ChipperCommand, json))
      }
      else
      {
         params.operation="ortho"
         bindData(command, BindUtil.fixParamNames(ChipperCommand, params))
      }
      def result = chipperService.getTile(command)
      response.contentType = result.contentType
      if(result.status != null) response.status        = result.status
      if(result.contentType) response.contentType      = result.contentType
      if(result.buffer?.length) response.contentLength = result.buffer.length

       def outputStream = null
       try{
          outputStream = response.getOutputStream()
          outputStream << result.buffer
       }
       catch(e)
       {
          log.error(e.toString())
       }
       finally{
          if(outputStream!=null)
          {
             try{
                outputStream.close()
             }
             catch(e)
             {
                log.debug(e.toString())
             }
          }
       }
   }
}
