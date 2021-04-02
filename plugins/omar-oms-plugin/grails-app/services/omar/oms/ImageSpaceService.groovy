package omar.oms

import groovy.json.JsonBuilder
import omar.core.DateUtil
import omar.core.HttpStatus

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

import joms.oms.ImageModel
import joms.oms.Info
import joms.oms.Keywordlist

import groovy.json.JsonSlurper
import groovy.transform.Memoized

import org.springframework.util.FastByteArrayOutputStream

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ImageSpaceService
{

  final Logger logger = LoggerFactory.getLogger("myLogger")

  static transactional = false
  def chipperService
  def grailsApplication

  @Memoized
  def getTileOverlay(GetTileCommand cmd)
  {
    def text = "${cmd.z}/${cmd.x}/${cmd.y}"

    def requestType = "GET"
    def requestMethod = "getTileOverlay"
    def responseTime
    Date startTime = new Date()
    Date endTime
    JsonBuilder logOutput
    def buffer

    BufferedImage image = new BufferedImage( cmd.tileSize, cmd.tileSize, BufferedImage.TYPE_INT_ARGB )
    FastByteArrayOutputStream ostream = new FastByteArrayOutputStream( ChipperUtil.DEFAULT_JPEG_SIZE  )

    Graphics2D g2d = image.createGraphics()
    Font font = new Font( "TimesRoman", Font.PLAIN, 18 )
    FontRenderContext frc = g2d.getFontRenderContext()
    TextLayout layout = new TextLayout( text, font, frc )
    Rectangle2D bounds = layout.getBounds()
    String format = cmd.outputFormat
    if(!format) format = "image/png"
    g2d.color = Color.red
    g2d.font = font
    g2d.drawRect( 0, 0, cmd.tileSize, cmd.tileSize )

    // Center Text in tile
    g2d.drawString( text,
        Math.rint( ( cmd.tileSize - bounds.width ) / 2 ) as int,
        Math.rint( ( cmd.tileSize - bounds.height ) / 2 ) as int )
    g2d.dispose()

    ImageIO.write( image, format.split("/")[-1], ostream )
    endTime = new Date()

    responseTime = Math.abs(startTime.getTime() - endTime.getTime())

    buffer = ostream.toByteArrayUnsafe()
    logOutput = new JsonBuilder(timestamp: DateUtil.formatUTC(startTime), requestType: requestType,
            requestMethod: requestMethod, httpStatus: 200, endTime: DateUtil.formatUTC(endTime),
            responseTime: responseTime, responseSize: buffer.length, filename: cmd.filename)

    log.info logOutput.toString()

    [contentType: format, buffer: buffer]
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
    println "getTile: inside function"
    def result = [status     : HttpStatus.NOT_FOUND,
                  contentType: "text/plain",
                  buffer     : "Unable to service tile".bytes]


    def requestType = "GET"
    def requestMethod = "getTile"
    def responseTime
    Date startTime = new Date()
    Date endTime
    JsonBuilder logOutput
    def indexOffset = findIndexOffset(cmd)
    Boolean canChip = cmd.z <= cmd.numResLevels
    println "getTile: variables set"
    if (canChip)
    {
      println "getTile: canChip"
      HashMap chipperFileOptions = [file: cmd.filename, entry: cmd.entry]
      Integer rrds = indexOffset - cmd.z
      ChipperCommand chipperCommand = new ChipperCommand()
      if(cmd.hist) chipperFileOptions.hist = cmd.hist
      if(cmd.ovr) chipperFileOptions.ovr = cmd.ovr
      if(cmd.geom) chipperFileOptions.geom = cmd.geom
      chipperCommand.cutBboxXywh = [cmd.x * cmd.tileSize, cmd.y * cmd.tileSize, cmd.tileSize, cmd.tileSize].join(',')
      chipperCommand.images = [ chipperFileOptions]
      chipperCommand.operation = "chip"
      chipperCommand.scale_2_8_bit = cmd.scale_2_8_bit
      chipperCommand.rrds = rrds
      chipperCommand.histOp = cmd.histOp
      chipperCommand.brightness = cmd.brightness
      chipperCommand.contrast = cmd.contrast
      chipperCommand.sharpenMode = cmd.sharpenMode
      chipperCommand.resamplerFilter = cmd.resamplerFilter
      chipperCommand.nullPixelFlip = cmd.nullPixelFlip
      if(cmd.transparent == null) {
        chipperCommand.transparent = true
      }else {
        chipperCommand.transparent = cmd.transparent
      }
      if(cmd.outputFormat) chipperCommand.outputFormat = cmd.outputFormat
      if (cmd.bands)
      {
        chipperCommand.bands = cmd.bands
      }

      if ( cmd.histCenterTile ) {
        chipperCommand.histCenter = cmd.histCenterTile
      }
      println "getTile: chipper variables set"
      try{
        result = chipperService.getTile(chipperCommand)
        println "getTile: chipperService succeeded"
      }
      catch(e)
      {
        println "getTile: chipperService failed! ${e}"
        result = [status     : HttpStatus.INTERNAL_SERVER_ERROR,
                  contentType: "text/plain",
                  buffer     : "${e}".bytes
                 ]

        endTime = new Date()
        responseTime = Math.abs(startTime.getTime() - endTime.getTime())
        logOutput = new JsonBuilder(timestamp: DateUtil.formatUTC(startTime), requestType: requestType,
                requestMethod: requestMethod, httpStatus: result.status, endTime: DateUtil.formatUTC(endTime),
                responseTime: responseTime, responseSize: result.buffer.length, filename: cmd.filename)

        log.info logOutput.toString()

      }
    }
    else
    {
      println "getTile: not enough resolution levels"
        result = [status     : HttpStatus.INTERNAL_SERVER_ERROR,
                  contentType: "text/plain",
                  buffer     : "Not Enough resolution levels to satisfy request".bytes
                 ]

    }

    endTime = new Date()

    responseTime = Math.abs(startTime.getTime() - endTime.getTime())

    logOutput = new JsonBuilder(timestamp: DateUtil.formatUTC(startTime), requestType: requestType,
            requestMethod: requestMethod, httpStatus: result.status, endTime: DateUtil.formatUTC(endTime),
            responseTime: responseTime, responseSize: result.buffer.length, filename: cmd.filename)

    log.info logOutput.toString()

    println "getTile: that's all"
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

  @Memoized
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

  @Memoized
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

  @Memoized
  def getRasterFiles(String id)
  {
    def result = []
    String getRasterFilesUrl = grailsApplication.config?.omar?.oms?.getRasterFilesUrl;
    try{

      if(getRasterFilesUrl)
      {
        Boolean addQuestion = !getRasterFilesUrl?.endsWith("?")
        def slurper = new JsonSlurper()
        if(id)
        {
          if(addQuestion)
          {
            getRasterFilesUrl="${getRasterFilesUrl}?"
            URL url = new URL("${getRasterFilesUrl}id=${id}".toString())
            String txt = url.text
            def obj = slurper.parseText(txt)
            if(obj)
            {
                obj.results.each{record->
                    result << [name:record.name, type:record.type]
                }
            }
          }
        }
      }
    }
    catch(e)
    {
      //logger.error(e.toString(), e)
      result = []
    }

    result
  }

  @Memoized
  def getThumbnail(GetThumbnailCommand cmd)
  {
    def result = [status:HttpStatus.OK, buffer:null]

    def requestType = "GET"
    def requestMethod = "getThumbnail"
    def responseTime
    String format = cmd.outputFormat?cmd?.outputFormat?.split('/')?.last():"jpeg"

    Date startTime = new Date()
    Date endTime
    JsonBuilder logOutput
    String thumbnailOverride
    def associatedFiles = getRasterFiles(cmd.id)
    if(associatedFiles)
    {
      associatedFiles.each{
        switch(it.type?.toLowerCase())
        {
        case "histogram":
          cmd.hist = it.name
          break
        case "overview":
          cmd.ovr = it.name
          break
        case "main":
          cmd.filename = it.name
          break
        case "geom":
          cmd.geom = it.name
          break
        case "thumbnail":
          thumbnailOverride = it.name
          break
        default:
          break
        }
      }
    }
    if(thumbnailOverride&&fileExists(thumbnailOverride))
    {
      File inputImage = new File(thumbnailOverride)
      def bufImg = JaiImage.fileToBufferedImage(inputImage)
      def image = JaiImage.createThumbnail(bufImg, cmd.size, format)

      int bufferSize = ( format == 'jpeg') ? ChipperUtil.DEFAULT_JPEG_SIZE : ChipperUtil.DEFAULT_PNG_SIZE
      def ostream = new FastByteArrayOutputStream(bufferSize)

      ImageIO.write(image, format, ostream)

      result = [status:HttpStatus.OK, contentType: "image/${format}", buffer: ostream.toByteArrayUnsafe()]
    }
    else if ( ! fileExists(cmd.filename?.toString() ) )
    {
      def image = getDefaultImage(cmd.size, cmd.size)
      int bufferSize = ( format == 'jpeg' ) ? ChipperUtil.DEFAULT_JPEG_SIZE : ChipperUtil.DEFAULT_PNG_SIZE
      def ostream = new FastByteArrayOutputStream(bufferSize)

      ImageIO.write(image, format, ostream)

      result = [status:HttpStatus.OK, contentType: "image/${format}", buffer: ostream.toByteArrayUnsafe()]
    }
    else
    {
      HashMap chipperFileOptions = [file:cmd.filename, entry:cmd.entry?:0]
      ChipperCommand chipperCommand = new ChipperCommand()
      if(cmd.hist)  chipperFileOptions.hist = cmd.hist
      if(cmd.ovr)   chipperFileOptions.ovr = cmd.ovr
      if(cmd.geom)  chipperFileOptions.geom = cmd.geom
      chipperCommand.histOp = cmd.histOp
      chipperCommand.images = [ chipperFileOptions ]
      chipperCommand.operation = "chip"
      chipperCommand.outputRadiometry = "ossim_uint8"
      chipperCommand.padThumbnail = cmd.padThumbnail?:false
      chipperCommand.threeBandOut = true
      chipperCommand.nullPixelFlip = cmd.nullPixelFlip
      chipperCommand.padThumbnail = cmd.padThumbnail
      chipperCommand.thumbnailResolution = cmd.size?:64
      chipperCommand.transparent = cmd.transparent?:false
      chipperCommand.outputFormat = cmd.outputFormat?:"image/png"
      if(cmd.transparent!=null) chipperCommand.transparent = cmd.transparent
      if(chipperCommand.outputFormat.toLowerCase().contains("jpeg"))
      {
        chipperCommand.transparent = false
      }
      try{
        result = chipperService.getTile(chipperCommand)
      }
      catch(e)
      {
        result = [status     : HttpStatus.INTERNAL_SERVER_ERROR,
                  contentType: "text/plain",
                  buffer     : "${e}".bytes
                 ]

        endTime = new Date()

        responseTime = Math.abs(startTime.getTime() - endTime.getTime())

        logOutput = new JsonBuilder(timestamp: DateUtil.formatUTC(startTime), requestType: requestType,
                requestMethod: requestMethod, httpStatus: result.status, endTime: DateUtil.formatUTC(endTime),
                responseTime: responseTime, responseSize: result.buffer.length, filename: cmd.filename)

        log.info logOutput.toString()
      }
    }

    endTime = new Date()

    responseTime = Math.abs(startTime.getTime() - endTime.getTime())

    logOutput = new JsonBuilder(timestamp: DateUtil.formatUTC(startTime), requestType: requestType,
            requestMethod: requestMethod, httpStatus: result.status, endTime: DateUtil.formatUTC(endTime),
            responseTime: responseTime, responseSize: result.buffer.length, filename: cmd.filename)

    log.info logOutput.toString()

    result
  }

  @Memoized
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
