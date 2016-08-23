package ru.korshun.cobagsmserver;

@SuppressWarnings("FieldCanBeLocal")
public class Loader {

    private Settings                    settings;
    private Sql                         sql;
    private Logger                      logger;

    public Loader() {

        this.settings =                                     new Settings();
        this.sql =                                          new Sql();
        this.logger =                                       new Logger(this.settings);

    }

    public Settings getSettingsInstance() {
        return settings;
    }

    public Sql getSqlInstance() {
        return sql;
    }

    public Logger getLoggerInstance() {
        return logger;
    }
}
