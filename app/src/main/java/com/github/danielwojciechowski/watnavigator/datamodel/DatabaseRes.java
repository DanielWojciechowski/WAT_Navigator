package com.github.danielwojciechowski.watnavigator.datamodel;

/**
 * Created by Daniel on 2015-05-13.
 */
public class DatabaseRes {
    //TODO przenieść do zewnętrznego pliku SQL
    private static final String buildings =
            "INSERT INTO building VALUES"+
            "('100', 52.253200, 20.900204),"+
            "('65', 52.255337, 20.903774),"+
            "('61', 52.254157, 20.902425),"+
            "('12', 52.253320, 20.901719),"+
            "('13', 52.253757, 20.901955),"+
            "('62', 52.254348, 20.900539),"+
            "('63', 52.255038, 20.901816),"+
            "('69', 52.255600, 20.899509),"+
            "('68', 52.256332, 20.899852),"+
            "('70', 52.255981, 20.898639),"+
            "('71', 52.256437, 20.899261),"+
            "('72', 52.257412, 20.899122),"+
            "('67', 52.256956, 20.900527),"+
            "('66', 52.256211, 20.902018),"+
            "('121', 52.255784, 20.899122),"+
            "('64', 52.254960, 20.904539),"+
            "('56', 52.254510, 20.905483),"+
            "('55', 52.253653, 20.907114),"+
            "('52', 52.252927, 20.908058)"+
    ";";

    public static String getBuildings() {
        return buildings;
    }
}
