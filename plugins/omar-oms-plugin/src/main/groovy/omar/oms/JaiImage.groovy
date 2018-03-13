package omar.oms

import javax.imageio.ImageIO
import javax.imageio.ImageReadParam
import javax.media.jai.ImageLayout
import javax.media.jai.JAI
import javax.media.jai.ParameterBlockJAI
import javax.media.jai.PlanarImage
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Point
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.Raster

/**
 * Created by sbortman on 12/7/15.
 */
class JaiImage
{
  static PlanarImage bufferedToPlanar(BufferedImage bi)
  {
    def planarImage = PlanarImage.wrapRenderedImage(bi)
    planarImage = JAI.create("NULL", planarImage)

    planarImage
  }
  static def selectBandsForRendering(PlanarImage planarImage)
  {
    def modifiedImage

    if(planarImage.sampleModel.numBands >=3)
    {
      modifiedImage = JAI.create("BandSelect", planarImage, [0, 1, 2] as int[])
    }
    else
    {
      modifiedImage = JAI.create("BandSelect", planarImage, [0, 0, 0] as int[])
    }

    modifiedImage
  }
  static def reformatImage(def image, int tileWidth = 256, int tileHeight = 256)
  {
    def imageLayout = new ImageLayout( image )

    imageLayout.setTileWidth( 256 )
    imageLayout.setTileHeight( 256 )

    def map = [( JAI.KEY_IMAGE_LAYOUT ): imageLayout]
    def hints = new RenderingHints( map )
    def formatParams = new ParameterBlockJAI( 'format' )

    formatParams.setSource( 'source0', image )
    image = JAI.create( 'format', formatParams, hints )
    image
  }

  static def readImage(def File file, int rLevel)
  {
    def istream = ImageIO.createImageInputStream( file )
    def reader = ImageIO.getImageReaders( istream )?.next()
    def image

    if ( reader )
    {
      def imageReadParam = new ImageReadParam()

      reader?.input = istream
      image = reader.readAsRenderedImage( rLevel, imageReadParam )
    }

    image
  }

  static def readImageInfo(File file)
  {
    def istream = ImageIO.createImageInputStream( file )
    def reader = ImageIO.getImageReaders( istream )?.next()
    def info = [:]

    if ( reader )
    {
      reader?.input = istream
      info.numImages = reader.getNumImages( true )
      info.minIndex = reader.minIndex

      for ( def z in ( info.minIndex..<info.numImages ) )
      {
        def imageReadParam = new ImageReadParam()
        def image = reader.readAsRenderedImage( z, imageReadParam )

        image = reformatImage( image )

        //println image.class.name

        def properties = ['width', 'height', 'tileWidth', 'tileHeight', 'numXTiles', 'numYTiles'].inject( [:] ) {
          a, b -> a."${b}" = image."${b}"; a
        }

        info."image${z}" = properties
      }
    }

    istream?.close()
    return info
  }

  static def getTile(GetTileCommand cmd)
  {
    def file = cmd.filename as File
    def imageInfo = readImageInfo( file )
    def index = findIndexOffset( imageInfo ) - ( cmd.z )
    def image = reformatImage( readImage( file, index ) )
    def tileImage = getTileAsImage( image, cmd.x, cmd.y )
    def ostream = new ByteArrayOutputStream()

    ImageIO.write( tileImage, cmd.format, ostream )

    [contentType: "image/${cmd.format}", buffer: ostream.toByteArray()]
  }

  static def getTileAsImage(image, x, y)
  {
    def raster = image.getTile( x, y )
    def dataBuffer = raster.getDataBuffer();
    def writableRaster = raster.createWritableRaster( image.sampleModel, dataBuffer, new Point( 0, 0 ) )
    def tileImage = new BufferedImage( image.colorModel, writableRaster, image.colorModel.isAlphaPremultiplied(), null );

    return tileImage
  }

  static def findIndexOffset(def imageInfo, def tileSize = 256)
  {
    def index

    for ( def i = imageInfo.minIndex; i < imageInfo.numImages; i++ )
    {
      def levelInfo = imageInfo["image${i}"]

      if ( levelInfo.width <= tileSize && levelInfo.height <= tileSize )
      {
        index = i
        break
      }
    }

    return index
  }

  static def createThumbnail(def img, Integer w, Integer h, String format="jpeg")
  {
    int srcW = img.width
    int srcH = img.height
    int x = 0
    int y = 0
    int tgtW = w
    int tgtH = h
    def maxSize = Math.max(srcW, srcH);
    def minTgt = Math.min(tgtW,tgtH)
    Double scale = maxSize/minTgt;
    Boolean transparentFlag = format?.toLowerCase() != "jpeg"
    println "FORMAT ===================== ${format}"
    BufferedImage thumbnailImg = new BufferedImage(w, h, transparentFlag?BufferedImage.TYPE_INT_ARGB:BufferedImage.TYPE_INT_RGB);//img.getType());
    //Adjust target
    if(scale >=1)
    {
        tgtW = srcW/scale
        tgtH = srcH/scale
    }
    else
    {
        tgtW = srcW
        tgtH = srcH
    }      
    Graphics2D g = thumbnailImg.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(img, 0, 0, tgtW, tgtH, 0, 0, srcW, srcH, null);
    g.dispose();

    thumbnailImg
  }
  static def createThumbnail(def img, Integer size, String format="jpeg")
  {
    createThumbnail(img, size, size, format);
  }

  static def fileToBufferedImage(File inputFile)
  {
    def image = ImageIO.read(inputFile)

    image
  }
}
