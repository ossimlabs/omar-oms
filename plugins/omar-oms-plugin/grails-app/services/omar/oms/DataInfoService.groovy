package omar.oms

class DataInfoService
{
  static transactional = false

  def infoGetterPool

  String getInfo( DataInfoCommand cmd)
  {
    getInfo(cmd.filename, cmd.entry)
  }
  String  getInfo( String filename, Integer entryId = null )
  {
    def infoGetter = infoGetterPool.borrowObject()
    def xml
    try{
        xml = infoGetter.runDataInfo(filename, entryId)
    }
    catch(def e)
    {
        xml = null
    }
    infoGetterPool.returnObject(infoGetter)

    return xml
  }
}
