package ru.korshun.cobagsmserver;


import java.io.*;

public class Logger {

    private boolean                     ready =             true;




    /**
     *  В конструкторе проверяем, создана ли папка для логов, если нет - создаем, если не получается - блокируем логгинг
     */
    public Logger(Settings settings) {

        File logDir =                                       new File(settings.getLOG_PATH());

        if(!checkLogPath(logDir) && !logDir.mkdir()) {
            System.out.println("Ошибка создания LOG директории");
            ready =                                         false;
        }

    }




    /**
     *  Проверяем, есть ли папка, куда будем складывать логи
     * @param path          - ссылка на объект File, наличие которого надо проверить
     * @return              - в случае, если папка есть, возвращаем true
     */
    private boolean checkLogPath(File path) {

        return path.exists();

    }




    /**
     *  Функция проверяет есть ли в файле ip адрес, с которого вылез блок
     * @param ip                - ip адрес
     * @param file              - ссылка на файл
     * @return                  - если ip есть в файле возвращается true
     * @throws IOException
     */
    private boolean existIpInFile(String ip, File file) throws IOException {

        BufferedReader br =                                 new BufferedReader(new FileReader(file));
        String sCurrentLine;

        while ((sCurrentLine = br.readLine()) != null) {
            if(sCurrentLine.equals(ip)) {
                return true;
            }
        }

        return false;
    }




    /**
     *  Функция проверяет есть ли в файле ip адрес, с которого вылез блок, и если его там нет, записывае его в конец файла
     * @param ip                - ip адрес
     * @param code              - радиоканал, нужен для создания папки с таким именем
     * @throws IOException
     */
    public void writeToLog(String ip, String code) throws IOException {

        if(ready) {
            File path =                                     new File(Main.getLoader().getSettingsInstance().getLOG_PATH()
                                                                + File.separator + code + File.separator + "ip.txt");

            if (!checkLogPath(path.getParentFile()) && (!path.getParentFile().mkdir() || !path.createNewFile())) {
                System.out.println("Ошибка создания log файла");
                return;
            }

            if(!existIpInFile(ip, path)) {

                BufferedWriter writer =                     new BufferedWriter(new FileWriter(path, true));

                try {
                    writer.write(ip + "\r\n");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


            }
        }

        else {
            System.out.println("Директория для логов не найдена");
        }

    }

}
