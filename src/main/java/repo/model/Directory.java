package repo.model;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
final public class Directory {

    private static final Comparator<? super FileContext> COMPARATOR = new Comparator<FileContext>() {
        @Override
        public int compare(FileContext a, FileContext b) {
            if(a.directory == b.directory)
                return a.filename.compareTo(b.filename);
            else
                return a.directory ? -1 : +1;
        }
    };

    final URI url;
    final List<FileContext> files;

    private Directory(Builder builder) {
        this.url = builder.url;
        files = new ArrayList<>(builder.files);
        Collections.sort(files, COMPARATOR);
    }

    public static Builder builder(URI url) {
        return new Builder(url);
    }

    final public static class Builder {
        final private URI url;
        private List<FileContext> files = new ArrayList<>();

        public Builder(URI url) {
            checkNotNull(url);
            this.url = url;
        }

        public Builder add(FileContext file) {
            checkNotNull(file);
            files.add(file);
            return this;
        }

        public Directory build() {
            return new Directory(this);
        }
    }
}
