package repo.provider;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


@Provider
@repo.annotation.CacheControl
public class CacheControlResponseFilter implements ContainerResponseFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

        if(!HttpMethod.GET.equals(requestContext.getMethod())) {
            return;
        }

        if(!responseContext.hasEntity()) {
            return;
        }

        if(responseContext.getHeaderString(HttpHeaders.CACHE_CONTROL) != null) {
            return;
        }

        repo.annotation.CacheControl cacheControl = resourceInfo.getResourceMethod()
                .getAnnotation(repo.annotation.CacheControl.class);

        if(cacheControl == null) {
            cacheControl = resourceInfo.getResourceClass()
                    .getAnnotation(repo.annotation.CacheControl.class);
        }

        if(cacheControl == null) {
            return;
        }

        final CacheControl cc = new CacheControl();
        final TimeUnit unit = cacheControl.unit();

        cc.setPrivate(cacheControl.isPrivate());
        cc.setMustRevalidate(cacheControl.mustRevalidate());
        cc.setNoCache(cacheControl.noCache());
        cc.setNoStore(cacheControl.noStore());
        cc.setNoTransform(cacheControl.noTransform());

        if(cacheControl.maxAge() >= 0) cc.setMaxAge((int) unit.toSeconds(cacheControl.maxAge()));
        if(cacheControl.sMaxAge() >= 0) cc.setSMaxAge((int) unit.toSeconds(cacheControl.sMaxAge()));

        responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, cc.toString());
    }
}