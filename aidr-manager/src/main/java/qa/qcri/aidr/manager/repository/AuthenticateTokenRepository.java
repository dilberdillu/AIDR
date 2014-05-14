package qa.qcri.aidr.manager.repository;

import qa.qcri.aidr.manager.hibernateEntities.AidrAuthenticateToken;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: jlucas
 * Date: 5/12/14
 * Time: 12:13 PM
 * To change this template use File | Settings | File Templates.
 */
public interface AuthenticateTokenRepository extends GenericRepository<AidrAuthenticateToken, Serializable>  {

    public Boolean isAuthorized(String token);
}
