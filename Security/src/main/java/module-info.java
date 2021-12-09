module com.udacity.catpoint.security {
    requires java.desktop;
    requires com.google.gson;
    requires java.prefs;
    requires com.google.common;
    requires miglayout;
    requires com.udacity.catpoint.image;
    opens com.udacity.catpoint.security.data to com.google.gson;

}