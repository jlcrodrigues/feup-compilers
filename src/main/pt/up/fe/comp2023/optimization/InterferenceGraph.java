package pt.up.fe.comp2023.optimization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class InterferenceGraph {
    private HashMap<String, Set<String>> graph;

    public InterferenceGraph(ArrayList<LivenessNode> nodes) {
        graph = new HashMap<>();
        for (LivenessNode node: nodes) {
            HashSet<String> defAndOut = new HashSet<>(node.getDef());
            defAndOut.addAll(node.getLiveOut());

            for (String in : node.getLiveIn()) {
                for (String out : defAndOut) {
                    if (!in.equals(out)) {
                        addEdge(in, out);
                    }
                }
            }
        }
    }

    private void addEdge(String a, String b) {
        if (!graph.containsKey(a))
            graph.put(a, new HashSet<>());
        if (!graph.containsKey(b))
            graph.put(b, new HashSet<>());

        graph.get(a).add(b);
        graph.get(b).add(a);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String key: graph.keySet()) {
            sb.append(key).append(": ");
            for (String value: graph.get(key)) {
                sb.append(value).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
