## 命令の32bit化

### 共通事項

命令フォーマットは次の2種類

* ABC型
```
MSB                          LSB
+-------+-------+-------+-------+
|opcode |   A   |   B   |   C   |
+-------+-------+-------+-------+
```
* AB型
```
MSB                          LSB
+-------+-------+-------+-------+
|opcode |   A   |      BB       |
+-------+-------+-------+-------+
```

* opcode, A, B, Cフィールドは全て同じビット数(1スロットサイズとする)．
* BBフィールドは2スロットサイズ．
* opcode はオペコード用
* A, B, Cフィールドに入り得る型
  * Register:  レジスタ番号
  * Subscript: 何かの添字っぽいint値
  * int: それ以外のint値
* BBフィールドに入り得る型
  * InstructionDisplacement: 命令へのディスプレイスメント
  * ConstantDisplacement: 定数プールへのディスプレイスメント
  * BigPrimitiveIndex: (OBCファイルのみ) 定数プール中のインデックス．ロード時にディスプレイスメントに変換
  * SmallPrimitive: fixnum, special 定数

### 各型のサイズ変更

eJSCがOBCファイルを生成する時に，ターゲットのビット数に合わせて
命令フォーマットを変更する．ビット数を抽象化するために，
InstructionBinaryFormatクラスを用意した．
```
        static class Bit32 extends InstructionBinaryFormat {
            public Bit32(int specPTag) {
                this.specPTag = specPTag;
            }
            @Override public int instructionBytes() { return 4; }
            @Override public int opcodeBits()       { return 8; }
            @Override public int aBits()            { return 8; }
            @Override public int bBits()            { return 8; }
            @Override public int cBits()            { return 8; }
            @Override public int ptagBits()         { return 3; }
        }

        static class Bit64 extends InstructionBinaryFormat {
            public Bit64(int specPTag) {
                this.specPTag = specPTag;
            }
            @Override public int instructionBytes() { return 8; }
            @Override public int opcodeBits()       { return 16; }
            @Override public int aBits()            { return 16; }
            @Override public int bBits()            { return 16; }
            @Override public int cBits()            { return 16; }
            @Override public int ptagBits()         { return 3; }
        }
```
`specPTag`と`ptagBits`はJSValueを32bit化するときに使う予定．

### Displacementを持つ命令のフォーマットの変更

次の命令はABC型からAB型に変更．
* BIGPRIMITIVE命令 (`number`, `string`, `regexp`, `error`)
  * BBフィールドは ConstantDisplacement
* JUMP, UNCONDJUMP (`jump`, `jumptrue`, `jumpfalse`)
  * BBフィールドは InstructionDisplacement

変更は以下の通り．結構多い．

* (eJSC) OBCFileComposerでこれらの命令をAB型に変更
* (eJSVM) instruction.hの命令アクセサを変更
* Displacement型 -> Instruction/ConstantDisplacement型
  * codeloader.c: 局所変数の型
  * insns-def:/*.idef: オペランドの型
  * vmgen: idefファイルとinstruction.defのパーサ
  * instruction.def: オペランドの型
  * gotta.py: `vmloop-inc` に生成されるコード

### 16bitを超えるfixnumのnumber化

コード生成時にfixnumのオペランドをチェックして，SmallPrimitiveの範囲を
超えていたらnumber命令に切り替える．あまり綺麗な実装ではない．

変更箇所は
* OBCFileComposer.javaの`addFixnumSmallPrimitive`

### miscな修正

* OBCFileComposer.javaの`BIG_ENDIAN`のためにバイトオーダをリバースした後のシフトによる補正
* codeloader.cの`convertToBc` (Bytecodeのバイト長を即値で書いていた)

### フィールドの値の範囲チェック

* SBCのロード時，ファイルからフィールドの値を読み出したときに範囲チェックをする．

## JSValueの32bit化

### `cint_to_fixnum`の廃止

cint_to_fixnumが使われていた箇所は主に以下の通り．

* cintの値であっても，JavaScriptの仕様で32bitに収まることが保証されている箇所があった． -> 30bitは超える可能性があるので`cint_to_number`に変更．
  * 配列の添字として扱われる整数の範囲
  * ビット演算
* バグで62bit以上の可能性があっても`cint_to_fixnum`を使っている箇所． -> `cint_to_number`に変更．
* 定数をFIXNUM型に変換する箇所． -> `FIXNUM_ZERO`, `FIXNUM_ONE`などを(必要であれば追加して)利用
* 別の変換マクロの中身． -> `cint_to_fixnum_nocheck`に改名して仕様を継続．
* 命令からfixnumの即値を取り出す -> `cint_to_fixnum_nocheck`に改名して仕様を継続．

### Fixnumの変更

## フィールドの32bit化

* JSArraySize
  * conversion macro
* JSObjectSize n_props...
* JSIteratorSize -> # of properties

## cint の64bit化


