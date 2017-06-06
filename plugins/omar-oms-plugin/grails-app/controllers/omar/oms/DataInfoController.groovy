package omar.oms
import omar.core.BindUtil

class DataInfoController {
   def dataInfoService

   def index() 
   { 
         forward action: 'getInfo'
   }
   
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

      render contentType: "application/xml", text: result

   }
}
