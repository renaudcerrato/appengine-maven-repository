package repo.provider;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;


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

        repo.annotation.CacheControl annotation = resourceInfo.getResourceMethod()
                .getAnnotation(repo.annotation.CacheControl.class);

        if(annotation == null) {
            annotation = resourceInfo.getResourceClass()
                    .getAnnotation(repo.annotation.CacheControl.class);
        }

        if(annotation == null) {
            return;
        }

        CacheControl cacheControl = null;

        if(annotation.property().isEmpty()) {

            final TimeUnit unit = annotation.unit();

            cacheControl = new CacheControl();

            cacheControl.setPrivate(annotation.isPrivate());
            cacheControl.setMustRevalidate(annotation.mustRevalidate());
            cacheControl.setNoCache(annotation.noCache());
            cacheControl.setNoStore(annotation.noStore());
            cacheControl.setNoTransform(annotation.noTransform());

            if (annotation.maxAge() >= 0) {
                cacheControl.setMaxAge((int) unit.toSeconds(annotation.maxAge()));
            }

            if (annotation.sMaxAge() >= 0) {
                cacheControl.setSMaxAge((int) unit.toSeconds(annotation.sMaxAge()));
            }
        }else {
            final String raw = System.getProperty(annotation.property());
            if(raw != null) cacheControl = CacheControl.valueOf(raw);
        }

        if(cacheControl != null) {
            responseContext.getHeaders().putSingle(HttpHeaders.CACHE_CONTROL, cacheControl.toString());
        }
    }
}