package repo.model;

final public class GcsFile {

    final public String filename;
    final public boolean directory;

    public GcsFile(String filename, boolean directory) {
        this.filename = filename;
        this.directory = directory;
    }
}
