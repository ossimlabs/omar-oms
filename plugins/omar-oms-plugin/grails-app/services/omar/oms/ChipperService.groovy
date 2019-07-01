package omar.oms

// import grails.transaction.Transactional
import omar.core.HttpStatus
import javax.imageio.ImageIO

import org.springframework.util.FastByteArrayOutputStream

class ChipperService {
    static transactional = false

    def getTile(ChipperCommand cmd) {
      HashMap chipperOptions = cmd?.toChipperOptions()
      HashMap result = [:]
      HashMap chipperResult = [:]
      String outputFormat = cmd.outputFormat?:"image/png"
      def hints = [type:outputFormat]

      if( cmd.validate() ) {
         if ( !new File( cmd.images[ 0 ].file ).exists() ) {
            chipperResult.status = HttpStatus.BAD_REQUEST
            chipperResult.statusMessage = "File not found."
            chipperResult.contentType = "text/plain"
         }
         else {
            if( cmd.transparent != null ) {
                hints.transparent = cmd.transparent
            }
            if( cmd.keepBands ) {
                hints.keepBands = cmd.keepBands
            }

            try {
                chipperResult             = ChipperUtil.runChipper( chipperOptions )
                chipperResult.image       = ChipperUtil.chipperResultToImage(chipperResult, hints)

                chipperResult.status      = HttpStatus.OK
                chipperResult.message     = ""
                chipperResult.contentType = "image/${outputFormat?.split("/")[-1]}"
                chipperResult.format      = outputFormat?.split("/")[-1]

                if(!chipperResult.image) {
                    chipperResult.status        = HttpStatus.BAD_REQUEST
                    chipperResult.statusMessage = "Unable to create an image."
                    chipperResult.contentType   = "text/plain"
                }
            }
            catch( e ) {
                chipperResult.status        = HttpStatus.BAD_REQUEST
                chipperResult.statusMessage = e.toString()
                chipperResult.contentType   = "text/plain"
                chipperResult.image = null
            }
        }
     }
      else {
        chipperResult.statusMessage = "Parameter values are invalid. Please check the paramter format"
        chipperResult.status        = HttpStatus.BAD_REQUEST
        chipperResult.contentType   = "text/plain"
      }

      result.status      = chipperResult.status
      result.contentType = chipperResult.contentType

      if(chipperResult.status == HttpStatus.OK)
      {
        if(chipperResult.image)
        {
          int bufferSize = ( chipperResult.format == 'jpeg') ? ChipperUtil.DEFAULT_JPEG_SIZE : ChipperUtil.DEFAULT_PNG_SIZE
          def ostream = new FastByteArrayOutputStream( bufferSize )

          try
          {
            ImageIO.write(chipperResult.image, chipperResult.format, ostream)
            result.buffer = ostream.toByteArrayUnsafe()
          }
          catch(e)
          {
            result.status = HttpStatus.INTERNAL_SERVER_ERROR
            result.contentType   = "text/plain"
            result.buffer = "${e}".bytes
          }
        }
      }
      else
      {
        if(chipperResult.statusMessage)
        {
          result.buffer = chipperResult.statusMessage.bytes
        }
      }

      result
    }

}
