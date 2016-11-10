package repo.resource;


import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import static repo.Application.*;

@Path("/")
@Singleton
public class RepositoryResource {

    private static final String DEFAULT_BUCKET = SystemProperty.applicationId.get() + ".appspot.com";
    private static final String BUCKET_NAME = System.getProperty(repo.Application.PROPERTY_BUCKET_NAME, DEFAULT_BUCKET);

    private final GcsService gcs = GcsServiceFactory.createGcsService();
    private final Template listTemplate;

    public RepositoryResource() throws IOException {
        Handlebars handlebars = new Handlebars();
        this.listTemplate = handlebars.compile("list");
    }

    @GET
    @RolesAllowed(value = {ROLE_WRITE, ROLE_READ, ROLE_LIST})
    @CacheControl(property = "repository.cache-control.list")
    @Produces(MediaType.TEXT_HTML)
    public String list(@Context UriInfo uriInfo) throws IOException {
        return list("", uriInfo);
    }

    @DELETE
    @Path("{dir: .*[/]}")
    @RolesAllowed(value = {ROLE_WRITE})
    public Response delete(@PathParam("dir") final String dir,
                           @Context final UriInfo uriInfo) throws IOException {

        ListResult list = gcs.list(BUCKET_NAME, new ListOptions.Builder().setPrefix(dir).setRecursive(true).build());

        int count = 0;
        while (list.hasNext()) {
            ListItem item = list.next();
            gcs.delete(new GcsFilename(BUCKET_NAME, item.getName()));
            count++;
        }

        return Response.ok(count).build();
    }

    @GET
    @Path("{dir: .*[/]}")
    @RolesAllowed(value = {ROLE_WRITE, ROLE_READ, ROLE_LIST})
    @CacheControl(property = Application.PROPERTY_CACHE_CONTROL_LIST)
    @Produces(MediaType.TEXT_HTML)
    public String list(@PathParam("dir") final String dir,
                       @Context final UriInfo uriInfo) throws IOException {

        final ListOptions options =
                new ListOptions
                        .Builder()
                        .setRecursive(false)
                        .setPrefix(dir)
                        .build();

        final ListResult listResult = gcs.list(BUCKET_NAME, options);

        if (!listResult.hasNext()) {
            throw new NotFoundException();
        }

        ArrayList<Object> fileList = new ArrayList<>();
        int dirLength = dir.length();

        while (listResult.hasNext()) {
            ListItem file = listResult.next();
            if (file.isDirectory() && file.getName().equals(dir)) {
                continue;
            }

            HashMap<String, Object> fileData = new HashMap<>();
            fileData.put("shortName", file.getName().substring(dirLength));
            fileData.put("name", file.getName());
            fileData.put("directory", file.isDirectory());
            fileList.add(fileData);
        }

        HashMap<String, Object> context = new HashMap<>();
        context.put("currentPath", dir.isEmpty() ? "/" : dir);
        context.put("fileList", fileList);

        return listTemplate.apply(context);
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
