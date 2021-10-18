import re

# [[byte, type], ... ]
def readGC(file_path):
  gc_cnt = -1
  ret = []
  with open(file_path, "r") as f:
    file_end = False
    during_dump = False
    while not file_end:
      line = f.readline()
      if "Total Runtime" in line:
        file_end = True
        continue

      if "GC" in line:
        ret.append([])
        gc_cnt += 1
        continue

      if len(line) > 0 and line[0] == 'D':
        byte, type, address, hidden_class = map(lambda x: int(x),re.findall(r"\d+", line))
        ret[gc_cnt].append((byte, type, address, hidden_class))

  return ret
