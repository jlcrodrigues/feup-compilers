package pt.up.fe.comp2023.optimization;

import org.specs.comp.ollir.Instruction;

import java.util.HashSet;
import java.util.Set;

public class LivenessNode {
    private int id;
    private final Set<String> def;
    private final Set<String> use;
    private final Set<String> liveIn;
    private final Set<String> liveOut;

    public LivenessNode(int id) {
        this.id = id;
        def = new HashSet<>();
        use = new HashSet<>();
        liveIn = new HashSet<>();
        liveOut = new HashSet<>();
    }

    public boolean addDef(String def) {
        return this.def.add(def);
    }

    public boolean addUse(String use) {
        return this.use.add(use);
    }

    /**
     * Update liveIn set <br>
     * liveIn(n) = use(n) U (liveOut(n) - def(n))
     */
    public boolean updateLiveIn(Instruction instruction) {
        boolean changed = liveIn.addAll(use);

        // liveOut - def
        Set<String> temp = new HashSet<>(liveOut);
        temp.removeAll(def);

        return changed || liveIn.addAll(temp);
    }

    public boolean addOut(Set<String> successorLiveIn) {
        return liveOut.addAll(successorLiveIn);
    }

    public Set<String> getDef() {
        return def;
    }

    public Set<String> getUse() {
        return use;
    }

    public Set<String> getLiveIn() {
        return liveIn;
    }

    public Set<String> getLiveOut() {
        return liveOut;
    }

    public int getId() {
        return id;
    }

    public String toString() {
        return "LivenessNode(" + id + "): " + def + " " + use + " " + liveIn + " " + liveOut;
    }
}
