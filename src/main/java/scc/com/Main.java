package scc.com;

import scc.com.dao.DatabaseConnection;
import scc.com.ui.MainMenu;

public class Main {
    public static void main(String[] args) {
        DatabaseConnection.getInstance().initialize();
        new MainMenu().show();
    }
}