package ch.bastiangardel.easypay.shiro;

import ch.bastiangardel.easypay.model.Permission;
import ch.bastiangardel.easypay.model.Role;
import ch.bastiangardel.easypay.model.User;
import ch.bastiangardel.easypay.repository.UserRepository;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Shiro authentication & authorization realm that relies on OrientDB as
 * datastore.
 */
@Component
public class MysqlDbRealm extends AuthorizingRealm {

    @Autowired
    private UserRepository userRepository;

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(
            final AuthenticationToken token)
            throws AuthenticationException {
        final UsernamePasswordToken credentials = (UsernamePasswordToken) token;
        final String email = credentials.getUsername();
        if (email == null) {
            throw new UnknownAccountException("Email not provided");
        }
        final User user = userRepository.findByEmailAndActive(email, true);
        if (user == null) {
            throw new UnknownAccountException("Account does not exist");
        }
        return new SimpleAuthenticationInfo(email, user.getPassword().toCharArray(),
                ByteSource.Util.bytes(email), getName());
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(
            final PrincipalCollection principals) {
        // retrieve role names and permission names
        final String email = (String) principals.getPrimaryPrincipal();
        final User user = userRepository.findByEmailAndActive(email, true);
        if (user == null) {
            throw new UnknownAccountException("Account does not exist");
        }
        final int totalRoles = user.getRoles().size();
        final Set<String> roleNames = new LinkedHashSet<>(totalRoles);
        final Set<String> permissionNames = new LinkedHashSet<>();
        if (totalRoles > 0) {
            for (Role role : user.getRoles()) {
                roleNames.add(role.getName());
                for (Permission permission : role.getPermissions()) {
                    permissionNames.add(permission.getName());
                }
            }
        }
        final SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roleNames);
        info.setStringPermissions(permissionNames);
        return info;
    }

}
