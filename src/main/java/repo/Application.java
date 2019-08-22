package repo;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.mustache.MustacheMvcFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import repo.provider.BasicSecurityContextRequestFilter;
import repo.provider.CacheControlResponseFilter;
import repo.provider.RolesAllowedDynamicFeature;
import repo.provider.User;
import repo.resource.RepositoryResource;

public class Application extends ResourceConfig {

    static private final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static final String PROPERTY_BUCKET_NAME = "repository.gcs.bucket";
    public static final String PROPERTY_CREDENTIALS_FILENAME = "repository.credentials.location";
    public static final String PROPERTY_CACHE_CONTROL_FETCH = "repository.cache-control.fetch";
    public static final String PROPERTY_CACHE_CONTROL_LIST = "repository.cache-control.list";
    public static final String PROPERTY_UNIQUE_ARTIFACT = "repository.unique.artifact";

    public static final String DEFAULT_CREDENTIALS_FILENAME = "WEB-INF/users.txt";

    public static final String ROLE_WRITE = "write";
    public static final String ROLE_READ = "read";
    public static final String ROLE_LIST = "list";

    private static File CREDENTIALS = new File(
            System.getProperty(PROPERTY_CREDENTIALS_FILENAME, DEFAULT_CREDENTIALS_FILENAME));

    public Application() throws IOException {
        final BasicSecurityContextRequestFilter filter = new BasicSecurityContextRequestFilter();
        filter.addAll(getUsers(CREDENTIALS));
        register(filter);
        register(RepositoryResource.class);
        register(RolesAllowedDynamicFeature.class);
        register(CacheControlResponseFilter.class);
        register(MustacheMvcFeature.class);
        property(MustacheMvcFeature.TEMPLATE_BASE_PATH, System.getProperty(MustacheMvcFeature.TEMPLATE_BASE_PATH));

    }

    private static List<User> getUsers(File file) throws IOException {
        final List<User> users = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                try {
                    if (line.startsWith("#") || line.trim().isEmpty()) {
                        continue;
                    }

                    final String[] splits = line.split(":");

                    if (splits.length != 3) {
                        LOGGER.warn("{}:{}: syntax error", file, lineNo);
                        continue;
                    }

                    final String[] roles = splits[2].split(",");
                    final User.Builder user = new User.Builder()
                            .credentials(splits[0], splits[1]);

                    for (String role : roles) {
                        user.role(role.trim());
                    }

                    users.add(user.build());
                }finally {
                    lineNo++;
                }
            }
        }
        return users;
    }
}
