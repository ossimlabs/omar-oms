package omar.oms

import grails.transaction.Transactional
import omar.core.HttpStatus

@Transactional
class ChipperService {

    def getTile(ChipperCommand cmd) {
      HashMap chipperOptions = cmd?.toChipperOptions()
    
      HashMap chipperResult = [:]
      String outputFormat = cmd.format?:"image/png"
      def hints = [type:outputFormat]
      if(cmd.transparent != null)
      {
         hints.transparent = cmd.transparent
      }
      try{
         chipperResult = ChipperUtil.runChipper( chipperOptions )

         chipperResult.image       = ChipperUtil.chipperResultToImage(chipperResult, hints)
         chipperResult.status      = HttpStatus.OK
         chipperResult.message     = ""
         chipperResult.contentType = "image/${outputFormat?.split("/")[-1]}"
         chipperResult.format      = outputFormat?.split("/")[-1]

         println chipperResult.image
         if(!chipperResult.image)
         {
            chipperResult.status        = HttpStatus.BAD_REQUEST
            chipperResult.statusMessage = "Unable to crate an image."
            chipperResult.contentType   = "text/plain"
         }
      }
      catch(e)
      {
         chipperResult.status        = HttpStatus.BAD_REQUEST
         chipperResult.statusMessage = e.toString()
         chipperResult.contentType   = "text/plain"
      }
      chipperResult
    }

}
