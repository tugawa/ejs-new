/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmdlc;

import java.util.HashMap;

public class Option {
    boolean mDisableMatchOptimisation = false;
    public boolean disableMatchOptimisation() {
        return mDisableMatchOptimisation;
    }

    HashMap<AvailableOptions, Object> options = new HashMap<AvailableOptions, Object>();

    public enum AvailableOptions {
        CMP_VERIFY_DIAGRAM("cmp:verify_diagram", Boolean.class),
        CMP_OPT_PASS("cmp:opt_pass", String.class),
        CMP_RAND_SEED("cmp:rand_seed", Integer.class),
        CMP_TREE_LAYER("cmp:tree_layer", String.class),
        GEN_USE_GOTO("gen:use_goto", Boolean.class),
        GEN_PAD_CASES("gen:pad_cases", Boolean.class),
        GEN_USE_DEFAULT("gen:use_default", Boolean.class),
        GEN_MAGIC_COMMENT("gen:magic_comment", Boolean.class),
        GEN_DEBUG_COMMENT("gen:debug_comment", Boolean.class),
        GEN_LABEL_PREFIX("gen:label_prefix", String.class),
        GEN_ADD_TYPELABEL("gen:type_label", Boolean.class);

        String key;
        Class<?> cls;
        AvailableOptions(String key, Class<?> cls) {
            this.key = key;
            this.cls = cls;
        }
    };

    int addOption(String opt, String[] args, int index) {
        for (AvailableOptions os: AvailableOptions.values()) {
            if (opt.equals("-X" + os.key)) {
                if (os.cls == String.class)
                    options.put(os, args[index]);
                else if (os.cls == Boolean.class)
                    options.put(os, Boolean.parseBoolean(args[index]));
                else if (os.cls == Integer.class)
                    options.put(os, Integer.parseInt(args[index]));
                return index + 1;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    public <T> T getOption(AvailableOptions opt, T defaultValue) {
        Object val = options.get(opt);
        if (val == null)
            return defaultValue;
        return (T) val;
    }
}
