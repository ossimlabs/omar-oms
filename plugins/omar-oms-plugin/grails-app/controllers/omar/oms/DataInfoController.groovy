package omar.oms

import omar.core.BindUtil
import io.swagger.annotations.*

@Api(value = "/dataInfo",
     description = "API operations in image space."
)
class DataInfoController {
   def dataInfoService
   static allowedMethods = [index: ["GET"],
                            getInfo: ["GET"]]

   def index()
   {
         forward action: 'getInfo'
   }

  @ApiOperation(value = "Get information from the passed in image file name",
                produces="application/xml",
                httpMethod="GET")
  @ApiImplicitParams([
          @ApiImplicitParam(name = 'filename', value = 'filename', defaultValue = '/data/s3/adhoc/16SEP08110842-P1BS-055998376010_01_P007.TIF', paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'entry', value = 'Entry', defaultValue = '0', paramType = 'query', dataType = 'integer', required=false)
  ])
   def getInfo()
   {
      DataInfoCommand command = new DataInfoCommand()
      def json = request.JSON
      if(json)
      {
         bindData(command, BindUtil.fixParamNames(DataInfoCommand, json))
      }
      else
      {
         bindData(command, BindUtil.fixParamNames(DataInfoCommand, params))
      }
      String result = dataInfoService.getInfo(command)
      if(result)
      {
          render contentType: "application/xml", text: result
      }
       else
      {
        response.status = 400
        render contentType: "text/plain", text: "Unable to get information for ${command.filename}"
      }

   }
}
