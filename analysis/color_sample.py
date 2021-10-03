import cv2
import numpy as np

from cell_types import CellTypeDict, WHITE

BLACK = (0,0,0)
FONT_TYPE = cv2.FONT_HERSHEY_PLAIN
SECTION_H = 200
SECTION_W = 500
PREFIX_LEN = 6

def create():
  hs = 5
  ws = 5
  img = np.zeros((SECTION_H * hs, SECTION_W * ws, 3), dtype=np.uint8)
  hw = 0
  for key, val in CellTypeDict.items():
    h = hw // ws
    w = hw % ws
    img[h*SECTION_H:(h+1)*SECTION_H, w*SECTION_W:(w+1)*SECTION_W] = val[1]
    cv2.putText(img, val[0][PREFIX_LEN:], (w*SECTION_W, h*SECTION_H+SECTION_H//3), FONT_TYPE, 3, BLACK, 2)

    hw += 1

  cv2.imwrite("label.png", img)

create()
