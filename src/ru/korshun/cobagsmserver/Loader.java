package ru.korshun.cobagsmserver;

@SuppressWarnings("FieldCanBeLocal")
public class Loader {

    private Settings                    settings;
    private Sql                         sql;

    public Loader() {

        this.settings =                                     new Settings();
        this.sql =                                          new Sql();

    }

    public Settings getSettingsInstance() {
        return settings;
    }

    public Sql getSqlInstance() {
        return sql;
    }
}
