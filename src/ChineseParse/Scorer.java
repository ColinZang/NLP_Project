package ChineseParse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;

/**
 * Can choose between running separately, or running together with PCFGParser
 * Adapted from TrainCorpus.java by Chen Chen
 * Created by Wenzhao on 5/4/16.
 */
public class Scorer {
    private final String USAGE = "java ParseScorer goldFolderPath ownFilePath";
    private List<String> goldSentences = new ArrayList<String>();
    private int currentPosFromParser = 0;
    private int correctFromParser = 0;

    public enum FileType {
        HTML, RAW;
    }

    public Scorer(String goldFolder) {
        File folder = new File(goldFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Your data path should be a folder!");
            System.exit(1);
        }
        File[] dataFiles = folder.listFiles();
        Arrays.sort(dataFiles, new Comp());
        for (File file : dataFiles) {
            String fileName = file.getName();
            String ext = GetExtension(fileName);
            // only process files with extension "fid"
            if (ext.equals("fid")) {
                ProcessOneFile(file);
            }
            else {
                System.out.println("goldFile must be in fid extension");
            }
        }
    }

    public void runFromParser(String sentence) {
        if (currentPosFromParser > goldSentences.size() - 1) {
            System.out.println("The number of lines don't match");
            return;
        }
        System.out.println("gold parse:");
        System.out.println(goldSentences.get(currentPosFromParser));
        System.out.println("own parse");
        System.out.println(sentence);
        if (sentence.equals(goldSentences.get(currentPosFromParser))) {
            correctFromParser++;
            System.out.println("correct");
        }
        else {
            System.out.println("incorrect");
        }
        currentPosFromParser++;
        if (currentPosFromParser == goldSentences.size()) {
            System.out.println(correctFromParser + " out of " + goldSentences.size() +
                    " sentences are correct.");
            System.out.println("The parsing accuracy is " +
                    (double)correctFromParser / goldSentences.size());
        }
    }

    private void run(String ownFile) {
        int total = 0;
        int correct = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(ownFile));
            String line;
            while ((line = reader.readLine()) != null) {
                if (total > goldSentences.size() - 1) {
                    System.out.println("The number of lines don't match");
                    return;
                }
                System.out.println("gold parse:");
                System.out.println(goldSentences.get(total));
                System.out.println("own parse:");
                System.out.println(line);
                if (line.equals(goldSentences.get(total))) {
                    correct++;
                    System.out.println("correct");
                }
                else {
                    System.out.println("incorrect");
                }
                total++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(correct + " out of " + total +" sentences are correct.");
        System.out.println("The parsing accuracy is " + (double)correct / total);
    }

    private void runParseval(String ownFile) {
        int total = 0;
        int correctOwnRule = 0;
        int correctGoldRule = 0;
        int totalOwnRule = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(ownFile));
            String line;
            while ((line = reader.readLine()) != null) {
                if (total > goldSentences.size() - 1) {
                    System.out.println("The number of lines don't match");
                    return;
                }
                System.out.println("gold parse:");
                System.out.println(goldSentences.get(total));
                System.out.println("own parse:");
                System.out.println(line);
                MyNode rootOne = getTree(line);
                MyNode rootTwo = getTree(goldSentences.get(total));
                List<String> ownRules = countRules(rootOne);
                List<String> goldRules = countRules(rootTwo);
                HashMap<String, Integer> gold = new HashMap<String, Integer>();
                for (String s: goldRules) {
                    if (gold.containsKey(s)) {
                        gold.put(s, gold.get(s) + 1);
                    }
                    else {
                        gold.put(s, 1);
                    }
                }
                HashMap<String, Integer> own = new HashMap<String, Integer>();
                for (String s: ownRules) {
                    if (own.containsKey(s)) {
                        own.put(s, own.get(s) + 1);
                    }
                    else {
                        own.put(s, 1);
                    }
                }
                int original = correctOwnRule;
                for (Map.Entry<String, Integer> entry: own.entrySet()) {
                    String s = entry.getKey();
                    int count = entry.getValue();
                    if (!gold.containsKey(s)) {
                        continue;
                    }
                    correctOwnRule += Math.min(gold.get(s), count);
                }
                System.out.println(goldRules.size());
                System.out.println(ownRules.size());
                System.out.println(correctOwnRule - original);
                correctGoldRule += goldRules.size();
                totalOwnRule += ownRules.size();
                total++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(correctOwnRule);
        System.out.println(totalOwnRule);
        System.out.println(correctGoldRule);
        double precision = (double)correctOwnRule / totalOwnRule;
        double recall = (double)correctOwnRule / correctGoldRule;
        double F = 2 * precision * recall / (precision + recall);
        System.out.println("precision is " + precision);
        System.out.println("recall is " + recall);
        System.out.println("F is " + F);
    }

    private MyNode getTree(String sentence) {
        Stack<MyNode> tagStack = new Stack<MyNode>();
        int bracket = 0;
        String token = "";
        MyNode root = null;
        for (int i = 0; i < sentence.length(); i++) {
            if (sentence.charAt(i) == '(') {
                bracket++;
                if (token.length() > 0) {
                    token = FilterSpace(token);
                    tagStack.push(new MyNode(token));
                    token = "";
                } else {
                    continue;
                }
            } else if (sentence.charAt(i) == ')') {
                bracket--;
                if (token.length() > 0) {
                    token = FilterSpace(token);
                    String[] temps = token.split("\\s+");
                    if (temps.length != 2) {
                        System.out.println("Token has problem: " + token);
                    }
                    tagStack.push(new MyNode(temps[0]));
                    token = "";
                }
                if (tagStack.isEmpty()) {
                    continue;
                }

                MyNode child = tagStack.pop();
                // because child node pop up, we know all its children
                // in BuildCFGs(), child is actually the parent

                if (!tagStack.isEmpty()) {
                    MyNode parent = tagStack.peek();
                    parent.childrenPointer.add(child);
                } else {
                    root = child;
                    // already reach root
                    // add dummy node
//                    MyNode dummy = new MyNode("SENTENCE");
//                    dummy.children.add(child.GetName());
                    continue;
                }

            } else if (sentence.charAt(i) == ' ') {
                if (token.length() == 0) {
                    continue;
                } else {
                    token += sentence.charAt(i);
                }
            } else {
                token += sentence.charAt(i);
            }
        }
        return root;
    }

    private List<String> countRules(MyNode node) {
        List<String> result = new ArrayList<String>();
        if (node == null) {
            return result;
        }
        helper(node, result);
        return result;
    }

    private void helper(MyNode node, List<String> result) {
        if (node.childrenPointer.size() == 0) {
            return;
        }
        for (MyNode child: node.childrenPointer) {
            helper(child, result);
        }
        for (MyNode child: node.childrenPointer) {
            if (child.leaf.size() == 0) {
                node.leaf.add(child);
            }
            else {
                for (MyNode n: child.leaf) {
                    node.leaf.add(n);
                }
            }
        }
        StringBuilder builder = new StringBuilder();
        for (MyNode n: node.leaf) {
            builder.append(n.GetName() + "\t");
        }
        result.add(builder.toString());
    }

    private String FilterSpace(String token) {
        int begin = 0;
        int end = token.length() - 1;
        while (begin < token.length()) {
            if (token.charAt(begin) == ' ') {
                begin++;
            } else {
                break;
            }
        }
        while (end >= begin) {
            if (token.charAt(end) == ' ') {
                end--;
            } else {
                break;
            }
        }
        if (begin == token.length()-1) {
            System.out.println("Token has problem: " + token);  // for debug
            return "";
        }
        return token.substring(begin, end+1);
    }

    private void ProcessOneFile(File file) {
        String content = GetFileContent(file);
        FileType fileType = null;
        if (content.contains("<DOC>")) {
            fileType = FileType.HTML;
            ProcessHTMLFile(content);
        } else if (content.charAt(0) == '('){
            fileType = FileType.RAW;
            ProcessRAWFile(content);
        } else {
            System.out.println("File type is wrong: " + file.getName());
            return;
        }
    }

    private String GetFileContent(File file) {
        String content = "";
        String curLine = "";
        try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(reader);
            while ((curLine = bufferedReader.readLine()) != null) {
                content += curLine;
            }
            bufferedReader.close();
            reader.close();
        } catch (FileNotFoundException e) {
            System.out.println("There is no file:" + file.getName());
        } catch (UnsupportedEncodingException e) {
            System.out.println("Cannot encode the file using UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }

    private void ProcessHTMLFile(String content) {
        Document doc = Jsoup.parse(content);
        Elements elements = doc.select("s");
        for (Element element : elements) {
            if (element.text().length() != 0) {
                String sentence = element.text();
                ProcessSentence(sentence);
            }
        }
    }

    private void ProcessRAWFile(String content) {
        String sentence = "";
        int bracket = 0;
        for (int i = 0; i < content.length(); i++) {
            sentence += content.charAt(i);
            if (content.charAt(i) == '(') {
                bracket++;
            } else if (content.charAt(i) == ')') {
                bracket--;
                if (bracket == 0) {
                    ProcessSentence(sentence);
                    // reset sentence
                    sentence = "";
                }
            }
        }
    }

    private void ProcessSentence(String sentence) {
        // make up the sentence first:
        // replace "\n"
        // replace all those multiple spaces with a single space
        sentence = sentence.replace("\n", " ");
        sentence = sentence.replaceAll("\\s+", " ");
        sentence = sentence.trim();
        goldSentences.add(sentence);
    }

    private String GetExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int extensionPos = filename.lastIndexOf('.');
        int lastUnixPos = filename.lastIndexOf('/');
        int lastWindowsPos = filename.lastIndexOf('\\');
        int lastSeparator = Math.max(lastUnixPos, lastWindowsPos);

        int index = lastSeparator > extensionPos ? -1 : extensionPos;
        if (index == -1) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
    }

    private class Comp implements Comparator<File> {
        @Override
        public int compare(File one, File two) {
            String nameOne = one.getName();
            String nameTwo = two.getName();
            // hard code
            int numberOne = Integer.parseInt(nameOne.substring(5, nameOne.indexOf(".", 0)));
            int numberTwo = Integer.parseInt(nameTwo.substring(5, nameTwo.indexOf(".", 0)));
            if (numberOne < numberTwo) {
                return -1;
            }
            else if (numberOne > numberTwo) {
                return 1;
            }
            else {
                return 0;
            }
        }
    }

    public static void main(String[] args) {
        String goldFolder = args[0];
        String ownFile = args[1];
        Scorer scorer = new Scorer(goldFolder);
        scorer.runParseval(ownFile);
    }
}


