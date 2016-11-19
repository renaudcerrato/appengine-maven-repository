package repo.model;

import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("unused")
final public class FileContext {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    final public String filename;
    final public boolean directory;
    final public String modified;
    final public String size;

    public FileContext(String filename, long size, Date lastModified, boolean directory) {
        this.filename = filename;
        this.directory = directory;
        this.modified = lastModified == null ? null : DATE_FORMAT.format(lastModified);
        this.size = directory ? "-" : humanReadableByteCount(size, false);
    }

    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + "B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f%sB", bytes / Math.pow(unit, exp), pre);
    }
}
