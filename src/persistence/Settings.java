package persistence;

import java.util.prefs.Preferences;

public class Settings {
    public Preferences prefs;
    private static Settings instance;

    public static final String INPUT = "INPUT";
    public static final String OUTPUT = "OUTPUT";
    public static final String EFP_INPUT = "EFP_INPUT";
    public static final String EFP_OUTPUT = "EFP_OUTPUT";
    public static final String STARTED = "STARTED";

    public static Settings getInstance() {
        if (instance == null)
            instance = new Settings();
        return instance;
    }

    private Settings(){
        prefs = Preferences.userRoot().node(this.getClass().getName());
    }
}
