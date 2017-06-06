package omar.oms

import omar.core.BindUtil
import omar.core.HttpStatus
import javax.imageio.ImageIO
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
      response.status = result.status
      if(result.status == HttpStatus.OK)
      {
         def outputStream = null
         try{
            outputStream = response.getOutputStream()
            ImageIO.write(result.image, result.format, response.getOutputStream())
         }
         catch(e)
         {
            log.debug(e.toString())
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
      else 
      {
         render result.statusMessage
      }
   }
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
      response.status = result.status
      if(result.status == HttpStatus.OK)
      {
         def outputStream = null
         try{
            outputStream = response.getOutputStream()
            ImageIO.write(result.image, result.format, response.getOutputStream())
         }
         catch(e)
         {
            log.debug(e.toString())
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
      else 
      {
         render result.statusMessage
      }
   }
}
