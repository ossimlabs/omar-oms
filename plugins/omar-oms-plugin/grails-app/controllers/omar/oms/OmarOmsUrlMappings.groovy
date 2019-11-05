package omar.oms

class OmarOmsUrlMappings
{

  static mappings = {

    "/imageSpace/getTile"( controller: 'imageSpace', action: 'index' )
    "/imageSpace/getTile"( controller: 'imageSpace', action: 'getTile' )
    "/imageSpace/getTileOverlay"( controller: 'imageSpace', action: 'getTileOverlay' )
    "/imageSpace/getThumbnail"( controller: 'imageSpace', action: 'getThumbnail' )
  }
}
