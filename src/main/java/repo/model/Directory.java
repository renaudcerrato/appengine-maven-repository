package repo.model;

import com.google.common.collect.ImmutableList;

import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;

final public class Directory {

    final public URI url;
    final public ImmutableList<GcsFile> files;

    private Directory(Builder builder) {
        this.url = builder.url;
        this.files = builder.files.build();
    }

    public static Builder builder(URI url) {
        return new Builder(url);
    }

    final public static class Builder {
        final private URI url;
        private ImmutableList.Builder<GcsFile> files = ImmutableList.builder();

        public Builder(URI url) {
            checkNotNull(url);
            this.url = url;
        }

        public Builder add(GcsFile file) {
            checkNotNull(file);
            files.add(file);
            return this;
        }

        public Directory build() {
            return new Directory(this);
        }
    }
}
