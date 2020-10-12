package omar.oms

import org.apache.commons.pool.PoolableObjectFactory

/**
 * Created with IntelliJ IDEA.
 * User: sbortman
 * Date: 7/27/12
 * Time: 12:31 PM
 * To change this template use File | Settings | File Templates.
 */
class InfoGetterPoolableObjectFactory implements PoolableObjectFactory
{
  public void destroyObject(Object infoGetter)
  {
    infoGetter?.cleanup()
    infoGetter = null
  }
  @Override
  public void activateObject(Object infoGetter)
  {
  }
  @Override
  public void passivateObject(Object infoGetter)
  {
  }
  
  public Object makeObject()
  {
    return new InfoGetter()
  }
  
  public boolean validateObject()
  {
    return true
  }
}
