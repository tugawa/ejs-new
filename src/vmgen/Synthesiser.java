/*
 * eJS Project
 * Kochi University of Technology
 * The University of Electro-communications
 *
 * The eJS Project is the successor of the SSJS Project at The University of
 * Electro-communications.
 */
package vmgen;

public abstract class Synthesiser implements GlobalConstantOptions {
    public abstract String synthesise(RuleSet plan, String prefix, InsnGen.Option option);

    public String getPTCode(String dispatchVar) {
        return "get_tag("+dispatchVar+")";
    }

    protected String getPTCode(String[] dispatchVars) {
        return getPTCode(dispatchVars[0]);
    }

    public String getHTCode(String dispatchVar) {
        return "gc_obj_header_type((void*) clear_tag("+dispatchVar+"))";
    }

    protected String getHTCode(String[] dispatchVars) {
        return getHTCode(dispatchVars[0]);
    }

    public String composeTagPairCode(String... ptcodes) {
        return "TAG_PAIR("+ptcodes[0]+", "+ptcodes[1]+")";
    }

    protected String getTagPairCode(String[] dispatchVars) {
        return composeTagPairCode(getPTCode(dispatchVars[0]), getPTCode(dispatchVars[1]));
    }
}
