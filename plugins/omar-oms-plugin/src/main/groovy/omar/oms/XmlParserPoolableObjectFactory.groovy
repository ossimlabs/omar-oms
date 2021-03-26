package omar.oms

import javax.xml.parsers.SAXParserFactory
import org.apache.commons.pool.PoolableObjectFactory

/**
 * Created with IntelliJ IDEA.
 * User: sbortman
 * Date: 7/27/12
 * Time: 1:52 PM
 * To change this template use File | Settings | File Templates.
 */
class XmlParserPoolableObjectFactory implements PoolableObjectFactory
{
  private SAXParserFactory parserFactory;

  public XmlParserPoolableObjectFactory( )
  {
    parserFactory = SAXParserFactory.newInstance();
  }

  public Object makeObject( ) throws Exception
  {
    return parserFactory.newSAXParser();
  }

  void destroyObject( Object o )
  {
    o = null;
  }

  public boolean validateObject( Object o )
  {
    if(o != null)
      return true;
    return false;
  }

  void activateObject( Object o)
  {
    if(o == null)
      o;
  }

  void passivateObject(Object o)
  {
    if(o == null)
      o;
  }
}
