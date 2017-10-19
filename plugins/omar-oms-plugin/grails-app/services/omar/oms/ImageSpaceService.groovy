package omar.oms

import groovy.json.JsonSlurper
import groovy.json.JsonBuilder
import omar.core.HttpStatus
import sun.awt.image.ToolkitImage

import javax.media.jai.PlanarImage
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Point
import java.awt.Transparency
import java.awt.color.ColorSpace
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import java.awt.image.ComponentColorModel
import java.awt.image.DataBuffer
import java.awt.image.DataBufferByte
import java.awt.image.PixelInterleavedSampleModel
import java.awt.image.Raster
import javax.imageio.ImageIO
import javax.media.jai.JAI

import joms.oms.Chipper
import joms.oms.ImageModel
import joms.oms.Info
import joms.oms.Keywordlist

import java.awt.image.RenderedImage

class ImageSpaceService
{
  static transactional = false
  def chipperService

  def getTileOverlay(GetTileCommand cmd)
  {
    def text = "${cmd.z}/${cmd.x}/${cmd.y}"

    BufferedImage image = new BufferedImage( cmd.tileSize, cmd.tileSize, BufferedImage.TYPE_INT_ARGB )
    ByteArrayOutputStream ostream = new ByteArrayOutputStream()
    def g2d = image.createGraphics()
    def font = new Font( "TimesRoman", Font.PLAIN, 18 )
    def bounds = new TextLayout( text, font, g2d.fontRenderContext ).bounds
    String format = cmd.outputFormat
    if(!format) format = "image/png"
    g2d.color = Color.red
    g2d.font = font
    g2d.drawRect( 0, 0, cmd.tileSize, cmd.tileSize )

    // Center Text in tile
    g2d.drawString( text,
        Math.rint( ( cmd.tileSize - bounds.@width ) / 2 ) as int,
        Math.rint( ( cmd.tileSize - bounds.@height ) / 2 ) as int )

    g2d.dispose()


    ImageIO.write( image, format.split("/")[-1], ostream )

    [contentType: format, buffer: ostream.toByteArray()]
  }


  def readImageInfo(String file)
  {
    def info = getImageInfoAsMap( file )
    def data = [numImages: info.number_entries as int]

    def images = []

    for ( def i in ( 0..<data.numImages ) )
    {
      def image = info["image${i}"]

      def entry = [
          entry: image.entry as int,
          numResLevels: image.number_decimation_levels as int,
          height: image.number_lines as int,
          width: image.number_samples as int,
      ]

      def resLevels = []

      for ( def l in ( 0..<entry.numResLevels ) )
      {
        resLevels << [
            resLevel: l,
            width: Math.ceil( entry.width / 2**l ) as int,
            height: Math.ceil( entry.height / 2**l ) as int
        ]
      }
      entry.resLevels = resLevels
      images << entry
    }

    data['images'] = images

    return data
  }

  def getImageInfoAsMap(String file)
  {
    def kwl = new Keywordlist()
    def info = new Info()

    info.getImageInfo( file, true, true, true, true, true, true, kwl )

    def data = [:]

    for ( def i = kwl.iterator; !i.end(); )
    {
      //println "${i.key}: ${i.value}"

      def names = i.key.split( '\\.' )
      def prev = data
      def cur = data

      for ( def name in names[0..<-1] )
      {
        if ( !prev.containsKey( name ) )
        {
          prev[name] = [:]
        }

        cur = prev[name]
        prev = cur
      }

      cur[names[-1]] = i.value.trim()
      i.next()
    }

    kwl.delete()
    info.delete()

    return data
  }
  Boolean fileExists(String connectionString)
  {
    // default to true for protocols other than file and
    // empty
    Boolean result = true
    URI uri = new URI(connectionString)
    String scheme = uri.scheme?.toLowerCase()

    if(!scheme || (scheme=="file"))
    {
      File testFile = new File(connectionString)
      result = testFile.exists();
    }

    result
  }
  Boolean isLocalFile(String connectionString)
  {
    Boolean result = false
    URI uri = new URI(connectionString)
    String scheme = uri.scheme?.toLowerCase()

    if(!scheme || (scheme=="file"))
    {
      result = true
    }

    result
  }
  def getTile(GetTileCommand cmd)
  {

    def imageInfo
    def imageEntry

    def result = [status     : HttpStatus.NOT_FOUND,
                  contentType: "plane/text",
                  buffer     : "Unable to service tile".bytes]

    println "getTile"
    log.info "getTile"

    def startTime = new Date()
    def internalTime = new Date()
    def processingTime
    def timestamp
    JsonBuilder logOutput
    def status = "chipper service returned successfully"

    startTime = System.currentTimeMillis()
    log.info "startTime" + startTime
    internalTime = startTime
    log.info "internalTime" + internalTime

    timestamp = new Date().format("YYYY-MM-DD HH:mm:ss.Ms")

    def indexOffset = findIndexOffset(cmd)
    Boolean canChip = cmd.z < cmd.numResLevels

    if (canChip)
    {
      log.info "timestamp" + timestamp
      Integer rrds = indexOffset - cmd.z
      ChipperCommand chipperCommand = new ChipperCommand()

      chipperCommand.cutBboxXywh = [cmd.x * cmd.tileSize, cmd.y * cmd.tileSize, cmd.tileSize, cmd.tileSize].join(',')
      bbox_midpoint = [ lat: (cmd.y + cmd.tileSize) / 2, lon: (cmd.x + cmd.tileSize) / 2 ]
      chipperCommand.images = [ [file: cmd.filename, entry: cmd.entry]]
      chipperCommand.operation = "chip"
      chipperCommand.scale_2_8_bit = cmd.scale_2_8_bit
      chipperCommand.rrds = rrds
      chipperCommand.histOp = cmd.histOp
      chipperCommand.brightness = cmd.brightness
      chipperCommand.contrast = cmd.contrast
      chipperCommand.sharpenMode = cmd.sharpenMode
      chipperCommand.resamplerFilter = cmd.resamplerFilter
      if(cmd.transparent == null) chipperCommand.transparent = true
      else chipperCommand.transparent = cmd.transparent
      if(cmd.outputFormat) chipperCommand.outputFormat = cmd.outputFormat
      if (cmd.bands)
      {
        chipperCommand.bands = cmd.bands
      }

      if ( cmd.histCenterTile ) {
        chipperCommand.histCenter = cmd.histCenterTile
      }
      try{
        result = chipperService.getTile(chipperCommand)
      }
      catch(e)
      {
        result = [status     : HttpStatus.INTERNAL_SERVER_ERROR,
                  contentType: "image/${hints.type}",
                  buffer     : "${e}".bytes
                 ]
        status = "internal server error"
        log.info "status" + status

      }
    }
    else
    {
        result = [status     : HttpStatus.INTERNAL_SERVER_ERROR,
                  contentType: "plain/text",
                  buffer     : "Not Enough resolution levels to satisfy request".bytes
                 ]
        status = "not enough resolution levels to satisfy request"
      log.info "status" + status

    }

    processingTime = internalTime - startTime

    log.info "before json"

    logOutput = new JsonBuilder(timestamp: timestamp, status: status, processingTime: processingTime,
            location: bbox_midpoint, resultsize: result.size())

    log.info logOutput.toString()

    result
  }

  def findIndexOffset(def cmd)
  {
    // GP: Currently this will not work correctly because the calling GUI
    // has no way of knowing the R-Levels to use.  It currently assumes that
    // a complete tile fits at the highest resolution but the image does
    // not guarantee that it has overviews beyond that.
    //
    // for now we will always return a full range and will ignore the resolutions
    // predefined by the image
    //
    Integer index = 0;
    Integer maxValue = Math.max(cmd.width, cmd.height)

    if((maxValue > 0)&&(cmd.tileSize > 0))
    {
      while(maxValue > cmd.tileSize)
      {
        maxValue /= 2

        ++index
      }
    }
    /*
    def index

    for ( def i = 0; i < image.numResLevels; i++ )
    {
      def levelInfo = image.resLevels[i]

      if ( levelInfo.width <= tileSize && levelInfo.height <= tileSize )
      {
        index = i
        break
      }
    }
    */
    return index
  }

  def computeUpIsUp(String filename, Integer entryId)
  {
    Double upIsUp = 0.0

    def imageSpaceModel = new ImageModel()
    if ( imageSpaceModel.setModelFromFile( filename, entryId as Integer ) )
    {
      upIsUp = imageSpaceModel.upIsUpRotation();
      imageSpaceModel.destroy()
      imageSpaceModel.delete()
    }

    return upIsUp
  }

  def computeNorthIsUp(String filename, Integer entryId)
  {
    Double northIsUp = 0.0

    def imageSpaceModel = new ImageModel()
    if ( imageSpaceModel.setModelFromFile( filename, entryId as Integer ) )
    {
      northIsUp = imageSpaceModel.northIsUpRotation();
      imageSpaceModel.destroy()
      imageSpaceModel.delete()
    }

    return northIsUp
  }

  def getThumbnail(GetThumbnailCommand cmd)
  {
    def result = [status:HttpStatus.OK, buffer:null]
    // Check to see if file exists
    if ( ! fileExists(cmd.filename?.toString() ) )
    {
      def image = getDefaultImage(cmd.size, cmd.size)
      def ostream = new ByteArrayOutputStream()
      def format = cmd?.outputFormat?.split('/')?.last()

      ImageIO.write(image, format, ostream)

      result = [status:HttpStatus.OK, contentType: "image/${format}", buffer: ostream.toByteArray()]
    }
    else
    {
      ChipperCommand chipperCommand = new ChipperCommand()
      chipperCommand.histOp = cmd.histOp
      chipperCommand.images = [ [file:cmd.filename, entry:cmd.entry?:0]]
      chipperCommand.operation = "chip"
      chipperCommand.outputRadiometry = "ossim_uint8"
      chipperCommand.padThumbnail = true
      chipperCommand.threeBandOut = true
      chipperCommand.thumbnailResolution = cmd.size?:64
      chipperCommand.outputFormat = cmd.outputFormat?:"image/jpeg"
      if(cmd.transparent!=null) chipperCommand.transparent = cmd.transparent
      try{
        result = chipperService.getTile(chipperCommand)
      }
      catch(e)
      {
        result = [status     : HttpStatus.INTERNAL_SERVER_ERROR,
                  contentType: "image/${hints.type}",
                  buffer     : "${e}".bytes
                 ]
      }
    }
    result
  }

  def getDefaultImage(int width, int height)
  {
    def image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    def g2d = image.createGraphics()

    g2d.paint = Color.red
    g2d.stroke = new BasicStroke(3)
    g2d.drawRect(0, 0, width, height)
    g2d.drawLine(0, 0, width, height)
    g2d.drawLine(width, 0, 0,  height)
    g2d.dispose()

    image
  }
}
