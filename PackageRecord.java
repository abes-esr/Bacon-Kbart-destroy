import java.time.LocalDate;

public class PackageRecord {
    private final int index;
    private final String provider;
    private final int providerId;
    private final String packageName;
    private final LocalDate date;

    public PackageRecord(int index, String provider, int providerId, String packageName, LocalDate date) {
        this.index = index;
        this.provider = provider;
        this.providerId = providerId;
        this.packageName = packageName;
        this.date = date;
    }

    public int getIndex() {
        return index;
    }

    public String getProvider() {
        return provider;
    }

    public int getProviderId() {
        return providerId;
    }

    public String getPackageName() {
        return packageName;
    }

    public LocalDate getDate() {
        return date;
    }
}
