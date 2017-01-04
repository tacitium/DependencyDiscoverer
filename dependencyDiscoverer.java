// AP3 Assessed Exercise 2
// Done by: Terence Tan Boon Kiat
// Matriculation ID: 15AC083B
// Glasgow ID: 2228167T
// This is my own work as defined in the Academic Ethics agreement I have signed.

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;

public class dependencyDiscoverer {

    static ArrayList<String> dirList = new ArrayList<>(); // stores the main directory from args and any from cpath
    static ArrayList<String> inputFileList = new ArrayList<>(); // stores the file names as keys to hashmap for printing later
    static ConcurrentLinkedQueue<String> workQueue = new ConcurrentLinkedQueue<>(); // stores files to process
    static ConcurrentHashMap<String, ArrayList<String>> anotherStructure = new ConcurrentHashMap<>(); // maps all file names to dependencies
    static int CRAWLER_THREADS; // number of crawler threads to be used

    public static void main(String[] args) throws Exception {
        dirList.add("."); // add current folder to list

        // loop to find all directories stated in args string
        for (String s : args) {
            if (s.contains("-I")) {
                dirList.add(s.substring(2, s.length())); // add all -I args to list
            }
        }

        // save cpath environment variable if it is not null
        String cpath = System.getenv("CPATH");
        if (cpath != null) {
            String[] paths = cpath.split(":");
            dirList.addAll(Arrays.asList(paths));
        }

        // save crawler threads environment variable if it is not null
        String crawlerthreads = System.getenv("CRAWLER_THREADS");
        if (crawlerthreads != null) {
            CRAWLER_THREADS = Integer.parseInt(crawlerthreads); // save number of threads from system var
        } else {
            CRAWLER_THREADS = 2; // predefine number of crawler threads
        }

        // loop through all the rest of the args to find file names
        for (String s : args) {
            if (!s.contains("-I")) {
                String fileExt = s.substring(s.lastIndexOf(".") + 1, s.length()); // get the file's extension
                if (fileExt.equals("c") || fileExt.equals("y") || fileExt.equals("l")) {
                    workQueue.add(s); // save to work queue for processing
                    inputFileList.add(s); // save to file list for printing later
                } else {
                    System.err.println("Illegal extension: ." + fileExt + " - must be .c, .y or .l");
                }
            }
        }

        ArrayList<Thread> threadList = new ArrayList<>(CRAWLER_THREADS);

        for (int i = 0; i < CRAWLER_THREADS; i++) {
            Thread worker = new Thread(new Worker());
            threadList.add(worker);
            worker.start();
        }
        for (int j = 0; j < CRAWLER_THREADS; j++) {
            threadList.get(j).join();
        }

        printDependencies();
    }

    public static class Worker implements Runnable {

        private String file;
        private LinkedList<String> workFileList; // stores the names of the files to be processed
        private ArrayList<String> dependencyList = new ArrayList<>(); // stores the values of #include

        public Worker() {
            workFileList = new LinkedList<>();
            dependencyList = new ArrayList<>();
        }

        public void run() {
            while (!workQueue.isEmpty() && workQueue.peek() != null) {
                file = workQueue.poll();
                for (String s : dirList) {
                    File checkFile = getFile(s, file);

                    if (checkFile != null) {
                        workFileList.add(file); // add into this duplicate queue so that dependencies can be added here as well

                        // run processFile while workFileList is not empty
                        String tempFileName;
                        while (!workFileList.isEmpty() && workFileList.peek() != null) {
                            tempFileName = workFileList.poll();
                            processFile(tempFileName);
                        }
                        anotherStructure.put(file, dependencyList); // add to hashmap
                        dependencyList = new ArrayList<>(); // creates a new dependency list for the next file
                    }
                }
            }
        }

        public void processFile(String name) {
            // to input files one by one from work queue and check for #include " "
            try {
                for (String s : dirList) {
                    File currentFile = getFile(s, name);

                    if (currentFile != null) {
                        FileReader fReader = new FileReader(currentFile);
                        BufferedReader bReader = new BufferedReader(fReader);
                        String line;

                        while ((line = bReader.readLine()) != null) {
                            if (line.contains("#include") && line.contains("\"")) {

                                String dependency = line.split("\"")[1];

                                if (!dependencyList.contains(dependency)) {
                                    workFileList.add(dependency);
                                    dependencyList.add(dependency);
                                }
                            }
                        }
                        bReader.close();
                    }
                }
            } catch (Exception e) {
                //System.err.println("Exception caught: " + e);
            }
        }
    }

    public static void printDependencies() {
        // get list of keys from processed file list to get hashmap value to print
        // split the processed file to change to .o for printing
        for (String file : inputFileList) {
            if (!anotherStructure.isEmpty() && anotherStructure.get(file) != null) {
                String printOut = "";

                ArrayList<String> hashLine = anotherStructure.get(file);

                printOut = file.split("\\.")[0] + ".o: " + file;
                for (int i = 0; i < hashLine.size(); i++) {
                    printOut += " " + hashLine.get(i);
                }
                System.out.println(printOut);
            }
        }
    }

    public static File getFile(String s, String inputName) {
        File tempFile = new File(s + "/" + inputName);

        if (tempFile.exists() && !tempFile.isDirectory()) {
            return tempFile;
        }
        return null;
    }
}
