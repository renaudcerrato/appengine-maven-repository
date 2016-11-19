package repo.resource;


import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.tools.cloudstorage.GcsFileMetadata;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.ListItem;
import com.google.appengine.tools.cloudstorage.ListOptions;
import com.google.appengine.tools.cloudstorage.ListResult;

import org.glassfish.jersey.server.mvc.Template;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Date;

import javax.annotation.security.RolesAllowed;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import repo.Application;
import repo.annotation.CacheControl;
import repo.model.Directory;
import repo.model.FileContext;

import static repo.Application.ROLE_LIST;
import static repo.Application.ROLE_READ;
import static repo.Application.ROLE_WRITE;

@Path("/")
@Singleton
public class RepositoryResource {

    private static final String DEFAULT_BUCKET = SystemProperty.applicationId.get() + ".appspot.com";
    private static final String BUCKET_NAME = System.getProperty(repo.Application.PROPERTY_BUCKET_NAME, DEFAULT_BUCKET);
    private static final String X_APP_ENGINE_BLOB_KEY = "X-AppEngine-BlobKey";

    private final GcsService gcs = GcsServiceFactory.createGcsService();
    private final BlobstoreService blobstore = BlobstoreServiceFactory.getBlobstoreService();

    @GET
    @Template(name= "/list.mustache")
    @RolesAllowed(value={ROLE_WRITE, ROLE_READ, ROLE_LIST})
    @CacheControl(property = Application.PROPERTY_CACHE_CONTROL_LIST)
    @Produces(MediaType.TEXT_HTML)
    public Directory list(@Context UriInfo uriInfo) throws IOException {
        return list("", uriInfo);
    }

    @GET
    @Path("{dir: .*[/]}")
    @Template(name= "/list.mustache")
    @RolesAllowed(value={ROLE_WRITE, ROLE_READ, ROLE_LIST})
    @CacheControl(property = Application.PROPERTY_CACHE_CONTROL_LIST)
    @Produces(MediaType.TEXT_HTML)
    public Directory list(@PathParam("dir") final String dir,
                          @Context final UriInfo uriInfo) throws IOException {

        final ListOptions options = new ListOptions.Builder()
                .setRecursive(false).setPrefix(dir).build();
        final ListResult list = gcs.list(BUCKET_NAME, options);

        if (!dir.isEmpty() && !list.hasNext()) {
            throw new NotFoundException();
        }

        final Directory.Builder directory = Directory.builder(URI.create(uriInfo.getPath()));

        while (list.hasNext()) {
            final ListItem file = list.next();
            final String name = file.getName();

            if (name.equals(dir)) {
                continue;
            }

            directory.add(new FileContext(name.substring(dir.length()), file.getLength(), file.getLastModified(), file.isDirectory()));
        }

        return directory.build();
    }

    @GET
    @Path("{file: .*}")
    @RolesAllowed(value={ROLE_WRITE, ROLE_READ})
    @CacheControl(property = Application.PROPERTY_CACHE_CONTROL_FETCH)
    public Response fetch(@PathParam("file") String file, @Context Request request) throws IOException {

        final GcsFilename filename = new GcsFilename(BUCKET_NAME, file);
        final GcsFileMetadata meta = gcs.getMetadata(filename);

        if (meta == null) {
            throw new NotFoundException();
        }

        final EntityTag etag = new EntityTag(meta.getEtag());
        final Date lastModified = meta.getLastModified();
        final String mimeType = meta.getOptions().getMimeType();

        Response.ResponseBuilder response = request.evaluatePreconditions(lastModified, etag);

        if (response == null) {
            final String path = String.format("/gs/%s/%s", filename.getBucketName(), filename.getObjectName());
            final BlobKey key = blobstore.createGsBlobKey(path);
            response = Response.ok();
            response.tag(etag);
            response.lastModified(lastModified);
            response.header(X_APP_ENGINE_BLOB_KEY, key.getKeyString());
        }

        if (mimeType != null) {
            response.type(mimeType);
        }

        return response.build();
    }

    @PUT
    @Path("{file: .*}")
    @RolesAllowed(ROLE_WRITE)
    public Response put(@PathParam("file") String file,
                        @HeaderParam(HttpHeaders.CONTENT_TYPE) String mimeType,
                        byte[] content) throws IOException {

        final GcsFilename filename = new GcsFilename(BUCKET_NAME, file);
        GcsFileOptions.Builder options = new GcsFileOptions.Builder();

        if(mimeType != null) {
            options.mimeType(mimeType);
        }

        gcs.createOrReplace(filename, options.build(), ByteBuffer.wrap(content));
        return Response.accepted().build();
    }
}
