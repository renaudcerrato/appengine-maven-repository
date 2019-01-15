package repo.provider;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class BasicSecurityContextRequestFilter implements ContainerRequestFilter {

    private static final String BASIC = "Basic";
    private static final String ANONYMOUS_TOKEN = Base64.getEncoder().encodeToString("*:*".getBytes());
    private final Map<String, User> userMap = new HashMap<>();


    public void add(User user) {
        userMap.put(user.authentication, user);
    }

    public void addAll(Collection<User> users) {
        for(User user: users) {
            add(user);
        }
    }

    @Override
    public void filter(ContainerRequestContext containerRequest) throws WebApplicationException {

        final String authorization = containerRequest.getHeaderString(HttpHeaders.AUTHORIZATION);
        User user = null;

        if(authorization == null) {
            user = userMap.get(ANONYMOUS_TOKEN);
        }else if(authorization.startsWith(BASIC)) {
            user = userMap.get(authorization.substring(BASIC.length() + 1));
        }

        if(user != null) {
            final boolean secure = containerRequest.getUriInfo().getBaseUri().getScheme().equals("https");
            containerRequest.setSecurityContext(new BasicSecurityContext(user, secure));
        }
    }

    private class BasicSecurityContext implements SecurityContext {
        private final User user;
        private final boolean secure;

        BasicSecurityContext(@Nullable User user, boolean secure) {
            this.user = user;
            this.secure = secure;
        }

        @Override @Nullable
        public Principal getUserPrincipal() {
            if(user == null) return null;
            return user.principal;
        }

        @Override
        public boolean isUserInRole(String role) {
            if(user == null) return false;
            return user.roles.contains(role);
        }

        @Override
        public boolean isSecure() {
            return secure;
        }

        @Override
        public String getAuthenticationScheme() {
            return BASIC_AUTH;
        }
    }
}