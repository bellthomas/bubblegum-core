package io.hbt.bubblegum.core.auxiliary;

public class MIMEHelper {
    private MIMEHelper() { /* Non instantiable */ }

    public static String fileNameToMimeType(String uri) {
        if(uri.contains(".")) {
            String[] parts = uri.split("\\.");
            switch (parts[parts.length -1]) {
                case "gif": return "image/gif";
                case "jpg":
                case "jpeg": return "image/jpeg";
                case "mp4": return "video/mp4";
                case "pdf": return "application/pdf";
                case "png": return "image/png";
                default: return "text/plain";
            }
        }
        return "text/plain";
    }
}
