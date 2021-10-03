import cv2
import numpy as np

from cell_types import CellTypeDict
from ReadFile import readGC

BLACK = (0,0,0)
CELL_LEN = 8

def original_analyze(snap_shot, title):
  total_bytes = 0
  for byte, _ in snap_shot:
    total_bytes += byte
  width = 8
  height = (total_bytes + 63) // 64
  img = np.zeros((height*CELL_LEN, width*CELL_LEN, 3), dtype=np.uint8)
  hw = 0
  object_nums = [0 for _ in range(33)]
  object_bytes = [0 for _ in range(33)]
  for byte, type in snap_shot:
    object_nums[type] += 1
    object_bytes[type] += byte
    section_num = byte // 8
    for i in range(section_num):
      h, w = hw // width, hw % width
      img[h*CELL_LEN:(h+1)*CELL_LEN, w*CELL_LEN:(w+1)*CELL_LEN] = CellTypeDict[type][1]
      img[(h+1)*CELL_LEN-1:(h+1)*CELL_LEN, w*CELL_LEN:(w+1)*CELL_LEN] = BLACK
      if i == section_num-1:
        img[h*CELL_LEN:(h+1)*CELL_LEN, (w+1)*CELL_LEN-1:(w+1)*CELL_LEN] = BLACK
      hw += 1

  cv2.imwrite("{0}.png".format(title), img)

  print("===== {0} =====".format(title))
  cache_lines = height
  total_bytes = 64 * cache_lines
  print("cache line: {0}, total byte: {1}".format(cache_lines, total_bytes))
  print("{0} KB".format(total_bytes//1024))
  for key, val in CellTypeDict.items():
    print("object type: {0}, count: {1}, byte: {2}".format(val[0], object_nums[key], object_bytes[key]))
  print("===== end =====")

def alignment_analyze(snap_shot, title):
  width = 8
  height = 0
  for byte, _ in snap_shot:
    height += (byte + 63) // 64

  img = np.zeros((height*CELL_LEN, width*CELL_LEN, 3), dtype=np.uint8)
  h = 0
  object_nums = [0 for _ in range(33)]
  object_bytes = [0 for _ in range(33)]
  for byte, type in snap_shot:
    object_nums[type] += 1
    object_bytes[type] += byte
    section_num = byte // 8
    hs = section_num // 8
    re = section_num  % 8
    for _ in range(hs):
      img[h*CELL_LEN:(h+1)*CELL_LEN, 0:width*CELL_LEN] = CellTypeDict[type][1]
      img[(h+1)*CELL_LEN-1:(h+1)*CELL_LEN, 0:width*CELL_LEN] = BLACK
      h += 1

    if re == 0:
      continue

    img[h*CELL_LEN:(h+1)*CELL_LEN, 0:re*CELL_LEN] = CellTypeDict[type][1]
    img[(h+1)*CELL_LEN-1:(h+1)*CELL_LEN, 0:re*CELL_LEN] = BLACK
    img[h*CELL_LEN:(h+1)*CELL_LEN, re*CELL_LEN-1:re*CELL_LEN] = BLACK
    h += 1

  cv2.imwrite("{0}.png".format(title), img)

  print("===== {0} =====".format(title))
  cache_lines = height
  total_bytes = 64 * cache_lines
  print("cache line: {0}, total byte: {1}".format(cache_lines, total_bytes))
  print("{0} KB".format(total_bytes//1024))
  for key, val in CellTypeDict.items():
    print("object type: {0}, count: {1}, byte: {2}".format(val[0], object_nums[key], object_bytes[key]))
  print("===== end =====")

result = readGC("../build.debug/a.txt")
for i, snap_shot in enumerate(result):
  original_analyze(snap_shot, "original{0}".format(i+1))
  alignment_analyze(snap_shot, "alignment{0}".format(i+1))
