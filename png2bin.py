import sys
from operator import itemgetter
from PIL import Image

if len(sys.argv) < 2:
    print("Usage: " + sys.argv[0] + " <image>")
    exit()

filename_img = sys.argv[1]
filename_bin = filename_img[:-3] + 'bin'
img = Image.open(filename_img)

d = [0x36, 0x80, 0x00, 0x00,
     0x36, 0x9A, 0x00, 0x00,
     0x36, 0xA3, 0x00, 0x00,
     0x36, 0xBF, 0x00, 0x00]

d2 = [0x36, 0x80, 0x00, 0x00,
     0x36, 0x81, 0x00, 0x00,
     0x36, 0x82, 0x00, 0x00,
     0x36, 0x83, 0x00, 0x00,
     0x36, 0x84, 0x00, 0x00,
     0x36, 0x85, 0x00, 0x00,
     0x36, 0x86, 0x00, 0x00,
     0x36, 0x87, 0x00, 0x00,
     0x36, 0x88, 0x00, 0x00,
     0x36, 0x89, 0x00, 0x00,
     0x36, 0x8A, 0x00, 0x00,
     0x36, 0x8B, 0x00, 0x00,
     0x36, 0x8C, 0x00, 0x00,
     0x36, 0x8D, 0x00, 0x00,
     0x36, 0x8E, 0x00, 0x00,
     0x36, 0x8F, 0x00, 0x00]

d = []

res = []
nb = 1

while len(res) < 400:
    res += [True]*nb
    res += [False]*nb
    nb *= 2

color_codes = {}
dsdf = {
    48: 0xDB,
    49: 0xE2,
    96: 0xDA,
    97: 0xE3,
    144: 0xDE,
    145: 0xE7,
    192: 0xDD,
    193: 0xE5,
}

def export_16():
    width = img.width
    height = img.height
    for y in range(height):
        d.append(0xB0)
        d.append(0x40)
        for x in range(0, width, 8):
            cols = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]
            for pixel in range(8):
                color = img.getpixel((x+pixel,y))
                cols[color] += 1
            nb_cols = 0
            for col in cols:
                if col > 0:
                    nb_cols += 1
            sorted_cols = sorted(enumerate(cols), key=itemgetter(1), reverse=True)
            top_two_cols = sorted_cols[:2]
            top_col = top_two_cols[0][0]
            other_col = top_two_cols[1][0]
            col_mapping = [i != top_two_cols[1][0] for i in range(16)]
            mask = 0x80
            val = 0
            for pixel in range(8):
                color = img.getpixel((x+pixel,y))
                if col_mapping[color]:
                    val += mask
                mask = mask >> 1
            d.append(val)
#            d.append(0xF0)
            d.append((top_col << 4) | (other_col))


def export():
    if img.width == 528:
        width = 528
        ctrl_code = 0x30
    elif img.width == 352:
        width = 352
        ctrl_code = 0x20
    else:
        print("Unknown size")
        exit()
    height = img.height

    for y in range(height):
#        if res[y]:
#            ctrl_code = 0x30
#            width = 528
#        else:
#            ctrl_code = 0x20
#            width = 352
        d.append(ctrl_code)
        if y in color_codes:
            d.append(color_codes[y])
        else:
            d.append(0x40)
        for x in range(0, width, 8):
            mask = 0x80
            val1 = 0
            val2 = 0
            for pixel in range(8):
                color = img.getpixel((x+pixel,y))
                if color & 1:
                    val1 += mask
                if color & 2:
                    val2 += mask
                mask = mask >> 1
            d.append(val1)
            d.append(val2)
    
    return
    for y in range(4):
        if res[y]:
#        if y < 120:
            d.append(0x30)
            width = 528
        else:
            d.append(0x20)
            width = 352
        d.append(0x40)
        for x in range(0, width, 8):
            mask = 0x80
            val1 = 0
            val2 = 0
            for pixel in range(8):
                color = img.getpixel((x+pixel,y))
                if color & 1:
                    val1 += mask
                if color & 2:
                    val2 += mask
                mask = mask >> 1
            d.append(val1)
            d.append(val2)

def analyze():
    for y in range(img.height):
        for x in range(0, 352-16, 8):
            colors = set()
            for pixel in range(8):
                color = img.getpixel((x+pixel+2, y))
                colors.add(color)
#            if len(colors) > 2:
#                print("X={}, Y={} : {} colors".format(x, y, colors))
    print()
    for y in range(img.height):
        colors = set()
        for x in range(0, 352):
            color = img.getpixel((x, y))
            colors.add(color)
        if len(colors) > 4:
            print("Line {} : {} colors ({})".format(y, len(colors), colors))

    exit()

analyze()
export()
nb_bytes = len(d)
print("Size: ", nb_bytes)
padding = 4096 - nb_bytes % 4096
print("Padding: ", padding)
d += [0] * padding

d.reverse()

with open(filename_bin, 'wb') as f:
    f.write(bytes(d))
#analyze()
