package ChineseParse;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ChenChen on 5/4/16.
 */
public class MyNode {
    private String name;
    public List<String> children;
    // added by Wenzhao
    public List<MyNode> childrenPointer;
    public List<MyNode> leaf;

    public MyNode(String n) {
        name = n;
        children = new ArrayList<String>();
        childrenPointer = new ArrayList<MyNode>();
        leaf = new ArrayList<MyNode>();
    }

    public String GetName() {
        return name;
    }
}
