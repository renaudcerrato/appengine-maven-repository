package repo.resource;


import com.google.appengine.api.utils.SystemProperty;
import com.google.appengine.tools.cloudstorage.*;

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
    private final String bucketName;
    private final GcsService gcs;

    public RepositoryResource() {
        gcs = GcsServiceFactory.createGcsService();
        bucketName = System.getProperty(repo.Application.PROPERTY_BUCKET_NAME, DEFAULT_BUCKET);
    }

    @GET
    @RolesAllowed(value={ROLE_WRITE, ROLE_READ, ROLE_LIST})
    @Produces(MediaType.TEXT_HTML)
    public StreamingOutput list() throws IOException {
        return list("");
    }

    @GET
    @Path("{dir: .*[/]}")
    @RolesAllowed(value={ROLE_WRITE, ROLE_READ, ROLE_LIST})
    @Produces(MediaType.TEXT_HTML)
    public StreamingOutput list(@PathParam("dir") final String dir) throws IOException {

        final ListOptions options = new ListOptions.Builder()
                .setRecursive(false).setPrefix(dir).build();
        final ListResult list = gcs.list(bucketName, options);

        if (!list.hasNext()) {
            throw new NotFoundException();
        }

        return new StreamingOutput() {
            @Override
            public void write(OutputStream output1) throws IOException, WebApplicationException {
                PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output1)));
                writer.append(String.format("<html><head><title>Index of %s</title></head><body>", dir));

                while (list.hasNext()) {
                    final ListItem file = list.next();
                    if (file.isDirectory() && file.getName().equals(dir)) {
                        continue;
                    }
                    final String filename = file.getName().substring(dir.length());
                    writer.append(String.format("<pre><a href=\"%s\">%s</a></pre>", filename, filename));
                }
                writer.append("</body></html>");
                writer.flush();
            }
        };
    }

    @GET
    @Path("{file: .*}")
    @RolesAllowed(value={ROLE_WRITE, ROLE_READ})
    @repo.annotation.CacheControl(maxAge = 30, mustRevalidate = true)
    public Response get(@PathParam("file") String file, @Context Request request) throws IOException {

        final GcsFilename filename = new GcsFilename(bucketName, file);
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
                        @HeaderParam("Content-Type") String mimeType,
                        byte[] content) throws IOException {

        final GcsFilename filename = new GcsFilename(bucketName, file);
        GcsFileOptions.Builder options = new GcsFileOptions.Builder();

        if(mimeType != null) {
            options.mimeType(mimeType);
        }

        gcs.createOrReplace(filename, options.build(), ByteBuffer.wrap(content));
        return Response.accepted().build();
    }
}
