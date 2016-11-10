package repo.resource;


import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.tools.cloudstorage.*;
import repo.Application;
import repo.annotation.CacheControl;

import javax.annotation.security.RolesAllowed;
import javax.inject.Singleton;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.Date;

import static repo.Application.*;

@Path("/")
@Singleton
public class RepositoryResource {

    private static final String DEFAULT_BUCKET = SystemProperty.applicationId.get() + ".appspot.com";
    private static final String BUCKET_NAME = System.getProperty(repo.Application.PROPERTY_BUCKET_NAME, DEFAULT_BUCKET);

    private final GcsService gcs = GcsServiceFactory.createGcsService();

    @GET
    @RolesAllowed(value = {ROLE_WRITE, ROLE_READ, ROLE_LIST})
    @CacheControl(property = "repository.cache-control.list")
    @Produces(MediaType.TEXT_HTML)
    public StreamingOutput list(@Context UriInfo uriInfo) throws IOException {
        return list("", uriInfo);
    }

    @GET
    @Path("delete/{dir: .*[/]}")
    @RolesAllowed(value = {ROLE_WRITE})
    public Response delete(@PathParam("dir") final String dir,
                           @Context final UriInfo uriInfo) throws IOException {

        ListResult list = gcs.list(BUCKET_NAME, new ListOptions.Builder().setPrefix(dir).setRecursive(true).build());

        while (list.hasNext()) {
            ListItem item = list.next();
            gcs.delete(new GcsFilename(BUCKET_NAME, item.getName()));
        }

        return Response.ok().build();
    }

    @GET
    @Path("{dir: .*[/]}")
    @RolesAllowed(value = {ROLE_WRITE, ROLE_READ, ROLE_LIST})
    @CacheControl(property = Application.PROPERTY_CACHE_CONTROL_LIST)
    @Produces(MediaType.TEXT_HTML)
    public StreamingOutput list(@PathParam("dir") final String dir,
                                @Context final UriInfo uriInfo) throws IOException {

        final ListOptions options = new ListOptions.Builder()
                .setRecursive(false).setPrefix(dir).build();
        final ListResult list = gcs.list(BUCKET_NAME, options);

        if (!list.hasNext()) {
            throw new NotFoundException();
        }

        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)));
                writer.append(String.format("<html><head><title>Index of %s</title></head><body>", uriInfo.getPath()));

                while (list.hasNext()) {
                    final ListItem file = list.next();
                    if (file.isDirectory() && file.getName().equals(dir)) {
                        continue;
                    }
                    final String filename = file.getName().substring(dir.length());

                    if (file.isDirectory()) {
                        writer.append(String.format("<pre><a href=\"%s\">%s</a>&nbsp;&nbsp;&nbsp;<a href=\"delete/%s\">delete</a></pre>", filename, filename, file.getName()));

                    } else {
                        writer.append(String.format("<pre><a href=\"%s\">%s</a></pre>", filename, filename));
                    }
                }
                writer.append("</body></html>");
                writer.flush();
            }
        };
    }

    @GET
    @Path("{file: .*}")
    @RolesAllowed(value = {ROLE_WRITE, ROLE_READ})
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
            final GcsInputChannel channel = gcs.openPrefetchingReadChannel(filename, 0, 1024 * 1024);
            response = Response.ok().entity(Channels.newInputStream(channel));
            response.tag(etag);
            response.lastModified(lastModified);
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

        if (mimeType != null) {
            options.mimeType(mimeType);
        }

        gcs.createOrReplace(filename, options.build(), ByteBuffer.wrap(content));
        return Response.accepted().build();
    }
}
