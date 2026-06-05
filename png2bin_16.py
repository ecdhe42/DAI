import sys
from PIL import Image

if len(sys.argv[0]) < 2:
    print("Enter a filename")
    exit()

filename_png = sys.argv[1]
filename_bin = filename_png[:-3] + 'bin'
img = Image.open(filename_png)

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

color_mo5_to_dai = [
    0, 1, 9, 5,
    13, 4, 15, 8,
    14, 2, 7, 11,
    12, 13, 10, 4
]

def export_16_col():
    if img.height == 240:
        d = [0x36, 0x80, 0x00, 0x00,
         0x36, 0x9A, 0x00, 0x00,
        0x36, 0xA3, 0x00, 0x00,
        0x36, 0xBF, 0x00, 0x00]
    else:
        d = []
    for y in range(img.height):
        d.append(0xB0)
        d.append(0x40)
        for x in range(0, 528, 8):
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

    nb_bytes = len(d)
    print("Size: ", nb_bytes)
    final_size = 32768
    while final_size < len(d):
        final_size += 4096
    padding = final_size - nb_bytes
    print("Padding: ", padding)
    d += [0] * padding
    d.reverse()
    return d

def draw():
    subpalette = [palette[0], palette[0], palette[0], palette[0]]
    for i in range(0xBFFE, 0xBFEE, -4):
        val = d[i]
        color = val & 0x0F
        color_idx = (val & 0x30) >> 4
        subpalette[color_idx] = palette[color]
    addr = 0xBFEF
    skip = 0
    for y in range(260):
        if skip:
            continue
        ctrl = d[addr]
        nb_lines = ctrl & 0x0F
        res_ctrl = (ctrl & 0x30) >> 4
        mode_ctrl = (ctrl & 0xC0) >> 6
        
#        print("0x{:02x} {:02x}".format(d[addr], d[addr-1]))
        col_ctrl = d[addr-1]
        if col_ctrl & 0x80:
            color = col_ctrl & 0x0F
            color_idx = (col_ctrl & 0x30) >> 4
            subpalette[color_idx] = palette[color]

        addr -= 2
        print("Line {:03}: 0x{:04x}".format(y, addr))

        if mode_ctrl == 2:
            for x in range(0, 352, 8):
                val2 = d[addr]
                val1 = d[addr-1]
                addr -= 2
                mask = 0x80
                color1 = palette[val1 >> 4]
                color2 = palette[val1 & 0x0F]
                for pixel in range(8):
                    if val2 & mask:
                        img.putpixel((x+pixel, y), color1)
                    else:
                        img.putpixel((x+pixel, y), color2)
                    
                    mask = mask >> 1            
        elif mode_ctrl == 0:
            for x in range(0, 352, 8):
                val2 = d[addr]
                val1 = d[addr-1]
                addr -= 2
                mask = 0x80
                color1 = palette[val1 >> 4]
                color2 = palette[val1 & 0x0F]
                for pixel in range(8):
                    val = 0
                    if val1 & mask:
                        val += 1
                    if val2 & mask:
                        val += 2
                    img.putpixel((x+pixel, y), subpalette[val])
                
                    mask = mask >> 1
        elif mode_ctrl == 1:
            for x in range(0, 352, 8):
                ch = d[addr]
                ctrl = d[addr-1]
                addr -= 2
                print(hex(ch))
                font_y = 1 + int(ch / 16) * 9
                font_x = 1 + (ch % 16) * 9
                for text_y in range(8):
                    for text_x in range(8):
                        pixel = fonts.getpixel((font_x+text_x, font_y+text_y))
                        if pixel != 0:
                            color = palette[2] if ctrl else palette[1]
                        else:
                            color = palette[0]
                        if (y+text_y) < 260:
                            img.putpixel((x+text_x, y+text_y), color)

            skip = 7
        else:
            print(mode_ctrl)

d = export_16_col()
with open(filename_bin, 'wb') as f:
    f.write(bytes(d))

print(filename_png, "=>", filename_bin)
