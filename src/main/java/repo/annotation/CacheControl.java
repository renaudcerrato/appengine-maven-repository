package repo.annotation;

import javax.ws.rs.NameBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

/**
 * Set the "Cache-Control" header.
 *
 * @see <a href='http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.3'>W3C Header
 *      Field Definitions</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@NameBinding
public @interface CacheControl {
    long maxAge() default -1;
    long sMaxAge() default -1;
    boolean isPrivate() default false;
    boolean noCache() default false;
    boolean noStore() default false;
    boolean noTransform() default false;
    boolean mustRevalidate() default false;
    TimeUnit unit() default TimeUnit.SECONDS;
}
