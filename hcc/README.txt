./ejsvm --dump-hcg aaa.hcg aaa.sbc
python hcc.py --dot aaa.dot aaa.hcg
dot -Tpng aaa.dot > aaa.png
