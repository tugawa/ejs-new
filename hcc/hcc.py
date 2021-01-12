import sys
import re
import argparse

BYTES_IN_JSVALUE = 8

class Field:
    field_size = {
        'UD': 1,
        'I1': 1,
        'I2': 2,
        'I4': 4,
        'I8': 8,
        'B':  1,
        'NL': 1,
        'F': 4,
        'D': 8,
        'J': 8,
    }

    def __init__(self, field_name, field_type, offset):
        self.name = field_name
        self.type = field_type
        self.offset = offset

    def size(self):
        return Field.field_size[self.type]

    def byte_range(self):
        return range(self.offset, self.offset + self.size())

    def upper_compatible(self, other):
        if self.type ==  other.type:
            return True
        if other.type == 'UD':
            return True
        if self.type == 'J':
            return True
        if self.type == 'D':
            return other.type in {'F', 'I4', 'I2', 'I1'}
        if self.type == 'F':
            return other.type in {'I2', 'I1'}
        if self.type == 'I8':
            return other.type in {'I4', 'I2', 'I1'}
        if self.type == 'I4':
            return other.type in {'I2', 'I1'}
        if self.type == 'I2':
            return other.type in {'I1'}
        return False

class Edge:
    def __init__(self, prop_name, prop_type, dest):
        self.prop_name = prop_name
        self.prop_type = prop_type
        self.dest = dest

class Node:
    address_table = {}
    last_node_id = -1

    def next_node_id():
        Node.last_node_id += 1
        return Node.last_node_id
    
    def __init__(self, line):
        # HC
        # 1              2              3 4  5 6         7 8     9     10    11
        # 0x7f8fa12058b0 0x7f8fa1205a10 N 26 1 aircraftA J 10830 10830 (tgen) 1
        xs = line.split(' ')
        self.line = line.rstrip()
        self.this_addr = xs[1]
        self.prev_addr = xs[2]
        self.is_entry = xs[3] == "E"
        self.loc = (int(xs[4]), int(xs[5]))
        self.prop_name = xs[6]
        self.prop_type = xs[7]
        self.n_entry = int(xs[8])
        self.n_leave = int(xs[9])
        self.name = xs[10]

        # derived attributes
        self.node_id = Node.next_node_id()
        self.prev = None
        self.transitions = []
        self.preds = []
        self.fields = []
        self.used_bytes = []
        
        Node.address_table[self.this_addr] = self

    def convert_address(self):
        if self.prev_addr == "0x0":
            self.prev = None
        else:
            self.prev = Node.address_table[self.prev_addr]
            self.preds.append(self.prev)
            
    def add_transition(self, prop_name, prop_type, n):
        e = Edge(prop_name, prop_type, n)
        self.transitions.append(e)

    def short_string(self):
        s = "HC id=%d prev=%d prop=%d size=%d" % (self.node_id, self.prev.node_id,
                                                  self.n_props(), self.size())
        if self.is_entry:
            s += " Entry(%d, %d)" % self.loc
        else:
            s += " %s:%s" % (self.prop_name, self.prop_type)
        s += " %d %d" % (self.n_entry, self.n_leave)
        if self.is_graveyard():
            s += " GRAVE"
        return s

    def n_props(self):
        return len(self.fields)

    def size(self):
        return len(self.used_bytes)
        
    def is_graveyard(self):
        return self.n_leave < self.n_entry
        
    def find_field_by_name(self, name):
        for f in self.fields:
            if f.name == name:
                return f
        return None
        
    def used_bytes_alloc(self, size):
        offset = 0
        while True:
            if len(self.used_bytes) <= offset:
                self.used_bytes.extend([False] * BYTES_IN_JSVALUE)
            if True not in self.used_bytes[offset:offset+size]:
                for i in range(offset, offset + size):
                    self.used_bytes[i] = True
                return offset
            offset += size

    def used_bytes_free(self, offset, size):
        for i in range(offset, offset + size):
            assert(self.used_bytes[i] == True)
            self.used_bytes[i] = False

    def remove_field(self, prop_name):
        f = self.find_field_by_name(prop_name)
        if f:
            self.used_bytes_free(f.offset, f.size())
            self.fields.remove(f)

    def add_field(self, prop_name, prop_type):
        size = Field.field_size[prop_type]
        offset = self.used_bytes_alloc(size)
        f = Field(prop_name, prop_type, offset)
        self.fields.append(f)
        
    def collect_fields(self):
        if not self.is_entry:
            # inherit from previous
            self.fields = self.prev.fields[:]
            self.used_bytes = self.prev.used_bytes[:]
            # remove old field of the same name (case of type change)
            self.remove_field(self.prop_name)
            # add field
            self.add_field(self.prop_name, self.prop_type)
            self.fields.sort(key=lambda f: f.offset)
        for e in self.transitions:
            e.dest.collect_fields()

    def overwrite_contents(self, n):
        self.transitions = n.transitions
        self.fields = n.fields
        self.used_bytes = n.used_bytes
        self.n_leave = n.n_leave
        for t in self.transitions:
            succ = t.dest
            succ.prev = self
            if n in succ.preds:
                succ.preds.remove(n)
            succ.preds.append(self)
    
    def skip_internal(self):
        def is_redundant(node):
            if len(node.transitions) != 1:
                return False
            if node.size() == node.transitions[0].dest.size():
                # transition does not apply any spatial penalty
                return True
            return not node.is_graveyard()
        while is_redundant(self):
            n = self.transitions[0].dest
            self.overwrite_contents(n)
        for t in self.transitions:
            t.dest.skip_internal()

    def upper_compatible(self, other):
        verbose = False
        if self.node_id == 99 and other.node_id == 102:
            verbose = True
        if self.n_props() != other.n_props():
            if verbose: print("different size")
            return False
        for i in range(0, self.n_props()):
            fa = self.fields[i]
            fb = other.fields[i]
            if not fa.upper_compatible(fb):
                return False
            for i in fa.byte_range():
                if i in fb.byte_range():
                    continue
                if i in other.used_bytes and not other.used_bytes[i]:
                    continue
                return False
        return True

    def find_transition_to(self, node):
        for t in self.transitions:
            if t.dest == node:
                return t
        return None

    def merge(self, other):
        # replace transitions to the other to this, and
        # adjust edges to predicessors
        for pred in other.preds:
            print("----")
            print(pred.short_string())
            print([x.dest.node_id for x in pred.transitions])
            print(other.short_string())
            print([x.node_id for x in other.preds])
            t = pred.find_transition_to(other)
            pred.transitions.remove(t)
            if pred not in self.preds:
                pred.add_transition(t.prop_name, t.prop_type, self)
                self.preds.append(pred)

        # merge successors set
        for t in other.transitions:
            if not self.find_transition_to(t.dest):
                self.transitions.append(t)

        # adjust other fields
        self.n_entry += other.n_entry
        self.n_leave += other.n_leave

def load_graph(lines):
    nodes = []

    # create node
    for line in lines:
        n = Node(line)
        nodes.append(n)

    # convert addr to node object
    for n in nodes:
        n.convert_address()

    # remove bot node
    for n in nodes:
        while n.prev and n.prev.prop_type == "bot" and not n.prev.is_entry:
            n.preds.remove(n.prev)
            n.preds.append(n.prev.prev)
            n.prev = n.prev.prev
    nodes = [n for n in nodes if n.prop_type != "bot" or n.is_entry]

    # create transition
    for n in nodes:
        if n.prev:
            n.prev.add_transition(n.prop_name, n.prop_type, n)
        
    entrypoints = [n for n in nodes if n.is_entry]

    # remove builtin
    entrypoints = [n for n in entrypoints if n.loc != (0, 0)]

    return entrypoints

# assume input is a tree
def merge_compatible_nodes(entrypoint):
    def collect_nodes(node):
        nodes = [node]
        for t in node.transitions:
            nodes += collect_nodes(t.dest)
        return nodes
    nodes = collect_nodes(entrypoint)
    for i in range(1, len(nodes)):
        na = nodes[i]
        for j in range(0, i):
            nb = nodes[j]
            if na.upper_compatible(nb):
                na.merge(nb)
            elif nb.upper_compatible(na):
                nb.merge(na)

def indent_print_node(file, indent, node):
    write(file, ("  " * indent) + node.short_string() + "\n")
    for f in node.fields:
        print(file,
              ("  " * indent) + "- (" + str(f.offset) + ")" + f.name + ":" + f.type + "\n")
    for e in node.transitions:
        n = e.dest
        indent_print_node(file, indent + 1, n)

def print_graph(f, roots):
    for n in roots:
        indent_print_node(f, 0, n)

def dot_output_node(fp, node):
    node_label = "[%d], die=%d\n" % (node.node_id, node.n_entry - node.n_leave)
    for f in node.fields:
        node_label += "%d %s:%s\l" % (f.offset, f.name, f.type)

    fp.write("%d [label=\"%s\"]\n" % (node.node_id, node_label))
    for t in node.transitions:
        n = t.dest
        tr_label = str(n.n_entry)
        fp.write("%d -> %d [label=\"%s\"]\n" % (node.node_id, n.node_id, tr_label))
        dot_output_node(fp, n)

def dot_output(fp, entrypoints):
    ROOT_ID = 10000
    fp.write("digraph G {\n")
    fp.write("%d [label=\"(root)\"]\n" % ROOT_ID)
    for n in entrypoints:
        fp.write("%d -> %d [label=\"(new@%d:%d)\n%d\"]\n" %
                 (ROOT_ID, n.node_id, n.loc[0], n.loc[1], n.n_entry))
        dot_output_node(fp, n)
    fp.write("}\n")

def main():
#    with open(sys.argv[1]) as f:
#        entrypoints = load_graph(f)
    args = process_argv()
    with open(args.input) as f:
        entrypoints = load_graph(f)

    for n in entrypoints:
        n.collect_fields()
        if not args.no_compile:
            n.skip_internal()
#            merge_compatible_nodes(n)

    if args.chcg:
        with open(args.chcg, "w") as f:
            print_graph(f, entrypoints)
#    dot_output(sys.stdout, entrypoints)
    if args.dot:
        with open(args.dot, "w") as f:
            dot_output(f, entrypoints)

def process_argv():
    ap = argparse.ArgumentParser()
    ap.add_argument("input", type = str)
    ap.add_argument("--dot", action = "store", type = str)
    ap.add_argument("--no-compile", action = "store_true")
    ap.add_argument("--chcg", action = "store", type = str)
    args = ap.parse_args()
    return args
    
if __name__ == "__main__":
    main()
    
