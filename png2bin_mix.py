import sys
from PIL import Image

if len(sys.argv[0]) < 2:
    print("Enter a filename")
    exit()

filename_png = sys.argv[1]
filename_bin = filename_png[:-3] + 'bin'
img = Image.open(filename_png)
img2 = Image.open("Images/dungeon.png")

palette = [
    (0,0,0),    # black
    (0,0,139),  # dark blue
    (0xdb, 0x3a, 0x38), # purple red
    (255,0,0),  # red

    (66,66,66), # purple brown
    (0x2e, 0xc6, 0x25), # emerand green
    (0xdb, 0x3a, 0x38), # kakhi brown
    (0xc0, 0x2e, 0xeb), # mustard brown

    (0xc2, 0xc1, 0xc7), # grey
    (0x4b, 0x3c, 0xf5), # middle blue
    (0xff, 0xa5, 0x03), # orange
    (0xff, 0x99, 0xfe), # pink

    (128,128,255),  # light blue
    (0x39, 0xfc, 0x82), # light green
    (255,255,0x28), # light yellow
    (255,255,255)   # white
]

d = [0x30, 0x80, 0x00, 0x00,
     0x30, 0x93, 0x00, 0x00,
     0x30, 0xA4, 0x00, 0x00,
     0x30, 0xBE, 0x00, 0x00]

col4_to_dai = {
    0: 0,
    1: 1,
    2: 2,
    9: 3,
}

color_mo5_to_dai = [
    0, 1, 9, 5,
    13, 4, 15, 8,
    14, 2, 7, 11,
    12, 13, 10, 4
]

def export_4_col():
    global d

    for y in range(200):
        d.append(0x30)
        d.append(0x40)
        for x in range(0, 528, 8):
            mask = 0x80
            val1 = 0
            val2 = 0
            for pixel in range(8):
                color = img2.getpixel((x+pixel,y))
                if color & 1:
                    val1 += mask
                if color & 2:
                    val2 += mask
                mask = mask >> 1
            d.append(val1)
            d.append(val2)            

def export_16_col():
    global d
    for y in range(201, img.height):
        d.append(0xA0)
        d.append(0x40)
        for x in range(0, 352, 8):
            color1 = img.getpixel((x, y))
            color2 = color1
            mask = 0x80
            val2 = 0
            for pixel in range(8):
                color = img.getpixel((x+pixel, y))
                if color != color1:
                    color2 = color
                    val2 += mask
                mask = mask >> 1
#            val1 = (color_mo5_to_dai[color2] << 4) | (color_mo5_to_dai[color1])
            val1 = (color2) << 4 | (color1)
            d.append(val2)
            d.append(val1)

    return d

def export_text(mode, caption):
    global d
    d.append(mode)
    d.append(0x40)
    
    i = 1
    for c in caption:
        d.append(0xFD if c == '?' else ord(c))
        d.append((i << 4) | 0)
        i += 1
        if i == 16:
            i = 1

def export_text_inv(mode, caption):
    global d
    d.append(mode)
    d.append(0x40)
    
    i = 1
    for c in caption:
        d.append(0xFD if c == '?' else ord(c))
        print(c, ord(c))
        d.append(i)
        i += 1
        if i == 16:
            i = 1

export_4_col()
export_16_col()
export_text(0xF8, "G?n?ration Micros - G?n?ration Micros - G?n?ration Micros - G?n?ra")
export_text_inv(0xE8, "G?n?ration Micros - G?n?ration Micros - G?n?")
export_text(0xDA, "G?n?ration Micros - G?")

nb_bytes = len(d)
print("Size: ", nb_bytes)
padding = 32768 - nb_bytes
print("Padding: ", padding)
d += [0] * padding
d.reverse()

with open(filename_bin, 'wb') as f:
    f.write(bytes(d))

print(filename_png, "=>", filename_bin)
