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
                httpMethod="POST,GET")
  @ApiImplicitParams([
          @ApiImplicitParam(name = 'filename', value = 'filename', defaultValue = '', paramType = 'query', dataType = 'string', required=true),
          @ApiImplicitParam(name = 'entry', value = 'Entry', defaultValue = '0', paramType = 'query', dataType = 'int', required=false)
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
      if(!result)
      {
        response.status = 400
        render contentType: "text/plain", text: "Unable to get information for ${command.filename}"
      }
      else
      {
        render contentType: "application/xml", text: result
      }

   }
}
