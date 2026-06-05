import sys

""""
00000000  31 00 18 69 56 49 45 52  47 45 45 54 08 08 20 45  |1..iVIERGEET.. E|
00000010  54 20 45 4e 46 41 4e 54  28 35 41 29 be 00 02 5d  |T ENFANT(5A)...]|
00000020  68 75 12 4a 88 61 80 ff  80 ff 80 ff 80 ff 80 ff  |hu.J.a..........|


00000000  ?  00 FS ?  [filename---------------------------  |1..iVIERGEET.. E| FS = filename size
00000010  -----------------------------------] be 00 02 5d  |T ENFANT(5A)...]|
00000020  [ TA] 12 4a 88 61 [data------------------------]  |hu.J.a..........| TA = target address

Target address: 0x7568
"""

"""
Checksum
0       0x59
1       0x5B   (digit << 1) ^ 0x59
2       0x5D
3       0x5F
4       0x51
5       0x53
6       0x55

0x10    0x79
0x80    0x58


0x58 0x00    0x5B (3?)
0x58 0x01    0x59 (1?)
0x58 0x02    0x5F (7?)
0x58 0x03    0x5D (5?)
0x58 0x04    0x53
"""

def checksum_byte(nb):
    seed = 0x59
    nb_s = nb << 1
    nb_ror = (nb_s & 0xFF) | ((nb_s & 0xF00) >> 8)
    return seed ^ nb_ror

def checksum_bytes(d):
    seed = 0x56
    for nb in d:
        seed = nb ^ seed
        seed_s = seed << 1
        seed = (seed_s & 0xFF) | ((seed_s & 0xF00) >> 8)
    return seed

filename_bin = sys.argv[1]
filename = bytes(filename_bin[:-4].upper(), 'UTF8')
filename = b'IMAGE'
filename_dai = filename_bin[:-3] + 'dai'

with open(filename_bin, 'rb') as f:
    d = f.read()

payload_size = len(d)
nb_pages = payload_size >> 12
payload_size_high = payload_size >> 8
payload_size_low = payload_size & 0xFF
start_addr = (0xC - nb_pages) << 4
start_addr_checksum = checksum_bytes([0x00, start_addr])

payload_size_high = payload_size >> 8
payload_size_low = payload_size & 0xFF
size_checksum = checksum_bytes([payload_size_high, payload_size_low])

payload_checksum = checksum_bytes(d)
"""
if start_addr == 0x40:
    start_addr_checksum = 0xD9
    size_checksum = 0x5B
    payload_checksum = 0x71
elif start_addr == 0x30:
    start_addr_checksum = 0x30
    size_checksum = 0x30
elif start_addr == 0x20:
    start_addr_checksum = 0x19
    size_checksum = 0xDB
    payload_checksum = 0xE1
else:
    print("??????")
    exit()
"""
dai_header = [0x31, 0] + \
             [len(filename), checksum_byte(len(filename))] + \
             list(filename) + [checksum_bytes(filename)] + \
             [0x00, 0x02, 0x5d] + \
             [0x00, start_addr, start_addr_checksum] + \
             [payload_size_high, payload_size_low] + [size_checksum]
dai_file = bytes(dai_header) + d + bytes([payload_checksum,])

with open(filename_dai, 'wb') as f:
    f.write(dai_file)
print(filename_bin, " => ", filename_dai)

""""
TOBYBAS.dai
00000000  30 00 09 4b 54 4f 42 59  20 31 39 38 32 66 00 fd  |0..KTOBY 1982f..|
00000010  a2 17 00 01 9f 14 00 00  00 05 14 00 00 00 0f 14  |................|
00000020  00 00 00 00 14 00 00 00  08 1c 00 0a aa 40 02 15  |.............@..|
00000030  00 00 1c ed 15 00 00 1d  06 ff a3 01 40 09 8f 40  |............@..@|
00000040  02 40 09 ac 40 02 6b 00  14 a2 67 23 46 35 2c 23  |.@..@.k...g#F5,#|
00000050  43 35 2c 23 44 35 2c 23  45 35 2c 23 33 45 2c 23  |C5,#D5,#E5,#3E,#|
00000060  46 30 2c 23 33 32 2c 23  34 30 2c 23 30 30 2c 23  |F0,#32,#40,#00,#|
00000070  33 32 2c 23 30 36 2c 23  46 44 2c 23 43 44 2c 23  |32,#06,#FD,#CD,#|
00000080  30 46 2c 23 45 46 2c 23  33 45 2c 23 33 30 2c 23  |0F,#EF,#3E,#30,#|
00000090  33 32 2c 23 34 30 2c 23  30 30 2c 23 33 32 2c 23  |32,#40,#00,#32,#|
000000a0  30 36 2c 23 46 44 2c 23  43 33 2c 23 34 44 2c 23  |06,#FD,#C3,#4D,#|
000000b0  43 31 04 00 1e 9a 0b 04  00 20 ad 00 3b 00 21 ad  |C1....... ..;.!.|
000000c0  01 20 18 33 41 50 50 55  59 45 5a 20 53 55 52 20  |. .3APPUYEZ SUR |
000000d0  52 45 54 55 52 4e 20 50  4f 55 52 20 4c 41 4e 43  |RETURN POUR LANC|
000000e0  45 52 20 4c 41 20 4c 45  43 54 55 52 45 20 44 55  |ER LA LECTURE DU|
000000f0  20 44 45 53 53 49 4e ff  09 00 32 b3 15 00 00 1c  | DESSIN...2.....|
00000100  ed ff 04 00 37 9a 0a 05  00 3c 89 00 3c 00 02 00  |....7....<..<...|
00000110  33 3f 11 58 14 00 00 00  00 11 59 14 00 00 00 00  |3?.X......Y.....|
00000120  01 41 04 00 00 00 00 03  52 57 31 04 00 00 00 00  |.A......RW1.....|
00000130  04 54 4f 42 59 04 00 00  00 00 04 4d 49 53 54 04  |.TOBY......MIST.|
00000140  00 00 00 00 00 75                                 |.....u|

00000000  FT 00 FS 4b [Filename-----------------] 66 00 fd  |0..KTOBY 1982f..| FT=file type, FS=filename size
00000010  a2 [data---------------------------------------]  |................|


TOBYDATA.dai
00000000  31 00 04 51 54 4f 42 59  e1 00 02 5d 68 75 12 4a  |1..QTOBY...]hu.J|
00000010  98 41 00 00 00 00 00 00  00 00 00 00 00 00 00 00  |.A..............|
00000020  00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00  |................|
...
00004a90  00 00 00 00 00 00 00 00  40 20 00 00 b8 36 00 00  |........@ ...6..|
00004aa0  a0 36 00 00 9f 36 00 00  85 36 09                 |.6...6...6.|

00000000  FT 00 FS CK [Filename ]  e1 00 02 5d [TA ] 12 [S  |1..QTOBY...]hu.J| S = payload size, CK=checksum filename
00000010  -] 41 00 00 00 00 00 00  00 00 00 00 00 00 00 00  |.A..............|
00000020  00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00  |................|
...
00004a90  00 00 00 00 00 00 00 00  40 20 00 00 b8 36 00 00  |........@ ...6..|
00004aa0  a0 36 00 00 9f 36 00 00  85 36 09                 |.6...6...6.|


"""
