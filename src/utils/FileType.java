package utils;

public class FileType {

    public static boolean isBill(final String fileName) {
        return fileName.toUpperCase().matches("^.*]B.*\\.GPD$");
    }

    public static boolean isReceipt(final String fileName) {
        return fileName.toUpperCase().matches("^.*]R.*\\.GPD$");
    }

    public static boolean isOrder(final String fileName) {
        return fileName.toUpperCase().contains("ORDERS");
    }
    public static boolean isCashUp(final String fileName) {
        return fileName.toUpperCase().contains("CASHUP");
    }
}
