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
                httpMethod="GET")
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
          @ApiImplicitParam(name = 'cutBboxXywh', value = 'Cut image box separated by commas: <x>,<y>,<width>,<height>', defaultValue = '',  paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'histOp', value = 'Histogram Operation',defaultValue = '',allowableValues="[none,auto-minmax,auto-percentile,std-stretch-1,std-stretch-2,std-stretch-3]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'outputRadiometry', value = 'Output radiometry', defaultValue = '', allowableValues="[ossim_uint8,ossim_uint11,ossim_uint16,ossim_sint16,ossim_float32,ossim_float64]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'bands', value = 'Bands', defaultValue = '',  paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'resamplerFilter', value = 'Which resampling engine to use', defaultValue = '',  allowableValues= "[nearest-neighbor, bilinear, cubic, gaussian, blackman, bspline, hanning, hamming, hermite, mitchell, quadratic, sinc, magic]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'outputFormat', value = 'Output format', defaultValue = '',  allowableValues= "[image/jpeg,image/png,image/gif,image/tiff]", paramType = 'query', dataType = 'string', required=false),
          @ApiImplicitParam(name = 'keepBands', value = 'Determine if we auto adjust bands or not', defaultValue = "false", paramType = 'query', dataType = 'boolean', required=false),
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
                httpMethod="GET")
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
