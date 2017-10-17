import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class General {

    private static final int kilo = 1024;
    private static HashMap<String, ArrayList<String>> duplicate = new HashMap<>();
    private static HashMap<Download, Thread> pool = new HashMap<>();

    public static void main(String[] args) throws IOException {
        int countThreads = 0;
        double limitSpeed = 0;
        String pathToFile = null;
        String nameDir = null;
        List<String> stringWithURL;
        List<String> listUrlToFile;
        List<String> listNameFile;
        long startTime;
        long endTime;
        long timeRunning;

        startTime = System.currentTimeMillis();
        breakDown(args);

        int step = 0;
        for (String flag : args) {
            switch (flag) {
                case "-n":
                    countThreads = Integer.parseInt(args[step + 1]);
                    break;
                case "-l":
                    limitSpeed = parseLimitSpeed(args[step + 1]);
                    break;
                case "-f":
                    pathToFile = args[step + 1];
                    break;
                case "-o":
                    nameDir = args[step + 1];
                    break;
            }
            step++;
        }

        stringWithURL = Files.readAllLines(Paths.get(pathToFile));
        listUrlToFile = new ArrayList<>();
        listNameFile = new ArrayList<>();
        for (String infoString : stringWithURL) {
            if (infoString.isEmpty()) {
                continue;
            } else {
                String urlToFile = infoString.substring(0, infoString.indexOf(" "));
                String nameFile = infoString.substring(infoString.lastIndexOf(" ") + 1);
                listUrlToFile.add(urlToFile);
                listNameFile.add(nameFile);
            }
        }

        execute(listUrlToFile, listNameFile, nameDir, countThreads, limitSpeed);

        if (duplicate.size() > 0) {
            while (true) {
                if (pool.values().stream().noneMatch((i) -> i.isAlive())) {
                    copyDuplicateFile(listNameFile, listUrlToFile, nameDir);
                    break;
                }
            }
        }

        int sumBytes = 0;
        for (Download download : pool.keySet()) {
            sumBytes += download.getCountBytes();
        }
        endTime = System.currentTimeMillis();
        timeRunning = endTime - startTime;
        System.out.println("Operating time of the program " + timeRunning/1000 + " s" + "\n");
        System.out.println("Total downloaded " + sumBytes + " bytes");
    }

    private static void breakDown(String[] args) {
        if (args.length < 8) {
            System.out.println("Нет одного из параметров");
            System.exit(0);
        } else if (checkArgs(args)){
            System.out.println("Неверное название параметра");
            System.exit(0);
        } else if (!new File(args[Arrays.asList(args).indexOf("-f") + 1]).exists()) {
            System.out.println("Отсутствует файл со списком загрузки");
            System.exit(0);
        } else if (!new File(args[Arrays.asList(args).indexOf("-o") + 1]).exists()) {
            System.out.println("Папка для скачивания отсуствует");
            System.exit(0);
        }
    }

    private static boolean checkArgs(String[] args) {
        boolean result = false;
        String[] flags = {"-n", "-l", "-o", "-f"};
        for (int i = 0; i < flags.length; i++) {
            if (!Arrays.asList(args).contains(flags[i])) {
                result = true;
            }
        }
        return result;
    }

    private static void execute(List listUrlToFile, List listNameFile, String nameDir,
                                int countThreads, double limitSpeed ) throws IOException {
        int sizeListUrl = listUrlToFile.size();
        if (sizeListUrl < countThreads) {
            runDownloading(0, sizeListUrl, listUrlToFile, listNameFile, nameDir, limitSpeed);
        } else {
            int beginStep = 0;
            int endStep = countThreads;
            while (beginStep < sizeListUrl) {
                if ((sizeListUrl - countThreads) < beginStep) {
                    endStep = sizeListUrl;
                    runDownloading(beginStep, endStep, listUrlToFile, listNameFile, nameDir, limitSpeed);
                } else {
                    runDownloading(beginStep, endStep, listUrlToFile, listNameFile, nameDir, limitSpeed);
                    if ((sizeListUrl - countThreads) < endStep) {
                        endStep = sizeListUrl;
                    } else {
                        endStep += countThreads;
                    }
                }
                beginStep += countThreads;
            }
        }
    }

    private static boolean findDuplicateUrl(List listUrl, int step) {
        return listUrl.subList(0, step).contains(listUrl.get(step));
    }

    private static void copyDuplicateFile(List<String> listNameFile, List<String> listUrlFile,
                                          String nameDir) throws IOException {
        Object[] keyArray = duplicate.keySet().toArray();
        for (int i = 0; i < duplicate.size(); i++) {
            for (int k = 0; k < duplicate.get(keyArray[i]).size(); k++) {
                Files.copy(new File(nameDir + "/" + listNameFile.get(listUrlFile.indexOf(keyArray[i]))).toPath(),
                        new File(nameDir + "/" + duplicate.get(keyArray[i]).get(k)).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void runDownloading(int beginStep, int endStep, List<String> listUrlToFile,
                                       List<String> listNameFile, String nameDir, double limitSpeed)throws IOException {
        for (int i = beginStep; i < endStep; i++) {
            if (findDuplicateUrl(listUrlToFile, i)) {
                duplicate.computeIfAbsent(listUrlToFile.get(i), (k) -> new ArrayList<String>()).add(listNameFile.get(i));
            } else {
                Download download = new Download(limitSpeed, listUrlToFile.get(i), listNameFile.get(i), nameDir);
                Thread thread = new Thread(download, listUrlToFile.get(i));
                pool.put(download, thread);
                thread.start();
            }
        }
    }

    private static double parseLimitSpeed(String limitSpeed) {
        double valueSpeed;
        int lengthValueArg = limitSpeed.length();
        if (limitSpeed.endsWith("k")) {
            valueSpeed = Double.parseDouble(
                    limitSpeed.substring(0, lengthValueArg - 1)) * kilo;
        } else if (limitSpeed.endsWith("m")) {
            valueSpeed = Double.parseDouble(
                    limitSpeed.substring(0, lengthValueArg - 1)) * kilo * kilo;
        } else {
            valueSpeed = Double.parseDouble(limitSpeed);
        }
        return valueSpeed;
    }
}