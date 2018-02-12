package utils;

public enum DataFileFormat {
    CSV, XML, JSON, EPGM;

    public static DataFileFormat fromString(String format) {
        switch (format.toLowerCase()){
            case "csv":
                return CSV;
            case "xml":
                return XML;
            case "epgm":
                return EPGM;
            default:
                throw new IllegalArgumentException("Invalid data format: " + format);
        }
    }
}