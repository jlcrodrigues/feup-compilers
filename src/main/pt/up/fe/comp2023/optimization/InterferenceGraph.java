package pt.up.fe.comp2023.optimization;

import java.util.*;

public class InterferenceGraph {
    private HashMap<String, Set<String>> edges;

    public InterferenceGraph(ArrayList<LivenessNode> nodes) {
        edges = new HashMap<>();
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
        if (!edges.containsKey(a))
            edges.put(a, new HashSet<>());
        if (!edges.containsKey(b))
            edges.put(b, new HashSet<>());

        edges.get(a).add(b);
        edges.get(b).add(a);
    }

    public HashMap<String, Integer> color(int maxRegisters) {
        int k = 0; // used registers
        Stack<String> stack = new Stack<>();

        Set<String> toRemove = new HashSet<>(edges.keySet());
        Iterator<String> it = toRemove.iterator();
        while (it.hasNext()) {
            String key = it.next();
            k = (maxRegisters != 0) ? maxRegisters : Math.max(k, 1 + edges.get(key).size());
            if (edges.get(key).size() > k) return null;
            stack.push(key);
            it.remove();
        }

        HashMap<String, Integer> color = new HashMap<>();
        while (!stack.isEmpty()) {
            String node = stack.pop();
            Set<String> neighbors = edges.get(node);
            Set<Integer> usedColors = new HashSet<>();
            for (String neighbor: neighbors) {
                if (color.containsKey(neighbor)) {
                    usedColors.add(color.get(neighbor));
                }
            }
            for (int i = 0; i < k; i++) {
                if (!usedColors.contains(i)) {
                    color.put(node, i);
                    break;
                }
            }
        }

        return color;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String key: edges.keySet()) {
            sb.append(key).append(": ");
            for (String value: edges.get(key)) {
                sb.append(value).append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
